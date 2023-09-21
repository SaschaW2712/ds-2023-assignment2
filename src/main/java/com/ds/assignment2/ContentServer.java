package com.ds.assignment2;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ContentServer {
    
    public static LamportClock clock = new LamportClock();
    public static String serverName;
    public static int port;
    public static String inputFilePath;
    public static long fileLastModified;
    
    public static void main(String[] args) {
        
        if (args.length >= 2) {
            String[] clientArgs = args[0].split(":");
            serverName = clientArgs[0];
            port = Integer.parseInt(clientArgs[1]);
            inputFilePath = args[1];
        } else {
            System.out.println("Invalid args.");
            return;
        }

        getServerClock();

        int x = 0;
        while (x < 3) {
            try {
                TimeUnit.MILLISECONDS.sleep(1000);
            } catch(InterruptedException e) {
                System.out.println("Error: Interrupted");
                System.out.println(e);
                return;
            }
            
            connectToAggregationServer();
            x++;
        }
    }

    public static void getServerClock() {
        try (Socket socket = new Socket(serverName, port)) {
            
            System.out.println("Connected to server socket\n");
            InputStream inputStream = socket.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            
            OutputStream outputStream = socket.getOutputStream();
            PrintWriter writer = new PrintWriter(outputStream, true);
            
            
            System.out.println("\nSending Clock Request\n");
            sendClockRequest(writer);                        
            
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
    
    public static void connectToAggregationServer() {
        System.out.println("Connecting to aggregation server to send data");
        ObjectMapper mapper = new ObjectMapper();
        
        File dataFile = new File(inputFilePath);


        if (dataFile.lastModified() != fileLastModified) {
            fileLastModified = dataFile.lastModified();

            ArrayList<WeatherData> weatherData;
            try {
                weatherData = parseInputFile(inputFilePath);

                System.out.println("\n\nWeather data from input file:");
                for (WeatherData data : weatherData) {
                    data.printData();
                }
            } catch (IOException ex) {
                System.out.println("IOException: " + ex.getLocalizedMessage());
                return;
            }

            for (WeatherData data : weatherData) {

                //Update data with new clock time in case a previous request updated the content server's clock
                data.setSentClockTime(clock.getValue());

                try (Socket socket = new Socket(serverName, port)) {
                    System.out.println("Connected to server socket\n");

                    InputStream inputStream = socket.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                    
                    OutputStream outputStream = socket.getOutputStream();
                    PrintWriter writer = new PrintWriter(outputStream, true);
                    
                    String jsonBodyString = mapper.writeValueAsString(data);
                    System.out.println("JSON to be sent: " + jsonBodyString);
                    
                    //Send PUT request to aggregation server
                    System.out.println("\nSending Put Request\n");
                    sendPUTRequest(writer, serverName, jsonBodyString);
                    
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
    }

    public static void sendClockRequest(
        PrintWriter writer
    ) { 
        // Send the GET request
        writer.println("GET /clock HTTP/1.1");
        writer.println("Host: " + serverName);
        writer.println("Clock-Time: " + clock.getValue());
        writer.println();
    }
    
    public static void sendPUTRequest(PrintWriter writer, String hostname, String jsonBodyString) {
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

    public static ArrayList<WeatherData> parseInputFile(
        String filePathString
    ) throws IOException {
        System.out.println("Parsing input file");
        ArrayList<WeatherData> data = new ArrayList<WeatherData>();

        try {
            BufferedReader reader = new BufferedReader(new FileReader(filePathString));
            String line = reader.readLine();

            WeatherData currentData = new WeatherData(clock.getValue(), System.currentTimeMillis());

            while (true) {
                String[] keyAndValue = line.split(":", 2);

                String key = keyAndValue[0];
                String value = keyAndValue[1];

                currentData.updateDataFromKeyValuePair(key, value);
                
                line = reader.readLine();
                System.out.println("Line: " + line);

                if (line == null) {
                    System.out.println("End of file");
                    data.add(currentData);
                    break;
                } else if (line.startsWith("id:")) {
                    System.out.println("Starting new entry");
                    data.add(currentData);
                    currentData = new WeatherData(clock.getValue(), System.currentTimeMillis());
                }
            }

            reader.close();
        } catch (IOException ex) {
            System.out.println("Error parsing input file: " + ex.getLocalizedMessage());
            ex.printStackTrace();
        }

        return data;
    }
}
