JARFILES=lib/jackson-core-2.12.5.jar:lib/jackson-databind-2.12.5.jar:lib/jackson-annotations-2.12.5.jar

all: compile

compile: AggregationServer.java ContentServer.java GETClient.java Tests.java
	javac -cp .:${JARFILES} -d classfiles $^

server: compile
	java -cp classfiles:${JARFILES} AggregationServer 4567

content: compile
	java -cp classfiles:${JARFILES} ContentServer localhost:4567 testWeatherData/content_server_1.txt

client: compile
	java -cp classfiles:${JARFILES} GETClient localhost:4567

test: compile
	java -cp classfiles:${JARFILES} Tests