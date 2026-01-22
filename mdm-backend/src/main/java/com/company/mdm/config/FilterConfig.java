package com.company.mdm.config;

import com.company.mdm.filter.ApiTokenFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Filter Configuration
 * Registers the ApiTokenFilter to secure all API endpoints
 */
@Configuration
public class FilterConfig {

    /**
     * Create ApiTokenFilter bean using constructor injection
     */
    @Bean
    public ApiTokenFilter apiTokenFilter(
            @Value("${mdm.api.key}") String apiKey,
            @Value("${mdm.api.key.header:X-API-KEY}") String apiKeyHeader
    ) {
        return new ApiTokenFilter(apiKey, apiKeyHeader);
    }

    @Bean
    public FilterRegistrationBean<ApiTokenFilter> apiTokenFilterRegistration(
            ApiTokenFilter filter
    ) {
        FilterRegistrationBean<ApiTokenFilter> registrationBean =
                new FilterRegistrationBean<>();

        registrationBean.setFilter(filter);
        registrationBean.addUrlPatterns("/*"); // protect everything
        registrationBean.setOrder(1);
        return registrationBean;
    }
}
