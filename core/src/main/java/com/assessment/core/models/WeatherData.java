package com.assessment.core.models;

public final class WeatherData {

    private final String city;
    private final String temperature;
    private final String description;
    private final String wind;

    private WeatherData(String city, String temperature, String description, String wind) {
        this.city = city;
        this.temperature = temperature;
        this.description = description;
        this.wind = wind;
    }

    public String getCity() {
        return city;
    }

    public String getTemperature() {
        return temperature;
    }

    public String getDescription() {
        return description;
    }

    public String getWind() {
        return wind;
    }
}
