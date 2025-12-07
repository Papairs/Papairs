package com.papairs.docs.config;

import com.papairs.docs.interceptor.UserIdValidationInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final UserIdValidationInterceptor userIdValidationInterceptor;

    public WebConfig(UserIdValidationInterceptor userIdValidationInterceptor) {
        this.userIdValidationInterceptor = userIdValidationInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(userIdValidationInterceptor)
                .addPathPatterns("/api/docs/**");
    }
}
