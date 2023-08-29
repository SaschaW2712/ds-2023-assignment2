all: compile
JAVAPATH=src/main/java/com/ds/assignment2
CLASSESPATH=target/classes
PKGNAME=com.ds.assignment2

compile: ${JAVAPATH}/AggregationServer.java ${JAVAPATH}/ContentServer.java ${JAVAPATH}/GETClient.java
	mvn compile

server:
	mvn exec:java@server -Dexec.args="8080"

content:
	mvn exec:java@content

client:
	mvn exec:java@client
