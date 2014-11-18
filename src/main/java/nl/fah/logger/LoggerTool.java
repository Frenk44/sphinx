package nl.fah.logger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
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
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;

/**
 * Created by Haulussy on 5-11-2014.
 *
 *  * TODO: make nice interface with record, pause, stop buttons and info panel
 */
public class LoggerTool extends JFrame {

    static DataLogger dataLogger;
    static String multicast = "239.0.0.5";
    static int port = 12345;
    static String data;

    static JLabel infoLabel;
    static JLabel logInfoLabel;
    static Logger logger = LoggerFactory.getLogger(DaemonLoggerProcess.class);

    static boolean logging = false;
    static boolean loggingPrev = false;

    private static void createAndShowGUI() {
        LoggerTool loggerTool = new LoggerTool(multicast, port);
        loggerTool.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        loggerTool.setSize(250, 100);
        loggerTool.setVisible(true);
        loggerTool.setTitle("Logger Tool");

        loggerTool.start();

    }

    public void start(){

    }

    private static class DaemonLoggerProcess implements Runnable {
        DatagramPacket packet;
        String dataId;
        String dataKey;
        String dataName;
        String dataType;
        int dataNrOfItems;

        public void run() {
            dataLogger = new DataLoggerImpl();

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

/*            try {
                socket.joinGroup(group);
            } catch (IOException e) {
                e.printStackTrace();
            }
*/
            boolean firstTime = true;
            while (true) {

                if (!logging && loggingPrev) {
                    try {
                            socket.leaveGroup(group);
                            infoLabel.setText("stop logging");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                if(logging && !loggingPrev){
                    infoLabel.setText("start logging");
                    try {
                        socket.joinGroup(group);

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                loggingPrev = logging;

                while (logging) {
                    logger.debug("Logger Daemon update");

                    dataKey = null;

                    byte[] buf = new byte[10 * 1024];
                    packet = new DatagramPacket(buf, buf.length);
                    try {
                        socket.receive(packet);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    String received = new String(packet.getData());
                    logger.debug(packet.getAddress().getHostName() + " sends\n" + received);

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


                    NodeList nodes = doc.getElementsByTagName("data");
                    if (nodes != null && (nodes.getLength() == 1)) {
                        dataNrOfItems = 0;
                        for (int j = 0; j < nodes.item(0).getChildNodes().getLength(); j++) {
                            if (nodes.item(0).getChildNodes().item(j).getNodeName().contentEquals("payload")) {
                                dataNrOfItems = nodes.item(0).getChildNodes().item(j).getChildNodes().getLength();
                            }
                            if (nodes.item(0).getChildNodes().item(j).getNodeName().contentEquals("header")) {

                                for (int jj = 0; jj < nodes.item(0).getChildNodes().item(j).getChildNodes().getLength(); jj++) {
                                    Node nnn = nodes.item(0).getChildNodes().item(j).getChildNodes().item(jj);
                                    if (nnn.getTextContent() != null && !nnn.getTextContent().isEmpty() && !nnn.getNodeName().contentEquals("#text")) {

                                        if (nnn.getNodeName().contentEquals("name")) {
                                            dataName = nnn.getTextContent();
                                        } else if (nnn.getNodeName().contentEquals("id")) {
                                            dataId = nnn.getTextContent();
                                        } else if (nnn.getNodeName().contentEquals("key")) {
                                            dataKey = nnn.getTextContent();
                                        } else if (nnn.getNodeName().contentEquals("type")) {
                                            dataType = nnn.getTextContent();
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        logger.debug("nodes==null or empty");
                    }

                    dataLogger.log(packet.getAddress().getHostName(), dataKey, dataName, dataType, data, dataNrOfItems);

                    logger.debug("LOG: nr. of items: " + dataLogger.getSize());
                    logInfoLabel.setText("LOG: nr. of items: " + dataLogger.getSize());

                    logger.debug(dataLogger.dumpLog());
                }

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }


        }


    }


    public LoggerTool(String ip, int port2) {
        infoLabel  = new JLabel();
        infoLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        logInfoLabel  = new JLabel();
        logInfoLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        setLayout(new BorderLayout());
        multicast = ip;
        port = port2;

        JButton startButton = new JButton(new AbstractAction("start") {
            @Override
            public void actionPerformed(ActionEvent e) {
                logging = true;
            }
        });

        JButton stopButton = new JButton(new AbstractAction("stop") {
            @Override
            public void actionPerformed(ActionEvent e) {
                logging = false;
            }
        });

        JButton clearButton = new JButton(new AbstractAction("clear") {
            @Override
            public void actionPerformed(ActionEvent e) {
                dataLogger.clear();
                infoLabel.setText("logging cleared");
            }
        });

        JButton saveButton = new JButton(new AbstractAction("save") {
            @Override
            public void actionPerformed(ActionEvent e) {
                final JFileChooser fc = new JFileChooser();
                int returnVal = fc.showSaveDialog(LoggerTool.this);

                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    File file = fc.getSelectedFile();
                    //This is where a real application would open the file.
                    logger.debug("Save file to: " + file.getAbsolutePath());
                    dataLogger.saveLog(file.getAbsolutePath());
                } else {
                    logger.debug("Open command cancelled by user.");
                }
            }
        });

        infoLabel.setText("logger tool initialised");
        add(infoLabel, BorderLayout.SOUTH);

        Panel ControlPanel = new Panel();
        ControlPanel.add(startButton);
        ControlPanel.add(stopButton);
        ControlPanel.add(clearButton);
        ControlPanel.add(saveButton);

        add(ControlPanel, BorderLayout.CENTER);

        Panel DestPanel = new Panel();
        JTextField ipTextField = new JTextField(8);
        ipTextField.setText(multicast);
        JTextField portTextField = new JTextField(4);
        portTextField.setText( Integer.toString(port)  );

        DestPanel.add(ipTextField);
        DestPanel.add(portTextField);
        DestPanel.add(logInfoLabel);

        add(DestPanel, BorderLayout.NORTH );

    }


    public static void main(String[] args) {
        Thread t = new Thread(new DaemonLoggerProcess());
        t.setPriority(Thread.MIN_PRIORITY);
        t.start();

        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createAndShowGUI();
            }
        });

    }
}
