package nl.fah.monitor;

import nl.fah.common.Types;
import nl.fah.common.Utils;
import nl.fah.logger.DataLogger;
import nl.fah.logger.DataLoggerImpl;
import nl.fah.monitor.data.MessageModel;
import nl.fah.stimulator.Validator;

import org.pcap4j.core.*;

import org.pcap4j.packet.Packet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Random;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.*;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static java.lang.System.exit;

public class Monitor extends JFrame {

    final nl.fah.monitor.data.MessageModel tableData = new nl.fah.monitor.data.MessageModel();
    final nl.fah.monitor.message.MessageModel tableMessageData = new nl.fah.monitor.message.MessageModel();

    JTable GUItable = new JTable(tableData);
    Enumeration<NetworkInterface> interfaces = null;
    JComboBox networkList;
    JComboBox networkSendList;
    Integer Counter = 0;

    HashMap<String, Color> colorMap;
    Random rand = new Random();

    public List<byte[]> packetList = new ArrayList<>();
    public List<Long> tvalList = new ArrayList<>();
    public List<String> senderList = new ArrayList<>();

    HashMap<Integer, String> dataStore = new HashMap<>();
    HashMap<Integer, Timestamp> dataTimeStore = new HashMap<>();

    final JInternalFrame jifMonAndStim = new JInternalFrame("Monitor and Stimulator")
    {

    };

    String[] getValues(int row, int column){

        String[] items3 = null;
        Object e = enums.get(row-4); //NOTE: 1st 4 ROWS are defined by the header!!!!
        if(e != null) {
            items3 = e.toString().split(",");
        }
        else{
            logger.debug("no values found for row: " + row);
        }
        return items3;
    }

    JTable tableMessage = new JTable(tableMessageData){

        @Override
        @SuppressWarnings("unchecked")
        public TableCellEditor getCellEditor(int row, int column)
        {
            logger.info( "row :" + row);
            logger.info( "column :" + column);
            // determine if table at pos (row,column) is of type enumeration
            String[] items = getValues(row,column);

            if (items != null && items.length > 0){
                logger.info("return CellEditor for ENUMS");

                SwingUtilities.invokeLater(new Runnable()
                {
                    public void run()
                    {
                        jifMonAndStim.updateUI();
                    }
                });

                return new DefaultCellEditor(new JComboBox(items));
            }
            else {
               if (row==1){
                   SwingUtilities.invokeLater(new Runnable()
                   {
                       public void run()
                       {
                           jifMonAndStim.updateUI();
                       }
                   });
                   return new CellEditorMessageType();
               }

               else{
                   logger.info("return default CellEditor");
                   return super.getCellEditor(row, column);
               }
            }

        }

    };


    void initD(){

        // init colorMap
        colorMap = new HashMap<>();

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

        GUItable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer()
        {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
            {
                String msgName = tableData.getValueAt(row, 1).toString();
                logger.debug("message = " + msgName);

                Color bcolor = null;

                for (String key : colorMap.keySet()) {
                    if(msgName.contentEquals(key))
                    {
                        bcolor = colorMap.get(msgName);
                    }
                }

                if(bcolor == null)
                {
                    final float hue = rand.nextFloat();
                    // Saturation between 0.1 and 0.3
                    final float saturation = (rand.nextInt(2000) + 1000) / 10000f;
                    final float luminance = 0.9f;
                    final Color color = Color.getHSBColor(hue, saturation, luminance);
                    colorMap.put(msgName, color);
                }

                final Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                c.setBackground(  bcolor );
                return c;
            }
        });
        GUItable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        GUItable.getSelectionModel().addListSelectionListener(new ListSelectionListener(){
            public void valueChanged(ListSelectionEvent event) {

                ListSelectionModel lsm=(ListSelectionModel) event.getSource();
                int selectedRow=lsm.getMinSelectionIndex();
                // do some actions here, for example
                // print first column value from selected row
                if(selectedRow>=0)
                {

                 //   String key = table.getValueAt(selectedRow, 0).toString();
                    String key = String.valueOf(selectedRow);
                    String xml = dataLogger.getPayLoad(key);
                    if( (xml != null) && !xml.isEmpty()) {
                        UpdateTable(tvalList.get(selectedRow), xml);
                    }
                    else logger.error("key " + key + " not found in logging" );
                }
            }
        });

    }

    JComboBox dataList;
    String dataName = "";

    DataLogger dataLogger = new DataLoggerImpl();

    int dataNrOfItems = 0;

    Hashtable buffer = new Hashtable();

    Label infoLabel;
    JTextField ipTextField;
    JTextField portTextField;
    JTextField ipTextFieldStim;
    JTextField portTextFieldStim;

    JComboBox dataType;
    JTextField dataKey;

    JScrollPane tableMessageScrollPane;

    boolean pause = true;
    String multicast_read = "239.10.20.1";
    int port_read = 6011;
    String multicast_write = "239.30.10.1";
    int port_write = 6001;

    Thread inputThread1;

    int nrOfLogs = 0;
    Logger logger = LoggerFactory.getLogger(Monitor.class);

    Hashtable enums = new Hashtable();

    public static boolean validIP(String ip) {
        if (ip == null || ip.isEmpty()) return false;
        ip = ip.trim();
        if ((ip.length() < 6) & (ip.length() > 15)) return false;

        try {
            Pattern pattern = Pattern.compile("^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$");
            Matcher matcher = pattern.matcher(ip);
            return matcher.matches();
        } catch (PatternSyntaxException ex) {
            return false;
        }
    }

    public static boolean validMulticast(String ip) {
        if (ip == null || ip.isEmpty()) return false;
        ip = ip.trim();
        if ((ip.length() < 6) & (ip.length() > 15)) return false;

        try {
            Pattern pattern = Pattern.compile("^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$");
            Matcher matcher = pattern.matcher(ip);
            return matcher.matches();
        } catch (PatternSyntaxException ex) {
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    public void initDataList(){
        logger.debug("initDataList and create inputThread");
        Properties prop = new Properties();
        try{
            InputStream is = new FileInputStream("sphinx.properties");
            if(is != null && is.available()>0) {
                prop.load(is);
                multicast_read = prop.getProperty(Types.CONFIG_READ_IP);
                port_read = Integer.parseInt(prop.getProperty(Types.CONFIG_READ_PORT));
                logger.info(Types.CONFIG_READ_IP + "=" + multicast_read);
                logger.info(Types.CONFIG_READ_PORT + "=" + port_read);

                multicast_write = prop.getProperty(Types.CONFIG_WRITE_IP);
                port_write = Integer.parseInt(prop.getProperty(Types.CONFIG_WRITE_PORT));
                logger.info(Types.CONFIG_WRITE_IP + "=" + multicast_write);
                logger.info(Types.CONFIG_WRITE_PORT + "=" + port_write);

                inputThread1 = new Thread(new InputProcess( "input-thread-1", multicast_read, port_read));
                logger.info("inputThread1 created");
            }
            else{
                logger.info("empty file or not existing: " + "sphinx.properties");
            }
        } catch (IOException e) {
            exit(0);
            e.printStackTrace();
        }


        URL myURL = getClass().getProtectionDomain().getCodeSource().getLocation();
        java.net.URI myURI = null;
        try {
            myURI = myURL.toURI();
        } catch (URISyntaxException e1)
        {}

        String FullPath = java.nio.file.Paths.get(myURI).toFile().toString();
        logger.info("FullPath:" + FullPath);
        String path = new File(FullPath).getParent() + "/templates/model1";
        logger.info("scanning directory:" + path);

        ArrayList<File> files;
        files = new ArrayList<File>(Arrays.asList(new File(path).listFiles()));
        logger.info("number of xml-files:" + files.size());

        String[] dataListNames = new String[files.size()];

        int j=0;
        for (int i = 0; i < files.size(); i++) {
            File ff = files.get(i);

            StringBuilder m = new StringBuilder();
            Boolean ok = Validator.Validate(path + "/" + ff.getName(), "data.xsd", m );

            if (ok) {
                logger.info(ff.getName() + " is OK" );
                dataListNames[j] = ff.getName().replace(".xml", "");
                j++;
            }
            else{
                logger.info(ff.getName() + " is NOT OK, reason=" + m );
            }
        }

        dataKey = new JTextField(6);
        dataKey.setText("");
        dataKey.setEditable(false);
        dataType = new JComboBox(new String[]{Types.MSG_TYPE_EVENT,Types.MSG_TYPE_CONTEXT,Types.MSG_TYPE_PERSISTENT});
        dataType.addActionListener(
                e -> {
                    JComboBox cb = (JComboBox) e.getSource();
                    String dataTypeString = (String) cb.getSelectedItem();
                    logger.debug("dataType:" + dataTypeString);

                    if (dataTypeString.contentEquals(Types.MSG_TYPE_EVENT)){
                        dataKey.setText("");
                        dataKey.setEditable(false);
                    }
                    else dataKey.setEditable(true);
                });

        dataList = new JComboBox(dataListNames){
            // on select of dataname, fill the table

        };


        dataList.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                JComboBox jcmbType = (JComboBox) e.getSource();
                String cmbType = (String) jcmbType.getSelectedItem();
                logger.debug("cmbType:" + cmbType);
                dataName = cmbType;

                URL myURL = getClass().getProtectionDomain().getCodeSource().getLocation();
                java.net.URI myURI = null;
                try {
                    myURI = myURL.toURI();
                } catch (URISyntaxException e1)
                {}

                String FullPath = java.nio.file.Paths.get(myURI).toFile().toString();
                String path = new File(FullPath).getParent();

                logger.info("path: " + path);
                String fname = path + "/templates/model1/" + cmbType + ".xml";
                logger.info("reading: " + fname);

                InputStream is = null;
               try {
                  is  = new FileInputStream(fname);
                } catch (IOException e1) {
                    e1.printStackTrace();
                }

                dataName = cmbType;

                String xml = Utils.getStringFromInputStream(is);
                logger.debug(xml);

                DocumentBuilderFactory dbf =
                        DocumentBuilderFactory.newInstance();
                DocumentBuilder db = null;
                try {
                    db = dbf.newDocumentBuilder();
                } catch (ParserConfigurationException e1) {
                    e1.printStackTrace();
                }
                InputSource is1 = new InputSource();
                is1.setCharacterStream(new StringReader(xml));

                Document doc = null;
                if (db != null) {
                    try {
                        doc = db.parse(is1);
                    } catch (SAXException e1) {
                        e1.printStackTrace();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }
                NodeList headernodes = doc.getElementsByTagName("header");
                logger.debug("headernodes length"  + headernodes.getLength());

                NodeList payloadnodes = doc.getElementsByTagName("payload");
                logger.debug("datanodes length:" + payloadnodes.getLength());

                Node headernode = headernodes.item(0);
                Node payloadnode = payloadnodes.item(0);

                logger.debug("headernode nr. of childs:" + headernode.getChildNodes().getLength());
                logger.debug("payloadnode nr. of childs:" + payloadnode.getChildNodes().getLength());

                for (int i = 0; i < headernode.getChildNodes().getLength(); i++) {
                    if (!headernode.getChildNodes().item(i).getNodeName().contentEquals("#text")) {
                        logger.debug(headernode.getChildNodes().item(i).getNodeName() + "=" + headernode.getChildNodes().item(i).getTextContent());
                    }
                }

                tableMessageData.clearData();
                Vector v = new Vector();
                v.add("MESSAGE");
                v.add("TEXT");
                v.add(cmbType);
                tableMessageData.addText(v);

                v = new Vector();
                v.add("TYPE");
                v.add("TEXT");
                v.add("EVENT");
                tableMessageData.addText(v);

                v = new Vector();
                v.add("TIME");
                v.add("TEXT");
                v.add(("253426356"));
                tableMessageData.addText(v);

                v = new Vector();
                v.add("KEY");
                v.add("TEXT");
                v.add("");
                tableMessageData.addText(v);

                enums.clear();
                int j=0;
                for (int i = 0; i < payloadnode.getChildNodes().getLength(); i++) {
                    if (payloadnode.getChildNodes().item(i).getNodeName().contentEquals("item")) {
                        String name = payloadnode.getChildNodes().item(i).getAttributes().getNamedItem("name").getTextContent();
                        String type = payloadnode.getChildNodes().item(i).getAttributes().getNamedItem("type").getTextContent();
                        logger.debug(name + " [" + type + "]");

                        if (type.contentEquals("enum")) {
                            String range = payloadnode.getChildNodes().item(i).getAttributes().getNamedItem("range").getTextContent();
                            logger.debug(name + " has range [" + range + "] put at location:" + i);
                            enums.put(j, range);
                        }

                        v = new Vector();
                        v.add(name);
                        v.add(type);
                        v.add(new String());

                        tableMessageData.addText(v);
                        j++;
                    }
                }

                tableMessageData.fireTableDataChanged();
                tableData.fireTableDataChanged();
            }
        });

    }

    private class InputProcess implements Runnable {

        String data;
        String dataName;
        String dataType;
        String dataKey = "NOT SET";
        String dataId = "NOT SET";
        String name;
        String ip;
        int port;

        long timeLastMouseEvent;

        DatagramPacket packet;

        boolean isAlreadyOneClick;

        public InputProcess(String name, String ip, int port)
        {
            this.name = name;
            this.ip = ip;
            this.port = port;
        }

        public void run() {

            GUItable.addMouseListener(new MouseAdapter(){

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
                    logger.debug(e.getSource().getClass() + aap + " position clicked = " + GUItable.rowAtPoint(e.getPoint())  + "," +  GUItable.columnAtPoint(e.getPoint()) );
                }


            });

            InetAddress group = null;
            MulticastSocket socket = null;
            NetworkInterface ni = null;

            // outerloop
            while(true) {
                try {
                    while(true){
                        if (!pause){

                            try {
                                ni = NetworkInterface.getByName(networkList.getSelectedItem().toString());
                            } catch (SocketException e) {
                                e.printStackTrace();
                            }

                            multicast_read = ipTextField.getText();
                            port = Integer.parseInt( portTextField.getText() );

                            try {
                                socket = new MulticastSocket(port);
                                socket.setReceiveBufferSize(10*1024);
                                group = InetAddress.getByName(multicast_read);
                                socket.setNetworkInterface(ni);

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

                    logger.info("START MONITORING ON " + multicast_read + ":" + port + " NW-interface: " + socket.getNetworkInterface().getDisplayName());

                    //innerloop
                    while(true) {
                        if (pause){
                            try {
                                socket.leaveGroup(group);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            logger.info("STOP MONITORING ON " + multicast_read + ":" + port + " NW-interface: " + socket.getNetworkInterface().getDisplayName());

                            break;
                        }
                        update(socket, ni.getName(), multicast_read, port);
                        logger.debug("sleep");
                        Thread.sleep(10);
                    }
                } catch (InterruptedException | SocketException e) { e.printStackTrace();}
            }
        }

        private void update(MulticastSocket socket, String displayName, String mc_addres, int port) {
            boolean TimeOut = false;
            byte[] buf = new byte[10*1024];
            packet = new DatagramPacket(buf, buf.length);

            try {
                TimeOut = false;
                socket.setSoTimeout(10);
                logger.debug("call socket.receive");
                socket.receive(packet);

                logger.debug("data received");
            } catch (SocketTimeoutException e) {
                TimeOut = true;
                logger.debug("socket receive timeout");
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (!TimeOut) {
                java.util.Date date = new java.util.Date();
                InetAddress address = packet.getAddress();
                logger.debug("address = " + address.getHostAddress());
                int lport = packet.getPort();
                packet = new DatagramPacket(buf, buf.length, address, lport);
                logger.debug("call packet.getData() " +  packet.getLength());
                String received = new String(packet.getData(),0 , packet.getLength());
                if( (received != null) && (!received.trim().isEmpty()))
                {
                    logger.debug(packet.getAddress().getHostName() + " sends [" + received + "]");
                    updateGui(date, received, displayName, mc_addres, lport);
                }

            }
        }

        @SuppressWarnings("unchecked")
        private void updateGui(Date rcvdate, String received, String displayName , String mc_addres, int port) {
            // do some XML parsing
            DocumentBuilderFactory dbf =
                    DocumentBuilderFactory.newInstance();
            DocumentBuilder db = null;
            Document doc = null;
            InputSource is = new InputSource();
            is.setCharacterStream(new StringReader(received.trim()));

            StringBuilder m = new StringBuilder();
            Validator.ValidateSource(received.trim(), "data.xsd", m);
            logger.debug(m.toString());

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
                String timeString = (new Timestamp(rcvdate.getTime())).toString();
                v.add(timeString);
                v.add(dataName);
                v.add(displayName);
                v.add(data.length());

                v.add(data);
                tableData.addText(v);
                tvalList.add((long) Counter);
                String uniqueId = String.valueOf(Counter);
                Counter++;
                buffer.put(uniqueId, received);
                nrOfLogs++;
                logger.debug("added [" + uniqueId + "] to buffer, size=" + buffer.size());

                // add last received message to the datamonitor table
                UpdateTable(rcvdate.getTime(), received);

                dataNrOfItems++;

                dataLogger.log(String.valueOf(dataNrOfItems), uniqueId, dataName, dataType, new Date().getTime(), data, dataNrOfItems);

            }
            JScrollBar vertical = tableMessageScrollPane.getVerticalScrollBar();
            logger.debug("vert. scroll value =" + vertical.getValue());
            logger.debug("data logger size =" + dataLogger.getSize());
            vertical.setValue(vertical.getMaximum() - 1);

            EventQueue.invokeLater(() -> repaint());
        }
    }

    @SuppressWarnings("unchecked")
    private void UpdateTable(long tmillis, String received) {
        // do some XML parsing
        DocumentBuilderFactory dbf =
                DocumentBuilderFactory.newInstance();
        DocumentBuilder db;
        Document doc = null;
        InputSource is = new InputSource();
        is.setCharacterStream(new StringReader(received.trim()));

        StringBuilder m = new StringBuilder();
        Validator.ValidateSource(received.trim(), "data.xsd", m);
        logger.debug(m.toString());

        String dataName;
        String dataType;
        String dataKey = "NOT SET";
        String dataId;

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
                        Node nnn = nodes.item(0).getChildNodes().item(j).getChildNodes().item(jj);
                        if (nnn.getTextContent() != null && !nnn.getTextContent().isEmpty() && !nnn.getNodeName().contentEquals("#text")) {

                            if (nnn.getNodeName().contentEquals(Types.DATA_NAME)) {
                                dataName = nnn.getTextContent();

                                Vector v = new Vector();
                                v.add("MESSAGE");
                                v.add("TEXT");
                                v.add(dataName);
                                tableMessageData.addText(v);

                            } else if (nnn.getNodeName().contentEquals(Types.DATA_ID)) {
                                dataId = nnn.getTextContent();
                                Vector v = new Vector();
                                v.add("ID");
                                v.add("TEXT");
                                v.add(dataId);
                                tableMessageData.addText(v);
                            } else if (nnn.getNodeName().contentEquals(Types.DATA_KEY)) {
                                dataKey = nnn.getTextContent();
                            } else if (nnn.getNodeName().contentEquals(Types.DATA_TYPE)) {
                                dataType = nnn.getTextContent();
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

                    //Passed the milliseconds to constructor of Timestamp class
                    Timestamp ts = new Timestamp(tmillis);

                    logger.debug(ts.toString());

                    Vector v2 = new Vector();
                    v2.add("TIME");
                    v2.add("TEXT");
                    v2.add(ts.toString());
                    tableMessageData.addText(v2);

                    if (dataKey != null && !dataKey.isEmpty()) {
                        Vector v3 = new Vector();
                        v3.add("KEY");
                        v3.add("TEXT");
                        v3.add(dataKey);
                        tableMessageData.addText(v3);
                    }

                    logger.debug("nr. of payload items:" + payload.getChildNodes().getLength());
                    for (int k = 0; k < payload.getChildNodes().getLength(); k++) {
                        if (payload.getChildNodes().item(k).getNodeName().contentEquals(Types.DATA_ITEM)) {
                            NamedNodeMap namedItem = payload.getChildNodes().item(k).getAttributes();

                            logger.debug(namedItem.getNamedItem(Types.DATA_NAME).getNodeValue() +
                                    "  value: " + namedItem.getNamedItem(Types.DATA_VALUE).getNodeValue() +
                                    "  type: " + namedItem.getNamedItem(Types.DATA_TYPE).getNodeValue());

                            Vector v = new Vector();
                            v.add(namedItem.getNamedItem(Types.DATA_NAME).getNodeValue());
                            v.add(namedItem.getNamedItem(Types.DATA_TYPE).getNodeValue());
                            v.add(namedItem.getNamedItem(Types.DATA_VALUE).getNodeValue());
                            tableMessageData.addText(v);

                            if (namedItem.getNamedItem(Types.DATA_RANGE) != null)
                                logger.debug("  range: " + namedItem.getNamedItem(Types.DATA_RANGE).getNodeValue());
                        }
                    }
                }
            }
        } else {
            logger.info("nodes==null or empty");
        }
    }

    public void start(){
        inputThread1.start();
    }

    public void setPriority(int priority)
    {
        inputThread1.setPriority(priority);
    }

    @SuppressWarnings("unchecked")
    public Monitor() {
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
                MessageModel dm = (MessageModel) GUItable.getModel();
                dm.clearData();
                dataLogger.clear();
                Counter = 0;
                nl.fah.monitor.message.MessageModel dm2 = (nl.fah.monitor.message.MessageModel)tableMessage.getModel();
                dm2.clearData();
            }
        });

        JButton sendButtonStim = new JButton(new AbstractAction("send") {
            Logger logger = LoggerFactory.getLogger(Monitor.class);

            public void sendData(String message, String ip, int port, String network){

                InetAddress group = null;
                try {
                    group = InetAddress.getByName(ip);
                } catch (UnknownHostException e6) {
                    e6.printStackTrace();
                }

                //create Multicast socket to to pretending group
                MulticastSocket s = null;
                NetworkInterface ni = null;

                try {
                    s = new MulticastSocket(port);
                    ni = NetworkInterface.getByName(network);

                    s.setNetworkInterface(ni);
                    s.joinGroup(group);
                    logger.info("joined group on ni " + network);

                    byte[] b = message.getBytes();

                    DatagramPacket dp = new DatagramPacket(b, b.length, group, port);

                    logger.info("send data using network interface: " + network + " displayname=" +  ni.getDisplayName());
                    s.send(dp);

                } catch (IOException e7) {
                    e7.printStackTrace();
                }
            }

            @Override
            @SuppressWarnings("unchecked")
            public void actionPerformed(ActionEvent e) {
                if(tableMessageData.getRowCount()>0)
                {
                    logger.debug("send data");
                    dataKey.setText(tableMessageData.getValueAt(3,2).toString());

                    String msg = " <?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                            "<data>\n" +
                            "    <header>\n" +
                            "        <name>" + tableMessageData.getValueAt(0,2).toString() + "</name>\n" +
                            "        <type>" + tableMessageData.getValueAt(1,2).toString() + "</type>\n";
                    if (!tableMessageData.getValueAt(1,2).toString().contentEquals("EVENT")) msg += "        <key>" + tableMessageData.getValueAt(3,2).toString() + "</key>\n";
                    msg += "    </header>\n";

                    String payload = "    <payload>\n";
                    for (int i=4;i<tableMessageData.getRowCount();i++){
                        payload += "        <item name='" + tableMessageData.getValueAt(i,0).toString()
                                + "' value='"+ tableMessageData.getValueAt(i,2).toString()
                                + "' type='"+ tableMessageData.getValueAt(i,1).toString()
                                + "' />\n";
                    }
                    payload +=   "    </payload>\n";

                    msg += payload;
                    msg += "</data>\n";

                    logger.debug(msg);

                    multicast_read = ipTextFieldStim.getText();
                    logger.debug("multicast=" + multicast_read);
                    logger.debug("port=" + portTextFieldStim.getText());

                    port_read =  Integer.parseInt( portTextFieldStim.getText().trim() );
                    sendData(msg, multicast_read, port_read, networkSendList.getSelectedItem().toString());
                    logger.debug("send data to " + multicast_read + ":" + port_read + " network:" + networkSendList.getSelectedItem().toString());
                }
            }
        });

        initDataList();

        ipTextField = new JTextField(multicast_read, 6);
        ipTextField.setText(multicast_read);
        ipTextField.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) {
                multicast_read = ipTextField.getText();
                Boolean v = validMulticast(multicast_read);
                if (!validMulticast(multicast_read)) ipTextField.setBackground(Color.red);
                else ipTextField.setBackground(Color.white);
                logger.info("multicast address has changed: " + multicast_read);
            }
            public void removeUpdate(DocumentEvent e) {
                multicast_read = ipTextField.getText();

                Boolean v = validMulticast(multicast_read);
                if (!validMulticast(multicast_read)) ipTextField.setBackground(Color.red);
                else ipTextField.setBackground(Color.white);

                logger.info("multicast address has changed: " + multicast_read + " valid="  + v);
            }
            public void insertUpdate(DocumentEvent e) {
                multicast_read = ipTextField.getText();
                Boolean v = validMulticast(multicast_read);
                if (!validMulticast(multicast_read)) ipTextField.setBackground(Color.red);
                else ipTextField.setBackground(Color.white);
                logger.info("multicast address has changed: " + multicast_read);
            }
        });

        portTextField = new JTextField(String.valueOf(port_read), 4);
        portTextField.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) {
                try{
                    port_read =  Integer.parseInt(portTextField.getText()) ;
                    portTextField.setBackground(Color.white);
                }
                catch(NumberFormatException e1){
                    portTextField.setBackground(Color.red);
                }
                logger.info("port number has changed: " + port_read);
            }
            public void removeUpdate(DocumentEvent e) {
                try{
                    port_read =  Integer.parseInt(portTextField.getText()) ;
                    portTextField.setBackground(Color.white);
                }
                catch(NumberFormatException e1){
                    portTextField.setBackground(Color.red);
                }
                logger.info("port number has changed: " + port_read);
            }
            public void insertUpdate(DocumentEvent e) {
                try{
                    port_read =  Integer.parseInt(portTextField.getText()) ;
                    portTextField.setBackground(Color.white);
                }
                catch(NumberFormatException e1){
                    portTextField.setBackground(Color.red);
                }
                logger.info("port number has changed: " + port_read);
            }
        });

        Panel StimControlPanel = new Panel();

        initD();

        try {
            interfaces = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {
            e.printStackTrace();
        }

        while (interfaces.hasMoreElements())
        {
            NetworkInterface networkInterface = interfaces.nextElement();
            logger.debug("network interface: " + networkInterface.getName() + " displayname=" +  networkInterface.getDisplayName());
        }

        logger.info("dataList items:" + dataList.getItemCount());
        StimControlPanel.add(dataList);

        Panel ControlPanel = new Panel();
        ControlPanel.add(ipTextField);
        ControlPanel.add(portTextField);

        try {
            interfaces = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {
            e.printStackTrace();
        }

        int nrOfInterFaces =0;
        while (interfaces.hasMoreElements())
        {
            try {
                NetworkInterface networkInterface = interfaces.nextElement();
                if (networkInterface.isUp() && (networkInterface.supportsMulticast() || networkInterface.isLoopback())) nrOfInterFaces++;
            } catch (SocketException e) {
                e.printStackTrace();
            }
        }

        logger.info("nr of network interface: "+ nrOfInterFaces);
        String[] nwStrings = new String[nrOfInterFaces];
        String[] nwSendStrings = new String[nrOfInterFaces];

        try {
            interfaces = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {
            e.printStackTrace();
        }

        int ind = 0;
        while(ind < nrOfInterFaces)
        {
            try{
                NetworkInterface networkInterface = interfaces.nextElement();
                if (networkInterface.isUp() && (networkInterface.supportsMulticast() || networkInterface.isLoopback())) {
                    logger.info("network interface: " + networkInterface.getName() + " [" +  networkInterface.getDisplayName() + "]");
                    nwStrings[ind] = networkInterface.getName();
                    nwSendStrings[ind] = networkInterface.getName();
                    ind++;

                }

            } catch (SocketException e) {
                e.printStackTrace();
            }
        }

        networkList = new JComboBox(nwStrings);
        networkSendList = new JComboBox(nwSendStrings);
        ControlPanel.add(networkList);
        ControlPanel.add(startButton);
        ControlPanel.add(stopButton);
        ControlPanel.add(clearButton);

        jifMonAndStim.add(ControlPanel, BorderLayout.NORTH);

        Panel DataPanel = new Panel();
        GUItable.setSize( new Dimension(430,100));
        GUItable.setPreferredScrollableViewportSize(new Dimension(430, 100));
        GUItable.setFillsViewportHeight(true);
        tableMessageScrollPane = new JScrollPane(GUItable);
        DataPanel.add(tableMessageScrollPane);
        DataPanel.add(new JScrollPane(tableMessage));

        ipTextFieldStim = new JTextField(multicast_write, 6);
        ipTextFieldStim.setText(multicast_write);
        ipTextFieldStim.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) {
                multicast_write = ipTextFieldStim.getText();
                Boolean v = validMulticast(multicast_write);
                if (!validMulticast(multicast_write)) ipTextFieldStim.setBackground(Color.red);
                else ipTextFieldStim.setBackground(Color.white);
                logger.info("multicast address has changed: " + multicast_write);
            }
            public void removeUpdate(DocumentEvent e) {
                multicast_write = ipTextFieldStim.getText();

                Boolean v = validMulticast(multicast_write);
                if (!validMulticast(multicast_write)) ipTextFieldStim.setBackground(Color.red);
                else ipTextFieldStim.setBackground(Color.white);

                logger.info("multicastStim address has changed: " + multicast_write + " valid="  + v);
            }
            public void insertUpdate(DocumentEvent e) {
                multicast_write = ipTextFieldStim.getText();
                Boolean v = validMulticast(multicast_write);
                if (!validMulticast(multicast_write)) ipTextFieldStim.setBackground(Color.red);
                else ipTextFieldStim.setBackground(Color.white);
                logger.info("multicast address has changed: " + multicast_write);
            }
        });

        portTextFieldStim = new JTextField(String.valueOf(port_write), 4);
        portTextFieldStim.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) {
                try{
                    port_read =  Integer.parseInt(portTextFieldStim.getText()) ;
                    portTextFieldStim.setBackground(Color.white);
                }
                catch(NumberFormatException e1){
                    portTextFieldStim.setBackground(Color.red);
                }
                logger.info("port number has changed: " + port_write);
            }
            public void removeUpdate(DocumentEvent e) {
                try{
                    port_write =  Integer.parseInt(portTextFieldStim.getText()) ;
                    portTextFieldStim.setBackground(Color.white);
                }
                catch(NumberFormatException e1){
                    portTextFieldStim.setBackground(Color.red);
                }
                logger.info("port number has changed: " + port_read);
            }
            public void insertUpdate(DocumentEvent e) {
                try{
                    port_write =  Integer.parseInt(portTextFieldStim.getText()) ;
                    portTextFieldStim.setBackground(Color.white);
                }
                catch(NumberFormatException e1){
                    portTextFieldStim.setBackground(Color.red);
                }
                logger.info("port number has changed: " + port_read);
            }
        });

        ImageIcon icon = new ImageIcon(this.getClass().getResource("/images/TreasuresEgypt_Sphinx-icon.png"),
                "sphinx logo");
        this.setIconImage(icon.getImage());

        StimControlPanel.add(dataList);
        StimControlPanel.add(ipTextFieldStim);
        StimControlPanel.add(portTextFieldStim);
        StimControlPanel.add(networkSendList);
        StimControlPanel.add(sendButtonStim);

        Panel StimPanel = new Panel();
        StimPanel.setLayout(new BoxLayout(StimPanel, BoxLayout.Y_AXIS));
        StimPanel.add(DataPanel);
        StimPanel.add(StimControlPanel);

        infoLabel = new Label();
        infoLabel.setText("idle");

        jifMonAndStim.add(StimPanel, BorderLayout.CENTER);
        jifMonAndStim.add(infoLabel, BorderLayout.SOUTH);
        jifMonAndStim.show();
        jifMonAndStim.updateUI();

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Monitor", jifMonAndStim);

        add(tabbedPane);
        //create a menu bar
        final JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        JMenu helpMenu = new JMenu("Help");

        JMenuItem newMenuItem = new JMenuItem("New");
        newMenuItem.setActionCommand("New");
        newMenuItem.setToolTipText("New window");
        newMenuItem.addActionListener(event -> {
            Monitor monitor = new Monitor();
            monitor.setSize(478,600);
            monitor.setVisible(true);
            monitor.setPriority(Thread.MAX_PRIORITY);
            monitor.start();
            monitor.setTitle("SPHINX Tool");
            monitor.setLocation(this.getLocation().x + 40, this.getLocation().y + 40);
        });

        JMenuItem openMenuItem = new JMenuItem("Open");
        openMenuItem.setActionCommand("Open");
        openMenuItem.setToolTipText("Open PCAP file");
        openMenuItem.addActionListener(event ->
        {
            dataTimeStore.clear();
            dataStore.clear();
            dataLogger.clear();
            this.LoadPcap();

            logger.info("file size = " + packetList.size());
            MessageModel dm = (MessageModel) GUItable.getModel();
            dm.clearData();
            tableData.clearData();
            tableMessageData.clearData();


            int nrOfMsgs = 0;

            for(byte[] m : packetList)
            {
                String xml = new String(Arrays.copyOfRange(m, 42, m.length), StandardCharsets.UTF_8).trim();
                logger.debug("xml = " + xml);
                try {
                    DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
                    logger.debug("Message is valid XML.");

                    dataStore.put(nrOfMsgs, xml);
                    Long tmillis = tvalList.get(nrOfMsgs);

                    logger.debug("tmillis=" + tmillis);

                    Timestamp ts = new Timestamp(tmillis);
                    dataTimeStore.put(nrOfMsgs, ts);
                    String address = senderList.get(nrOfMsgs);

                    dataLogger.log(address, String.valueOf( nrOfMsgs), dataName, "xml", tmillis, xml, nrOfMsgs);
                    updateData(tmillis, xml, address);
                    nrOfMsgs++;
                    Counter++;

                } catch (Exception e) {
                    logger.info("Message is not valid XML.");
                }



            }
        } );

        JMenuItem exitMenuItem = new JMenuItem("Exit");
        exitMenuItem.setActionCommand("Exit");
        exitMenuItem.setToolTipText("Exit application");
        exitMenuItem.addActionListener(event -> { exit(1); });

        fileMenu.add(newMenuItem);
        fileMenu.add(openMenuItem);
        fileMenu.add(exitMenuItem);

        JMenuItem aboutMenuItem = new JMenuItem("About");
        aboutMenuItem.addActionListener(event -> {
            JEditorPane ep = new JEditorPane("text/html",
                    "<html><body><p>Sphinx Tool is developed by Gezinsman.nl <br/>\n" +
                    "to help engineers for testing and debugging <br/>\n" +
                    "software applications in distributed systems.</p> <br/>\n" +
                            "<p>Checkout https://github.com/Frenk44/sphinx <br/>\n" +
                            "to get more information.</p> <br/>\n" +
                    "</body></html>");
            ep.setEditable(false);
            ep.setBackground(this.getBackground());

            JOptionPane.showMessageDialog(this,
                    ep,
                    "About Sphinx",
                    JOptionPane.INFORMATION_MESSAGE,
                    icon);
        });
        helpMenu.add(aboutMenuItem);

        menuBar.add(fileMenu);
        menuBar.add(helpMenu);

        setJMenuBar(menuBar);

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                logger.info("close sphinx");
                exit(0);
            }
        });
    }

    private boolean LoadPcap() {
        final JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileFilter() {

            public String getDescription() {
                return "Wireshark PCAP logging (*.pcap)";
            }

            public boolean accept(File f) {
                if (f.isDirectory()) {
                    return true;
                } else {
                    String filename = f.getName().toLowerCase();
                    return filename.endsWith(".pcap") ;
                }
            }
        });

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            logger.info("Selected file: " + selectedFile.getAbsolutePath());
            int nrPackets = 0;
            try {
                PcapHandle handler = Pcaps.openOffline(selectedFile.getAbsolutePath());
                Packet packet;

                packetList = new ArrayList<>();
                tvalList = new ArrayList<>();

                while ((packet = handler.getNextPacket()) != null) {
                    nrPackets++;

                    Timestamp ts = handler.getTimestamp();
                    logger.debug("ts = " + ts.toString());
                    packetList.add(packet.getRawData());
                    tvalList.add(ts.getTime());

                    String ifName = handler.getDlt().name();
                    String dlName =  Pcaps.dataLinkTypeToName(handler.getDlt());

                    senderList.add(ifName + "/" + dlName );
                }

                logger.info("nr of packets in pcap file: " +  nrPackets);
                handler.close();

                return true;

            } catch (PcapNativeException | NotOpenException ex) {
                logger.error("Error encoding pcap file", ex);

            }
        }

        return false;
    }

    @SuppressWarnings("unchecked")
    private void updateData(long tmillis, String received, String dataSender) {
        boolean TimeOut = false;

        if (!TimeOut) {

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
            int dataSize = received.length();

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
                v.add((new Timestamp(tmillis)).toString());
                v.add(dataName);
                v.add(dataSender);
                v.add(dataSize);
                v.add(data);
                tableData.addText(v);
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
        Validator.ValidateSource(received.trim(), "data.xsd", m);
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
}