package com.assessment.core.models;

import com.assessment.core.services.WeatherService;
import com.day.cq.wcm.api.Page;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

@Model(
        adaptables = SlingHttpServletRequest.class,
        defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class WeatherModel {

    private static final Logger LOG = LoggerFactory.getLogger(WeatherModel.class);

    @Self
    private SlingHttpServletRequest request;

    @ValueMapValue
    private String city;

    @Inject
    private Page currentPage;

    @OSGiService
    private WeatherService weatherService;

    private WeatherData weatherData;

    @PostConstruct
    protected void init() {
        String requestedCity = getCity();

        try {
            this.weatherData = weatherService.getForecast(getCity(), request.getResource());
        } catch (Exception e) {
            LOG.error("Error getting weather city: {}", requestedCity, e);
        }
    }

    public String getCity() {
        return (city != null && !city.isEmpty()) ? city : "Bogota";
    }

    public WeatherData getWeatherData() {
        return weatherData;
    }

    public String getPageTitle() {
        return currentPage != null ? currentPage.getTitle() : "Weather Page";
    }

    public WeatherService getWeatherService() {
        return weatherService;
    }
}
