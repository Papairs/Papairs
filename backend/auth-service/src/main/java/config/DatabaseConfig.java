package config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EntityScan(basePackages = "com.papairs.auth.model")
@EnableJpaRepositories(basePackages = "com.papairs.auth.repository")
public class DatabaseConfig {
    // Spring Boot auto-configuration handles the rest
    // Using application-auth.properties for database settings
}
