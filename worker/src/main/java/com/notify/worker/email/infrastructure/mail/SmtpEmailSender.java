package com.notify.worker.email.infrastructure.mail;

import com.notify.worker.email.application.port.out.SendEmailPort;
import com.notify.worker.email.domain.model.EmailDeliveryResult;
import com.notify.worker.email.domain.model.EmailMessage;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

@RequiredArgsConstructor
@Slf4j
public class SmtpEmailSender implements SendEmailPort {

    private final JavaMailSender javaMailSender;

    @Override
    public EmailDeliveryResult send(EmailMessage message) {
        if (log.isDebugEnabled()) {
            log.debug("Sending email to {} via SMTP", message.recipientEmail());
        }
        try {
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8");
            helper.setTo(message.recipientEmail());
            helper.setSubject(message.subject());
            helper.setText(message.body(), true);

            javaMailSender.send(mimeMessage);

            if (log.isInfoEnabled()) {
                log.info("Email sent successfully to {}", message.recipientEmail());
            }
            return EmailDeliveryResult.success(message.messageId(), message.recipientEmail());
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error("Failed to send email to {}: {}", message.recipientEmail(), e.getMessage());
            }
            return EmailDeliveryResult.failure(message.messageId(), message.recipientEmail(), e.getMessage());
        }
    }
}
