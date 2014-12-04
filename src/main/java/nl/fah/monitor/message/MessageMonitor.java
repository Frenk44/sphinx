package nl.fah.monitor.message;

/**
 * Created by Haulussy on 27-10-2014.
 */

import nl.fah.common.Types;
import nl.fah.stimulator.Validator;
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

public class MessageMonitor extends JFrame {

    final MessageModel tableData = new MessageModel();
    JTable table = new JTable(tableData);

    String multicast = "239.0.0.5";
    int port = 12345;

    Label infoLabel;
    TextField ipTextField = new TextField(multicast);
    TextField portTextField = new TextField("12345");

    Thread t = new Thread(new InputProcess());
    boolean pause = true;

    Logger logger = LoggerFactory.getLogger(MessageMonitor.class);

    private class InputProcess implements Runnable {

        String data;
        String dataName;
        String dataId = "NOT SET";
        String dataKey = "NOT SET";
        String dataType = "NOT SET";

        long timeLastMouseEvent;

        DatagramPacket packet;

        public void run() {

            table.addMouseListener( new MouseAdapter(){

                @Override
                public void mouseClicked(MouseEvent e)
                {
                    Long n = (new Date().getTime() - timeLastMouseEvent);
                    logger.debug("mouse clicked" + n);
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
                    logger.debug("RESET");
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

                                logger.info("listening to " + multicast + ":" +  port);
                                break;
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            Thread.sleep(250);
                        }
                        else Thread.sleep(1000);
                    }

                    //innerloop
                    while(true) {
                        if (pause){
                            try {
                                logger.debug("LEAVE GROUP\n");
                                socket.leaveGroup(group);
                                logger.info("STOP LOGGING\n");
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            break;
                        }

                        dataKey = "";
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
                String received = new String(packet.getData());
                logger.debug(packet.getAddress().getHostName() + " sends\n" + received);
                UpdateTable(received);


            }
            // TODO : auto scroll down
        }

        private void UpdateTable(String received) {
            // do some XML parsing
            DocumentBuilderFactory dbf =
                    DocumentBuilderFactory.newInstance();
            DocumentBuilder db = null;
            Document doc = null;
            InputSource is = new InputSource();
            is.setCharacterStream(new StringReader(received.trim()));

            StringBuilder m = new StringBuilder();
            Validator.ValidateSource(received.trim(), "src/main/resources/data.xsd", m);
            logger.info(m.toString());

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

            if (nodes != null && (nodes.getLength() == 1)) {

                tableData.clearData();
                for (int j = 0; j < nodes.item(0).getChildNodes().getLength(); j++) {
                    if (nodes.item(0).getChildNodes().item(j).getNodeName().contentEquals("header")) {

                        for (int jj = 0; jj < nodes.item(0).getChildNodes().item(j).getChildNodes().getLength(); jj++) {
                            Node nnn = nodes.item(0).getChildNodes().item(j).getChildNodes().item(jj);
                            if (nnn.getTextContent() != null && !nnn.getTextContent().isEmpty() && !nnn.getNodeName().contentEquals("#text")) {

                                if (nnn.getNodeName().contentEquals(Types.DATA_NAME)) {
                                    dataName = nnn.getTextContent();

                                    Vector v = new Vector();
                                    v.add(new String("MESSAGE"));
                                    v.add(new String("TEXT"));
                                    v.add(new String(dataName));
                                    tableData.addText(v);

                                } else if (nnn.getNodeName().contentEquals(Types.DATA_ID)) {
                                    dataId = nnn.getTextContent();
                                    Vector v = new Vector();
                                    v.add(new String("ID"));
                                    v.add(new String("TEXT"));
                                    v.add(new String(dataId));
                                    tableData.addText(v);
                                } else if (nnn.getNodeName().contentEquals(Types.DATA_KEY)) {
                                    dataKey = nnn.getTextContent();
                                } else if (nnn.getNodeName().contentEquals(Types.DATA_TYPE)) {
                                    dataType = nnn.getTextContent();
                                    Vector v = new Vector();
                                    v.add(new String("TYPE"));
                                    v.add(new String("TEXT"));
                                    v.add(new String(dataType));
                                    tableData.addText(v);
                                }
                            }
                        }
                    } else if (nodes.item(0).getChildNodes().item(j).getNodeName().contentEquals("payload")) {
                        Node payload = nodes.item(0).getChildNodes().item(j);

                        Date date = new Date();
                        long time = date.getTime();
                        //Passed the milliseconds to constructor of Timestamp class
                        Timestamp ts = new Timestamp(time);

                        logger.debug(ts.toString());

                        Vector v2 = new Vector();
                        v2.add(new String("TIME"));
                        v2.add(new String("TEXT"));
                        v2.add(new String(ts.toString()));
                        tableData.addText(v2);

                        if (dataKey != null && !dataKey.isEmpty()) {
                            Vector v3 = new Vector();
                            v3.add(new String("KEY"));
                            v3.add(new String("TEXT"));
                            v3.add(new String(dataKey));
                            tableData.addText(v3);
                        }

                        logger.debug("nr. of payload items:" + payload.getChildNodes().getLength());
                        for (int k = 0; k < payload.getChildNodes().getLength(); k++) {
                            if (payload.getChildNodes().item(k).getNodeName().contentEquals(Types.DATA_ITEM)) {
                                NamedNodeMap aaaa = payload.getChildNodes().item(k).getAttributes();

                                logger.debug(aaaa.getNamedItem(Types.DATA_NAME).getNodeValue() +
                                        "  value: " + aaaa.getNamedItem(Types.DATA_VALUE).getNodeValue() +
                                        "  type: " + aaaa.getNamedItem(Types.DATA_TYPE).getNodeValue());

                                Vector v = new Vector();
                                v.add(new String(aaaa.getNamedItem(Types.DATA_NAME).getNodeValue()));
                                v.add(new String(aaaa.getNamedItem(Types.DATA_TYPE).getNodeValue()));
                                v.add(new String(aaaa.getNamedItem(Types.DATA_VALUE).getNodeValue()));
                                tableData.addText(v);

                                if (aaaa.getNamedItem(Types.DATA_RANGE) != null)
                                    logger.debug("  range: " + aaaa.getNamedItem(Types.DATA_RANGE).getNodeValue());
                            }
                        }
                    }
                }
            } else {
                logger.info("nodes==null or empty");
            }


        }
    }

    public void start(){
        t.start();
    }

    public MessageMonitor() {
        setLayout(new BorderLayout());

        JButton startButton = new JButton(new AbstractAction("start") {
            @Override
            public void actionPerformed(ActionEvent e) {
                logger.debug("ButtonTest::start CALLED");
                infoLabel.setText("monitoring");
                pause = false;
            }
        });

        JButton stopButton = new JButton(new AbstractAction("stop") {
            @Override
            public void actionPerformed(ActionEvent e) {
                logger.debug("ButtonTest::stop CALLED");
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

        ipTextField.setText(multicast);
        portTextField.setText(Integer.toString(port));

        Panel ControlPanel = new Panel();
        ControlPanel.add(ipTextField);
        ControlPanel.add(portTextField);
        ControlPanel.add(startButton);
        ControlPanel.add(stopButton);
        ControlPanel.add(clearButton);

        add(ControlPanel, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);

        infoLabel = new Label();
        infoLabel.setText("idle");

        add(infoLabel, BorderLayout.SOUTH);

    }

}