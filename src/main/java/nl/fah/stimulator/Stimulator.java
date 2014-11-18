package nl.fah.stimulator;

/**
 * Created by Haulussy on 27-10-2014.
 */

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.CharacterData;
import org.w3c.dom.*;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Vector;

public class Stimulator extends JFrame {
    JComboBox dataList;
    JComboBox dataType;
    JTextField dataKey;
    JTextField ipTextField;
    JTextField portTextField;

    String modelPath = "/templates/model1";
    String dataName = "";
    String dataId = "";
    int dataSize = 0;
    String multicast = "239.0.0.5";
    int port = 12345;

    Hashtable enums = new Hashtable();

    Logger logger = LoggerFactory.getLogger(Stimulator.class);

    public void sendData(String message, String ip, int port){

        InetAddress group = null;
        try {
            group = InetAddress.getByName(ip);
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

        logger.debug("data send");

    }


    // convert InputStream to String
    private String getStringFromInputStream(InputStream iss) {

        BufferedReader br = null;
        StringBuilder sb = new StringBuilder();

        String line;
        try {

            br = new BufferedReader(new InputStreamReader(iss));
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return sb.toString();

    }


    public void init(){
        URL location = this.getClass().getResource( modelPath);
        String FullPath = location.getPath();
        logger.debug("scanning directory:" + FullPath);

        files = new ArrayList<File>(Arrays.asList(new File(FullPath).listFiles()));
        logger.info("number of xml-files:" + files.size());

        String[] dataListNames = new String[files.size()];

        int j=0;
        for (int i = 0; i < files.size(); i++) {
            File ff = files.get(i);

            StringBuilder m = new StringBuilder();
            Boolean ok = Validator.Validate("src/main/resources/templates/model1/" + ff.getName(),"src/main/resources/data.xsd", m );

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
        dataType = new JComboBox(new String[]{"EVENT","CONTEXT","PERSISTENT"}){
            // event-> on select make textBox only invalid if datatype=EVENT
            public void actionPerformed(ActionEvent e) {
                JComboBox cb = (JComboBox)e.getSource();
                String dataTypeName = (String)cb.getSelectedItem();
                logger.debug("dataTYpe=" + dataTypeName);
/*               if (dataTypeName.contentEquals("EVENT")) {
                   dataKey.setText("");
                   dataKey.setEditable(false);
               }
                else dataKey.setEditable(true);*/
            }
        };
        dataType.addActionListener(
                new ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        JComboBox dataType = (JComboBox) e.getSource();
                        String dataTypeString = (String) dataType.getSelectedItem();
                        logger.debug("dataType:" + dataTypeString);

                        if (dataTypeString.contentEquals("EVENT")){
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


                String fname = "/templates/model1/" + cmbType + ".xml";
                InputStream is = Stimulator.class.getResourceAsStream(fname);
                logger.debug("reading: " + fname);
                dataName = cmbType;

                String xml = getStringFromInputStream(is);

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

                tableData.clearData();
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

                        Vector v = new Vector();
                        v.add(new String(name));
                        v.add(new String(type));
                        v.add(new String());

                        tableData.addText(v);
                        j++;
                    }
                }
            }
        });

        ipTextField = new JTextField(8);
        ipTextField.setText(multicast);
        portTextField = new JTextField(4);
        portTextField.setText( Integer.toString(port)  );

    }

    public static String getCharacterDataFromElement(Element e) {
        Node child = e.getFirstChild();

        if (child instanceof CharacterData) {
            CharacterData cd = (CharacterData) child;
            return cd.getData();
        }
        return "?";
    }

    String[] getValues(int row, int column){
        String[] items3 = null;
        Object e = enums.get(row);
        if(e != null) {
            items3 = e.toString().split(",");
        }
        return items3;
    }

    public Stimulator() {
        init();

        setLayout(new BorderLayout());

        JButton sendButton = new JButton(new AbstractAction("send") {
            @Override
            public void actionPerformed(ActionEvent e) {

                String sel = (String)dataType.getSelectedItem();

                String msg = (String) " <?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                        "<data>\n" +
                        "    <header>\n" +
                        "        <name>"+   dataName + "</name>\n" +
                        "        <id>" + dataId + "</id>\n" +
                        "        <size>" + dataSize + "</size>\n" +
                        "        <type>" + dataType.getSelectedItem() + "</type>\n";
                if (!sel.contentEquals("EVENT")) msg += "        <key>" + dataKey.getText() + "</key>\n";
                msg += "    </header>\n";

                String payload = "    <payload>\n";
                for (int i=0;i<tableData.getRowCount();i++){
                    payload += "        <item name='" + tableData.getValueAt(i,0).toString()
                            + "' value='"+ tableData.getValueAt(i,2).toString()
                            + "' type='"+ tableData.getValueAt(i,1).toString()
                            + "' />\n";
                }
                payload +=   "    </payload>\n";

                msg += payload;
                msg += "</data>\n";

                logger.info(msg);

                multicast = ipTextField.getText();
                logger.debug("multicast=" + multicast);
                logger.debug("port=" + portTextField.getText());

                port =  Integer.parseInt( portTextField.getText().trim() );
                sendData(msg, multicast, port);
                logger.debug("send data");
            }
        });

        Panel ControlPanel = new Panel();
        ControlPanel.add(dataList);
        ControlPanel.add(dataType);
        ControlPanel.add(dataKey);

        Panel DestPanel = new Panel();
        DestPanel.add(ipTextField);
        DestPanel.add(portTextField);
        DestPanel.add(sendButton);


        add(ControlPanel, BorderLayout.NORTH);
        add(DestPanel);

        add(new JScrollPane(table), BorderLayout.CENTER);
        add(DestPanel, BorderLayout.SOUTH);
    }

    final StimulatorModel tableData = new StimulatorModel();
    JTable table = new JTable(tableData){
        //  Determine editor to be used by row
        @Override
        public TableCellEditor getCellEditor(int row, int column)
        {
            int modelColumn = convertColumnIndexToModel( column );
            logger.debug( "modelColumn :" + modelColumn);
            logger.debug( "row :" + row);
            logger.debug( "column :" + column);
            // determine if table at pos (row,column) is of type enumeration
            String[] items = getValues(row,column);

            if (items != null && items.length > 0){
                JComboBox comboBox3 = new JComboBox(items);
                DefaultCellEditor dce3 = new DefaultCellEditor(comboBox3);
                return dce3;
            }
            else
                return super.getCellEditor(row, column);
        }
    };

    ArrayList<File> files;

    Thread t = new Thread(new StimulatorProcess());

    private class StimulatorProcess implements Runnable {

        public void run() {

            // thread loop
            while (true){

                // do something or nothing

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
        }

    }

    public void start(){
        t.start();
    }

}
