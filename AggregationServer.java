
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

    public static PrintStream outputStream = new PrintStream(System.out);

    public static LamportClock clock = new LamportClock();
    public static String dataFilePath = "weather-data/";
    public static boolean initalised = false;
    public static ServerSocket serverSocket;
    public static boolean isRunning = true;

    public static void main(String[] args) {
        int port = 4567;
        isRunning = true;

        //Confirm that necessary arguments have been provided
        if (args.length >= 1) {
            port = Integer.parseInt(args[0]);
        }

        //Redirect system output if requested
        if (args.length == 2) {
            try {
                PrintWriter writer = new PrintWriter(args[1]);
                writer.print("");
                writer.close();
                
                outputStream = new PrintStream(new FileOutputStream(args[1]), true);
            } catch(FileNotFoundException e) {
                System.out.println("Couldn't find output file");
                return;
            }
        }

        //Check for in-progress requests and re-filter weather data
        recoverIfNeeded();
        refreshDataFile();
 
        try {
            serverSocket = new ServerSocket(port);
            outputStream.println("Server is listening on port " + port);
 
            while (isRunning) {
                Socket socket = serverSocket.accept();
 
                outputStream.println("New client connected");
 
                // Create input and output streams for communication
                InputStream inputStream = socket.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

                PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);

                //Handle the incoming request
                handleRequest(reader, writer);
                
                socket.close();
            }

        } catch (UnknownHostException ex) {
            outputStream.println("Server not found: " + ex.getMessage());
         } catch (JsonParseException ex) {
            outputStream.println("JSON parsing error: " + ex.getMessage());
        } catch (JsonMappingException ex) {
            outputStream.println("JSON mapping error: " + ex.getMessage());
        } catch (IOException ex) {
            shutdown();
            outputStream.println("I/O error: " + ex.getMessage());
        }
    }

    //Handles a PUT or GET request
    private static void handleRequest(
        BufferedReader reader,
        PrintWriter writer
    ) throws IOException {
        // Read the HTTP request from the client
        String request = reader.readLine();

        if (request != null) {
            if (request.startsWith("GET /weatherdata")) {
                handleGETRequest(reader, writer, "weatherdata");
            } else if (request.startsWith("GET /clock")) {
                handleGETRequest(reader, writer, "clock");
            } else if (request.startsWith("PUT")) {
                handlePUTRequest(reader, writer);
            } else {
                handleBadRequest(reader, writer);
            }
        } else {
            handleBadRequest(reader, writer);
        }
    }

    //Parses and handles a GET request
    private static void handleGETRequest(
        BufferedReader reader,
        PrintWriter writer,
        String type
    ) {
        try {
            String line;

            while (!(line = reader.readLine()).isEmpty()) {
                if (line.startsWith("Clock-Time:")) {

                    //Update local clock time if necessary
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

                    //If there is no data available, send 404 and exit
                    outputStream.println("No data found, sending 404");
                    writer.println("HTTP/1.1 404 NOT-FOUND\n + Clock-Time: " + clock.getValue());
                    return;
                }

                String weatherData = mapper.writeValueAsString(data.get());
                response = response + "\n\n" + weatherData;
            }

            //If request was processed okay, write back with 200 response

            outputStream.println("Response being sent:\n" + response + "\n\n");

            writer.println(response);
            
        } catch (Exception ex) {
            outputStream.println("Error in GET request handler: " + ex.getLocalizedMessage());
            ex.printStackTrace();
        }
    }
 
    //Handles a weather data PUT request
    private static void handlePUTRequest(
        BufferedReader reader,
        PrintWriter writer
    ) throws IOException {
        String line;

        while (!(line = reader.readLine()).isEmpty()) {
            if (line.startsWith("Clock-Time:")) {
                //Update local clock time if necessary
                int contentServerClockTime = Integer.parseInt(line.split(":", 2)[1].trim());
                clock.updateValue(contentServerClockTime);
            }
        }

        // Read the JSON body from the request
        StringBuilder jsonBody = new StringBuilder();

        while ((line = reader.readLine()) != null) {
            jsonBody.append(line);
        }

        String parsedJSONString = jsonBody.toString();

        //If the JSON is empty, send 204
        if (parsedJSONString == "") {
            outputStream.println("No content sent, sending 204");
            writer.println("HTTP/1.1 204 NO-CONTENT\n" + "Clock-Time: " + clock.getValue());
            return;
        }

        try {
            updateWithNewWeatherData(parsedJSONString);
        } catch(JsonMappingException ex) {
            //If JSON cannot be parsed, send 500
            outputStream.println("Error in weather data JSON");
            ex.printStackTrace();

            outputStream.println("Sending 500 to PUT client");
            writer.println("HTTP/1.1 500 INVALID-JSON\n" + "Clock-Time: " + clock.getValue() + "\n\n" + "Received JSON: " + parsedJSONString);
            return;
        }
        
        //Increment clock for successful data update
        clock.tick();

        // If all has gone well send 200, or 201 for server's first successful PUT
        String responseCode = initalised ? "200 OK" : "201 HTTP_CREATED";

        outputStream.println("Sending " + responseCode + " to PUT client");
        writer.println("HTTP/1.1 " + responseCode + "\n" + "Clock-Time: " + clock.getValue());
        initalised = true;
    }

    //Handles a bad/invalid request
    public static void handleBadRequest(
        BufferedReader reader,
        PrintWriter writer
    ) {
        try {
            String line;

            while (!(line = reader.readLine()).isEmpty()) {
                if (line.startsWith("Clock-Time:")) {

                    //Update local clock time if necessary
                    int getClientClockTime = Integer.parseInt(line.split(":", 2)[1].trim());
                    clock.updateValue(getClientClockTime);
                }
            }

            String response = "HTTP/1.1 400 Bad Request\n\nInvalid request!";
            outputStream.println("Sending 400");
            writer.println(response);
        } catch (Exception ex) {
            outputStream.println("Error in bad request handler: " + ex.getLocalizedMessage());
        } 
    }

    //Returns latest WeatherData object, if there is any
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

    //Updates data file with a new JSON string, and re-sorts & re-filters data
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

            data.add(newWeatherData);

            //Update data file with updated array
            writeWeatherArrayToFile(data);

            //Filter and sort updated data file
            refreshDataFile();

            tempFile.delete();
        } catch (JsonProcessingException ex) {
            outputStream.println("JSONMappingException in updateWeatherData: " + ex.getLocalizedMessage());
            throw ex;
        } catch (IOException ex) {
            outputStream.println("IOException in updateWeatherData: " + ex.getLocalizedMessage());
            throw ex;
        } 
    }
    
    //Gets the data from the server's data file as an ArrayList of WeatherData objects
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
                outputStream.println("No data yet");
            }

            return dataArrayList;

        } catch(Exception ex) {
            outputStream.println("Error in getWeatherArrayFromFile: " + ex.getLocalizedMessage());
            ex.printStackTrace();
            return new ArrayList<WeatherData>();
        }
    }

    //Writes an ArrayList of WeatherData objects as JSON into the server's data file
    public static void writeWeatherArrayToFile(ArrayList<WeatherData> weatherDataList) {
        ObjectMapper mapper = new ObjectMapper();
        JsonFactory jsonFactory = new JsonFactory();

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(dataFilePath + "data"))) {
            JsonGenerator generator = jsonFactory.createGenerator(writer);

            // Write the updated weatherDataList to the file
            mapper.writeValue(generator, weatherDataList);

            generator.flush();
        } catch (Exception ex) {
            outputStream.println("Error in writeWeatherArrayToFile: " + ex.getLocalizedMessage());
        }
    }

    //Sorts an ArrayList of WeatherData objects by their Lamport Clock time
    public static ArrayList<WeatherData> sortByCreationClockTime(
        ArrayList<WeatherData> weatherData
    ) {
        Collections.sort(weatherData);
        return weatherData;
    }

    //Removes any data from content servers that last sent a PUT over 30 seconds ago
    public static ArrayList<WeatherData> filterInactiveContentServers(
        ArrayList<WeatherData> weatherData
    ) {
            Map<String, Long> contentServerIDsWithRecency = getContentServerIDsAndRecency(weatherData);
            
            Long millis30SecondsAgo = System.currentTimeMillis() - 30000; //30000 milliseconds = 30 seconds
            
            ArrayList<WeatherData> recentWeatherData = new ArrayList<WeatherData>();

            for (WeatherData data : weatherData) {
                Long serverLastUpdate = contentServerIDsWithRecency.get(data.getId());

                if (serverLastUpdate == null || serverLastUpdate >= millis30SecondsAgo) {
                    recentWeatherData.add(data);
                }
            }

            return recentWeatherData;
    }


    //Given an ArrayList of WeatherData objects, returns an object with each content server's ID and last PUT timestamp
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
            outputStream.println("Unsupported operation exception in getContentServerIDsAndRecency");
            ex.printStackTrace();
            throw ex;
        }
    }

    //Given a sorted ArrayList of WeatherData objects, removes any older than the last 20.
    public static ArrayList<WeatherData> filterLeastRecentData(
        ArrayList<WeatherData> weatherData
    ) {
        while (weatherData.size() > 20) {
                weatherData.remove(0);
        }

        return weatherData;
    }

    //Sorts and filters data from the server's weather data file, updating the file with the finished data.
    public static void refreshDataFile(
    ) {
        outputStream.println("Refreshing data");
        ArrayList<WeatherData> data = getWeatherArrayFromFile();

        data = sortByCreationClockTime(data);

        data = filterInactiveContentServers(data);

        data = filterLeastRecentData(data);

        //Update data file with updated array
        writeWeatherArrayToFile(data);
    }

    //If a temporary data file exists, finish processing it and update the server's data file
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
            outputStream.println("Exception in recovery: " + ex.getLocalizedMessage());
            ex.printStackTrace();
        }
    }

    //Shut down the server's socket
    public static void shutdown() {
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
