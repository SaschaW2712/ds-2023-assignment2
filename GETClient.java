import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

public class GETClient {
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
            writer.println();

            System.out.println("Reading from server");
 
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
 
 
        } catch (UnknownHostException ex) {
 
            System.out.println("Server not found: " + ex.getMessage());
 
        } catch (IOException ex) {
 
            System.out.println("I/O error: " + ex.getMessage());
        }
    }
}
