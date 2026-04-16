package com.carddemo.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.SkipListener;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Step-level skip listener – increments the rejected-transaction counter
 * and logs details whenever a {@link TransactionPostingException} is skipped.
 *
 * Mirrors COBOL paragraph {@code 2500-WRITE-REJECT-REC}.
 */
@Slf4j
@RequiredArgsConstructor
public class Cbtrn02cSkipListener implements SkipListener<Object, Object> {

    private final AtomicLong skippedCount;

    @Override
    public void onSkipInProcess(Object item, Throwable t) {
        skippedCount.incrementAndGet();
        if (item instanceof com.carddemo.model.TransactionData tx) {
            log.warn("REJECTED transaction id={} acct={} reason={}",
                    tx.getTranId(), tx.getTranAcctId(), t.getMessage());
        }
    }

    @Override
    public void onSkipInRead(Throwable t) {
        log.warn("Skip on read: {}", t.getMessage());
    }

    @Override
    public void onSkipInWrite(Object item, Throwable t) {
        skippedCount.incrementAndGet();
        log.warn("Skip on write for item {}: {}", item, t.getMessage());
    }
}
