

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.ObjectMapper;

public class GETClient {
    
    public static PrintStream outputStream = new PrintStream(System.out);

    public static LamportClock clock = new LamportClock();
    public static int retries = 0;

    public static String serverName;
    public static int port;
    
    public static void main(String[] args) {
        clock = new LamportClock();
        retries = 0;
        serverName = "";
        port = 0;


        //Confirm that necessary arguments have been provided
        if (args.length >= 1) {
            String[] clientArgs = args[0].split(":");
            serverName = clientArgs[0];
            port = Integer.parseInt(clientArgs[1]);
        } else {
            System.out.println("Invalid args provided.");
            return;
        }

        //Redirect system output if requested
        if (args.length == 2) {
            try {
                PrintWriter writer = new PrintWriter(args[1]);
                writer.print("");
                writer.close();

                outputStream = new PrintStream(new FileOutputStream(args[1], true));
            } catch(FileNotFoundException e) {
                System.out.println("Couldn't find output file");
                return;
            }
        }

        //Attempt to connect to an aggregation server
        setUpServer();
        if (retries >= 3) {
            outputStream.println("Failed to get server clock, exiting.");
            return;
        }
           
        retries = 0; //reset retries, so that weather data GET request can also retry

        getAndPrintWeatherData();

        if (retries >= 3) {
            outputStream.println("Failed to get weather data, exiting.");
            return;
        }
    }
    
    //Connects to the aggregation server and synchronises local Lamport Clock time to match the server's
    public static void setUpServer() {        
        try(Socket socket = new Socket(serverName, port);) {
            
            // outputStream.println("Connected to server socket");
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            
            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);

            //GET and update clock
            writeClockRequest(writer);
            socket.shutdownOutput();
            handleServerResponse(reader, RequestType.Clock);

        } catch (UnknownHostException ex) {
            outputStream.println("Server not found: " + ex.getMessage());
        } catch (IOException ex) {
            outputStream.println("I/O error: " + ex.getMessage());
            retry(RequestType.Clock);
        }
    }

    //Connects to the aggregation server and GETs the latest weather data.
    public static void getAndPrintWeatherData() {
        try(Socket socket = new Socket(serverName, port);) {
            
            outputStream.println("Connected to server socket");
            InputStream inputStream = socket.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            
            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);

            //GET and print weather
            writeWeatherDataRequest(writer);
            socket.shutdownOutput();
            handleServerResponse(reader, RequestType.WeatherData);

        } catch (UnknownHostException ex) {
            outputStream.println("Server not found: " + ex.getMessage());
        } catch (IOException ex) {
            outputStream.println("I/O error in getAndPrintWeatherData: " + ex.getMessage());
        }
    }

    //Writes the request for GET "/clock" to a given PrintWriter.
    public static void writeClockRequest(
        PrintWriter writer
    ) { 
        writer.println("GET /clock HTTP/1.1");
        writer.println("Host: " + serverName);
        writer.println("Clock-Time: " + clock.getValue());
        writer.println();
    }

    //Writes the request for GET "/weatherdata" to a given PrintWriter.
    public static void writeWeatherDataRequest(
        PrintWriter writer
    ) { 
        // Send the GET request
        writer.println("GET /weatherdata HTTP/1.1");
        writer.println("Host: " + serverName);
        writer.println("Clock-Time: " + clock.getValue());
        writer.println();
    }
    
    //Handles an aggregation server response
    public static void handleServerResponse(
        BufferedReader reader,
        RequestType requestType
    ) throws IOException {
        try {            
            String headerLine = reader.readLine();

            if (headerLine.startsWith("HTTP/1.1 200 OK")) {
                handleOKResponse(reader, requestType);
            } else if (headerLine.startsWith("HTTP/1.1 400")) {
                handle400Response(reader, requestType);
                return;
            } else if (headerLine.startsWith("HTTP/1.1 404")) {
                handle404Response(reader);
                return;
            } else {
                handleInvalidServerResponse(reader, requestType);
            }
            
            
        } catch(IOException ex) {
            outputStream.println("I/O error: " + ex.getMessage());
            retry(requestType);
        }
    }
    
    //Handles a 200 OK response, updating local clock time and printing data (if the request was for weather data)
    public static void handleOKResponse(
        BufferedReader reader,
        RequestType requestType
    ) throws IOException {
        
        String clockLine = reader.readLine();

        //The first line consumed after headers should be the server's clock
        if (clockLine.startsWith("Clock-Time:")) {
            int serverClockTime = Integer.parseInt(clockLine.split(":", 2)[1].trim());
            clock.updateValue(serverClockTime);
        } else {
            handleInvalidServerResponse(reader, requestType);
            return;
        }
        
        if (requestType == RequestType.WeatherData) {
            //Ignore any lines up to the JSON body
            while (!(reader.readLine()).isEmpty()) {}
        
            ObjectMapper mapper = new ObjectMapper();
            
            WeatherData weatherData;
            weatherData = mapper.readValue(reader.readLine(), WeatherData.class);
            weatherData.setClockTime(clock.getValue());
            
            weatherData.printData(outputStream);
        }
    }
    
    //Handles a 400 response by triggering the "retry" loop
    public static void handle400Response(
        BufferedReader reader,
        RequestType requestType
    ) throws IOException {
        outputStream.println("Server returned 400 response");
        
        retry(requestType);
    }

    //Handles a 404 response by printing an explanation for the lack of data.
    public static void handle404Response(
        BufferedReader reader
    ) throws IOException {
        outputStream.println("Server returned 404 response, no weather data is available.");
    }
    
    //Handles an invalid formatted server response by triggering the "retry" loop
    public static void handleInvalidServerResponse(
        BufferedReader reader,
        RequestType requestType
    ) throws IOException {
        outputStream.println("Server returned invalid response format");

        retry(requestType);
    }

    //Retries the given request type up to 3 times (for a total of 4 attempts including the initial one)
    public static void retry(
        RequestType requestType
    ) {
        //Client will retry connection & request up to three times (for a total of 4 attempts)
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
                setUpServer();
            } else if (requestType == RequestType.WeatherData) {
                getAndPrintWeatherData();
            }
        } else {
            outputStream.println("Exceeded max retries.");
            return;
        }
    }
}
