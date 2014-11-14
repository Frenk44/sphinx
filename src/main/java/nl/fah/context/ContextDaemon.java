package nl.fah.context;

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
import java.util.Set;

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
        String dataKey;

        public void run() {
            // thread loop 1 Hz

            System.out.println("Context DaemonStorageProcess start");

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
                System.out.println("Context Daemon update");

                dataKey = null;


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
                    System.out.println("command received");
                    String type = cmdnodes.item(0).getAttributes().getNamedItem("type").getTextContent();
                    String value = cmdnodes.item(0).getAttributes().getNamedItem("value").getTextContent();
                    System.out.println("type=" + type);
                    System.out.println("value=" + value);

                    if (type.contentEquals("GET")
                            && value.contentEquals("LIST")){
                        Collection<String> allData = contextData.getAll();

                        System.out.println(allData.size());
                        Iterator<String> iterator = allData.iterator();
                        while(iterator.hasNext()){
                            String data = iterator.next();
                           // System.out.println(data);

                            String destIP = cmdnodes.item(0).getAttributes().getNamedItem("dest").getTextContent();
                            String destPort = cmdnodes.item(0).getAttributes().getNamedItem("port").getTextContent();

                            sendData( data, destIP, Integer.parseInt(destPort));
                       /*     try {
                                Thread.sleep(50);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
*/
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
                                        //System.out.println("NAME=" +  nnn.getTextContent());

                                    }
                                    else if (nnn.getNodeName().contentEquals("id")){
                                        //System.out.println("ID=" + nnn.getTextContent());
                                    }
                                    else if (nnn.getNodeName().contentEquals("key")){
                                        System.out.println("KEY=" + nnn.getTextContent());
                                        dataKey = nnn.getTextContent();
                                    }
                                    else if (nnn.getNodeName().contentEquals("type")){
                                        System.out.println("TYPE=" + nnn.getTextContent());
                                    }
                                }
                            }
                        }
                        /*
                        else if (nodes.item(0).getChildNodes().item(j).getNodeName().contentEquals("payload")){
                            Node payload = nodes.item(0).getChildNodes().item(j);

                            Date date= new Date();
                            //getTime() returns current time in milliseconds
                            long time = date.getTime();
                            //Passed the milliseconds to constructor of Timestamp class
                            Timestamp ts = new Timestamp(time);

                            System.out.println(ts);

                            Vector v2 = new Vector();
                            v2.add(new String("TIME"));
                            v2.add(new String("TEXT"));
                            v2.add(new String(ts.toString()));

                            System.out.println("nr. of payload items:" + payload.getChildNodes().getLength());
                            for (int k=0;k<payload.getChildNodes().getLength();k++){
                                //             System.out.println("childnode " + k);
                                //             System.out.println("   type: " + payload.getChildNodes().item(k).getNodeType());
                                //             System.out.println("   value: " + payload.getChildNodes().item(k).getNodeValue());
                                //             System.out.println("   name: " + payload.getChildNodes().item(k).getNodeName());
                                //             System.out.println("   text: " + payload.getChildNodes().item(k).getTextContent());
                                if (payload.getChildNodes().item(k).getNodeName().contentEquals("item")) {
                                    //               System.out.println("   nr. of attributes: " + payload.getChildNodes().item(k).getAttributes().getLength());
                                    NamedNodeMap aaaa = payload.getChildNodes().item(k).getAttributes();

                                    System.out.print(aaaa.getNamedItem("name").getNodeValue() +
                                            "  value: " + aaaa.getNamedItem("value").getNodeValue() +
                                            "  type: " + aaaa.getNamedItem("type").getNodeValue());


                                    Vector v = new Vector();
                                    v.add(new String(aaaa.getNamedItem("name").getNodeValue()));
                                    v.add(new String(aaaa.getNamedItem("type").getNodeValue()));
                                    v.add(new String(aaaa.getNamedItem("value").getNodeValue()));

                                    if (aaaa.getNamedItem("range") != null)
                                        System.out.print("  range: " + aaaa.getNamedItem("range").getNodeValue());

                                    System.out.println();

                                }
                            }
                        }
                        */
                    }
                }
                else{
                    System.out.println("nodes==null or empty");
                }


                if (dataKey != null && !dataKey.isEmpty() && dataKey.length()>0) {
                    System.out.println("store context data with key=" + dataKey);
                    contextData.put(dataKey, data);
                }

                Set<String> aap = contextData.getList();
                System.out.println(aap.size());

            }
        }
    }

    public static void sendData(String message, String IP, int PORT){

        String multicast = IP;
        int port = PORT;

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

        int  count = 0;
        byte[] b = message.getBytes();

        DatagramPacket dp = new DatagramPacket(b, b.length, group, port);
        try {
            s.send(dp);
        } catch (IOException e) {
            e.printStackTrace();
        }

        java.util.Date date = new java.util.Date();

        System.out.println("data send");

    }


    public static void main(String[] args) {
        //contextData = new DaemonStorageImpl();

        Thread t = new Thread(new DaemonStorageProcess());
        t.setPriority(Thread.MIN_PRIORITY);
        t.start();

    }
}
