CFLAGS:=-c -Wall -Weffc++ -g
LDFLAGS:=-lboost_system -lboost_locale -lboost_filesystem -lboost_thread

all: bin/encoder.o EchoClient
	g++ -o bin/client bin/encoder.o bin/connectionHandler.o bin/echoClient.o $(LDFLAGS) 

EchoClient: bin/connectionHandler.o bin/echoClient.o
	
bin/connectionHandler.o: src/connectionHandler.cpp
	g++ $(CFLAGS) -o bin/connectionHandler.o src/connectionHandler.cpp

bin/echoClient.o: src/echoClient.cpp
	g++ $(CFLAGS) -o bin/echoClient.o src/echoClient.cpp
	
bin/encoder.o: src/encoder.cpp
	g++ $(CFLAGS) -o bin/encoder.o src/encoder.cpp
	
.PHONY: clean
clean:
	rm -f bin/*
