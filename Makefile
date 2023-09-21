all: compile

compile: AggregationServer.java ContentServer.java GETClient.java
	javac -cp .:lib/jackson-core-2.12.5.jar:lib/jackson-databind-2.12.5.jar -d classfiles $^

server:
	java -cp classfiles AggregationServer