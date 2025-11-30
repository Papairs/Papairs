package com.papairs.auth.config;

import com.papairs.auth.security.Sha256TokenHasher;
import com.papairs.auth.security.TokenHasher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public TokenHasher tokenHasher() {
        return new Sha256TokenHasher();
    }
}
