package com.ds.assignment2;

import java.io.*;
import java.net.*;
import java.util.Iterator;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/*
 * TODO:
 *      - Server replicas
 *      - Store weather data in a single file
 *      - Handle updating weather data
 *          - Deleting data from content servers that haven't sent PUT in 30 seconds
 *          - Deleting older than last 20 entries
 *      - Send weather data matching client's lamport clock
 *      - PUT requests being inserted in Lamport clock order, not timestamp
 *      - Automated testing
 *      - Invalid requests
 *      - Handling when not all fields are there for weather data
 *      - Handling multiple data pieces in one file (content server)
 */

public class AggregationServer {

    public static LamportClock clock = new LamportClock();

    public static void main(String[] args) {
        int port = 4567;

        if (args.length >= 1) {
            port = Integer.parseInt(args[0]);
        }
 
        try (ServerSocket serverSocket = new ServerSocket(port)) {
 
            System.out.println("Server is listening on port " + port);
 
            while (true) {
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
        //Increment clock
        clock.tick();

        // Read the HTTP request from the client
        String request = reader.readLine();

        System.out.println("Just read client request");
        if (request != null && request.startsWith("GET")) {
            handleGETRequest(reader, writer);

        } else if (request != null && request.startsWith("PUT")) {
            handlePUTRequest(reader, writer);

        } else {
            handleBadRequest(reader, writer);
        }
    }

    private static void handleGETRequest(
        BufferedReader reader,
        PrintWriter writer
    ) {
        try {
            String line;
            while (!(line = reader.readLine()).isEmpty()) {
                if (line.startsWith("Clock-Time:")) {
                    int getClientClockTime = Integer.parseInt(line.split(":", 2)[1].trim());

                    System.out.println("GET client clock time: " + getClientClockTime);
                    System.out.println("Server clock time: " + clock.getValue());
                    clock.updateValue(getClientClockTime);

                    System.out.println("Server updated clock time: " + clock.getValue());
                }
            }

            // Process the request and send the appropriate response
            ObjectMapper mapper = new ObjectMapper();
            File latestDataFile = new File("target/classes/com/ds/assignment2/weather-data/data");
            String weatherData = mapper.writeValueAsString(mapper.readTree(latestDataFile));

            System.out.println(weatherData);
            String response = "HTTP/1.1 200 OK\n" + "Clock-Time: " + clock.getValue() + "\n\n" + weatherData;
            System.out.println("Sending 200");

            // Write the JSON line to the writer
            writer.println(response);
            
        } catch (Exception ex) {
            System.out.println("Error in GET request handler: " + ex.getLocalizedMessage());
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

                System.out.println("PUT client's clock time: " + contentServerClockTime);
                System.out.println("Server clock time: " + clock.getValue());
                
                clock.updateValue(contentServerClockTime);
                System.out.println("Server updated clock time: " + clock.getValue());
            }
        }

        // Read the JSON body from the request
        StringBuilder jsonBody = new StringBuilder();

        Long receivedAt = System.currentTimeMillis();

        while ((line = reader.readLine()) != null) {
            jsonBody.append(line);
        }

        String parsedJSONString = jsonBody.toString();

        updateWeatherData(parsedJSONString, receivedAt);

        System.out.println("Sending 200 to PUT client");

        // Respond
        writer.println("HTTP/1.1 200 OK\n" + "Clock-Time: " + clock.getValue() + "\n\n" + "Received JSON: " + parsedJSONString);
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

                    System.out.println("GET client clock time: " + getClientClockTime);
                    System.out.println("Server clock time: " + clock.getValue());
                    clock.updateValue(getClientClockTime);

                    System.out.println("Server updated clock time: " + clock.getValue());
                }
            }

            String response = "HTTP/1.1 400 Bad Request\n\nInvalid request!";
            System.out.println("Sending 400");
            writer.println(response);
        } catch (Exception ex) {
            System.out.println("Error in bad request handler: " + ex.getLocalizedMessage());
        } 
    }


 /*
  * Immediately write to temp file
  * Read all entries from current data file and sort by recency added
  *     - Do we need a data class for weatherdata + recency?
  *     - Class for content server id & last communicated timestamp, each time we get a push we check 30secs and delete
  *     old ones
  *     - Sort by added timestamp and delete older than 20 entries
  * Once old content server data removed and older data removed, replace weatherdata array, re-write data file and delete temp file
  * If we fail during this logic ^, on start-up check if we have a temp file and run the update logic again if so
  */
  
    public static void updateWeatherData(
        String newDataString,
        Long receivedAt
    ) {
        try {
            //Write new string to file immediately
            BufferedWriter fileWriter = new BufferedWriter(new FileWriter("target/classes/com/ds/assignment2/weather-data/temp"));
            fileWriter.write(newDataString);
            fileWriter.close();

            ObjectMapper mapper = new ObjectMapper();
            WeatherData data = mapper.readValue(newDataString, WeatherData.class);

            System.out.println("Data in updateWeatherData: ");
            data.printData();
            //We only keep: last 20, data from content servers active in the last 30 seconds that are still connected

        } catch(Exception ex) {
            System.out.println("Error updating weather data: " + ex.getLocalizedMessage());
        }
    }
    
    public static void getWeatherArrayFromFile() {
        ObjectMapper mapper  = new ObjectMapper();
        JsonFactory jsonFactory = new JsonFactory();
        try(BufferedReader reader = new BufferedReader(new FileReader("luser.txt"))) {
            JsonParser parser = jsonFactory.createParser(reader);
            Iterator<WeatherData> value = mapper.readValues(parser, WeatherData.class);

            value.forEachRemaining((dataObject) -> { 
                System.out.println(dataObject);
            });


        } catch(Exception ex) {
            
        }
    }
}
