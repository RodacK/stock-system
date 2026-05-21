package com.rob.inventory.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "app.security")
@Data
public class ApiKeyProperties {

    private Map<String, String> apiKeys = new HashMap<>();
}