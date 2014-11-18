package nl.fah.monitor.message;

/**
 * Created by Haulussy on 27-10-2014.
 */

import javax.swing.*;

public class MonitorMessageTool extends JFrame {

    private static void createAndShowGUI() {
        MessageMonitor messageMonitor = new MessageMonitor();
        messageMonitor.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        messageMonitor.setSize(400, 300);
        messageMonitor.setVisible(true);

        messageMonitor.start();

        messageMonitor.setTitle("Monitor Message Tool");

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
