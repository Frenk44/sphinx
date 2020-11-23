package nl.fah.test;

/**
 * The monitor tool is a webbased program to let the user use a webbrowser
 * to connect to SPHINX multicast port and monitor xml message traffic.
 *
 */

/**
 *
 based on Java ECHO client with UDP sockets example
 Silver Moon (m00n.silv3r@gmail.com)
 */

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.*;


public class WebMonitorTool {
    static String multicast = "239.0.0.5";
    static int port = 12345;
    static String data;

    private static class InputProcess implements Runnable {

        DatagramPacket packet;

        public void run() {

            InetAddress group = null;
            MulticastSocket socket = null;
            try {
                socket = new MulticastSocket(port);
                group = InetAddress.getByName(multicast);
            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                socket.joinGroup(group);
            } catch (IOException e) {
                e.printStackTrace();
            }


            while(true) {
                try {
                    byte[] buf = new byte[10*1024];
                    packet = new DatagramPacket(buf, buf.length);
                    try {
                        socket.receive(packet);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    String received = new String(packet.getData());
                   System.out.println(packet.getAddress().getHostName() + " sends\n" + received);

                    data = received.trim();

                    Thread.sleep(10);
                } catch (InterruptedException e) { e.printStackTrace();}

            }
        }
    }

    public static void main(String args[])
    {
        Thread t = new Thread(new InputProcess());
        t.start();

        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(8001), 0);
            server.createContext("/xsl", new MyXslHandler());
            server.createContext("/sphinx", new MyHandler());

            // create handlers for xsl service

            server.setExecutor(null); // creates a default executor
            server.start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static class MyHandler implements HttpHandler {
        public void handle(HttpExchange httpExchange) throws IOException {

            String parameters= httpExchange.getRequestURI().getQuery();
            System.out.println("parameters:" + parameters);

            String response = data.toString();
            httpExchange.sendResponseHeaders(200, response.length());
            OutputStream os = httpExchange.getResponseBody();
            os.write(response.getBytes());

            os.close();
        }
    }

    static class MyXslHandler implements HttpHandler {
        public void handle(HttpExchange httpExchange) throws IOException {

            String parameters= httpExchange.getRequestURI().getQuery();
            System.out.println(parameters);

            String content = null;
            ClassLoader classloader = Thread.currentThread().getContextClassLoader();
            InputStream is = classloader.getResourceAsStream("monitor.xsl");

            is.read();
            BufferedReader in = new BufferedReader(new InputStreamReader(is));
            String line = null;
            String Xml= "";
            while((line = in.readLine()) != null) {
                Xml += line;
            }
            is.close();

            String response = Xml.toString();
            httpExchange.sendResponseHeaders(200, response.length());
            OutputStream os = httpExchange.getResponseBody();
            os.write(response.getBytes());

            os.close();
        }
    }
}
