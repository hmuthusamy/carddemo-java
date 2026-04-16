package com.carddemo.batch;

/**
 * Thrown by the batch processor when a single transaction cannot be posted.
 *
 * <p>Wraps the underlying cause (e.g. account-not-found) and is registered in
 * {@link Cbtrn02cJobConfig} as a skippable exception so the job continues
 * processing remaining transactions – analogous to the COBOL program writing
 * a reject record to DALYREJS-FILE instead of abending.</p>
 */
public class TransactionPostingException extends RuntimeException {

    public TransactionPostingException(String message) {
        super(message);
    }

    public TransactionPostingException(String message, Throwable cause) {
        super(message, cause);
    }
}
