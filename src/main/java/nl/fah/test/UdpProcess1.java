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

public class UdpProcess1 {
    static String multicast = "239.0.0.1";
    static int port = 6001;

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
                            "        <name>persoonsgegevens</name>\n" +
                            "        <type>PERIODIC</type>\n" +
                            "    </header>\n" +
                            "    <payload>\n" +
                            "        <item name='voornaam' value='Jan' type='text' />\n" +
                            "        <item name='achternaam' value='Struik' type='text' />\n" +
                            "        <item name='geslacht' value='man' type='enum' range='man,vrouw,onbekend' />\n" +
                            "        <item name='leeftijd' value='26' type='number' min='0' max='100' />\n" +
                            "        <item name='straatnaam' value='Eilardaheerd' type='text' />\n" +
                            "        <item name='huisnummer' value='28c' type='text' />\n" +
                            "        <item name='postcode' value='9736BC' type='text' />\n" +
                            "        <item name='stad' value='Groningen' type='text' range='Groningen,Assen,Leeuwarden'/>\n" +
                            "    </payload>\n" +
                            "</data>\n";
                    byte[] b = msg.getBytes();

                    DatagramPacket  dp = new DatagramPacket(b , b.length , group , port);
                    try {
                        s.send(dp);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    java.util.Date date= new java.util.Date();


                    System.out.println("data send");
                    Thread.sleep(4000);
                } catch (InterruptedException e) { e.printStackTrace();}
            }
        }
    }

    public static void main(String args[])
    {
        Thread t = new Thread(new WriteProcess());
        t.setPriority(Thread.MAX_PRIORITY);
        t.start();
    }


}
