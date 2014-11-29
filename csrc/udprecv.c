//===================================================== file = mclient.c =====
//=  A multicast client to receive multicast datagrams                       =
//============================================================================
//=  Notes:                                                                  =
//=    1) This program receives on a multicast address and outputs the       =
//=       received buffer to the screen.                                     =
//=    2) Conditionally compiles for Winsock                                 =
//=    3) The multicast group address is GROUP_ADDR.                         =
//=--------------------------------------------------------------------------=
//=  Build: gcc -o udprecv udprecv.o -g -Wall -lws2_32 -lAdvapi32
//============================================================================
//#define  WIN                // WIN for Winsock and BSD for BSD sockets

//----- Include files -------------------------------------------------------
#include <stdio.h>          // Needed for printf()
#include <stdlib.h>         // Needed for memcpy()
#ifdef WIN
#include "Ws2tcpip.h"
#endif
#ifdef BSD
#include <sys/types.h>    // Needed for system defined identifiers.
#include <netinet/in.h>   // Needed for internet address structure.
#include <sys/socket.h>   // Needed for socket(), bind(), etc...
#include <arpa/inet.h>    // Needed for inet_ntoa()
#include <fcntl.h>
#include <netdb.h>
#endif

//----- Defines -------------------------------------------------------------
#define PORT_NUM         6001             // Port number used
#define GROUP_ADDR "239.0.0.1"            // Address of the multicast group
#define MAXBUFSIZE 65536 // Max UDP Packet size is 64 Kbyte

//===== Main program ========================================================
int main(void)
{
#ifdef WIN
	WSADATA wsaData;                        // Stuff for WSA functions
#endif

	unsigned int sock; // Multicast socket descriptor
	struct sockaddr_in saddr;
	struct ip_mreq imreq;

	int status, socklen;
	char buffer[MAXBUFSIZE];

	// set content of struct saddr and imreq to zero
	memset(&saddr, 0, sizeof(struct sockaddr_in));
	memset(&imreq, 0, sizeof(struct ip_mreq));

#ifdef WIN
	WSAStartup(MAKEWORD(1, 1), &wsaData);
	printf("WSAStartup OK\n");
#endif

	// open a UDP socket
	sock = socket(AF_INET, SOCK_DGRAM, 0);
	if (sock < 0)
		perror("Error creating socket"), exit(0);
	printf("socket OK\n");
	fflush(stdout); 

	int reuse = 1;
	if (setsockopt(sock, SOL_SOCKET, SO_REUSEADDR, (char *)&reuse, sizeof
		(reuse)) < 0)
	{
		perror("Setting SO_REUSEADDR error");
		exit(0);
	}
	printf("Setting SO_REUSEADDR...OK.\n");

	// bind socket
	saddr.sin_family = AF_INET;
	saddr.sin_port = htons(PORT_NUM); // listen on port 
	saddr.sin_addr.s_addr = htonl(INADDR_ANY); // bind socket to any interface
	status = bind(sock, (struct sockaddr *)&saddr, sizeof(struct sockaddr_in));
	if (status < 0)
		perror("Error binding socket to interface"), exit(0);
	printf("bind OK\n");
	fflush(stdout);

	// JOIN multicast group on default interface
	imreq.imr_multiaddr.s_addr = inet_addr(GROUP_ADDR);
	imreq.imr_interface.s_addr = INADDR_ANY; // use DEFAULT interface
	status = setsockopt(sock, IPPROTO_IP, IP_ADD_MEMBERSHIP,
		(const char *)&imreq, sizeof(struct ip_mreq));
	printf("joined group %s on port %d OK\n", GROUP_ADDR, PORT_NUM);
	fflush(stdout);

	// receive packet from socket
	for (;;){
		printf("waiting\n");
		fflush(stdout);
		socklen = sizeof(struct sockaddr_in);
		status = recvfrom(sock, buffer, MAXBUFSIZE, 0,
			(struct sockaddr *)&saddr, &socklen);

		printf("received %s\n", buffer);
		fflush(stdout);
	}
	// shutdown socket
	shutdown(sock, 2);

	// Close and clean-up
#ifdef WIN
	closesocket(sock);
	WSACleanup();
#endif
#ifdef BSD
	close(sock);
#endif

	return 0;
}