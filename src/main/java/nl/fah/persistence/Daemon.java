package nl.fah.persistence;

import nl.fah.context.DaemonStorage;
import nl.fah.context.DaemonStorageImpl;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

public class Daemon{
    static String multicast = "239.0.0.5";
    private static InetAddress GROUP;
    static int PORT = 12345;
    static DaemonStorage ContextDaemon;

    public static void main(String args[])
    {
        MulticastSocket multicastSocket = null;
        ContextDaemon = new DaemonStorageImpl();

        try
        {
            GROUP = InetAddress.getByName(multicast);
            multicastSocket = new MulticastSocket(PORT);
            multicastSocket.joinGroup(GROUP);


            //buffer to receive incoming data
        //    byte[] buffer = new byte[65536];
        //    DatagramPacket incoming = new DatagramPacket(buffer, buffer.length);

            //2. Wait for an incoming data
            echo("Server socket created. Waiting for incoming data....");

            //communication loop
            while(true)
            {
                byte[] receiveData = new byte[2048];
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                multicastSocket.receive(receivePacket);

                System.out.println( "aaa" +  new String(receivePacket.getData()) );
                System.out.println(  " received from " + receivePacket.getAddress() );

                // TODO: store as unique KEY value!
                ContextDaemon.put("app", new String(receivePacket.getData()) );
                ContextDaemon.put("apps", new String(receivePacket.getData()) );
                System.out.println( "ccc" + ContextDaemon.getList().size());

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