package com.ds.assignment2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.ObjectMapper;

public class GETClient {
    
    public static LamportClock clock = new LamportClock();
    public static int retries = 0;

    public static String hostname = "localhost";
    public static int port = 4567;
    
    public static void main(String[] args) {
        if (args.length >= 2) {
            hostname = args[0];
            port = Integer.parseInt(args[1]);
        }

        setUpServer();
    }
    
    public static void setUpServer(
    ) {        
        try(Socket socket = new Socket(hostname, port);) {
            
            System.out.println("Connected to server socket");
            InputStream inputStream = socket.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            
            OutputStream outputStream = socket.getOutputStream();
            PrintWriter writer = new PrintWriter(outputStream, true);

            sendServerRequest(writer);
            
            handleServerResponse(reader, writer);

        } catch (UnknownHostException ex) {
            System.out.println("Server not found: " + ex.getMessage());
        } catch (IOException ex) {
            System.out.println("I/O error: " + ex.getMessage());
        }
    }

    public static void sendServerRequest(
        PrintWriter writer
    ) { 
        // Send the GET request
        writer.println("GET / HTTP/1.1");
        writer.println("Host: " + hostname);
        writer.println("Clock-Time: " + clock.getValue());
        writer.println();
    }
    
    
    public static void handleServerResponse(
        BufferedReader reader,
        PrintWriter writer
    ) {
        try {
            System.out.println("Reading from server");
            
            String headerLine = reader.readLine();
            if (headerLine.startsWith("HTTP/1.1 200 OK")) {
                handleOKResponse(reader);
            } else if (headerLine.startsWith("HTTP/1.1 400")) {
                handle400Response(reader);
                return;
            } else {
                handleInvalidServerResponse(reader);
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
            handleInvalidServerResponse(reader);
            return;
        }
        
        while (!(reader.readLine()).isEmpty()) {}
        ObjectMapper mapper = new ObjectMapper();
        
        WeatherData weatherData;
        weatherData = mapper.readValue(reader.readLine(), WeatherData.class);
        weatherData.setClockTime(clock.getValue());
        
        weatherData.printData();
    }
    
    public static void handle400Response(
        BufferedReader reader
    ) throws IOException {
        System.out.println("Server returned 400 response");
        
        retry(reader);
    }
    
    public static void handleInvalidServerResponse(
        BufferedReader reader
    ) throws IOException {
        System.out.println("Server returned invalid response format");

        retry(reader);
    }

    public static void retry(
        BufferedReader reader
    ) throws IOException {
        if (retries < 3) {
            retries++;
            try {
                TimeUnit.MILLISECONDS.sleep(5000);
            } catch(InterruptedException e) {
                System.out.println("Error: Interrupted");
                System.out.println(e);
                return;
            }

            while (reader.readLine() != null) {}

            setUpServer();
        } else {
            System.out.println("exceeded retries");
            return;
        }
    }
}
