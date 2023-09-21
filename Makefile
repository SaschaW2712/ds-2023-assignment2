all: compile
JAVAPATH=src/main/java/com/ds/assignment2

compile: ${JAVAPATH}/AggregationServer.java ${JAVAPATH}/ContentServer.java ${JAVAPATH}/GETClient.java
	mvn compile

server:
	mvn exec:java@server -Dexec.args="4567"

content:
	mvn exec:java@content -Dexec.args="localhost:4567 target/classes/weather-data1.txt"

client:
	mvn exec:java@client -Dexec.args="localhost:4567"
