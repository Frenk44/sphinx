package nl.fah.test;

import nl.fah.monitor.data.MessageModel;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

/**
 * Created by Haulussy on 27-10-2014.
 */
public class RowAdderTest extends JFrame {

    final static MessageModel tableData = new MessageModel();
    static JTable table = new JTable(tableData);
    static JTextField textField;

    public static void main(String[] args) {
        textField = new JTextField();


        final JTextField textField  = new JTextField();
        textField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                Vector v = new Vector();
                v.add(new String("drol"));
                tableData.addText(v);
                textField.setText("");
            }
        });
        //add(textField, BorderLayout.SOUTH);
    }


}
