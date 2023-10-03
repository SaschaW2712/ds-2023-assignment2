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

The cases tested automatically are:
    UPDATE TEST CASES

ADD TESTING DIR ORGANISATION