package nl.fah.logger;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.swing.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.StringReader;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;

/**
 * Created by Haulussy on 5-11-2014.
 *
 *  * TODO: make nice interface with record, pause, stop buttons and info panel
 */
public class LoggerTool extends JFrame {

    static DataLogger dataLogger;
    static String multicast = "239.0.0.5";
    static int port = 12345;
    static String data;

    private static void createAndShowGUI() {
        LoggerTool loggerTool = new LoggerTool();
        loggerTool.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        loggerTool.setSize(250, 100);
        loggerTool.setVisible(true);
        loggerTool.setTitle("Logger Tool");

        loggerTool.start();

    }

    public void start(){

    }

    private static class DaemonLoggerProcess implements Runnable {
        DatagramPacket packet;
        String dataId;
        String dataKey;
        String dataName;
        String dataType;

        public void run() {
            dataLogger = new DataLoggerImpl();

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

            while (true){
                System.out.println("Logger Daemon update");

                dataKey = null;

                byte[] buf = new byte[10*1024];
                packet = new DatagramPacket(buf, buf.length);
                try {
                    socket.receive(packet);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                String received = new String(packet.getData());
                //System.out.println(packet.getAddress().getHostName() + " sends\n" + received);

                data = received.trim();
                // data can be xml-data or command

                // do some XML parsing
                DocumentBuilderFactory dbf =
                        DocumentBuilderFactory.newInstance();
                DocumentBuilder db = null;
                Document doc = null;
                InputSource is = new InputSource();
                is.setCharacterStream(new StringReader(data));

                try {
                    db = dbf.newDocumentBuilder();
                    doc = db.parse(is);
                } catch (ParserConfigurationException e) {
                    e.printStackTrace();
                } catch (SAXException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }


                NodeList nodes = doc.getElementsByTagName("data");
                if (nodes != null && (nodes.getLength() ==1)){

                    for (int j=0;j<nodes.item(0).getChildNodes().getLength();j++){
                        if (nodes.item(0).getChildNodes().item(j).getNodeName().contentEquals("header")){

                            for (int jj=0;jj<nodes.item(0).getChildNodes().item(j).getChildNodes().getLength();jj++){
                                Node nnn = nodes.item(0).getChildNodes().item(j).getChildNodes().item(jj);
                                if (nnn.getTextContent() != null && !nnn.getTextContent().isEmpty() && !nnn.getNodeName().contentEquals("#text")) {

                                    if (nnn.getNodeName().contentEquals("name")){
                                        dataName = nnn.getTextContent();
                                    }
                                    else if (nnn.getNodeName().contentEquals("id")){
                                        dataId = nnn.getTextContent();
                                    }
                                    else if (nnn.getNodeName().contentEquals("key")){
                                        dataKey = nnn.getTextContent();
                                    }
                                    else if (nnn.getNodeName().contentEquals("type")){
                                        dataType = nnn.getTextContent();
                                    }
                                }
                            }
                        }
                    }
                }
                else{
                    System.out.println("nodes==null or empty");
                }

                dataLogger.log(packet.getAddress().getHostName(), dataKey, dataName, dataType, data);

                System.out.println("LOG: nr. of items: " + dataLogger.getSize());
                System.out.println(dataLogger.dumpLog());

            }
        }
    }

    public static void main(String[] args) {
        Thread t = new Thread(new DaemonLoggerProcess());
        t.setPriority(Thread.MIN_PRIORITY);
        t.start();

        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createAndShowGUI();
            }
        });


    }
}
