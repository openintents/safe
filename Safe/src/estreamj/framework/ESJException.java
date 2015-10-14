package estreamj.framework;

public class ESJException extends Exception {
    private static final long serialVersionUID = 6375271013846829471L;

    public ESJException() {
        super();
    }

    public ESJException(String message) {
        super(message);
    }

    public ESJException(Throwable cause) {
        super(cause);
    }

    public ESJException(String message, Throwable cause) {
        super(message, cause);
    }
}
