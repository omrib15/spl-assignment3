#include <stdlib.h>
#include <boost/locale.hpp>
#include <boost/thread.hpp>
#include "../include/connectionHandler.h"
#include "../include/utf8.h"
#include "../include/encoder.h"


/**
* This code assumes that the server replies the exact text the client sent it (as opposed to the practical session example)
*/

void writerFunc(ConnectionHandler* c)
{
	while (1&&c!=NULL) {

		const short bufsize = 1024;
		char buf[bufsize];
		std::cin.getline(buf, bufsize);
		std::string line(buf);
		if (!c->sendLine(line)) {
			std::cout << "Disconnected. Exiting...\n" << std::endl;
			//c->close();
			break;

		}
	}

}

void listenFunc(ConnectionHandler* c)
	{
	while (1) {
	        std::string input;

	        if (!c->getLine(input)) {
	            std::cout << "Disconnected. Exiting...\n" << std::endl;
	            break;
	        }

			int len=input.length();
			input.resize(len-1);
	        std::cout <<  input << " " << std::endl << std::endl;
	        if (input == "SYSMSG QUIT ACCEPTED") {
	            std::cout << "Exiting...\n" << std::endl;

	            c->close();

	            break;
	        }
	    }

	}


int main (int argc, char *argv[]) {

	if (argc < 3) {
        std::cerr << "Usage: " << argv[0] << " host port" << std::endl << std::endl;
        return -1;
    }
    std::string host = argv[1];
    unsigned short port = atoi(argv[2]);
    ConnectionHandler connectionHandler(host, port);
    if (!connectionHandler.connect()) {
        std::cerr << "Cannot connect to " << host << ":" << port << std::endl;
        return 1;
    }
    boost::thread writingThread(writerFunc, &connectionHandler);
    boost::thread listeningThread(listenFunc, &connectionHandler);

    listeningThread.join();


 return 0;


}






