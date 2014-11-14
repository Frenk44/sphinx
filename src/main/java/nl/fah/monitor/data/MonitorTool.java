package nl.fah.monitor.data;

/**
 * Created by Haulussy on 27-10-2014.
 */

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public class MonitorTool extends JFrame {

    class DataElement {
        public String receivedDateTime;
        public String name;
        public String data;
        public int sequence;
        public String key;
    }

    List dataBuffer = new ArrayList<DataElement>();

    final SimpleModel tableData = new SimpleModel();

    JScrollPane scrollPane;

    private static void createAndShowGUI() {
        RowAdder ra = new RowAdder("239.0.0.5");
        ra.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        ra.setSize(400, 300);
        ra.setVisible(true);

        ra.start();

        ra.setTitle("Monitor Tool");

    }

    public static void main(String[] args) {
        //Schedule a job for the event-dispatching thread:
        //creating and showing this application's GUI.
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createAndShowGUI();
            }
        });
    }

}
