all: compile

compile: AggregationServer.java ContentServer.java GETClient.java
	javac -d classfiles $^

server: compile
	java -classpath classfiles AggregationServer

content: compile
	java -classpath classfiles ContentServer

client: compile
	java -classpath classfiles GETClient