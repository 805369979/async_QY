package async.commission.exception;

/**
 * 如果任务在执行发生异常，则抛该exception
 * @version 1.0
 */
public class SkippedException extends RuntimeException {
    public SkippedException() {
        super();
    }

    public SkippedException(String message) {
        super(message);
    }
}
