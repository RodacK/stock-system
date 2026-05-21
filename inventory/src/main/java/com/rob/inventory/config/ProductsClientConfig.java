package com.rob.inventory.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class ProductsClientConfig {

    @Value("${app.products.base-url}")
    private String baseUrl;

    @Value("${app.products.api-key}")
    private String apiKey;

    @Value("${app.products.connect-timeout:2s}")
    private Duration connectTimeout;

    @Value("${app.products.read-timeout:3s}")
    private Duration readTimeout;

    @Bean
    public RestClient productsRestClient() {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(connectTimeout)
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(readTimeout);
        return RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .requestFactory(factory)
                .build();
    }
}