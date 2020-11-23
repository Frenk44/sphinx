package nl.fah.monitor.data;

import nl.fah.monitor.message.MessageModel;
import nl.fah.monitor.message.MessageMonitor;

import javax.swing.*;

public class MonitorTool extends JFrame {

    private static void createAndShowGUI() {
        DataMonitor ra = new DataMonitor();

        ra.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        ra.setSize(480, 672);
        ra.setVisible(true);
        ra.start();
        ra.setTitle("Sphinx Tool 2019");
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
