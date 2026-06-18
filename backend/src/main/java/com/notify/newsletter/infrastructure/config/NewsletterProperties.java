package com.notify.newsletter.infrastructure.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ConfigurationProperties(prefix = "app.newsletter")
public class NewsletterProperties {

    private int confirmationTokenExpirationHours = 24;
}
