package nl.fah.monitor;

import nl.fah.common.Types;
import nl.fah.common.Utils;
import nl.fah.logger.DataLogger;
import nl.fah.logger.DataLoggerImpl;
import nl.fah.monitor.data.MessageModel;
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
import javax.swing.event.*;
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
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.*;
import java.sql.Timestamp;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.io.FileInputStream;

public class Monitor extends JFrame {

    final nl.fah.monitor.data.MessageModel tableData = new nl.fah.monitor.data.MessageModel();
    final nl.fah.monitor.message.MessageModel tableMessageData = new nl.fah.monitor.message.MessageModel();

    JTable table = new JTable(tableData);
    Enumeration<NetworkInterface> interfaces = null;
    JComboBox networkList;
    JComboBox networkSendList;

    final JInternalFrame jifMonAndStim = new JInternalFrame("Monitor and Stimulator")
    {
    };

    String[] getValues(int row, int column){

        String[] items3 = null;
        Object e = enums.get(row-4); //NOTE: 1st 4 ROWS are defined by the header!!!! TODO: improve this hack!
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
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getSelectionModel().addListSelectionListener(new ListSelectionListener(){
            public void valueChanged(ListSelectionEvent event) {

                ListSelectionModel lsm=(ListSelectionModel) event.getSource();
                int selectedRow=lsm.getMinSelectionIndex();
                // do some actions here, for example
                // print first column value from selected row
                if(selectedRow>=0)
                {
                    String key = table.getValueAt(selectedRow, 0).toString();
                    System.out.println(key);

                    String xml = dataLogger.getPayLoad(key);
                    UpdateTable(xml);
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
    String multicast = "239.0.0.1";
    int port = 6001;
    String multicastStim = "239.0.0.2";
    int portStim = 2121;

    Thread inputThread1 = new Thread(new InputProcess( "input-thread-1",  multicast, port));

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
    public void initDataList(){
        logger.info("initDataList ");
        Properties prop = new Properties();
        try{
            InputStream is = new FileInputStream("sphinx.properties");
            if(is != null && is.available()>0) {
                prop.load(is);
                multicast = prop.getProperty(Types.CONFIG_PERSISTENCE_DAEMON_IP);
                port = Integer.parseInt(prop.getProperty(Types.CONFIG_PERSISTENCE_DAEMON_PORT));
                logger.info("sphinx.context.daemon.ip=" + prop.getProperty("sphinx.context.daemon.ip"));

            }
            else{
                logger.info("empty file or not existing: " + "sphinx.properties");
            }
        } catch (IOException e) {
            System.exit(0);
            e.printStackTrace();
        }

        logger.info("multicast=" + multicast);
        logger.info("port=" + port);
        URL myURL = getClass().getProtectionDomain().getCodeSource().getLocation();
        java.net.URI myURI = null;
        try {
            myURI = myURL.toURI();
        } catch (URISyntaxException e1)
        {}

        String FullPath = java.nio.file.Paths.get(myURI).toFile().toString();
        logger.info("FullPath:" + FullPath);
       // String path = new File(FullPath).getParent() + "\\templates\\model1";
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
                new ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        JComboBox cb = (JComboBox) e.getSource();
                        String dataTypeString = (String) cb.getSelectedItem();
                        logger.debug("dataType:" + dataTypeString);

                        if (dataTypeString.contentEquals(Types.MSG_TYPE_EVENT)){
                            dataKey.setText("");
                            dataKey.setEditable(false);
                        }
                        else dataKey.setEditable(true);
                    }

                } );

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

                //String FullPath =  "/Users/fhaul/work/monitor-gezinsman/test/templates/model1/"; // TODO
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
                v.add(new String("MESSAGE"));
                v.add(new String("TEXT"));
                v.add(new String(cmbType));
                tableMessageData.addText(v);

                v = new Vector();
                v.add(new String("TYPE"));
                v.add(new String("TEXT"));
                v.add(new String("EVENT"));
                tableMessageData.addText(v);

                v = new Vector();
                v.add(new String("TIME"));
                v.add(new String("TEXT"));
                v.add(new String("253426356"));
                tableMessageData.addText(v);

                v = new Vector();
                v.add(new String("KEY"));
                v.add(new String("TEXT"));
                v.add(new String(""));
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
                            logger.info(name + " has range [" + range + "] put at location:" + i);
                            enums.put(j, range);
                        }

                        v = new Vector();
                        v.add(new String(name));
                        v.add(new String(type));
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

                            multicast = ipTextField.getText();
                            port = Integer.parseInt( portTextField.getText() );

                            try {
                                socket = new MulticastSocket(port);
                                socket.setReceiveBufferSize(10*1024);
                                group = InetAddress.getByName(multicast);
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

                    logger.info("START MONITORING ON " + multicast + ":" + port + " NW-interface: " + socket.getNetworkInterface().getDisplayName());

                    //innerloop
                    while(true) {
                        if (pause){
                            try {
                                socket.leaveGroup(group);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            logger.info("STOP MONITORING ON " + multicast + ":" + port + " NW-interface: " + socket.getNetworkInterface().getDisplayName());

                            break;
                        }
                        update(socket);
                        logger.debug("sleep");
                        Thread.sleep(10);
                    }
                } catch (InterruptedException | SocketException e) { e.printStackTrace();}
            }

        }

        private void update(MulticastSocket socket) {
            boolean TimeOut = false;
            byte[] buf = new byte[10*1024];
            packet = new DatagramPacket(buf, buf.length);

            try {
                TimeOut = false;
                socket.setSoTimeout(10);
                logger.debug("call socket.receive");
                socket.receive(packet);


                logger.info("data received");
            } catch (SocketTimeoutException e) {
                TimeOut = true;
                logger.debug("socket receive timeout");
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (!TimeOut) {

                java.util.Date date = new java.util.Date();
                InetAddress address = packet.getAddress();
                logger.info("address = " + address.getHostAddress());
                int port = packet.getPort();
                packet = new DatagramPacket(buf, buf.length, address, port);
                logger.info("call packet.getData() " +  packet.getLength());
                String received = new String(packet.getData(),0 , packet.getLength());
                logger.info(packet.getAddress().getHostName() + " sends\n" + received);
                updateGui(date, received);


            }
            else{
                // time out
            }
        }

        private void updateGui(Date date, String received) {
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
                String timeString = (new Timestamp(date.getTime())).toString();
                v.add(timeString);
                v.add(new String(dataName));
                v.add(new String(dataType));
                v.add(new String(dataKey));

                v.add(new String(data));
                tableData.addText(v);

                String uniqueId = timeString;
                buffer.put(new String(uniqueId), new String(received));
                nrOfLogs++;
                logger.debug("added " + uniqueId + " to buffer, size=" + buffer.size());

                // add last received message to the datamonitor table

                UpdateTable(received);

                dataNrOfItems++;

                dataLogger.log(packet.getAddress().getHostName(), uniqueId, dataName, dataType, data, dataNrOfItems);


            }
            JScrollBar vertical = tableMessageScrollPane.getVerticalScrollBar();
            logger.debug("vert. scroll value =" + vertical.getValue());
            logger.debug("data logger size =" + dataLogger.getSize());
            vertical.setValue(vertical.getMaximum() - 1);

            EventQueue.invokeLater(new Runnable() {
                public void run() {
                    repaint();
                }
            });
        }
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
        Validator.ValidateSource(received.trim(), "data.xsd", m);
        logger.debug(m.toString());

        String data;
        String dataName;
        String dataType;
        String dataKey = "NOT SET";
        String dataId = "NOT SET";


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
                                v.add(new String("MESSAGE"));
                                v.add(new String("TEXT"));
                                v.add(new String(dataName));
                                tableMessageData.addText(v);

                            } else if (nnn.getNodeName().contentEquals(Types.DATA_ID)) {
                                dataId = nnn.getTextContent();
                                Vector v = new Vector();
                                v.add(new String("ID"));
                                v.add(new String("TEXT"));
                                v.add(new String(dataId));
                                tableMessageData.addText(v);
                            } else if (nnn.getNodeName().contentEquals(Types.DATA_KEY)) {
                                dataKey = nnn.getTextContent();
                            } else if (nnn.getNodeName().contentEquals(Types.DATA_TYPE)) {
                                dataType = nnn.getTextContent();
                                Vector v = new Vector();
                                v.add(new String("TYPE"));
                                v.add(new String("TEXT"));
                                v.add(new String(dataType));
                                tableMessageData.addText(v);
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
                    tableMessageData.addText(v2);

                    if (dataKey != null && !dataKey.isEmpty()) {
                        Vector v3 = new Vector();
                        v3.add(new String("KEY"));
                        v3.add(new String("TEXT"));
                        v3.add(new String(dataKey));
                        tableMessageData.addText(v3);
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
                            tableMessageData.addText(v);

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

    public void start(){
        inputThread1.start();
    }

    public void setPriority(int priority)
    {
        inputThread1.setPriority(priority);
    }

    public Monitor() {
        //setLayout(new BorderLayout());

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
                dataLogger.clear();
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
                    ni = NetworkInterface.getByName(network); // TODO: make all interfaces available on GUI

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







/*

                Enumeration<NetworkInterface> interfaces = null;
                try {
                    interfaces = NetworkInterface.getNetworkInterfaces();
                } catch (SocketException e) {
                    e.printStackTrace();
                }

                while (interfaces.hasMoreElements())
                {
                    NetworkInterface networkInterface = interfaces.nextElement();
                    if(networkInterface.getName().contentEquals("enp1s0"))
                    {
                        ni = networkInterface;
                    }
                    logger.info("network interface: " + networkInterface.getName() + " displayname=" +  networkInterface.getDisplayName());
                }

 */

            }

            @Override
            public void actionPerformed(ActionEvent e) {
                logger.debug("send data");

                String sel = (String)dataType.getSelectedItem();

                dataKey.setText(tableMessageData.getValueAt(3,2).toString());

                String msg = (String) " <?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
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

                multicast = ipTextFieldStim.getText();
                logger.debug("multicast=" + multicast);
                logger.debug("port=" + portTextFieldStim.getText());

                port =  Integer.parseInt( portTextFieldStim.getText().trim() );
                sendData(msg, multicast, port, networkSendList.getSelectedItem().toString());
                logger.info("send data to " + multicast + ":" + port + " network:" + networkSendList.getSelectedItem().toString());
            }
        });

        ipTextField = new JTextField(multicast, 6);
        ipTextField.setText(multicast);
        ipTextField.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) {
                multicast = ipTextField.getText();
                Boolean v = validMulticast(multicast);
                if (!validMulticast(multicast)) ipTextField.setBackground(Color.red);
                else ipTextField.setBackground(Color.white);
                logger.info("multicast address has changed: " + multicast);
            }
            public void removeUpdate(DocumentEvent e) {
                multicast = ipTextField.getText();

                Boolean v = validMulticast(multicast);
                if (!validMulticast(multicast)) ipTextField.setBackground(Color.red);
                else ipTextField.setBackground(Color.white);

                logger.info("multicast address has changed: " + multicast + " valid="  + v);
            }
            public void insertUpdate(DocumentEvent e) {
                multicast = ipTextField.getText();
                Boolean v = validMulticast(multicast);
                if (!validMulticast(multicast)) ipTextField.setBackground(Color.red);
                else ipTextField.setBackground(Color.white);
                logger.info("multicast address has changed: " + multicast);
            }
        });

        portTextField = new JTextField(String.valueOf(port), 4);
        portTextField.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) {
                try{
                    port =  Integer.parseInt(portTextField.getText()) ;
                    portTextField.setBackground(Color.white);
                }
                catch(NumberFormatException e1){
                    portTextField.setBackground(Color.red);
                }
                logger.info("port number has changed: " + port);
            }
            public void removeUpdate(DocumentEvent e) {
                try{
                    port =  Integer.parseInt(portTextField.getText()) ;
                    portTextField.setBackground(Color.white);
                }
                catch(NumberFormatException e1){
                    portTextField.setBackground(Color.red);
                }
                logger.info("port number has changed: " + port);
            }
            public void insertUpdate(DocumentEvent e) {
                try{
                    port =  Integer.parseInt(portTextField.getText()) ;
                    portTextField.setBackground(Color.white);
                }
                catch(NumberFormatException e1){
                    portTextField.setBackground(Color.red);
                }
                logger.info("port number has changed: " + port);
            }
        });

        Panel StimControlPanel = new Panel();

        initDataList();

        initD();



        try {
            interfaces = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {
            e.printStackTrace();
        }

        while (interfaces.hasMoreElements())
        {
            NetworkInterface networkInterface = interfaces.nextElement();
            logger.info("network interface: " + networkInterface.getName() + " displayname=" +  networkInterface.getDisplayName());
        }



        logger.info("dataList: ");

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
                interfaces.nextElement();                
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
        table.setSize( new Dimension(430,100));
        table.setPreferredScrollableViewportSize(new Dimension(430, 100));
        table.setFillsViewportHeight(true);
        tableMessageScrollPane = new JScrollPane(table);
        DataPanel.add(tableMessageScrollPane);
        DataPanel.add(new JScrollPane(tableMessage));

        ipTextFieldStim = new JTextField(multicastStim, 6);
        ipTextFieldStim.setText(multicastStim);
        ipTextFieldStim.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) {
                multicastStim = ipTextFieldStim.getText();
                Boolean v = validMulticast(multicastStim);
                if (!validMulticast(multicastStim)) ipTextFieldStim.setBackground(Color.red);
                else ipTextFieldStim.setBackground(Color.white);
                logger.info("multicast address has changed: " + multicastStim);
            }
            public void removeUpdate(DocumentEvent e) {
                multicastStim = ipTextFieldStim.getText();

                Boolean v = validMulticast(multicastStim);
                if (!validMulticast(multicastStim)) ipTextFieldStim.setBackground(Color.red);
                else ipTextFieldStim.setBackground(Color.white);

                logger.info("multicastStim address has changed: " + multicastStim + " valid="  + v);
            }
            public void insertUpdate(DocumentEvent e) {
                multicastStim = ipTextFieldStim.getText();
                Boolean v = validMulticast(multicastStim);
                if (!validMulticast(multicastStim)) ipTextFieldStim.setBackground(Color.red);
                else ipTextFieldStim.setBackground(Color.white);
                logger.info("multicast address has changed: " + multicastStim);
            }
        });

        portTextFieldStim = new JTextField(String.valueOf(portStim), 4);
        portTextFieldStim.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) {
                try{
                    port =  Integer.parseInt(portTextFieldStim.getText()) ;
                    portTextFieldStim.setBackground(Color.white);
                }
                catch(NumberFormatException e1){
                    portTextFieldStim.setBackground(Color.red);
                }
                logger.info("port number has changed: " + portStim);
            }
            public void removeUpdate(DocumentEvent e) {
                try{
                    portStim =  Integer.parseInt(portTextFieldStim.getText()) ;
                    portTextFieldStim.setBackground(Color.white);
                }
                catch(NumberFormatException e1){
                    portTextFieldStim.setBackground(Color.red);
                }
                logger.info("port number has changed: " + port);
            }
            public void insertUpdate(DocumentEvent e) {
                try{
                    portStim =  Integer.parseInt(portTextFieldStim.getText()) ;
                    portTextFieldStim.setBackground(Color.white);
                }
                catch(NumberFormatException e1){
                    portTextFieldStim.setBackground(Color.red);
                }
                logger.info("port number has changed: " + port);
            }
        });



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

    //    ImageIcon icon = new ImageIcon(this.getClass().getResource("images/TreasuresEgypt_Sphinx-icon.png"),
    //            "a pretty but meaningless splat");
    //    this.setIconImage(icon.getImage());


    }

}