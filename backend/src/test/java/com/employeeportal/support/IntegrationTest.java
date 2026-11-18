package com.employeeportal.support;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.employeeportal.TestcontainersConfiguration;

/**
 * Full application context against a real PostgreSQL (Testcontainers) and an in-memory SMTP server
 * (GreenMail), with MockMvc. All integration tests share one cached context, database and mailbox.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class, TestMailbox.class, TestAccounts.class})
public @interface IntegrationTest {
}
