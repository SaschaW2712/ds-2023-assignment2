package com.ds.assignment2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

import com.fasterxml.jackson.databind.ObjectMapper;

public class GETClient {

    public static LamportClock clock = new LamportClock();

    public static void main(String[] args) {
        String hostname = "localhost";
        int port = 8080;
        if (args.length >= 2) {
            hostname = args[0];
            port = Integer.parseInt(args[1]);
        }
 
        try (Socket socket = new Socket(hostname, port)) {
 
            System.out.println("Connected to server socket");
            InputStream inputStream = socket.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            
            OutputStream outputStream = socket.getOutputStream();
            PrintWriter writer = new PrintWriter(outputStream, true);
 
            // Send the GET request
            writer.println("GET / HTTP/1.1");
            writer.println("Host: " + hostname);
            writer.println("Clock-Time: " + clock.getValue());
            writer.println();

            handleServerResponse(reader);
        } catch (UnknownHostException ex) {
 
            System.out.println("Server not found: " + ex.getMessage());
 
        } catch (IOException ex) {
 
            System.out.println("I/O error: " + ex.getMessage());
        }
    }

    public static void handleServerResponse(
        BufferedReader reader
    ) {
        try {
            System.out.println("Reading from server");
 
            String headerLine = reader.readLine();
            if (headerLine.startsWith("HTTP/1.1 200 OK")) {
                handleOKResponse(reader);
            } else if (headerLine.startsWith("HTTP/1.1 500")) {
                handle500Response(reader);
            } else {
                handleInvalidServerResponse();
            }


        } catch(IOException ex) {
            System.out.println("I/O error: " + ex.getMessage());
        }
    }

    public static void handleOKResponse(
        BufferedReader reader
    ) throws IOException {

        String clockLine = reader.readLine();
        if (clockLine.startsWith("Clock-Time:")) {
            int serverClockTime = Integer.parseInt(clockLine.split(":", 2)[1].trim());
            clock.updateValue(serverClockTime);
        } else {
            handleInvalidServerResponse();
            return;
        }

        ObjectMapper mapper = new ObjectMapper();
        
        WeatherData weatherData;
        weatherData = mapper.readValue(reader.readLine(), WeatherData.class);
        weatherData.setClockTime(clock.getValue());
        
        weatherData.printData();
    }

    public static void handle500Response(
        BufferedReader reader
    ) {
        System.out.println("Server returned 500 response");

        //TODO: retry after delay
    }

    public static void handleInvalidServerResponse() {
        System.out.println("Server return invalid response format");

        //TODO: retry after delay
    }
}
