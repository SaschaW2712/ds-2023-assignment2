package com.ds.assignment2;

import java.io.*;
import java.net.*;
import com.fasterxml.jackson.core.*;

public class AggregationServer {

    public static void main(String[] args) {
        int port = 8080;

        if (args.length >= 1) {
            port = Integer.parseInt(args[1]);
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
        // Process the request and send the appropriate response
        String response = "HTTP/1.1 200 OK\r\n\r\nHello, here's the weather info!";
        System.out.println("Sending 200");
        writer.println(response);
    }

        private static void handlePUTRequest(
            BufferedReader reader,
            PrintWriter writer
        ) throws IOException {
            String line;
            while (!(line = reader.readLine()).isEmpty()) {
                // Skip headers for now
            }

            // Read the JSON body from the request
            StringBuilder jsonBody = new StringBuilder();

            while ((line = reader.readLine()) != null) {
                jsonBody.append(line);
            }

            String parsedJSONString = jsonBody.toString();

            // Process the JSON body
            writer.println("HTTP/1.1 200 OK\r\n\r\n" + "Received JSON: " + parsedJSONString);

    }

}
