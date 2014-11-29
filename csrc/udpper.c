#define WIN32_LEAN_AND_MEAN
#define _WIN32_WINNT 0x501

#include <windows.h>
#include <winsock2.h>
#include <ws2tcpip.h>
#include <stdlib.h>
#include <stdio.h>
#include <sys/time.h>
#include <time.h>

#define DEFAULT_BUFLEN 512

long long current_timestamp() {
	struct timeval te;
	gettimeofday(&te, NULL); // get current time
	long long milliseconds = te.tv_sec * 1000LL + te.tv_usec / 1000; // caculate milliseconds
	return milliseconds;
}

int __cdecl main(int argc, char **argv) 
{
    WSADATA wsaData;
    SOCKET ConnectSocket = INVALID_SOCKET;
    struct addrinfo *result = NULL,
                    *ptr = NULL,
                    hints;
    char sendbuf[400];
    strcpy(sendbuf,"<?xml version=\"1.0\" encoding=\"utf-8\"?><data><header><name>school</name><id></id><size>0</size><type>EVENT</type></header><payload><item name='naam' value='Willem Lodewijk' type='text' /><item name='plaats' value='Groningen' type='text' /><item name='niveau' value='VWO' type='enum' /><item name='duur' value='minder dan 1 jaar' type='enum' /></payload></data>");
    //char recvbuf[DEFAULT_BUFLEN];
    int iResult;
    //int recvbuflen = DEFAULT_BUFLEN;
    
    // Validate the parameters
    if (argc != 3) {
        printf("usage: %s server-name port\n", argv[0]);
        return 1;
    }

    // Initialize Winsock
    iResult = WSAStartup(MAKEWORD(2,2), &wsaData);
    if (iResult != 0) {
        printf("WSAStartup failed with error: %d\n", iResult);
        return 1;
    }

    ZeroMemory( &hints, sizeof(hints) );
    hints.ai_family = AF_INET;
    hints.ai_socktype = SOCK_DGRAM;
    hints.ai_protocol = IPPROTO_UDP;

    // Resolve the server address and port
    iResult = getaddrinfo(argv[1], argv[2], &hints, &result);
    if ( iResult != 0 ) {
        printf("getaddrinfo failed with error: %d\n", iResult);
        WSACleanup();
        return 1;
    }

    // Attempt to connect to an address until one succeeds
    for(ptr=result; ptr != NULL ;ptr=ptr->ai_next) {

        // Create a SOCKET for connecting to server
        ConnectSocket = socket(ptr->ai_family, ptr->ai_socktype, ptr->ai_protocol);
        if (ConnectSocket == INVALID_SOCKET) {
            printf("socket failed with error: %d\n", WSAGetLastError());
            WSACleanup();
            return 1;
        }
	int option = 1;
    int ret=setsockopt(ConnectSocket, SOL_SOCKET, SO_BROADCAST, (char*)&option, sizeof(option));

	if(ret>=0) {
        	// Connect to server.
        	iResult = connect( ConnectSocket, ptr->ai_addr, (int)ptr->ai_addrlen);
        	if (iResult == SOCKET_ERROR) {
            		closesocket(ConnectSocket);
            		ConnectSocket = INVALID_SOCKET;
            		continue;
        	}
	}
        break;
    }

    freeaddrinfo(result);

    if (ConnectSocket == INVALID_SOCKET) {
        printf("Unable to connect to server!\n");
        WSACleanup();
        return 1;
    }

    // Send an initial buffer
	long long t = current_timestamp();
	long long dt = 1000; // ms
	for (;;){
		iResult = send( ConnectSocket, sendbuf, (int)strlen(sendbuf), 0 );
		if (iResult == SOCKET_ERROR) {
			printf("send failed with error: %d\n", WSAGetLastError());
			closesocket(ConnectSocket);
			WSACleanup();
			return 1;
		}
		t = t + dt;
		printf("Sending data t=%I64u !\n", current_timestamp());
		fflush(stdout);
		Sleep(t - current_timestamp());
	}

    printf("Bytes Sent: %d\n", iResult);

    // shutdown the connection since no more data will be sent
    iResult = shutdown(ConnectSocket, SD_SEND);
    if (iResult == SOCKET_ERROR) {
        printf("shutdown failed with error: %d\n", WSAGetLastError());
        closesocket(ConnectSocket);
        WSACleanup();
        return 1;
    }

    // cleanup
    closesocket(ConnectSocket);
    WSACleanup();

    return 0;
}

