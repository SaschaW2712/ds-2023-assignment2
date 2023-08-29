import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
 
            InputStream inputStream = socket.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
 
            String content = reader.readLine();
 
            System.out.println(content);
 
 
        } catch (UnknownHostException ex) {
 
            System.out.println("Server not found: " + ex.getMessage());
 
        } catch (IOException ex) {
 
            System.out.println("I/O error: " + ex.getMessage());
        }
    }
}
