

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

enum RequestType {
    Clock,
    WeatherData
}

public class ContentServer {
    
    public static PrintStream outputStream = new PrintStream(System.out);

    public static LamportClock clock = new LamportClock();
    public static String serverName;
    public static int port;
    public static String inputFilePath;
    public static long fileLastModified;

    public static ArrayList<WeatherData> weatherData;

    public static int retries = 0;
    
    public static void main(String[] args) {
        clock = new LamportClock();
        retries = 0;
        
        if (args.length >= 2) {
            String[] clientArgs = args[0].split(":");
            serverName = clientArgs[0];
            port = Integer.parseInt(clientArgs[1]);
            inputFilePath = args[1];
        } else {
            System.out.println("Invalid args.");
            return;
        }

        if (args.length == 3) {
            try {
                PrintWriter writer = new PrintWriter(args[2]);
                writer.print("");
                writer.close();

                outputStream = new PrintStream(new FileOutputStream(args[2], true));
            } catch(FileNotFoundException e) {
                System.out.println("Couldn't find output file");
                return;
            }
        }

        //Get initial server clock
        getServerClock();
        if (retries >= 3) {
            outputStream.println("Failed to get server clock, exiting.");
            return;
        }
        
        retries = 0;

        //Send data
        updateWeatherDataIfChanged();
        sendAllWeatherDataToServer();
        if (retries >= 3) {
            outputStream.println("Failed to send weather data, exiting.");
        }
    }

    public static void getServerClock() {
        try (Socket socket = new Socket(serverName, port)) {
            
            outputStream.println("Connected to server socket");
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            
            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
            
            outputStream.println("Sending Clock Request");
            writeClockRequest(writer);                        
            
            // Shutdown output to signal the end of the request
            socket.shutdownOutput();
            
            handleServerResponse(reader);        
        } catch (UnknownHostException ex) {
            outputStream.println("Server not found: " + ex.getMessage());
            retryOnError(RequestType.Clock);
        } catch (JsonParseException ex) {
            outputStream.println("JSON parsing error: " + ex.getMessage());
            retryOnError(RequestType.Clock);
        } catch (JsonMappingException ex) {
            outputStream.println("JSON mapping error: " + ex.getMessage());
            retryOnError(RequestType.Clock);
        } catch (IOException ex) {
            outputStream.println("I/O error: " + ex.getMessage());
            retryOnError(RequestType.Clock);
        }    
    }
    
    public static void updateWeatherDataIfChanged() {
        File dataFile = new File(inputFilePath);

        if (dataFile.lastModified() != fileLastModified) {
            fileLastModified = dataFile.lastModified();

            try {
                weatherData = parseInputFile(inputFilePath);
            } catch (IOException ex) {
                outputStream.println("IOException: " + ex.getLocalizedMessage());
            }
        }
    }

    public static void sendAllWeatherDataToServer() {
        for (WeatherData data : weatherData) {
            //Update data with new clock time in case a previous request updated the content server's clock
            data.setClockTime(clock.getValue());
            sendSingleWeatherDataToServer(data);
        }
    }

    public static void sendSingleWeatherDataToServer(
        WeatherData data
    ) {
        try (Socket socket = new Socket(serverName, port)) {
            outputStream.println("Connected to server socket");

            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
            
            ObjectMapper mapper = new ObjectMapper();

            String jsonBodyString = mapper.writeValueAsString(data);
            
            //Send PUT request to aggregation server
            outputStream.println("Sending Put Request");
            writeDataPUTRequest(writer, serverName, jsonBodyString);
            
            socket.shutdownOutput();
            handleServerResponse(reader);
        
        } catch (UnknownHostException ex) {
            outputStream.println("Server not found: " + ex.getMessage());
            retryOnError(RequestType.WeatherData);
        } catch (JsonParseException ex) {
            outputStream.println("JSON parsing error: " + ex.getMessage());
            retryOnError(RequestType.WeatherData);
        } catch (JsonMappingException ex) {
            outputStream.println("JSON mapping error: " + ex.getMessage());
            retryOnError(RequestType.WeatherData);
        } catch (IOException ex) {
            outputStream.println("I/O error: " + ex.getMessage());
            retryOnError(RequestType.WeatherData);
        }
    }

    public static void writeClockRequest(
        PrintWriter writer
    ) { 
        // Send the GET request
        writer.println("GET /clock HTTP/1.1");
        writer.println("Host: " + serverName);
        writer.println("Clock-Time: " + clock.getValue());
        writer.println();
    }
    
    public static void writeDataPUTRequest(PrintWriter writer, String hostname, String jsonBodyString) {
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
            outputStream.println("Response from server:");
            
            String line;
            while ((line = reader.readLine()) != null) {

                //Update lamport clock from server
                if (line.startsWith("Clock-Time: ")) {
                    String clockLine = line.split(":")[1].trim();
                    clock.updateValue(Integer.parseInt(clockLine));
                }
                outputStream.println(line);
            }         
               
        } catch(IOException ex) {
            outputStream.println("IOException handling server response: " + ex.getLocalizedMessage());
            retryOnError(RequestType.WeatherData);
        }
    }

    public static void retryOnError(RequestType requestType) {
        //Client will retry connection & PUT request up to three times
        if (retries < 3) {
            retries++;

            try {
                //Sleeps for a short period in case the issue can be resolved with time
                TimeUnit.MILLISECONDS.sleep(1000);
            } catch(InterruptedException e) {
                outputStream.println("Error: Interrupted");
                outputStream.println(e);
                return;
            }

            if (requestType == RequestType.Clock) {
                getServerClock();
            } else if (requestType == RequestType.WeatherData) {
                updateWeatherDataIfChanged();
                sendAllWeatherDataToServer();
            }
        } else {
            outputStream.println("Exceeded max retries.");
            return;
        }
    }

    public static ArrayList<WeatherData> parseInputFile(
        String filePathString
    ) throws IOException {
        ArrayList<WeatherData> data = new ArrayList<WeatherData>();

        try {
            BufferedReader reader = new BufferedReader(new FileReader(filePathString));
            String line = reader.readLine();

            WeatherData currentData = new WeatherData(clock.getValue(), System.currentTimeMillis());
            
            if (line != null) {
                while (true) {
                    String[] keyAndValue = line.split(":", 2);
                    
                    String key = keyAndValue[0];
                    String value = keyAndValue[1];
                    
                    currentData.updateDataFromKeyValuePair(key, value);
                    
                    line = reader.readLine();
                    
                    if (line == null) {
                        // outputStream.println("End of file");
                        data.add(currentData);
                        break;
                    } else if (line.startsWith("id:")) {
                        // outputStream.println("Starting new entry");
                        data.add(currentData);
                        currentData = new WeatherData(clock.getValue(), System.currentTimeMillis());
                    }
                }
            }

            reader.close();
        } catch (IOException ex) {
            outputStream.println("Error parsing input file: " + ex.getLocalizedMessage());
            ex.printStackTrace();
        }

        return data;
    }
}
