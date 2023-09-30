JARFILES=lib/jackson-core-2.12.5.jar:lib/jackson-databind-2.12.5.jar:lib/jackson-annotations-2.12.5.jar

all: compile

compile: AggregationServer.java ContentServer.java GETClient.java Tests.java
	javac -cp .:${JARFILES} -d classfiles $^

server:
	java -cp classfiles:${JARFILES} AggregationServer 4567

content:
	java -cp classfiles:${JARFILES} ContentServer localhost:4567 content-server-input/weather-data1.txt

client:
	java -cp classfiles:${JARFILES} GETClient localhost:4567

test:
	java -cp classfiles:${JARFILES} Tests