package com.ds.assignment2;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ContentServer {
    
    public static LamportClock clock = new LamportClock();
    public static String hostname = "localhost";
    public static int port = 4567;
    public static long fileLastModified;
    
    public static void main(String[] args) {
        
        if (args.length >= 2) {
            hostname = args[0];
            port = Integer.parseInt(args[1]);
        } else {
            System.out.println("Invalid args.");
            return;
        }
        
        int x = 0;
        while (x < 3) {
            try {
                TimeUnit.MILLISECONDS.sleep(5000);
            } catch(InterruptedException e) {
                System.out.println("Error: Interrupted");
                System.out.println(e);
                return;
            }
            
            connectToAggregationServer();
            x++;
        }
        
        
    }
    
    public static void connectToAggregationServer() {
        ObjectMapper mapper = new ObjectMapper();
        
        File dataFile = new File("target/classes/com/ds/assignment2/content-server-input/weather-data1.txt");
        
        if (dataFile.lastModified() != fileLastModified) {
            fileLastModified = dataFile.lastModified();
            WeatherData weatherData = new WeatherData(clock.getValue(), "target/classes/com/ds/assignment2/content-server-input/weather-data1.txt");

            try (Socket socket = new Socket(hostname, port)) {
                
                System.out.println("Connected to server socket\n");
                InputStream inputStream = socket.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                
                OutputStream outputStream = socket.getOutputStream();
                PrintWriter writer = new PrintWriter(outputStream, true);
                
                System.out.println("File has been modified\n");                
                
                String jsonBodyString = mapper.writeValueAsString(weatherData);
                
                //Send PUT request to aggregation server
                System.out.println("\nSending Put Request\n");
                sendPUTRequest(writer, hostname, jsonBodyString);
                
                // Shutdown output to signal the end of the request
                socket.shutdownOutput();
                
                handleServerResponse(reader);
            
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
    }
    
    public static void sendPUTRequest(PrintWriter writer, String hostname, String jsonBodyString) {
        clock.tick();
        // Send the PUT request
        writer.println("PUT / HTTP/1.1");
        writer.println("Host: " + hostname);
        writer.println("Content-Type: application/json");
        writer.println("Content-Length: " + jsonBodyString.length());
        writer.println("Clock-Time: " + clock.getValue());
        writer.println();
        writer.println(jsonBodyString);
    }
    
    public static void handleServerResponse(BufferedReader reader) {
        try {
            //Read server response
            System.out.println("\nReading from server\n");
            
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("Clock-Time: ")) {
                    String clockLine = line.split(":")[1].trim();
                    clock.updateValue(Integer.parseInt(clockLine));
                }
                System.out.println(line);
            }
        } catch(IOException ex) {
            System.out.println("IOException handling server resonse: " + ex.getLocalizedMessage());
        }
    }
}
