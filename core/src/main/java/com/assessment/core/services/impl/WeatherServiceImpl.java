package com.assessment.core.services.impl;

import com.assessment.core.config.WeatherTenantConfig;
import com.assessment.core.models.WeatherData;
import com.assessment.core.services.WeatherService;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
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
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component(service = WeatherService.class, immediate = true)
@Designate(ocd = WeatherServiceImpl.OsgiConfig.class)
public class WeatherServiceImpl implements WeatherService {

    private static final Logger LOG = LoggerFactory.getLogger(WeatherServiceImpl.class);
    private final Gson gson = new Gson();

    private Cache<String, WeatherData> weatherCache;

    @ObjectClassDefinition(name = "Weather Service Global Configuration")
    public @interface OsgiConfig {
        @AttributeDefinition(name = "Global API Key")
        String api_key() default "";

        @AttributeDefinition(name = "Global Endpoint")
        String api_endpoint() default "https://goweather.xyz/weather/%s";

        @AttributeDefinition(name = "Cache TTL (seconds)")
        int cache_ttl_seconds() default 900;
    }

    private String globalApiKey;
    private String globalApiEndpoint;

    @Activate
    protected void activate(OsgiConfig config) {
        this.globalApiKey = config.api_key();
        this.globalApiEndpoint = config.api_endpoint();

        this.weatherCache = CacheBuilder.newBuilder()
                .expireAfterWrite(config.cache_ttl_seconds(), TimeUnit.SECONDS)
                .maximumSize(100)
                .build();

        LOG.info("WeatherService Activado con TTL de caché: {}s", config.cache_ttl_seconds());
    }

    @Override
    public WeatherData getForecast(String city, Resource resource) throws Exception {
        if (city == null || city.isBlank()) {
            return null;
        }

        String cacheKey = city.toLowerCase().trim();

        WeatherData cachedData = weatherCache.getIfPresent(cacheKey);
        if (cachedData != null) {
            LOG.debug("Cache HIT para ciudad: {}", cacheKey);
            return cachedData;
        }
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

        WeatherData freshData = fetchFromApi(city, apiEndpoint, apiKey);
        if (freshData != null) {
            weatherCache.put(cacheKey, freshData);
        }

        return freshData;
    }


    private WeatherData fetchFromApi(String city, String endpoint, String apiKey) {
        try {
            String encodedCity = URLEncoder.encode(city, StandardCharsets.UTF_8);
            String urlString = String.format(endpoint, encodedCity);

            if (apiKey != null && !apiKey.isBlank()) {
                urlString += (urlString.contains("?") ? "&" : "?") + "apikey=" + apiKey;
            }

            URL url = URI.create(urlString).toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                    String jsonResponse = reader.lines().collect(Collectors.joining("\n"));
                    return gson.fromJson(jsonResponse, WeatherData.class);
                }
            } else {
                LOG.warn("API Error {}", connection.getResponseCode());
            }
        } catch (Exception e) {
            LOG.error("Error getting weather of : {}", city, e);
        }
        return null;
    }
}
