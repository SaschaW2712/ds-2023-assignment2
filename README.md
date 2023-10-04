To compile the project, run `make`

-----------

RUNNING SERVERS AND CLIENTS 

To start an Aggregation Server:
    - With default port, run `make server`
    - With custom arguments, run:
    `java -cp classfiles:lib/jackson-core-2.12.5.jar:lib/jackson-databind-2.12.5.jar:lib/jackson-annotations-2.12.5.jar AggregationServer <port number>`
    E.g. `4444`


To start a Content Server:
    - With default arguments, run `make content`
    - With custom arguments, run:
    `java -cp classfiles:lib/jackson-core-2.12.5.jar:lib/jackson-databind-2.12.5.jar:lib/jackson-annotations-2.12.5.jar ContentServer <server name>:<port number> <path to input file>`
    E.g. `localhost:4444 testWeatherData/content_server_1.txt`

To start a GET Client:
    - With default arguments, run `make client`
    - With custom arguments, run:
    `java -cp classfiles:lib/jackson-core-2.12.5.jar:lib/jackson-databind-2.12.5.jar:lib/jackson-annotations-2.12.5.jar GETClient <server name>:<port number>`
    E.g. `localhost:4444`


-----------

AUTOMATED TESTING

Run automated tests with `make test`.

Organisation of test files:
    - Expected outputs for content servers / GET clients used in testing are in `testExpectedOutputs`
    - Actual outputs are in `testObservedOutputs`
        - This directory also includes observed outputs for aggregation servers used in testing, for debugging purposes.
    - Weather data files used in testing are in `testWeatherData`

The cases covered by automated testing are outlined below, with the specific function provided in brackets for easy reference. Some of these cases are covered together with other tests (for example, a test that content servers receive a 201 response on their first successful PUT request will also test that the aggregation server can process and respond to PUT requests correctly).

Content server:
    - Content server synchronises with the server's Lamport Clock on startup through a GET request(`testContentServerOKRequests`)
    - Content server unable to connect to aggregation server retries the unsuccessful request three times and then exits (`testContentServerWithoutAvailableServer`)
    - First valid content server PUT request receives a 201 response (`testContentServerOKRequests`)
    - Subsequent valid content server PUT requests receive a 200 response (`testContentServerOKRequests`)
    - Content server can parse and send multiple pieces of weather data in the same input file (`testContentServerOKRequests`)
    - Content server with an empty data file will not attempt to send empty data to the aggregation server (`testContentServerDoesNotSendEmpty`)

GET client:
    - GET client synchronises with the server's Lamport Clock on startup through a GET request (`testGETClientOKRequest`)
    - GET client unable to connect to aggregation server retries the unsuccessful request three times and then exits (`testGETClientWithoutAvailableServer`)
    - GET client correctly requests and handles the response for a weather data GET request and prints the data (`testGETClientOKRequest`)
    - GET client appropriately handles a 404 response when there is not yet any data available (`testAggregationServerFlushesOldData`)

Aggregation server:
    - Aggregation server responds to Lamport Clock GET request with the latest Lamport Clock value (`testContentServerOKRequests`, `testGETClientOKRequest`)
    - Aggregation server can receive and respond to a correct weather data PUT request (`testContentServerOKRequests`)
    - Aggregation server responds to weather data GET request with latest weather data by Lamport Clock value, if there is data available (`testGETClientOKRequest`)
    - Aggregation server responds to weather data GET request with 404 code, if there is no data available (`testAggregationServerFlushesOldData`)
    - Aggregation server flushes data older than 30 seconds (`testAggregationServerFlushesOldData`)
    - Aggregation server persists weather data across executions (`testGETClientOKRequest`)
        - i.e. Valid weather data will still be available if the server is stopped and restarted
    - Aggregation server refreshes (sorts and filters) its weather data on startup (and deletes data older than 30 seconds) (`testAggregationServerFlushesOldData`)


