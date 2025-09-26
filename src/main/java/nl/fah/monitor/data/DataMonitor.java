package nl.fah.monitor.data;

/**
 *
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
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.StringReader;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.Vector;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;

public class DataMonitor extends JFrame {
    Logger logger = LoggerFactory.getLogger(nl.fah.monitor.data.DataMonitor.class);
    final nl.fah.monitor.data.MessageModel tableData = new nl.fah.monitor.data.MessageModel();
    JTable table = new JTable(tableData);

    int nrOfMsgs = 0;

    final nl.fah.monitor.message.MessageModel tableMessageData = new nl.fah.monitor.message.MessageModel();
    JTable tableMessage = new JTable(tableMessageData);

    HashMap<Integer, String> dataStore = new HashMap<Integer, String>();
    HashMap<Integer, Timestamp> dataTimeStore = new HashMap<Integer, Timestamp>();

    Label infoLabel;
    JTextField ipTextField;
    JTextField portTextField;
    protected MulticastSocket socket = null;
    protected byte[] buf = new byte[10*1024];
    sharedProcess sharedData = new sharedProcess();

    Thread sharedDataThread = new Thread(sharedData);
    Thread receiverThread = new Thread(new receiverProcess(sharedData));
    Thread updateGuiThread = new Thread(new updateGuiProcess(sharedData));


    boolean pause = true;
    String multicast = "239.0.0.4";
    int port = 5474;

    private class receiverProcess implements Runnable {
        sharedProcess sharedData = null;

        public receiverProcess(sharedProcess sharedData) {
            this.sharedData = sharedData;
        }

        @Override
        public void run() {
            logger.info("receiverProcess started");

            ipTextField = new JTextField(multicast, 8);
            portTextField = new JTextField(String.valueOf(port), 4);

            try {
                socket = new MulticastSocket(Integer.parseInt(portTextField.getText()));
            } catch (IOException e) {
                logger.error(e.getLocalizedMessage());
            }
            InetAddress group = null;
            try {
                group = InetAddress.getByName(ipTextField.getText());
            } catch (UnknownHostException e) {
                logger.error(e.getLocalizedMessage());
            }
            try {
                socket.joinGroup(group);
            } catch (IOException e) {
                logger.error(e.getLocalizedMessage());
            }

            while (true) {
                logger.debug("checking pause cmd");
                while(!pause){
                    logger.info("listening for data");
                    DatagramPacket packet = new DatagramPacket(buf, buf.length);
                    try {
                        socket.receive(packet);
                    } catch (IOException e) {
                        logger.error(e.getLocalizedMessage());
                    }
                    String received = new String(
                            packet.getData(), 0, packet.getLength());
                    String hSender = packet.getAddress().getHostAddress();
                    int hPort = packet.getPort();
                    logger.info("receiverProcess received: " + received);
                    sharedData.putData(received, hSender, hPort);

                    if ("end".equals(received)) {
                        try {
                            socket.leaveGroup(group);
                        } catch (IOException e) {
                            logger.error(e.getLocalizedMessage());
                        }
                        socket.close();
                        break;
                    }
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    logger.error(e.getLocalizedMessage());
                }
            }
        }
    }

    private class updateGuiProcess implements Runnable {
        Logger logger = LoggerFactory.getLogger(DataMonitor.class);

        String last_data = "xxxx";

        long timeLastMouseEvent;

        sharedProcess sharedData;
        Timestamp timestamp = null;
        long sequence = -1;
        String xml = null;

        public updateGuiProcess(sharedProcess sharedData) {
            this.sharedData = sharedData;
        }

        public void run() {
            table.addMouseListener( new MouseAdapter(){

                @Override
                public void mouseClicked(MouseEvent e)
                {
                    Long n = (new Date().getTime() - timeLastMouseEvent);
                    logger.info("mouse clicked");

                    if (e.getClickCount() == 2 && !e.isConsumed()) {
                        logger.info("double click");
                    }
                    else{
                        //only accept single click if time last
                      if   ( n > 1000)
                          logger.info("single click");
                    }

                    timeLastMouseEvent = new Date().getTime();

                    String clickCnt = " clickcount=" + e.getClickCount();
                    logger.info(e.getSource().getClass() + clickCnt + " position clicked = " + table.rowAtPoint(e.getPoint())  + "," +  table.columnAtPoint(e.getPoint()) );
                }


            });

            while(true){
                if (!pause) {
                    logger.debug("UpdateGuiProcess running");

                    if ( (sharedData != null)
                            && (sharedData.getData() != null))
                    {
                        if (sequence != sharedData.getSequence()){
                            logger.info("sequence: " + sharedData.getSequence());
                            last_data = xml;
                            xml = sharedData.getData();
                            timestamp = sharedData.getTimeValidity();
                            sequence = sharedData.getSequence();
                            logger.info(xml);
                            String interfaceName = "eth0";
                            updateData(xml, interfaceName);
                            Date date = new Date();
                            long time = date.getTime();
                            //Passed the milliseconds to constructor of Timestamp class
                            Timestamp ts = new Timestamp(time);
                            UpdateMessageTable(xml, ts);

                            logger.debug(ts.toString());
                            dataStore.put(nrOfMsgs, xml);
                            dataTimeStore.put(nrOfMsgs, ts);
                            nrOfMsgs++;
                            logger.info("datastore size: " + dataStore.size());
                        }
                        else
                        {
                            logger.debug("no new data received");
                            try {
                                Thread.sleep(5);
                            } catch (InterruptedException e) {
                                logger.error(e.getLocalizedMessage());
                            }
                        }
                    }
                    else
                    {
                        try {
                            Thread.sleep(5);
                        } catch (InterruptedException e) {
                            logger.error(e.getLocalizedMessage());
                        }
                    }
                }
                else
                {
                    logger.debug("UpdateGuiProcess paused");
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        logger.error(e.getLocalizedMessage());
                    }
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void UpdateMessageTable(String received, Timestamp ts) {
        // do some XML parsing
        DocumentBuilderFactory dbf =
                DocumentBuilderFactory.newInstance();
        DocumentBuilder db = null;
        Document doc = null;
        InputSource is = new InputSource();
        is.setCharacterStream(new StringReader(received.trim()));

        StringBuilder m = new StringBuilder();
        Validator.ValidateSource(received.trim(), "src/main/resources/data.xsd", m);
        logger.debug("validator output: " + m.toString());

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

            tableMessageData.clearData();
            for (int j = 0; j < nodes.item(0).getChildNodes().getLength(); j++) {
                if (nodes.item(0).getChildNodes().item(j).getNodeName().contentEquals("header")) {

                    for (int jj = 0; jj < nodes.item(0).getChildNodes().item(j).getChildNodes().getLength(); jj++) {
                        Node node = nodes.item(0).getChildNodes().item(j).getChildNodes().item(jj);
                        if (node.getTextContent() != null && !node.getTextContent().isEmpty() && !node.getNodeName().contentEquals("#text")) {

                            if (node.getNodeName().contentEquals(Types.DATA_NAME)) {
                                String dataName = node.getTextContent();

                                Vector v = new Vector();
                                v.add("MESSAGE");
                                v.add("TEXT");
                                v.add(dataName);
                                tableMessageData.addText(v);

                            } else if (node.getNodeName().contentEquals(Types.DATA_ID)) {
                                String dataId = node.getTextContent();
                                Vector v = new Vector();
                                v.add("ID");
                                v.add("TEXT");
                                v.add(dataId);
                                tableMessageData.addText(v);
                            } else if (node.getNodeName().contentEquals(Types.DATA_KEY)) {
                                String dataKey = node.getTextContent();
                            } else if (node.getNodeName().contentEquals(Types.DATA_TYPE)) {
                                String dataType = node.getTextContent();
                                Vector v = new Vector();
                                v.add("TYPE");
                                v.add("TEXT");
                                v.add(dataType);
                                tableMessageData.addText(v);
                            }
                        }
                    }
                } else if (nodes.item(0).getChildNodes().item(j).getNodeName().contentEquals("payload")) {
                    Node payload = nodes.item(0).getChildNodes().item(j);

                    //Date date = new Date();
                    //long time = date.getTime();
                    //Passed the milliseconds to constructor of Timestamp class
                    //Timestamp ts = new Timestamp(time);

                    logger.debug(ts.toString());

                    Vector v2 = new Vector();
                    v2.add("TIME");
                    v2.add("TEXT");
                    v2.add(ts.toString());
                    tableMessageData.addText(v2);

                    logger.debug("nr. of payload items:" + payload.getChildNodes().getLength());
                    for (int k = 0; k < payload.getChildNodes().getLength(); k++) {
                        if (payload.getChildNodes().item(k).getNodeName().contentEquals(Types.DATA_ITEM)) {
                            NamedNodeMap namedNodeMap = payload.getChildNodes().item(k).getAttributes();

                            logger.debug(namedNodeMap.getNamedItem(Types.DATA_NAME).getNodeValue() +
                                    "  value: " + namedNodeMap.getNamedItem(Types.DATA_VALUE).getNodeValue() +
                                    "  type: " + namedNodeMap.getNamedItem(Types.DATA_TYPE).getNodeValue());

                            Vector v = new Vector();
                            v.add(namedNodeMap.getNamedItem(Types.DATA_NAME).getNodeValue());
                            v.add(namedNodeMap.getNamedItem(Types.DATA_TYPE).getNodeValue());
                            v.add(namedNodeMap.getNamedItem(Types.DATA_VALUE).getNodeValue());
                            tableMessageData.addText(v);

                            if (namedNodeMap.getNamedItem(Types.DATA_RANGE) != null)
                                logger.debug("  range: " + namedNodeMap.getNamedItem(Types.DATA_RANGE).getNodeValue());
                        }
                    }
                }
            }
        } else {
            logger.info("nodes==null or empty");
        }
    }

    @SuppressWarnings("unchecked")
    private void updateData(String received, String ifName) {
        logger.info("updating gui");

        boolean TimeOut = false;

        if (!TimeOut) {

            java.util.Date date = new java.util.Date();
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

            String dataName = null;
            String dataKey = "";
            String dataType = "";
            String dataId = "";
            int dataSize = 0;
            String dataSender = ifName;

            if (nodes != null && (nodes.getLength() == 1)) {

                for (int j = 0; j < nodes.item(0).getChildNodes().getLength(); j++) {
                    if (nodes.item(0).getChildNodes().item(j).getNodeName().contentEquals("header")) {

                        for (int jj = 0; jj < nodes.item(0).getChildNodes().item(j).getChildNodes().getLength(); jj++) {
                            Node node = nodes.item(0).getChildNodes().item(j).getChildNodes().item((jj));
                            logger.debug(jj + ". [" + node.getNodeName() + "]");
                            if (node != null && node.getTextContent() != null && !node.getTextContent().isEmpty() && !node.getNodeName().contentEquals("#text")) {

                                if (node.getNodeName().contentEquals("name")) {
                                    logger.debug("NAME=" + node.getTextContent());
                                    dataName = node.getTextContent();
                                }
                                if (node.getNodeName().contentEquals("id")) {
                                    logger.debug("ID=" + node.getTextContent());
                                    dataId = node.getTextContent();
                                }

                                if (node.getNodeName().contentEquals("type")) {
                                    logger.debug("TYPE=" + node.getTextContent());
                                    dataType = node.getTextContent();
                                }

                                if (node.getNodeName().contentEquals("key")) {
                                    logger.debug("KEY=" + node.getTextContent());
                                    dataKey = node.getTextContent();
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
                String data = received.trim();

                Vector v = new Vector();
                v.add((new Timestamp(date.getTime())).toString());
                v.add(dataName);
                v.add(dataSender);
                v.add(dataSize);

                v.add(data);
                tableData.addText(v);
            }
        }
    }


    public void start(){
        sharedDataThread.setPriority(Thread.MAX_PRIORITY);
        sharedDataThread.start();

        updateGuiThread.setPriority(Thread.MAX_PRIORITY);
        updateGuiThread.start();

        receiverThread.setPriority(Thread.MAX_PRIORITY);
        receiverThread.start();

    }

    void initDataMonitor(){
        tableMessage.setDefaultRenderer(Object.class, new DefaultTableCellRenderer()
        {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
            {
                final Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (row > 3) c.setBackground(row % 2 == 0 ?  new Color(230,240,230) : Color.WHITE);
                else{
                    c.setFont( new Font(c.getFont().getName(), Font.BOLD, c.getFont().getSize()) );
                    c.setBackground(  Color.LIGHT_GRAY );
                }
                return c;
            }
        });

        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer()
        {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
            {
                final Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                c.setBackground(row % 2 == 0 ? new Color(230,240,230)  : Color.WHITE);
                return c;
            }
        });

        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

            @Override
            public void valueChanged(ListSelectionEvent e) {

                // JUST IGNORE WHEN USER HAS ATLEAST ONE SELECTION
                if(e.getValueIsAdjusting())
                {
                    return;
                }
                ListSelectionModel lsm=(ListSelectionModel) e.getSource();

                if(lsm.isSelectionEmpty())
                {
              //      JOptionPane.showMessageDialog(null, "No selection");
                }else
                {
                    int selectedRow=lsm.getMinSelectionIndex();
                    String xml = dataStore.get(selectedRow);
                    Timestamp ts = dataTimeStore.get(selectedRow);
                    logger.debug("row " + selectedRow + " has stored: " + xml);
                    UpdateMessageTable(xml, ts);

                }
            }
        });

    }

    public DataMonitor() {
        setLayout(new BorderLayout());
        initDataMonitor();
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
                dataStore.clear();
                sharedData.clearSequence();
                nrOfMsgs = 0;
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
        Panel mainPanel = new Panel();

        JScrollPane scrollDataPane = new JScrollPane(table);
        scrollDataPane.setPreferredSize(new Dimension(460, 240));
        mainPanel.add(scrollDataPane, BorderLayout.NORTH);

        JScrollPane scrollMessagePane = new JScrollPane(tableMessage);
        scrollMessagePane.setPreferredSize(new Dimension(460, 320));
        mainPanel.add(scrollMessagePane, BorderLayout.SOUTH);
        add(mainPanel, BorderLayout.CENTER);


        infoLabel = new Label();
        infoLabel.setText("idle");

        add(infoLabel, BorderLayout.SOUTH);

    }

}