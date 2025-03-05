package concurrence.server.domain;

public class ServerExceptions {
    public static class SeverStartException extends RuntimeException {
        public SeverStartException(String message, Throwable cause) {
            super(message, cause);
        }
    }
    
    public static class ClientCOnnectionException extends RuntimeException {
        public ClientCOnnectionException(String message, Throwable cause) {
            super(message, cause);
        }
    }


    public static class ClientHandlingException extends RuntimeException {
        public ClientHandlingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
