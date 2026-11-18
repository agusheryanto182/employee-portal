package com.employeeportal.mail;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.Context;

import com.employeeportal.config.AppProperties;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;

/** Renders an HTML email from a Thymeleaf template in {@code templates/mail/} and sends it over SMTP. */
@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;
    private final ITemplateEngine templateEngine;
    private final AppProperties properties;

    public void sendAccountEmail(AccountEmailRequested email) {
        Context context = new Context();
        context.setVariable("fullName", email.fullName());
        context.setVariable("link", email.link());
        context.setVariable("validFor", describe(email.validFor()));
        send(email.to(), email.kind().subject(), templateEngine.process(email.kind().template(), context));
    }

    private void send(String to, String subject, String html) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
            helper.setFrom(properties.mail().from());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
        } catch (MessagingException ex) {
            throw new MailSendException("Could not build email", ex);
        }
    }

    /** "24 hours", "1 hour", "30 minutes". */
    static String describe(Duration duration) {
        long hours = duration.toHours();
        if (hours > 0 && duration.toMinutesPart() == 0) {
            return hours + (hours == 1 ? " hour" : " hours");
        }
        long minutes = duration.toMinutes();
        return minutes + (minutes == 1 ? " minute" : " minutes");
    }
}
