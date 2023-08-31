package com.ds.assignment2;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class WeatherData {
    String id;
    String name;
    String state;
    String timeZone;
    Double lat;
    Double lon;
    //TODO: figure out LocalDateTime parsing
    String localDateTimeStamp;
    Long localDateTimeMilliseconds;
    Double airTemp;
    Double apparentTemp;
    String cloud;
    Double dewPT;
    Double pressure;
    int relativeHumidity;
    String windDirection;
    int windSpeedKMH;
    int windSpeedKT;

    public WeatherData() {}

    public WeatherData(
        String id,
        String name,
        String state,
        String timeZone,
        Double lat,
        Double lon,
        String localDateTimeStamp,
        Long localDateTimeMilliseconds,
        Double airTemp,
        Double apparentTemp,
        String cloud,
        Double dewPT,
        Double pressure,
        int relativeHumidity,
        String windDirection,
        int windSpeedKMH,
        int windSpeedKT
    ) {
        this.id = id;
        this.name = name;
        this.state =  state;
        this.timeZone = timeZone;
        this.lat = lat;
        this.lon = lon;
        this.localDateTimeStamp = localDateTimeStamp;
        this.localDateTimeMilliseconds = localDateTimeMilliseconds;
        this.airTemp = airTemp;
        this.apparentTemp = apparentTemp;
        this.cloud = cloud;
        this.dewPT = dewPT;
        this.pressure = pressure;
        this.relativeHumidity = relativeHumidity;
        this.windDirection = windDirection;
        this.windSpeedKMH = windSpeedKMH;
        this.windSpeedKT = windSpeedKT;
    }

    public WeatherData(String filePath) {
        try {
			BufferedReader reader = new BufferedReader(new FileReader(filePath));
			String line = reader.readLine();

			while (line != null) {
				System.out.println(line);
				// read next line
                String[] keyAndValue = line.split(":", 2);

                String key = keyAndValue[0];
                String value = keyAndValue[1];

                parseInputKeyValuePair(key, value);
                
                line = reader.readLine();
			}

			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
    }

    public String asJSONString() {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.writeValueAsString(this);
        } catch(JsonProcessingException ex) {
            System.out.println("JSON processing error: " + ex.toString());
            return "";
        }
    }

    public void printData() {
        System.out.println("Content server id: " + this.id);
        System.out.println("Name: " + this.name);
        System.out.println("State: " + this.state);
        System.out.println("Time zone: " + this.timeZone);
        System.out.println("Latitude: " + this.lat);
        System.out.println("Longitude: " + this.lon);
        System.out.println("Local date & time: " + this.localDateTimeStamp);
        System.out.println("Local data & time (long): " + this.localDateTimeMilliseconds);
        System.out.println("Air temperature: " + this.airTemp);
        System.out.println("Apparent temperature: " + this.apparentTemp);
        System.out.println("Cloud: " + this.cloud);
        System.out.println("Dew PT: " + this.dewPT);
        System.out.println("Pressure: " + this.pressure);
        System.out.println("Relative humidity: " + this.relativeHumidity);
        System.out.println("Wind direction: " + this.windDirection);
        System.out.println("Wind speed (KMH): " + this.windSpeedKMH);
        System.out.println("Wind speed (KT): " + this.windSpeedKT);
    }

    public void parseInputKeyValuePair(String key, String value) {
        //TODO: validate input
        try {
            switch (key) {
            case "id":
                this.setId(value);
                break;

            case "name":
                this.setName(value);
                break;
               
            case "state":
                this.setState(value);
                break;
               
            case "time_zone":
                this.setTimeZone(value);
                break;
               
            case "lat":
                Double lat = Double.parseDouble(value);
                this.setLat(lat);
                break;
               
            case "lon":
                Double lon = Double.parseDouble(value);
                this.setLat(lon);
                break;
               
            case "local_date_time":
                this.setLocalDateTimeStamp(value);
                break;
               
            case "local_date_time_full":
                Long localDateTimeMillis = Long.parseLong(value);
                this.setLocalDateTimeMilliseconds(localDateTimeMillis);
                break;
        
            case "air_temp":
                Double airTemp = Double.parseDouble(value);
                this.setAirTemp(airTemp);
                break;
               
            case "apparent_t":
                Double apparentTemp = Double.parseDouble(value);
                this.setApparentTemp(apparentTemp);
                break;
               
            case "cloud":
                this.setCloud(value);
                break;

            case "dewpt":
                Double dewPT = Double.parseDouble(value);
                this.setDewPT(dewPT);
                break;

            case "press":
                Double pressure = Double.parseDouble(value);
                this.setPressure(pressure);
                break;

            case "rel_hum":
                int relativeHumidity = Integer.parseInt(value);
                this.setRelativeHumidity(relativeHumidity);
                break;

            case "wind_dir":
                this.setWindDirection(value);
                break;

            case "wind_spd_kmh":
                int windSpeedKMH = Integer.parseInt(value);
                this.setWindSpeedKMH(windSpeedKMH);
                break;

            case "wind_spd_kt":
                int windSpeedKT = Integer.parseInt(value);
                this.setWindSpeedKT(windSpeedKT);
                break;

            default:
                // Uh oh?
            }
        } catch (Exception ex) {
            System.out.println("Error in key/value parsing: " + ex.getClass().getName() + ex.getLocalizedMessage());
        }
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }

    public Double getLat() {
        return lat;
    }

    public void setLat(Double lat) {
        this.lat = lat;
    }

    public Double getLon() {
        return lon;
    }

    public void setLon(Double lon) {
        this.lon = lon;
    }

    public String getLocalDateTimeStamp() {
        return localDateTimeStamp;
    }

    public void setLocalDateTimeStamp(String localDateTimeStamp) {
        this.localDateTimeStamp = localDateTimeStamp;
    }

    public Long getLocalDateTimeMilliseconds() {
        return localDateTimeMilliseconds;
    }

    public void setLocalDateTimeMilliseconds(Long localDateTimeMilliseconds) {
        this.localDateTimeMilliseconds = localDateTimeMilliseconds;
    }

    public Double getAirTemp() {
        return airTemp;
    }

    public void setAirTemp(Double airTemp) {
        this.airTemp = airTemp;
    }

    public Double getApparentTemp() {
        return apparentTemp;
    }

    public void setApparentTemp(Double apparentTemp) {
        this.apparentTemp = apparentTemp;
    }

    public String getCloud() {
        return cloud;
    }

    public void setCloud(String cloud) {
        this.cloud = cloud;
    }

    public Double getDewPT() {
        return dewPT;
    }

    public void setDewPT(Double dewPT) {
        this.dewPT = dewPT;
    }

    public Double getPressure() {
        return pressure;
    }

    public void setPressure(Double pressure) {
        this.pressure = pressure;
    }

    public int getRelativeHumidity() {
        return relativeHumidity;
    }

    public void setRelativeHumidity(int relativeHumidity) {
        this.relativeHumidity = relativeHumidity;
    }

    public String getWindDirection() {
        return windDirection;
    }

    public void setWindDirection(String windDirection) {
        this.windDirection = windDirection;
    }

    public int getWindSpeedKMH() {
        return windSpeedKMH;
    }

    public void setWindSpeedKMH(int windSpeedKMH) {
        this.windSpeedKMH = windSpeedKMH;
    }

    public int getWindSpeedKT() {
        return windSpeedKT;
    }

    public void setWindSpeedKT(int windSpeedKT) {
        this.windSpeedKT = windSpeedKT;
    }
}
