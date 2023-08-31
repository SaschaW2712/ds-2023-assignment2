package com.ds.assignment2;

import java.io.*;
import java.net.*;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class AggregationServer {

    public static LamportClock clock = new LamportClock();

    public static void main(String[] args) {
        int port = 8080;

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
            handleGETRequest(writer);

        } else if (request != null && request.startsWith("PUT")) {
            handlePUTRequest(reader, writer);
        } else {
            String response = "HTTP/1.1 400 Bad Request\r\n\r\nInvalid request!";
            System.out.println("Sending 400");
            writer.println(response);
        }
    }

    private static void handleGETRequest(
        PrintWriter writer
    ) {
        try {
            // Process the request and send the appropriate response
            ObjectMapper mapper = new ObjectMapper();
            File latestDataFile = new File("target/classes/com/ds/assignment2/weather-data/data");
            String weatherData = mapper.writeValueAsString(mapper.readTree(latestDataFile));

            System.out.println(weatherData);
            String response = "HTTP/1.1 200 OK\n" + "CLOCK: " + clock.getValue() + "\n\n" + weatherData;
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
                clock.updateValue(contentServerClockTime);
            }
        }

        // Read the JSON body from the request
        StringBuilder jsonBody = new StringBuilder();

        while ((line = reader.readLine()) != null) {
            jsonBody.append(line);
        }

        String parsedJSONString = jsonBody.toString();

        ObjectMapper mapper = new ObjectMapper();

        WeatherData data = mapper.readValue(parsedJSONString, WeatherData.class);

        //Write data to file immediately
        mapper.writeValue(new File("target/classes/com/ds/assignment2/weather-data/" + "data"), data);

        System.out.println("Sending 200 to PUT client");

        // Respond
        writer.println("HTTP/1.1 200 OK\n" + "CLOCK: " + clock.getValue() + "\n\n" + "Received JSON: " + parsedJSONString);
    }

}
