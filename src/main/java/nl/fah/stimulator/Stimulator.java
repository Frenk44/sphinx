package nl.fah.stimulator;

/**
 * Created by Haulussy on 27-10-2014.
 */

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
    String modelPath = "/templates/model1";
    String dataName = "";
    String dataId = "";
    int dataSize = 0;

    Hashtable enums = new Hashtable();

    public void sendData(String message){

        String multicast = "239.0.0.5";
        int port = 12345;

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
        System.out.println("scan /resources/templates/model1 directory");
        URL location = this.getClass().getResource( modelPath);
        String FullPath = location.getPath();
        System.out.println(FullPath);

        files = new ArrayList<File>(Arrays.asList(new File(FullPath).listFiles()));
        System.out.println(files.size());

        String[] dataListNames = new String[files.size()];

        for (int i = 0; i < files.size(); i++) {
            File ff = files.get(i);
            dataListNames[i] = ff.getName().replace(".xml","");
            System.out.println(ff.getName());
        }

        dataList = new JComboBox(dataListNames){
            // on select of dataname, fill the table

        };


        dataList.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                JComboBox jcmbType = (JComboBox) e.getSource();
                String cmbType = (String) jcmbType.getSelectedItem();
                System.out.println("cmbType:" + cmbType);
                dataName = cmbType;

                /*

                TODO:
                1) load xml template from resources/templates/model1 directory
                2) fill table with attribute names
                3) make 2nd column editable

                 */

                String fname = "/templates/model1/" + cmbType + ".xml";
                InputStream is = Stimulator.class.getResourceAsStream(fname);
                System.out.println("reading: " + fname);
                dataName = cmbType;

                String xml = getStringFromInputStream(is);

                System.out.println(xml);


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
                System.out.println("headernodes length"  + headernodes.getLength());

                NodeList payloadnodes = doc.getElementsByTagName("payload");
                System.out.println("datanodes length:" + payloadnodes.getLength());

                Node headernode = headernodes.item(0);
                Node payloadnode = payloadnodes.item(0);

                System.out.println("headernode nr. of childs:" + headernode.getChildNodes().getLength());
                System.out.println("payloadnode nr. of childs:" + payloadnode.getChildNodes().getLength());

                for (int i = 0; i < headernode.getChildNodes().getLength(); i++) {
                    if (!headernode.getChildNodes().item(i).getNodeName().contentEquals("#text")) {
                        System.out.println(headernode.getChildNodes().item(i).getNodeName() + "=" + headernode.getChildNodes().item(i).getTextContent());
                    }
                }

                tableData.clearData();
                enums.clear();
                int j=0;
                for (int i = 0; i < payloadnode.getChildNodes().getLength(); i++) {
                    if (payloadnode.getChildNodes().item(i).getNodeName().contentEquals("item")) {
                        String name = payloadnode.getChildNodes().item(i).getAttributes().getNamedItem("name").getTextContent();
                        String type = payloadnode.getChildNodes().item(i).getAttributes().getNamedItem("type").getTextContent();
                        System.out.println(name + " [" + type + "]");

                        if (type.contentEquals("enum")) {
                            String range = payloadnode.getChildNodes().item(i).getAttributes().getNamedItem("range").getTextContent();
                            System.out.println(name + " has range [" + range + "] put at location:" + i);
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
                String msg = (String) " <?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                        "<data>\n" +
                        "    <header>\n" +
                        "        <name>"+   dataName + "</name>\n" +
                        "        <id>" + dataId + "</id>\n" +
                        "        <size>" + dataSize + "</size>\n" +
                        "        <type>PERIODIC</type>\n" +
                        "    </header>\n";

                //TODO: get data from table
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

                // TODO: evaluate XML with data.xsd before sending


                sendData(msg);
                System.out.println("send data");
            }
        });

        Panel ControlPanel = new Panel();
        ControlPanel.add(dataList);
        ControlPanel.add(sendButton);
        add(ControlPanel, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    final StimulatorModel tableData = new StimulatorModel();
    JTable table = new JTable(tableData){
        //  Determine editor to be used by row
        @Override
        public TableCellEditor getCellEditor(int row, int column)
        {
            int modelColumn = convertColumnIndexToModel( column );
            System.out.println( "modelColumn :" + modelColumn);
            System.out.println( "row :" + row);
            System.out.println( "column :" + column);
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
