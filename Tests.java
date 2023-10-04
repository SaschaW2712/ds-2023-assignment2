
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.rmi.NotBoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;


enum TestingIDType {
    ContentServer,
    AggregationServer,
    GETClient
}
public class Tests extends Thread {

    private Tests() {}

    public static AggregationServer aggregationServer;
    public static int passCount = 0;

    public static void main(String[] args) throws FileNotFoundException, IOException, NotBoundException {
        try {
            int numTests = 7;

            testContentServerWithoutAvailableServer();
            testGETClientWithoutAvailableServer();

            startAggregationServer(1);
            TimeUnit.MILLISECONDS.sleep(1000); //Wait for server to start up

            testContentServerOKRequests();
            testContentServerDoesNotSendEmpty();
            testGETClientOKRequest(2);

            stopAggregationServer();
            TimeUnit.MILLISECONDS.sleep(1000); //Wait for server to stop fully

            startAggregationServer(2);
            TimeUnit.MILLISECONDS.sleep(1000); //Wait for server to start up

            testGETClientOKRequest(3);

            testAggregationServerFlushesOldData();

            stopAggregationServer();


            System.out.println();
            System.out.println(passCount + "/" + numTests + " Tests Passed");
        } catch(Exception e) {
            System.out.println("Tests exception:");
            System.out.print(e);
            return;
        }
    }

    public static void testContentServerWithoutAvailableServer() {
        System.out.println("Running test: Content server without available aggregation server.");

        runContentServer(1);

        boolean result = assertEqualFileContents(1, "content_server");
        if (result) { passCount++; };
    }

    public static void testGETClientWithoutAvailableServer() {
        System.out.println("Running test: GET client without available aggregation server.");

        runGETClient(1);

        boolean result = assertEqualFileContents(1, "get_client");
        if (result) { passCount++; };
    }

    public static void testContentServerOKRequests() {
        System.out.println("Running test: Correct content server PUT requests to an available aggregation server.");

        runContentServer(2);

        boolean result = assertEqualFileContents(2, "content_server");
        if (result) { passCount++; };
    }

    public static void testContentServerDoesNotSendEmpty() {
        System.out.println("Running test: Content server does not send data if input is empty.");

        runContentServer(3);
        
        boolean result = assertEqualFileContents(2, "content_server");
        if (result) { passCount++; };
    }

    public static void testGETClientOKRequest(int testingID) {
        System.out.println("Running test: Correct GET client request with available data.");

        runGETClient(testingID);

        boolean result = assertEqualFileContents(testingID, "get_client");
        if (result) { passCount++; };
    }

    public static void testAggregationServerFlushesOldData() {
        System.out.println("\nATTENTION!");
        System.out.println("About to test flushing of old data. A delay of 30 seconds will occur, then the tests will complete.");
        System.out.println("This is the last test.");
        System.out.println();
        System.out.println("Running test: Aggregation server flushes old data, and GET client handles no weather data being available.");

        try {
            TimeUnit.MILLISECONDS.sleep(30000);
        } catch(InterruptedException e) {
            System.out.println("Error: Interrupted");
            System.out.println(e);
            return;
        }

        runGETClient(4);

        boolean result = assertEqualFileContents(4, "get_client");
        if (result) { passCount++; };
    }

    public static void runContentServer(int testingID) {
        String outputFilePath = getObservedOutputFilePath(testingID, "content_server");
        String dataFilePath = getDataFilePath(testingID);
        String url = "localhost:4567";

        String[] args = { url, dataFilePath, outputFilePath };

        // System.out.println("Args: " + Arrays.toString(args));

        Thread contentServerThread = new Thread(() -> {
            ContentServer.main(args);
        });
        contentServerThread.start();
        // System.out.println("Started content server " + testingID);

        try {
            contentServerThread.join();
        } catch (InterruptedException e) {
            System.out.println("Error joining thread for content server " + testingID);
            e.printStackTrace();
        }
    }

    public static void runGETClient(int testingID) {
        String outputFilePath = "testObservedOutputs/get_client_" + String.valueOf(testingID);
        String url = "localhost:4567";

        String[] args = { url, outputFilePath };

        // System.out.println("Args: " + Arrays.toString(args));
        Thread getClientThread = new Thread(() -> {
            GETClient.main(args);
        });
        getClientThread.start();
        // System.out.println("Started GET client " + testingID);

        try {
            getClientThread.join();
        } catch (InterruptedException e) {
                System.out.println("Error joining thread for GET client " + testingID);
            e.printStackTrace();
        }
    }

    public static void startAggregationServer(int testingID) throws IOException {
        String portNumber = "4567";
        String outputFilePath = "testObservedOutputs/aggregation_server_" + String.valueOf(testingID);

        String[] args = { portNumber, outputFilePath };

        aggregationServer = new AggregationServer();

        // System.out.println("Starting aggregation server " + String.valueOf(testingID));
        Thread serverThread = new Thread(() -> {
            aggregationServer.main(args);
        });

        serverThread.start();
    }

    public static void stopAggregationServer() {
        // System.out.println("Stopping aggregation server");
        aggregationServer.shutdown();
        aggregationServer = null;
    }

    public static String getObservedOutputFilePath(int testingID, String type) {
        return "testObservedOutputs/" + type + "_" + String.valueOf(testingID);
    }

    public static String getExpectedOutputFilePath(int testingID, String type) {
        return "testExpectedOutputs/" + type + "_" + String.valueOf(testingID);
    }

    public static String getDataFilePath(int testingID) {
        return "testWeatherData/content_server_" + String.valueOf(testingID) + ".txt";
    }

    public static boolean assertEqualFileContents(
        int testingID,
        String type
    ) {
        String observedOutputFilePath = getObservedOutputFilePath(testingID, type);
        String expectedOutputFilePath = getExpectedOutputFilePath(testingID, type);

        try {
            String[] observedOutput = getFileLinesAsArray(observedOutputFilePath);
            String[] expectedOutput = getFileLinesAsArray(expectedOutputFilePath);

            if (Arrays.equals(observedOutput, expectedOutput)) {
                System.out.println("Test PASSED");
                System.out.println();
                return true;
            } else {
                System.out.println("Test FAILED");
                System.out.println("Expected: " + Arrays.toString(expectedOutput) + "");
                System.out.println("Observed: " + Arrays.toString(observedOutput));
                System.out.println();
                return false;
            }
        } catch(Exception e) {
            System.out.println("Test error:" + e.getLocalizedMessage());
            System.out.print(e);
            return false;
        }
    }

    //Generate arguments to pass to client
    //Takes a string for input and output file names (can be "sysout"), and whether to append "clearall" to the end of the arguments
    public static String[] getClientArgsFromFile(
        String inputFileName,
        String outputFileName,
        boolean appendClearAll
    ) throws IOException {
        try {
            String[] inputFileLines = getFileLinesAsArray(inputFileName);

            ArrayList<String> clientArgs = new ArrayList<String>();

            for (String line : inputFileLines) {
                clientArgs.add(line);
            }

            clientArgs.add(outputFileName);

            // if (appendClearAll) {
            //     clientArgs.add("clearall");
            // }

            return clientArgs.toArray(new String[clientArgs.size()]);
        } catch(Exception e) {
            System.out.println("getClientArgsFromFile error:");
            System.out.print(e);
            throw(e);
        }
    }

    //Given an input file name, returns an array of the file's lines as Strings
    public static String[] getFileLinesAsArray(String filename) throws IOException {
        try {
            FileReader fileReader = new FileReader(filename);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            List<String> lines = new ArrayList<String>();

            String line = null;

            while ((line = bufferedReader.readLine()) != null) {
                lines.add(line);
            }

            bufferedReader.close();

            return lines.toArray(new String[lines.size()]);            
        } catch(Exception e) {
            System.out.println("getFileLinesAsArray error:");
            System.out.print(e);
            throw(e);
        }

    }

}