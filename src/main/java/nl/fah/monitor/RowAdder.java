package nl.fah.monitor;

/**
 * Created by Haulussy on 27-10-2014.
 */

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
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.StringReader;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Vector;

public class RowAdder extends JFrame {

    final SimpleModel tableData = new SimpleModel();
    JTable table = new JTable(tableData);

    Thread t = new Thread(new InputProcess());
    boolean pause = true;

    private class InputProcess implements Runnable {

        String multicast = "239.0.0.5";
        int port = 12345;
        String data;
        String dataName;
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
                    System.out.println("mouse clicked" + n);

                    if (e.getClickCount() == 2 && !e.isConsumed()) {
                            System.out.println("double click");
                    }
                    else{
                        //only accept single click if time last
                      if   ( n > 1000)
                        System.out.println("single click");
                    }

                    timeLastMouseEvent = new Date().getTime();

                    String aap = " clickcount=" + e.getClickCount();
                    System.out.println(e.getSource().getClass() + aap + " position clicked = " + table.rowAtPoint(e.getPoint())  + "," +  table.columnAtPoint(e.getPoint()) );
                }


            });

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


            // outerloop
            while(true) {
                try {
                    while(true){
                        if (!pause){

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
            byte[] buf = new byte[10*1024];
            packet = new DatagramPacket(buf, buf.length);
            try {
                socket.receive(packet);
            } catch (IOException e) {
                e.printStackTrace();
            }
            java.util.Date date= new java.util.Date();
            String received = new String(packet.getData());
            System.out.println(packet.getAddress().getHostName() + " sends\n" + received);

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

            if (nodes != null && (nodes.getLength() ==1)){

                for (int j=0;j<nodes.item(0).getChildNodes().getLength();j++){
                    if (nodes.item(0).getChildNodes().item(j).getNodeName().contentEquals("header")){

                        for (int jj=0;jj<nodes.item(0).getChildNodes().getLength();jj++){
                            Node nnn = nodes.item(0).getChildNodes().item(j).getChildNodes().item(jj);
                            if (nnn.getTextContent() != null && !nnn.getTextContent().isEmpty() && !nnn.getNodeName().contentEquals("#text")) {

                                if (nnn.getNodeName().contentEquals("name")){
                                    //System.out.println("NAME=" +  nnn.getTextContent());
                                    dataName = nnn.getTextContent();

                                }
                                if (nnn.getNodeName().contentEquals("id")){
                                    //System.out.println("ID=" + nnn.getTextContent());
                                    dataId = nnn.getTextContent();
                                }
                            }
                        }
                    }
                    else if (nodes.item(0).getChildNodes().item(j).getNodeName().contentEquals("payload")){
                        Node payload = nodes.item(0).getChildNodes().item(j);

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
                                if (aaaa.getNamedItem("range") != null)
                                   System.out.print("  range: " + aaaa.getNamedItem("range").getNodeValue());

                                System.out.println();

                            }
                        }
                    }
                }
            }
            else{
                System.out.println("nodes==null or empty");
            }

            data = received.trim();
            Vector v = new Vector();
            v.add( ( new Timestamp( date.getTime() ) ).toString() );
            v.add(new String(dataName));
            v.add(new String(dataId));

            v.add(new String(data));
            tableData.addText( v );

            // TODO : auto scroll down
        }
    }


    public void start(){
        t.start();
    }

    public RowAdder() {
        final JTextField textField  = new JTextField();
        setLayout(new BorderLayout());

        JButton startButton = new JButton(new AbstractAction("start") {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("ButtonTest::start CALLED");
                pause = false;
            }
        });

        JButton stopButton = new JButton(new AbstractAction("stop") {
            @Override
            public void actionPerformed(ActionEvent e) {
                pause = true;
            }
        });

        JButton clearButton = new JButton(new AbstractAction("clear") {
            @Override
            public void actionPerformed(ActionEvent e) {
                SimpleModel dm = (SimpleModel)table.getModel();
                dm.clearData();
            }
        });


        Panel ControlPanel = new Panel();
        ControlPanel.add(startButton);
        ControlPanel.add(stopButton);
        ControlPanel.add(clearButton);

        add(ControlPanel, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);

        textField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                Vector v = new Vector();
                v.add(new String(textField.getText() + "datafrom UDP"));

                tableData.addText(v);
                textField.setText("");
            }
        });
        add(textField, BorderLayout.SOUTH);

    }

}