package nl.fah.monitor;

import javax.swing.*;

public class MonitorTool extends JFrame {
    private static void createAndShowGUI() {
        Monitor monitor = new Monitor();
       // monitor.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        monitor.setSize(478,600);
        monitor.setVisible(true);
        monitor.setPriority(Thread.MAX_PRIORITY);
        monitor.start();
        monitor.setTitle("SPHINX Tool");
    }

    public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createAndShowGUI();
            }
        });
    }

}
