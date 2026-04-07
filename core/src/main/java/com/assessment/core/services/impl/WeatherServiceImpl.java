package com.assessment.core.services.impl;

import com.assessment.core.config.WeatherTenantConfig; // Tu interfaz de CAConfig
import com.assessment.core.models.WeatherData;
import com.assessment.core.services.WeatherService;
import com.google.gson.Gson;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.caconfig.ConfigurationBuilder;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

@Component(service = WeatherService.class, immediate = true)
@Designate(ocd = WeatherServiceImpl.OsgiConfig.class) // Apunta a la configuración interna
public class WeatherServiceImpl implements WeatherService {

    private static final Logger LOG = LoggerFactory.getLogger(WeatherServiceImpl.class);
    private final Gson gson = new Gson();

    @ObjectClassDefinition(name = "Weather Service Global Configuration")
    public @interface OsgiConfig {
        @AttributeDefinition(name = "Global API Key")
        String api_key() default "";

        @AttributeDefinition(name = "Global Endpoint")
        String api_endpoint() default "https://goweather.xyz/weather/%s";
    }

    private String globalApiKey;
    private String globalApiEndpoint;

    @Activate
    protected void activate(OsgiConfig config) {
        this.globalApiKey = config.api_key();
        this.globalApiEndpoint = config.api_endpoint();
    }

    @Override
    public WeatherData getForecast(String city, Resource resource) throws Exception {
        String apiKey = globalApiKey;
        String apiEndpoint = globalApiEndpoint;

        if (resource != null) {
            WeatherTenantConfig tenantConfig = resource.adaptTo(ConfigurationBuilder.class)
                    .as(WeatherTenantConfig.class);

            if (StringUtils.isNotBlank(tenantConfig.api_key())) {
                apiKey = tenantConfig.api_key();
                apiEndpoint = tenantConfig.api_endpoint();
            }
        }

        String urlString = String.format(apiEndpoint, URLEncoder.encode(city, StandardCharsets.UTF_8));
        urlString += (urlString.contains("?") ? "&" : "?") + "apikey=" + apiKey;

        URL url = URI.create(urlString).toURL();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        if (connection.getResponseCode() == 200) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                return gson.fromJson(reader.lines().collect(Collectors.joining("\n")), WeatherData.class);
            }
        }
        return null;
    }
}
