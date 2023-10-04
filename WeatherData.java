

import java.io.PrintStream;

public class WeatherData implements Comparable<WeatherData> {
    String id;
    int sentClockTime;
    Long createdAtMillis;
    String name;
    String state;
    String timeZone;
    Double lat;
    Double lon; 
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

    
    /*
     * CONSTRUCTORS
     */

    public WeatherData() {};

    public WeatherData(int clockTime, Long createdAtMillis) {
        this.sentClockTime = clockTime;
        this.createdAtMillis = createdAtMillis;
    }

    public WeatherData(
        int clockTime,
        Long createdAtMillis,
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
        this.sentClockTime = clockTime;
        this.createdAtMillis = createdAtMillis;
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


    /*
     * UTILITY FUNCTIONS
     */

    //Prints the object's data in a readable format
    public void printData(PrintStream outputStream) {
        outputStream.println("Content server id: " + this.id);
        outputStream.println("Name: " + this.name);
        outputStream.println("State: " + this.state);
        outputStream.println("Time zone: " + this.timeZone);
        outputStream.println("Latitude: " + this.lat);
        outputStream.println("Longitude: " + this.lon);
        outputStream.println("Local date & time: " + this.localDateTimeStamp);
        outputStream.println("Local date & time (long): " + this.localDateTimeMilliseconds);
        outputStream.println("Air temperature: " + this.airTemp);
        outputStream.println("Apparent temperature: " + this.apparentTemp);
        outputStream.println("Cloud: " + this.cloud);
        outputStream.println("Dew PT: " + this.dewPT);
        outputStream.println("Pressure: " + this.pressure);
        outputStream.println("Relative humidity: " + this.relativeHumidity);
        outputStream.println("Wind direction: " + this.windDirection);
        outputStream.println("Wind speed (KMH): " + this.windSpeedKMH);
        outputStream.println("Wind speed (KT): " + this.windSpeedKT);
    }

    //Given a key and value, find the WeatherData attribute matching the key and attempt to update that attribute with the value provided
    public void updateDataFromKeyValuePair(String key, String value) {
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
                //Do not set a value for invalid key
            }
        } catch (Exception ex) {
            System.out.println("Error in key/value parsing: " + ex.getClass().getName() + ex.getLocalizedMessage());
        }
    }

    //Compares two WeatherData objects, by comparing their Lamport Clock values.
    @Override
    public int compareTo(WeatherData other) {
        return this.sentClockTime - other.sentClockTime;
    }
    

    /*
     * GETTERS AND SETTERS
     */

    public int getSentClockTime() {
        return sentClockTime;
    }

    public void setClockTime(int clockTime) {
        this.sentClockTime = clockTime;
    }

    public Long getCreatedAtMillis() {
        return createdAtMillis;
    }

    public void setCreatedAtMillis(Long createdAtMillis) {
        this.createdAtMillis = createdAtMillis;
    }

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
