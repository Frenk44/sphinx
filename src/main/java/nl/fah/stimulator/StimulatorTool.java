package nl.fah.stimulator;

/**
 * creeert voor alle xml files in de resources/data directory
 * een data object die de gebruiker kan selecteren en invullen
 * om een UDP xml bericht te versturen.
 *
 */

import javax.swing.*;

public class StimulatorTool extends JFrame {

    private static void createAndShowGUI() {
        Stimulator stimulator = new Stimulator();
        stimulator.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        stimulator.setSize(500, 300);
        stimulator.setVisible(true);
        stimulator.setTitle("Stimulator Tool");

        stimulator.start();

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
