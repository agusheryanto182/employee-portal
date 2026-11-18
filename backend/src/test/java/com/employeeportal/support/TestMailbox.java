package com.employeeportal.support;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetupTest;

import jakarta.mail.Address;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

/**
 * An in-memory SMTP server (GreenMail) for the whole test context, plus helpers to wait for the
 * asynchronous emails and read the links in them. Registered by {@link IntegrationTest}.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestMailbox {

    private static final Pattern LINK = Pattern.compile("href=\"(http[^\"]*\\?token=([A-Za-z0-9_-]+))\"");
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    @Bean(initMethod = "start", destroyMethod = "stop")
    GreenMail greenMail() {
        return new GreenMail(ServerSetupTest.SMTP.dynamicPort());
    }

    @Bean
    DynamicPropertyRegistrar mailProperties(GreenMail greenMail) {
        return registry -> {
            registry.add("spring.mail.host", () -> "127.0.0.1");
            registry.add("spring.mail.port", () -> greenMail.getSmtp().getPort());
        };
    }

    @Bean
    Inbox inbox(GreenMail greenMail) {
        return new Inbox(greenMail);
    }

    /** Received emails, filtered by recipient so tests sharing the server do not interfere. */
    public static class Inbox {

        private final GreenMail greenMail;

        Inbox(GreenMail greenMail) {
            this.greenMail = greenMail;
        }

        public List<MimeMessage> messagesTo(String email) {
            return Arrays.stream(greenMail.getReceivedMessages()).filter(message -> isFor(message, email)).toList();
        }

        /** Waits until {@code email} has received {@code count} messages and returns the latest. */
        public MimeMessage awaitMessage(String email, int count) {
            Instant deadline = Instant.now().plus(TIMEOUT);
            while (Instant.now().isBefore(deadline)) {
                List<MimeMessage> messages = messagesTo(email);
                if (messages.size() >= count) {
                    return messages.get(messages.size() - 1);
                }
                try {
                    Thread.sleep(50);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(ex);
                }
            }
            throw new AssertionError("Expected " + count + " email(s) to " + email + " within " + TIMEOUT);
        }

        public static String html(MimeMessage message) {
            try {
                return (String) message.getContent();
            } catch (Exception ex) {
                throw new IllegalStateException(ex);
            }
        }

        public static String link(MimeMessage message) {
            Matcher matcher = LINK.matcher(html(message));
            if (!matcher.find()) {
                throw new AssertionError("No link in email");
            }
            return matcher.group(1);
        }

        public static String token(MimeMessage message) {
            Matcher matcher = LINK.matcher(html(message));
            if (!matcher.find()) {
                throw new AssertionError("No link in email");
            }
            return matcher.group(2);
        }

        private static boolean isFor(MimeMessage message, String email) {
            try {
                Address[] recipients = message.getRecipients(Message.RecipientType.TO);
                return recipients != null && Arrays.stream(recipients).anyMatch(address -> address.toString().equalsIgnoreCase(email));
            } catch (MessagingException ex) {
                throw new IllegalStateException(ex);
            }
        }
    }
}
