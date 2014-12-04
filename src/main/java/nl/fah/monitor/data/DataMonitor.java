package nl.fah.monitor.data;

/**
 * Created by Haulussy on 27-10-2014.
 */

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.swing.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.StringReader;
import java.net.*;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Vector;

public class DataMonitor extends JFrame {

    final MessageModel tableData = new MessageModel();
    JTable table = new JTable(tableData);
    Label infoLabel;
    JTextField ipTextField;
    JTextField portTextField;

    Thread t = new Thread(new InputProcess());
    boolean pause = true;
    String multicast = "239.0.0.5";
    int port = 12345;

    private class InputProcess implements Runnable {
        Logger logger = LoggerFactory.getLogger(DataMonitor.class);

        String data;
        String dataName;
        String dataType;
        String dataKey = "NOT SET";
        String dataId = "NOT SET";

        long timeLastMouseEvent;

        DatagramPacket packet;

        boolean isAlreadyOneClick;

        public void run() {

            table.addMouseListener( new MouseAdapter(){

                @Override
                public void mouseClicked(MouseEvent e)
                {
                    Long n = (new Date().getTime() - timeLastMouseEvent);
                    logger.debug("mouse clicked");

                    if (e.getClickCount() == 2 && !e.isConsumed()) {
                        logger.debug("double click");
                    }
                    else{
                        //only accept single click if time last
                      if   ( n > 1000)
                          logger.debug("single click");
                    }

                    timeLastMouseEvent = new Date().getTime();

                    String aap = " clickcount=" + e.getClickCount();
                    logger.debug(e.getSource().getClass() + aap + " position clicked = " + table.rowAtPoint(e.getPoint())  + "," +  table.columnAtPoint(e.getPoint()) );
                }


            });

            InetAddress group = null;
            MulticastSocket socket = null;

            // outerloop
            while(true) {
                try {
                    while(true){
                        if (!pause){

                            multicast = ipTextField.getText();
                            port = Integer.parseInt( portTextField.getText() );
                            logger.info("START MONITORING ON " + multicast + ":" + port);
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

                            break;
                        }
                        else Thread.sleep(250);
                    }

                    //innerloop
                    while(true) {
                        if (pause){
                            try {
                                socket.leaveGroup(group);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            break;
                        }
                        update(socket);
                        Thread.sleep(10);
                    }
                } catch (InterruptedException e) { e.printStackTrace();}
            }

        }

        private void update(MulticastSocket socket) {
            boolean TimeOut = false;
            byte[] buf = new byte[10*1024];
            packet = new DatagramPacket(buf, buf.length);
            try {
                TimeOut = false;
                socket.setSoTimeout(10);
                socket.receive(packet);
            } catch (SocketTimeoutException e) {
                TimeOut = true;
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (!TimeOut) {

                java.util.Date date = new java.util.Date();
                String received = new String(packet.getData());
                logger.info(packet.getAddress().getHostName() + " sends\n" + received);

                // do some XML parsing
                DocumentBuilderFactory dbf =
                        DocumentBuilderFactory.newInstance();
                DocumentBuilder db = null;
                Document doc = null;
                InputSource is = new InputSource();
                is.setCharacterStream(new StringReader(received.trim()));

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

                dataName = null;
                dataKey = "";
                dataType = "";
                dataId = "";

                if (nodes != null && (nodes.getLength() == 1)) {

                    for (int j = 0; j < nodes.item(0).getChildNodes().getLength(); j++) {
                        if (nodes.item(0).getChildNodes().item(j).getNodeName().contentEquals("header")) {

                            for (int jj = 0; jj < nodes.item(0).getChildNodes().item(j).getChildNodes().getLength(); jj++) {
                                Node nnn = nodes.item(0).getChildNodes().item(j).getChildNodes().item((jj));
                                logger.debug(jj + ". [" + nnn.getNodeName() + "]");
                                if (nnn != null && nnn.getTextContent() != null && !nnn.getTextContent().isEmpty() && !nnn.getNodeName().contentEquals("#text")) {

                                    if (nnn.getNodeName().contentEquals("name")) {
                                        logger.debug("NAME=" + nnn.getTextContent());
                                        dataName = nnn.getTextContent();

                                    }
                                    if (nnn.getNodeName().contentEquals("id")) {
                                        logger.debug("ID=" + nnn.getTextContent());
                                        dataId = nnn.getTextContent();
                                    }

                                    if (nnn.getNodeName().contentEquals("type")) {
                                        logger.debug("TYPE=" + nnn.getTextContent());
                                        dataType = nnn.getTextContent();
                                    }

                                    if (nnn.getNodeName().contentEquals("key")) {
                                        logger.debug("KEY=" + nnn.getTextContent());
                                        dataKey = nnn.getTextContent();
                                    }
                                }
                            }
                        } else if (nodes.item(0).getChildNodes().item(j).getNodeName().contentEquals("payload")) {
                            Node payload = nodes.item(0).getChildNodes().item(j);

                            logger.debug("nr. of payload items:" + payload.getChildNodes().getLength());
                            for (int k = 0; k < payload.getChildNodes().getLength(); k++) {
                                logger.debug("childnode " + k);
                                logger.debug("   type: " + payload.getChildNodes().item(k).getNodeType());
                                logger.debug("   value: " + payload.getChildNodes().item(k).getNodeValue());
                                logger.debug("   name: " + payload.getChildNodes().item(k).getNodeName());
                                logger.debug("   text: " + payload.getChildNodes().item(k).getTextContent());
                                if (payload.getChildNodes().item(k).getNodeName().contentEquals("item")) {
                                    logger.debug("   nr. of attributes: " + payload.getChildNodes().item(k).getAttributes().getLength());
                                    NamedNodeMap namedNodeMap = payload.getChildNodes().item(k).getAttributes();

                                    logger.debug(namedNodeMap.getNamedItem("name").getNodeValue() +
                                            "  value: " + namedNodeMap.getNamedItem("value").getNodeValue() +
                                            "  type: " + namedNodeMap.getNamedItem("type").getNodeValue());
                                    if (namedNodeMap.getNamedItem("range") != null)
                                        logger.debug("  range: " + namedNodeMap.getNamedItem("range").getNodeValue());
                                }
                            }
                        }
                    }
                } else {
                    logger.debug("nodes==null or empty");
                }

                if (dataName != null) {
                    data = received.trim();
                    Vector v = new Vector();
                    v.add((new Timestamp(date.getTime())).toString());
                    v.add(new String(dataName));
                    v.add(new String(dataType));
                    v.add(new String(dataKey));

                    v.add(new String(data));
                    tableData.addText(v);
                }
                // TODO : auto scroll down
            }
            else{
                // time out
            }
        }
    }


    public void start(){
        t.start();
    }

    public DataMonitor() {
        setLayout(new BorderLayout());

        JButton startButton = new JButton(new AbstractAction("start") {
            @Override
            public void actionPerformed(ActionEvent e) {
                infoLabel.setText("monitoring");
                pause = false;
            }
        });

        JButton stopButton = new JButton(new AbstractAction("stop") {
            @Override
            public void actionPerformed(ActionEvent e) {
                infoLabel.setText("stopped");
                pause = true;
            }
        });

        JButton clearButton = new JButton(new AbstractAction("clear") {
            @Override
            public void actionPerformed(ActionEvent e) {
                MessageModel dm = (MessageModel)table.getModel();
                dm.clearData();
            }
        });

        JButton contextButton = new JButton(new AbstractAction("context") {
            Logger logger = LoggerFactory.getLogger(DataMonitor.class);

            @Override
            public void actionPerformed(ActionEvent e) {
                String message = "<command type=\"GET\" value=\"LIST\" dest=\""
                        + multicast+"\" port=\"" + port + "\" />";

                InetAddress group = null;
                try {
                    group = InetAddress.getByName(multicast);
                } catch (UnknownHostException e6) {
                    e6.printStackTrace();
                }

                //create Multicast socket to to pretending group
                MulticastSocket s = null;
                try {
                    s = new MulticastSocket(port);
                } catch (IOException e7) {
                    e7.printStackTrace();
                }
                if (group != null && s != null) try {
                    s.joinGroup(group);
                } catch (IOException e8) {
                    e8.printStackTrace();
                }

                int  count = 0;
                byte[] b = message.getBytes();

                DatagramPacket dp = new DatagramPacket(b, b.length, group, port);
                try {
                    s.send(dp);
                } catch (IOException e9) {
                    e9.printStackTrace();
                }

                java.util.Date date = new java.util.Date();
                logger.debug("context cmd send");

            }

        });

        ipTextField = new JTextField(multicast, 8);
        portTextField = new JTextField(String.valueOf(port), 4);

        Panel ControlPanel = new Panel();
        ControlPanel.add(ipTextField);
        ControlPanel.add(portTextField);
        ControlPanel.add(startButton);
        ControlPanel.add(stopButton);
        ControlPanel.add(clearButton);
        ControlPanel.add(contextButton);

        Panel InfoPanel = new Panel();
        add(InfoPanel, BorderLayout.SOUTH);

        add(ControlPanel, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);

        infoLabel = new Label();
        infoLabel.setText("idle");

        add(infoLabel, BorderLayout.SOUTH);

    }

}