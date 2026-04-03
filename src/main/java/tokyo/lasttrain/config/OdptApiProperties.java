package tokyo.lasttrain.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "odpt.api")
public record OdptApiProperties(
        String baseUrl,
        String consumerKey
) {}