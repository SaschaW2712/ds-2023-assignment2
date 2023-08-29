all: compile
JAVAPATH=src/main/java/com/ds/assignment2
CLASSESPATH=target/classes

compile: ${JAVAPATH}/AggregationServer.java ${JAVAPATH}/ContentServer.java ${JAVAPATH}/GETClient.java
	mvn compile

server: ${CLASSESPATH}/AggregationServer.class
	java -classpath ${CLASSESPATH} AggregationServer

content: ${CLASSESPATH}/AggregationServer.class ${CLASSESPATH}/ContentServer.class
	java -classpath ${CLASSESPATH} ContentServer

client:  ${CLASSESPATH}/AggregationServer.class ${CLASSESPATH}/GETClient.class
	java -classpath ${CLASSESPATH} GETClient