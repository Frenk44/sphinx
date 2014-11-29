CC=g++
CFLAGS=-g -Wall -DWIN
LIBS=-lws2_32 -lAdvapi32
DEPS = 
OBJ = udpper.o udprecv.o

%.o: %.c $(DEPS)
	$(CC) -c -o $@ $< $(CFLAGS)

udpper: udpper.o
	$(CC) -o $@ $^ $(CFLAGS) $(LIBS)

udprecv: udprecv.o
	$(CC) -o $@ $^ $(CFLAGS) $(LIBS)

clean:
	rm *.o
	rm *.exe
