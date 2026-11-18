package com.employeeportal.mail;

import java.time.Duration;

/**
 * Published inside the transaction that creates a one-time link. The email is sent only after that
 * transaction commits ({@link AccountEmailListener}).
 */
public record AccountEmailRequested(Kind kind, String to, String fullName, String link, Duration validFor) {

    public enum Kind {
        INVITE("mail/invite", "You're invited to Employee Portal"),
        PASSWORD_RESET("mail/reset-password", "Reset your Employee Portal password");

        private final String template;
        private final String subject;

        Kind(String template, String subject) {
            this.template = template;
            this.subject = subject;
        }

        public String template() {
            return template;
        }

        public String subject() {
            return subject;
        }
    }
}
