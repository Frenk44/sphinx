package nl.fah.persistence;

import nl.fah.common.Types;
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
import java.io.InputStream;
import java.io.StringReader;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Properties;

/**
 * Created by Haulussy on 19-11-2014.
 *
 * The persistence daemon operates like the context daemon,
 *  however xml-data is stored on disk (REST db)
 */
public class PersistenceDaemon {

    static DaemonStorage storage;
    static String multicast = "239.0.0.5";
    static String fileStore = "C://tmp/sphinx";
    static int port = 12345;
    static String data;
    static String propFile = "/sphinx.properties";

    Logger logger = LoggerFactory.getLogger(PersistenceDaemon.class);

    private static class DaemonStorageProcess implements Runnable {
        DatagramPacket packet;
        String dataId;
        String dataKey;
        String dataName;
        String dataType;

        Logger logger = LoggerFactory.getLogger(PersistenceDaemon.class);

        public void run() {
            logger.info("Persistence DaemonStorageProcess start");

            InputStream is = DaemonStorageProcess.class.getResourceAsStream(propFile);
            Properties prop = new Properties();
            logger.info("reading: " + propFile);
            try {
                if(is != null && is.available()>0) {
                    prop.load(is);
                    multicast = prop.getProperty(Types.CONFIG_PERSISTENCE_DAEMON_IP);
                    logger.info("multicast=" + multicast);
                    port = Integer.parseInt(prop.getProperty(Types.CONFIG_PERSISTENCE_DAEMON_PORT));
                    logger.info("port=" + port);
                    fileStore = prop.getProperty(Types.CONFIG_PERSISTENCE_DAEMON_FILESTORE);
                    logger.info("fileStore=" + fileStore);
                }
                else{
                    logger.info("empty file or not existing: " + propFile);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            storage = new DaemonStorageImpl();
            storage.SetFileLocation(fileStore);
            storage.load();

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
                logger.info("Persistence Daemon update");

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
                InputSource iss = new InputSource();
                iss.setCharacterStream(new StringReader(data));

                try {
                    db = dbf.newDocumentBuilder();
                    doc = db.parse(iss);
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
                        Collection<String> allData = storage.getAll();

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

                                    if (nnn.getNodeName().contentEquals(Types.DATA_NAME)){
                                        dataName = nnn.getTextContent();
                                    }
                                    else if (nnn.getNodeName().contentEquals(Types.DATA_ID)){
                                        dataId = nnn.getTextContent();
                                    }
                                    else if (nnn.getNodeName().contentEquals(Types.DATA_KEY)){
                                        dataKey = nnn.getTextContent();
                                    }
                                    else if (nnn.getNodeName().contentEquals(Types.DATA_TYPE)){
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

                if (dataType != null && dataType.contentEquals(Types.MSG_TYPE_PERSISTENT) && dataKey != null && !dataKey.isEmpty() && dataKey.length()>0) {
                    String key = dataName + dataKey;
                    logger.info("store persistent data with key=" + key);
                    logger.info("store persistent data with data=" + data);
                    storage.put(key, data);
                }

                logger.info("number of persistent items: " + storage.getList().size());

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