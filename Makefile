all: compile

compile: AggregationServer.java GETClient.java
	javac -d classfiles $^

server: compile
	java -classpath classfiles AggregationServer

client: compile
	java -classpath classfiles GETClient