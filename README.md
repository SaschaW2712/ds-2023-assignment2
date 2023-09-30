To compile the project, run `make`

Currently basic functionality is mostly implemented:
    - Text sending works
    - client, server and content server processes start up and communicate
    - PUT operation works for one content server
    - GET operation works for many read clients
    - Aggregation server expunging expired data works (30s)
    - Retry on errors (server not available etc) works
        - The GET client and Content Server will retry their request three times.

Full functionality is mostly implemented
    - Lamport clocks are implemented
    - All error codes are implemented
    - Content servers are NOT YET replicated and fault tolerant

Other things still to be implemented:
    - Automated testing harness (testing plan is at the bottom of this file)

-----------

AGGREGATION SERVER
To start the Aggregation Server:
    - With default port, run `make server`
    - With custom arguments, run:
    `java -cp classfiles:lib/jackson-core-2.12.5.jar:lib/jackson-databind-2.12.5.jar:lib/jackson-annotations-2.12.5.jar AggregationServer <port number>`


To start a Content Server:
    - With default arguments, run `make content`
    - With custom arguments, run:
    `java -cp classfiles:lib/jackson-core-2.12.5.jar:lib/jackson-databind-2.12.5.jar:lib/jackson-annotations-2.12.5.jar ContentServer <server name>:<port number> <path to input file>`
    E.g. `localhost:4444 content-server-input/weather-data1.txt`

To start a GET Client:
    - With default arguments, run `make client`
    - With custom arguments, run:
    `java -cp classfiles:lib/jackson-core-2.12.5.jar:lib/jackson-databind-2.12.5.jar:lib/jackson-annotations-2.12.5.jar GETClient <server name>:<port number>`
    E.g. `localhost:4444`


-----------

TESTING PLAN

The test cases currently planned for automated testing implementation:
    - 200 OK server response works
    - 204 No Content can be verified by the aggregation server determining that there is no JSON content 
    - Bad server responses retry request three times and then fail if still unsuccessful
        - If a subsequent request is successful, operation continues as expected
    - When client crashes before sending request, it can detect and resend the pending request on startup
    - When Server crashes before sending server response, it can detect and resend the pending request on startup
    - Client and Server don't try to process a pending request on startup if there isn’t one
    - 201 is sent on first update from content server
    - Multiple input entries in the same data file can be sent by content server
    - Aggregation server can handle requests in quick succession
    - Aggregation server responds with the latest entry by clock time to GET client