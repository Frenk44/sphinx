package nl.fah.test;

/**
 * Created by Haulussy on 23-10-2014.
 */


/**
 Java ECHO server with UDP sockets example
 Silver Moon (m00n.silv3r@gmail.com)
 */

import java.io.*;
        import java.net.*;

public class UdpServer{
    static String multicast = "239.0.0.5";
    private static InetAddress GROUP;
    static int PORT = 12345;

    public static void main(String args[])
    {
        MulticastSocket multicastSocket = null;

        try
        {
            GROUP = InetAddress.getByName(multicast);
            multicastSocket = new MulticastSocket(PORT);
            multicastSocket.joinGroup(GROUP);


            //buffer to receive incoming data
        //    byte[] buffer = new byte[65536];
        //    DatagramPacket incoming = new DatagramPacket(buffer, buffer.length);

            //2. Wait for an incoming data
            echo("Server socket created. Waiting for incoming data...");

            //communication loop
            while(true)
            {
                byte[] receiveData = new byte[2048];
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                multicastSocket.receive(receivePacket);

                System.out.println( new String(receivePacket.getData()) + " received from " + receivePacket.getAddress() );

            }
        }

        catch(IOException e)
        {
            System.err.println("IOException " + e);
        }
    }

    //simple function to echo data to terminal
    public static void echo(String msg)
    {
        System.out.println(msg);
    }
}