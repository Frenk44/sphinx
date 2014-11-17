package nl.fah.context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.StringReader;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Iterator;

/**
 * Created by Haulussy on 5-11-2014.
 */
public class ContextDaemon {

    static DaemonStorage contextData;
    static String multicast = "239.0.0.5";
    static int port = 12345;
    static String data;


    private static class DaemonStorageProcess implements Runnable {
        DatagramPacket packet;
        String dataId;
        String dataKey;
        String dataName;
        String dataType;

        Logger logger = LoggerFactory.getLogger(DaemonStorageProcess.class);

        public void run() {
            logger.info("Context DaemonStorageProcess start");

            contextData = new DaemonStorageImpl();

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
                logger.info("Context Daemon update");

                dataKey = null;


                byte[] buf = new byte[10*1024];
                packet = new DatagramPacket(buf, buf.length);
                try {
                    socket.receive(packet);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                String received = new String(packet.getData());
                logger.info(packet.getAddress().getHostName() + " sends\n" + received);

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

                NodeList cmdnodes = doc.getElementsByTagName("command");
                if (cmdnodes != null && (cmdnodes.getLength() == 1)) {
                    String type = cmdnodes.item(0).getAttributes().getNamedItem("type").getTextContent();
                    String value = cmdnodes.item(0).getAttributes().getNamedItem("value").getTextContent();
                    logger.info("command received, type=" + type + ", value=" + value);

                    if (type.contentEquals("GET")
                            && value.contentEquals("LIST")){
                        Collection<String> allData = contextData.getAll();

                        Iterator<String> iterator = allData.iterator();
                        while(iterator.hasNext()){
                            String data = iterator.next();

                            String destIP = cmdnodes.item(0).getAttributes().getNamedItem("dest").getTextContent();
                            String destPort = cmdnodes.item(0).getAttributes().getNamedItem("port").getTextContent();

                            sendData( data, destIP, Integer.parseInt(destPort));
                        }
                    }
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
                    logger.info("nodes==null or empty");
                }


                if (dataKey != null && !dataKey.isEmpty() && dataKey.length()>0) {
                    logger.info("store context data with key=" + dataKey);
                    contextData.put(dataKey, data);
                }

                logger.info("number of context items: " + contextData.getList().size());

            }
        }
    }

    public static void sendData(String message, String IP, int PORT){

        String multicast = IP;
        int port = PORT;
        Logger logger = LoggerFactory.getLogger(DaemonStorageProcess.class);

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

        byte[] b = message.getBytes();

        DatagramPacket dp = new DatagramPacket(b, b.length, group, port);
        try {
            s.send(dp);
        } catch (IOException e) {
            e.printStackTrace();
        }

        java.util.Date date = new java.util.Date();

        logger.info("data send");

    }

    public static void main(String[] args) {
        Thread t = new Thread(new DaemonStorageProcess());
        t.setPriority(Thread.MIN_PRIORITY);
        t.start();
    }
}
