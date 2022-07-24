package async.commission.demo;

import async.commission.entity.WorkResult;
import async.commission.executor.timer.SystemClock;
import async.commission.template.AbstractNode;

import java.util.Arrays;
import java.util.Map;

/**
 * 秦同学
 */
public class Node8 extends AbstractNode<String, String> {

    @Override
    public String action(String object, Map<String, AbstractNode<String, String>> allWrappers) throws InterruptedException {
//        Thread.sleep(1000L);
        return this.getTaskName() + "执行完成";
    }

    @Override
    public String nodeName() {
        return "node8";
    }

    @Override
    public void setFather(AbstractNode<String, String>... nodes) {
        this.fatherHandler.addAll(Arrays.asList(nodes));
    }

    @Override
    public void setSon(AbstractNode<String, String>... nodes) {
        this.sonHandler.addAll(Arrays.asList(nodes));
    }

    /**
     * 任务开始的监听
     */
    @Override
    public void begin() {
        super.begin();
        System.out.println(this.nodeName() + "开始执行");
    }
    @Override
    public void result(boolean success, String param, WorkResult<String> workResult) {
        if (success) {
            System.out.println("callback success--" + SystemClock.now() + "----" + workResult.getResult()
                    + "-threadName:" + Thread.currentThread().getName());
        } else {
            System.err.println("callback failure--" + SystemClock.now() + "----" + workResult.getResult()
                    + "-threadName:" + Thread.currentThread().getName());
        }
    }

    @Override
    public String defaultValue() {
        return "worker0--default";
    }

}
