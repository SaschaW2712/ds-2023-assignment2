all: compile
JAVAPATH=src/main/java/com/ds/assignment2

compile: AggregationServer.java ContentServer.java GETClient.java
	javac -d classfiles $^

server:
	mvn exec:java@server -Dexec.args="4567"
	java -classpath classfiles assignment2.AggregationServer "4567"

content:
	mvn exec:java@content -Dexec.args="localhost:4567 target/classes/weather-data1.txt"
		java -classpath classfiles assignment2.ContentServer "4567"


client:
	mvn exec:java@client -Dexec.args="localhost:4567"

server: compile
	java -classpath classfiles assignment1.CalculatorServer