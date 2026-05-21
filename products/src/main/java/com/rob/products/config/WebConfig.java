package com.rob.products.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.addPathPrefix("/v1",
                c -> c.getPackageName().startsWith("com.rob.products.controller.v1"));
    }

    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.stream()
                .filter(MappingJackson2HttpMessageConverter.class::isInstance)
                .map(MappingJackson2HttpMessageConverter.class::cast)
                .findFirst()
                .ifPresent(converter -> {
                    List<MediaType> types = new ArrayList<>(converter.getSupportedMediaTypes());
                    types.add(MediaType.valueOf("application/vnd.api+json"));
                    converter.setSupportedMediaTypes(types);
                });
    }
}