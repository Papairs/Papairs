package com.papairs.docs.config;

import com.backblaze.b2.client.B2StorageClient;
import com.backblaze.b2.client.B2StorageClientFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "storage.mode", havingValue = "b2")
public class B2Config {

    @Value("${b2.key-id}")
    private String keyId;

    @Value("${b2.application-key}")
    private String applicationKey;

    @Bean
    public B2StorageClient b2StorageClient() {
        return B2StorageClientFactory
                .createDefaultFactory()
                .create(keyId, applicationKey, "Papairs-Docs-Service");
    }
}
