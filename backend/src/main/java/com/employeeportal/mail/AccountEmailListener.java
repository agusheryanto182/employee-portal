package com.employeeportal.mail;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Sends account emails after the transaction commits, on a background thread. A slow or broken
 * SMTP server therefore never fails or delays the admin's request, and no email is sent for a
 * change that was rolled back. If sending fails, the admin can still copy the link.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AccountEmailListener {

    private final MailService mailService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(AccountEmailRequested event) {
        try {
            mailService.sendAccountEmail(event);
        } catch (RuntimeException ex) {
            log.error("Could not send {} email to {}", event.kind(), event.to(), ex);
        }
    }
}
