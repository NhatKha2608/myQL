package vn.edu.qlhs.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessagePreparator;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.io.InputStream;
import java.util.Properties;

@Configuration
public class MailConfig {

    /** Fallback when SMTP is not configured (dev/test). AuthService already catches send failures. */
    @Bean
    @ConditionalOnMissingBean(JavaMailSender.class)
    public JavaMailSender noOpJavaMailSender() {
        return new JavaMailSender() {
            @Override
            public MimeMessage createMimeMessage() {
                return new MimeMessage(Session.getInstance(new Properties()));
            }

            @Override
            public MimeMessage createMimeMessage(InputStream contentStream) {
                return createMimeMessage();
            }

            @Override
            public void send(MimeMessage mimeMessage) throws MailException {
                // no-op
            }

            @Override
            public void send(MimeMessage... mimeMessages) throws MailException {
                // no-op
            }

            @Override
            public void send(MimeMessagePreparator mimeMessagePreparator) throws MailException {
                // no-op
            }

            @Override
            public void send(MimeMessagePreparator... mimeMessagePreparators) throws MailException {
                // no-op
            }

            @Override
            public void send(SimpleMailMessage simpleMessage) throws MailException {
                // no-op
            }

            @Override
            public void send(SimpleMailMessage... simpleMessages) throws MailException {
                // no-op
            }
        };
    }
}
