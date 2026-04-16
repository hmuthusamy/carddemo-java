package com.carddemo.exception;

import com.carddemo.service.MessageService.ErrorCode;

/**
 * ApplicationException – thrown wherever the COBOL programs would set a
 * non-zero RETURN-CODE and GOBACK (or PERFORM 9000-ABEND).
 *
 * <p>Wraps an {@link ErrorCode} so the {@link GlobalExceptionHandler} can
 * map it to the appropriate HTTP status and structured error response.
 */
public class ApplicationException extends RuntimeException {

    private final ErrorCode errorCode;
    private final String    context;

    public ApplicationException(ErrorCode errorCode, String context) {
        super(buildMessage(errorCode, context));
        this.errorCode = errorCode;
        this.context   = context;
    }

    public ApplicationException(ErrorCode errorCode) {
        this(errorCode, "");
    }

    public ApplicationException(ErrorCode errorCode, String context, Throwable cause) {
        super(buildMessage(errorCode, context), cause);
        this.errorCode = errorCode;
        this.context   = context;
    }

    public ErrorCode getErrorCode() { return errorCode; }
    public String    getContext()   { return context;   }

    private static String buildMessage(ErrorCode ec, String ctx) {
        return ctx == null || ctx.isBlank()
                ? ec.getDescription()
                : ec.getDescription() + " [" + ctx + "]";
    }
}
