package com.employeeportal.mail;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.Test;

class MailServiceTest {

    @Test
    void describesLinkLifetimesInWords() {
        assertThat(MailService.describe(Duration.ofHours(24))).isEqualTo("24 hours");
        assertThat(MailService.describe(Duration.ofHours(1))).isEqualTo("1 hour");
        assertThat(MailService.describe(Duration.ofMinutes(30))).isEqualTo("30 minutes");
        assertThat(MailService.describe(Duration.ofMinutes(90))).isEqualTo("90 minutes");
    }
}
