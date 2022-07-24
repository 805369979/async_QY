package async.commission.entity;

import async.commission.enums.ResultState;
import lombok.ToString;

/**
 * 执行结果
 */
@ToString
public class WorkResult<V> {
    /**
     * 标记是哪个节点执行的结果
     */
    String name;
    /**
     * 执行的结果
     */
    private V result;
    /**
     * 结果状态
     */
    private ResultState resultState;
    private Exception ex;

    public WorkResult(String name) {
        this(name,null,null);
    }

    public WorkResult(String name , V result, ResultState resultState) {
        this(name, result, resultState, null);
    }

    public WorkResult(String name, V result, ResultState resultState, Exception ex) {
        this.name = name;
        this.result = result;
        this.resultState = resultState;
        this.ex = ex;
    }


    public static <V> WorkResult<V> defaultResult(String name) {
        return new WorkResult<>(name,null, ResultState.DEFAULT);
    }

    public Exception getEx() {
        return ex;
    }

    public void setEx(Exception ex) {
        this.ex = ex;
    }

    public V getResult() {
        return result;
    }

    public void setResult(V result) {
        this.result = result;
    }

    public ResultState getResultState() {
        return resultState;
    }

    public void setResultState(ResultState resultState) {
        this.resultState = resultState;
    }
}
