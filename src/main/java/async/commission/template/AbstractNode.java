package async.commission.template;

import async.commission.callback.ICallback;
import async.commission.callback.IWorker;
import async.commission.entity.Context;
import async.commission.entity.WorkResult;
import async.commission.enums.ResultState;
import async.commission.executor.timer.SystemClock;
import lombok.Data;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

@Data
public abstract class AbstractNode<T, V> implements IWorker<T, V>, ICallback<T, V> {
    // 节点的名字
    String taskName;
    // 节点执行结果状态
    ResultState status = ResultState.DEFAULT;
    //将来要处理的param参数
    private T param;

    // 节点状态
    private static final int FINISH = 1;
    private static final int ERROR = 2;
    private static final int WORKING = 3;
    private static final int INIT = 0;

    // 依赖的父节点
    protected List<AbstractNode<T, V>> fatherHandler = new ArrayList<>();
    // 下游的子节点
    protected List<AbstractNode<T, V>> sonHandler = new ArrayList<>();
    /**
     * 标记该事件是否已经被处理过了，譬如已经超时返回false了，后续rpc又收到返回值了，则不再二次回调
     * 经试验,volatile并不能保证"同一毫秒"内,多线程对该值的修改和拉取
     * <p>
     * 1-finish, 2-error, 3-working
     */
    private AtomicInteger state = new AtomicInteger(0);
    // 默认节点执行结果
    private volatile WorkResult<V> workResult = WorkResult.defaultResult(nodeName());
    // 该map存放所有的节点名字和节点的映射
    private Map<String, AbstractNode<T, V>> forParamUserMap;

    public void register(Map<String, AbstractNode<T, V>> forParamUserMap) {
        // 设置节点名称
        String nodeName = nodeName();
        this.setTaskName(nodeName);
        this.forParamUserMap = forParamUserMap;
        forParamUserMap.put(this.taskName, this);
    }

    /**
     * 处理节点的门面入口
     * @param executorService  指定的线程池
     * @param remainTime   剩余执行时间
     * @param context   节点执行上下文
     * @param forParamUseNodes  该异步执行任务的map映射
     */
    public void template(ExecutorService executorService, long remainTime, Context context, Map<String, AbstractNode<T, V>> forParamUseNodes) {
        // 将注册自己进map中
        register(forParamUseNodes);
        long now = SystemClock.now();
        if (remainTime <= 0) {
            fastFail(INIT, null);
            runSon(context, executorService, now, remainTime);
            return;
        }
        //如果当前任务已经完成了，依赖的其他任务拿到锁再进来时，不需要执行下面的逻辑了。
        if (getState().get() != INIT && !CollectionUtils.isEmpty(this.getFatherHandler())) {
            return;
        }
        if (!checkFather()) {
            return;
        }
        // 执行自身任务
        workResult = workerDoJob();
        // 执行子类
        runSon(context, executorService, now, remainTime);
    }

    /**
     * 具体的单个worker执行任务
     */
    private WorkResult<V> workerDoJob() {
        //避免重复执行
        if (!checkIsNullResult()) {
            return workResult;
        }
        try {
            //如果已经不是init状态了，说明正在被执行或已执行完毕。这一步很重要，可以保证任务不被重复执行
            if (!compareAndSetState(INIT, WORKING)) {
                return workResult;
            }

            // 执行回调函数
            begin();
            //执行耗时操作
            // 执行自身逻辑
            V resultValue = this.action(param, forParamUserMap);

            //如果状态不是在working,说明别的地方已经修改了
            if (!compareAndSetState(WORKING, FINISH)) {
                return workResult;
            }

            workResult.setResultState(ResultState.SUCCESS);
            workResult.setResult(resultValue);
            //回调成功
            result(true, param, workResult);
            return workResult;
        } catch (Exception e) {
            //避免重复回调
            if (!checkIsNullResult()) {
                return workResult;
            }
            fastFail(WORKING, e);
            return workResult;
        }
    }

    /**
     * 总控制台超时，停止所有任务
     */
    public void stopNow() {
        if (getState().get() == INIT || getState().get() == WORKING) {
            fastFail(getState().get(), null);
        }
    }

    private boolean compareAndSetState(int expect, int update) {
        return this.state.compareAndSet(expect, update);
    }

    private boolean checkIsNullResult() {
        return ResultState.DEFAULT == workResult.getResultState();
    }

    /**
     * 快速失败
     */
    private boolean fastFail(int expect, Exception e) {
        //试图将它从expect状态,改成Error
        if (!compareAndSetState(expect, ERROR)) {
            return false;
        }
        //尚未处理过结果
        if (checkIsNullResult()) {
            if (e == null) {
                workResult = WorkResult.defaultResult(taskName);
            } else {
                workResult = defaultExResult(e);
            }
        }
        result(false, param, workResult);
        return true;
    }

    private WorkResult<V> defaultExResult(Exception ex) {

        workResult.setResultState(ResultState.EXCEPTION);
        workResult.setResult(null);
        workResult.setEx(ex);
        return workResult;
    }
    // 给节点起名字
    public abstract String nodeName();

    /**
     * 给当前节点添加依赖的父节点
     * @param nodes
     */
    public abstract void setFather(AbstractNode<T, V>... nodes);
    /**
     * 给当前节点添加依赖的子节点
     * @param nodes
     */
    public abstract void setSon(AbstractNode<T, V>... nodes);

    //检查父类是否完成 true:可以执行自己，false：不可以

    /**
     * 检查父类依赖
     * @return
     */
    public synchronized boolean checkFather() {
        //如果没有父类，则可以开始执行自己
        if (this.fatherHandler.size() == 0) {
            return true;
        }
        // 如果有父类的话，检查父类是否执行完成
        boolean existNoFinish = false;
        boolean hasError = false;
        for (AbstractNode node : this.fatherHandler) {
            WorkResult curResult = node.getWorkResult();
            //为null或者isWorking，说明它依赖的某个任务还没执行到或没执行完
            if (node.getState().get() == INIT || node.getState().get() == WORKING) {
                existNoFinish = true;
                break;
            }
            if (ResultState.TIMEOUT == curResult.getResultState()) {
                workResult = WorkResult.defaultResult(taskName);
                hasError = true;
                break;
            }
            if (ResultState.EXCEPTION == curResult.getResultState()) {
                workResult = defaultExResult(curResult.getEx());
                hasError = true;
                break;
            }
        }
        //只要有失败的或者未完成的
        if (hasError || existNoFinish) {
            return false;
        }
        return true;
    }

    /**
     * 执行当前节点的子节点
     * @param context   上下文
     * @param executorService  线程池
     * @param now  当前时间
     * @param remainTime  距离指定的超时时间还剩余的时间
     */
    public void runSon(Context context, ExecutorService executorService, long now, long remainTime) {
        // 没有子节点的时候
        if (CollectionUtils.isEmpty(this.sonHandler)) {
            return;
        }
        //花费的时间
        long costTime = SystemClock.now() - now;

        // 只有一个子节点的时候
        if (sonHandler.size() == 1) {
            executorService.execute(() ->
            {
                sonHandler.get(0).template(executorService, remainTime - costTime, context, forParamUserMap);
            });
            return;
        }
        CountDownLatch count = new CountDownLatch(sonHandler.size());

        for (int i = 0; i < sonHandler.size(); i++) {
            AbstractNode node = sonHandler.get(i);
            executorService.execute(() ->
            {
                node.template(executorService, remainTime - costTime, context, forParamUserMap);
                count.countDown();
            });
        }
        try {
            count.await();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}