package com.assessment.core.config;

import org.apache.sling.caconfig.annotation.Configuration;
import org.apache.sling.caconfig.annotation.Property;

@Configuration(label = "Weather Tenant Configuration", description = "Weather settings per brand/site")
public @interface WeatherTenantConfig {

    @Property(label = "API Key", description = "Tenant specific API Key")
    String api_key() default "default-key";

    @Property(label = "Endpoint", description = "API Endpoint template")
    String api_endpoint() default "https://goweather.xyz/weather/%s";
}