

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

enum RequestType {
    WeatherData,
    Clock
}

public class GETClient {
    
    public static LamportClock clock = new LamportClock();
    public static int retries = 0;

    public static String serverName;
    public static int port;
    
    public static void main(String[] args) {
        if (args.length >= 1) {
            String[] clientArgs = args[0].split(":");
            serverName = clientArgs[0];
            port = Integer.parseInt(clientArgs[1]);
        } else {
            System.out.println("Invalid args provided.");
            return;
        }

        setUpServer();
        getAndPrintWeatherData();
    }
    
    public static void setUpServer() {        
        try(Socket socket = new Socket(serverName, port);) {
            
            // System.out.println("Connected to server socket");
            InputStream inputStream = socket.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            
            OutputStream outputStream = socket.getOutputStream();
            PrintWriter writer = new PrintWriter(outputStream, true);

            //GET and update clock
            writeClockRequest(writer);
            socket.shutdownOutput();
            handleServerResponse(reader, RequestType.Clock);

        } catch (UnknownHostException ex) {
            System.out.println("Server not found: " + ex.getMessage());
        } catch (IOException ex) {
            System.out.println("I/O error: " + ex.getMessage());
        }
    }

    public static void getAndPrintWeatherData() {
        try(Socket socket = new Socket(serverName, port);) {
            
            System.out.println("Connected to server socket");
            InputStream inputStream = socket.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            
            OutputStream outputStream = socket.getOutputStream();
            PrintWriter writer = new PrintWriter(outputStream, true);

            //GET and print weather
            writeWeatherDataRequest(writer);
            socket.shutdownOutput();
            handleServerResponse(reader, RequestType.WeatherData);

        } catch (UnknownHostException ex) {
            System.out.println("Server not found: " + ex.getMessage());
        } catch (IOException ex) {
            System.out.println("I/O error in getAndPrintWeatherData: " + ex.getMessage());
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

    public static void writeWeatherDataRequest(
        PrintWriter writer
    ) { 
        // Send the GET request
        writer.println("GET /weatherdata HTTP/1.1");
        writer.println("Host: " + serverName);
        writer.println("Clock-Time: " + clock.getValue());
        writer.println();
    }
    
    
    public static void handleServerResponse(
        BufferedReader reader,
        RequestType type
    ) {
        try {            
            String headerLine = reader.readLine();
            if (headerLine.startsWith("HTTP/1.1 200 OK")) {
                handleOKResponse(reader, type);
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
        BufferedReader reader,
        RequestType type
    ) throws IOException {
        
        String clockLine = reader.readLine();

        //The first line consumed after headers should be the server's clock
        if (clockLine.startsWith("Clock-Time:")) {
            int serverClockTime = Integer.parseInt(clockLine.split(":", 2)[1].trim());
            clock.updateValue(serverClockTime);
        } else {
            handleInvalidServerResponse(reader);
            return;
        }
        
        if (type == RequestType.WeatherData) {
            //Ignore any lines up to the JSON body
            while (!(reader.readLine()).isEmpty()) {}
        
            ObjectMapper mapper = new ObjectMapper();
            
            WeatherData weatherData;
            weatherData = mapper.readValue(reader.readLine(), WeatherData.class);
            weatherData.setClockTime(clock.getValue());
            
            weatherData.printData();
        }
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
        //Client will retry connection & request up to three times

        if (retries < 3) {
            retries++;

            try {
                //Sleeps for a short period in case the issue can be resolved with time
                TimeUnit.MILLISECONDS.sleep(5000);

            } catch(InterruptedException e) {
                System.out.println("Error: Interrupted");
                System.out.println(e);
                return;
            }

            while (reader.readLine() != null) {}

            setUpServer();
            getAndPrintWeatherData();
        } else {
            System.out.println("Exceeded max retries, exiting.");
            return;
        }
    }
}
