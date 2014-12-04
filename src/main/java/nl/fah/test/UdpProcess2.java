package nl.fah.test;

/**
 * Created by Haulussy on 23-10-2014.
 */

/**
 Java ECHO client with UDP sockets example
 Silver Moon (m00n.silv3r@gmail.com)
 */

import java.io.*;
import java.net.*;

public class UdpProcess2 {
    static String multicast = "239.0.0.5";
    static int port = 12345;

    private static class WriteProcess implements Runnable {
        public void run() {

            InetAddress group = null;
            try {
                group = InetAddress.getByName(multicast);
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }

            //create Multicast socket to to pretending group
            MulticastSocket s = null;
            try {
                s = new MulticastSocket(port);
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (group != null && s != null) try {
                s.joinGroup(group);
            } catch (IOException e) {
                e.printStackTrace();
            }
            int count = 0;
            while(true) {
                try {
                    String msg = (String)" <?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                            "<data>\n" +
                            "    <header>\n" +
                            "        <name>adresgegevens</name>\n" +
                            "        <type>PERIODIC</type>\n" +
                            "    </header>\n" +
                            "    <payload>\n" +
                            "        <item name='straat' value='Jansteenstraat' type='text' />\n" +
                            "        <item name='postcode' value='9734BC' type='text' />\n" +
                            "        <item name='huisnummer' value='28c' type='text' />\n" +
                            "        <item name='stad' value='Groningen' type='text' />\n" +
                            "    </payload>\n" +
                            "</data>\n";
                    byte[] b = msg.getBytes();

                    DatagramPacket  dp = new DatagramPacket(b , b.length , group , port);
                    try {
                        s.send(dp);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    Thread.sleep(250);
                } catch (InterruptedException e) { e.printStackTrace();}
            }
        }
    }

    public static void main(String args[])
    {
        Thread t = new Thread(new WriteProcess());
        t.start();
    }


}
