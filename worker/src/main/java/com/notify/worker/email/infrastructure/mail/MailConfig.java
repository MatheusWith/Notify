package com.notify.worker.email.infrastructure.mail;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;

@Configuration
public class MailConfig {

    private final JavaMailSender javaMailSender;

    public MailConfig(JavaMailSender javaMailSender) {
        this.javaMailSender = javaMailSender;
    }

    @Bean
    public SmtpEmailSender smtpEmailSender() {
        return new SmtpEmailSender(javaMailSender);
    }
}
