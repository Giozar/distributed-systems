package com.giozar04.transactions.domain.classes;

public class TransactionExceptions {
    public static class TransactionNotFoundException extends RuntimeException {
        public TransactionNotFoundException(String message, Throwable cause) {
            super(message);
        }
    }

    public static class TransactionCreationException extends RuntimeException {
        public TransactionCreationException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class TransactionUpdateException extends RuntimeException {
        public TransactionUpdateException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class TransactionDeletionException extends RuntimeException {
        public TransactionDeletionException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class TransactionListException extends RuntimeException {
        public TransactionListException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class TransactionRetrievalException extends  RuntimeException {
        public TransactionRetrievalException(String message, Throwable cause) {
            super(message, cause);
        }
    }

}
