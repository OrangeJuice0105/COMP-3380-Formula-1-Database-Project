## This Makefile will be only be used to run on Aviary only.

.PHONY: build run clean

CLASSPATH = .:mssql-jdbc-13.4.0.jre11.jar

build:
	javac -cp $(CLASSPATH) *.java

run: build
	java -cp $(CLASSPATH) SQLServer

clean:
	rm -f *.class