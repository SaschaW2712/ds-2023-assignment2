
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;

public class AggregationServer {

    public static LamportClock clock = new LamportClock();
    public static String dataFilePath = "weather-data/";
    public static boolean initalised = false;
    public static ServerSocket serverSocket;
    public static boolean isRunning = true;

    public static void main(String[] args) {
        int port = 4567;

        if (args.length >= 1) {
            port = Integer.parseInt(args[0]);
        }

        recoverIfNeeded();
        refreshDataFile();
 
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("Server is listening on port " + port);
 
            while (isRunning) {
                Socket socket = serverSocket.accept();
 
                System.out.println("New client connected");
 
                // Create input and output streams for communication
                InputStream inputStream = socket.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

                OutputStream outputStream = socket.getOutputStream();
                PrintWriter writer = new PrintWriter(outputStream, true);

                handleRequest(reader, writer);
                
                socket.close();
            }

        } catch (UnknownHostException ex) {
            System.out.println("Server not found: " + ex.getMessage());
         } catch (JsonParseException ex) {
            System.out.println("JSON parsing error: " + ex.getMessage());
        } catch (JsonMappingException ex) {
            System.out.println("JSON mapping error: " + ex.getMessage());
        } catch (IOException ex) {
            System.out.println("I/O error: " + ex.getMessage());
        }
    }

    private static void handleRequest(
        BufferedReader reader,
        PrintWriter writer
    ) throws IOException {
        // Read the HTTP request from the client
        String request = reader.readLine();

        // System.out.println("Just read client request");
        if (request != null) {
            if (request.startsWith("GET /weatherdata")) {
                handleGETDataRequest(reader, writer, "weatherdata");
            } else if (request.startsWith("GET /clock")) {
                handleGETDataRequest(reader, writer, "clock");
            } else if (request.startsWith("PUT")) {
                handlePUTRequest(reader, writer);
            } else {
                handleBadRequest(reader, writer);
            }
        } else {
            handleBadRequest(reader, writer);
        }
    }

    private static void handleGETDataRequest(
        BufferedReader reader,
        PrintWriter writer,
        String type
    ) {
        try {
            String line;

            while (!(line = reader.readLine()).isEmpty()) {
                if (line.startsWith("Clock-Time:")) {
                    int getClientClockTime = Integer.parseInt(line.split(":", 2)[1].trim());
                    clock.updateValue(getClientClockTime);
                }
            }

            String response = "HTTP/1.1 200 OK\n" + "Clock-Time: " + clock.getValue();
            if (type == "weatherdata") {
                // Process the request and send the appropriate response
                ObjectMapper mapper = new ObjectMapper();
                
                Optional<WeatherData> data = getLatestWeatherData();

                if (!data.isPresent()) {
                    System.out.println("No data found, sending 404");
                    writer.println("HTTP/1.1 404 NOT-FOUND\n + Clock-Time: " + clock.getValue());
                    return;
                }

                String weatherData = mapper.writeValueAsString(data.get());
                response = response + "\n\n" + weatherData;
            }

            System.out.println("Sending 200");

            System.out.println("Response being sent:" + response + "\n\n");
            // Write the JSON line to the writer
            writer.println(response);
            
        } catch (Exception ex) {
            System.out.println("Error in GET request handler: " + ex.getLocalizedMessage());
            ex.printStackTrace();
        }
    }
 
    private static void handlePUTRequest(
        BufferedReader reader,
        PrintWriter writer
    ) throws IOException {
        String line;
        while (!(line = reader.readLine()).isEmpty()) {
            if (line.startsWith("Clock-Time:")) {
                int contentServerClockTime = Integer.parseInt(line.split(":", 2)[1].trim());

                // System.out.println("PUT client's clock time: " + contentServerClockTime);
                // System.out.println("Server clock time: " + clock.getValue());
                
                clock.updateValue(contentServerClockTime);
                // System.out.println("Server updated clock time: " + clock.getValue());
            }
        }

        // Read the JSON body from the request
        StringBuilder jsonBody = new StringBuilder();

        while ((line = reader.readLine()) != null) {
            jsonBody.append(line);
        }

        String parsedJSONString = jsonBody.toString();
        if (parsedJSONString == "") {
            System.out.println("No content sent, sending 204");
            writer.println("HTTP/1.1 204 NO-CONTENT\n" + "Clock-Time: " + clock.getValue());
            return;
        }

        try {
            updateWithNewWeatherData(parsedJSONString);
        } catch(JsonMappingException ex) {
            System.out.println("Error in weather data JSON");
            ex.printStackTrace();
            System.out.println("Sending 500 to PUT client");
            writer.println("HTTP/1.1 500 INVALID-JSON\n" + "Clock-Time: " + clock.getValue() + "\n\n" + "Received JSON: " + parsedJSONString);
            return;
        }
        
        //Increment clock for successful data update
        clock.tick();

        // Respond
        String responseCode = initalised ? "200 OK" : "201 HTTP_CREATED";

        System.out.println("Sending " + responseCode + " to PUT client");
        writer.println("HTTP/1.1 " + responseCode + "\n" + "Clock-Time: " + clock.getValue());
        initalised = true;
    }

    public static void handleBadRequest(
        BufferedReader reader,
        PrintWriter writer
    ) {
        try {
            String line;
            while (!(line = reader.readLine()).isEmpty()) {
                if (line.startsWith("Clock-Time:")) {
                    int getClientClockTime = Integer.parseInt(line.split(":", 2)[1].trim());

                    // System.out.println("GET client clock time: " + getClientClockTime);
                    // System.out.println("Server clock time: " + clock.getValue());
                    clock.updateValue(getClientClockTime);

                    // System.out.println("Server updated clock time: " + clock.getValue());
                }
            }

            String response = "HTTP/1.1 400 Bad Request\n\nInvalid request!";
            System.out.println("Sending 400");
            writer.println(response);
        } catch (Exception ex) {
            System.out.println("Error in bad request handler: " + ex.getLocalizedMessage());
        } 
    }

    public static Optional<WeatherData> getLatestWeatherData(
    ) {
        refreshDataFile();
        ArrayList<WeatherData> data = getWeatherArrayFromFile();
        if (data.size() > 0) {
            return Optional.of(data.get(data.size() - 1));
        } else {
            return Optional.empty();
        }
    }

    public static void updateWithNewWeatherData(
        String newDataString
    ) throws IOException, JsonMappingException {
        try {
            //Write new string to file immediately
            File tempFile = new File(dataFilePath + "temp");
            if (!tempFile.exists()) {
                tempFile.createNewFile();
            }

            BufferedWriter fileWriter = new BufferedWriter(new FileWriter(dataFilePath + "temp"));
            fileWriter.write(newDataString);
            fileWriter.close();

            ObjectMapper mapper = new ObjectMapper();
            WeatherData newWeatherData = mapper.readValue(newDataString, WeatherData.class);
            
            //Get current weather data and add new one to array
            ArrayList<WeatherData> data = getWeatherArrayFromFile();

            // System.out.println("Just got data from file");
            data.add(newWeatherData);

            //Update data file with updated array
            writeWeatherArrayToFile(data);
            // System.out.println("Just wrote new data to file");

            //Filter and sort updated data file
            refreshDataFile();

            tempFile.delete();
            // System.out.println("Just deleted temp file");

        } catch (JsonProcessingException ex) {
            System.out.println("JSONMappingException in updateWeatherData: " + ex.getLocalizedMessage());
            throw ex;
        } catch (IOException ex) {
            System.out.println("IOException in updateWeatherData: " + ex.getLocalizedMessage());
            throw ex;
        } 
    }
    
    public static ArrayList<WeatherData> getWeatherArrayFromFile() {
        ObjectMapper mapper  = new ObjectMapper();
        JsonFactory jsonFactory = new JsonFactory();
        try(BufferedReader reader = new BufferedReader(new FileReader(dataFilePath + "data"))) {
            JsonParser parser = jsonFactory.createParser(reader);
            ArrayList<WeatherData> dataArrayList = new ArrayList<WeatherData>();

            try {
                List<WeatherData> dataFromFile = mapper.readValue(parser, new TypeReference<List<WeatherData>>() {});
                for (WeatherData data : dataFromFile) {
                    dataArrayList.add(data);
                }
            } catch (MismatchedInputException ex) {
                System.out.println("No data yet");
            }

            return dataArrayList;

        } catch(Exception ex) {
            System.out.println("Error in getWeatherArrayFromFile: " + ex.getLocalizedMessage());
            ex.printStackTrace();
            return new ArrayList<WeatherData>();
        }
    }

    public static void writeWeatherArrayToFile(ArrayList<WeatherData> weatherDataList) {
        ObjectMapper mapper = new ObjectMapper();
        JsonFactory jsonFactory = new JsonFactory();

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(dataFilePath + "data"))) {
            JsonGenerator generator = jsonFactory.createGenerator(writer);

            // Write the updated weatherDataList to the file
            mapper.writeValue(generator, weatherDataList);

            generator.flush();
        } catch (Exception ex) {
            System.out.println("Error in writeWeatherArrayToFile: " + ex.getLocalizedMessage());
        }
    }

    public static ArrayList<WeatherData> sortByCreationClockTime(
        ArrayList<WeatherData> weatherData
    ) {
        Collections.sort(weatherData);
        return weatherData;
    }

    public static ArrayList<WeatherData> filterInactiveContentServers(
        ArrayList<WeatherData> weatherData
    ) {
            Map<String, Long> contentServerIDsWithRecency = getContentServerIDsAndRecency(weatherData);
            
            //There are 30000 milliseconds in 30 seconds
            Long millis30SecondsAgo = System.currentTimeMillis() - 30000;
            
            ArrayList<WeatherData> recentWeatherData = new ArrayList<WeatherData>();

            for (WeatherData data : weatherData) {
                Long serverLastUpdate = contentServerIDsWithRecency.get(data.getId());

                if (serverLastUpdate == null || serverLastUpdate >= millis30SecondsAgo) {
                    recentWeatherData.add(data);
                }
            }

            return recentWeatherData;

    }

    public static Map<String, Long> getContentServerIDsAndRecency(
        ArrayList<WeatherData> weatherDataList
    ) throws UnsupportedOperationException {
        try {
            Map<String, Long> serverIDsWithLastUpdate = new HashMap<>();

            for (WeatherData data : weatherDataList) {
                Long lastUpdateForServerID = serverIDsWithLastUpdate.get(data.getId());

                if (lastUpdateForServerID == null || lastUpdateForServerID < data.getCreatedAtMillis()) {
                    serverIDsWithLastUpdate.put(data.getId(), data.getCreatedAtMillis());
                }
            }

            return serverIDsWithLastUpdate;
        } catch (UnsupportedOperationException ex) {
            System.out.println("Unsupported operation exception in getContentServerIDsAndRecency");
            ex.printStackTrace();
            throw ex;
        }
    }

    public static ArrayList<WeatherData> filterLeastRecentData(
        ArrayList<WeatherData> weatherData
    ) {
        while (weatherData.size() > 20) {
                weatherData.remove(0);
        }

        return weatherData;
    }

    public static void refreshDataFile(
    ) {
        System.out.println("Refreshing data");
        ArrayList<WeatherData> data = getWeatherArrayFromFile();

        data = sortByCreationClockTime(data);
        // System.out.println("Just sorted by creation time");

        data = filterInactiveContentServers(data);
        // System.out.println("Just filtered by content server inactivity");

        data = filterLeastRecentData(data);
        // System.out.println("Just filtered old data");

        //Update data file with updated array
        writeWeatherArrayToFile(data);
    }

    public static void recoverIfNeeded() {
        try {
            //Check if temporary processing file exists, indicating we stopped midway through processing data
            File temporaryDataFile = new File(dataFilePath + "temp");
            if (temporaryDataFile.exists()) {
                BufferedReader reader = new BufferedReader(new FileReader(temporaryDataFile));

                ArrayList<WeatherData> existingData = getWeatherArrayFromFile();

                String unprocessedJSONString = reader.readLine();
                reader.close();

                if (unprocessedJSONString != null) {
                    ObjectMapper mapper = new ObjectMapper();
                    WeatherData newWeatherData = mapper.readValue(unprocessedJSONString, WeatherData.class);

                    existingData.add(newWeatherData);
                    writeWeatherArrayToFile(existingData);

                    //Filter and sort with new data
                    refreshDataFile();
                }
            }

            temporaryDataFile.delete();
        } catch (Exception ex) {
            System.out.println("Exception in recovery: " + ex.getLocalizedMessage());
            ex.printStackTrace();
        }
    }

    public void shutdown() {
        isRunning = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
