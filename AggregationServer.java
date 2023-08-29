import java.io.*;
import java.net.*;

public class AggregationServer {

    public static void main(String[] args) {
        int port = 8080;

        ServerSocket serverSocket = null;

        try {
            serverSocket = new ServerSocket(port);
            System.out.println("Server is listening on port " + port);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected: " + clientSocket.getInetAddress().getHostAddress());

                // Create input and output streams for communication
                InputStream clientInputStream = clientSocket.getInputStream();
                InputStreamReader clientinputStreamReader = new InputStreamReader(clientInputStream);
                BufferedReader clientInputReader = new BufferedReader(clientinputStreamReader);

                PrintWriter clientOutputWriter = new PrintWriter(clientSocket.getOutputStream(), true);

                // Read the incoming request
                String request = clientInputReader.readLine();
                System.out.println("Received request: " + request);

                // Process the request (in this case, just sending a basic response)
                String response = "HTTP/1.1 200 OK\r\n\r\nHello, this is a basic server!";
                clientOutputWriter.println(response);

                // Close the streams and socket
                clientInputReader.close();
                clientOutputWriter.close();
                clientSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
