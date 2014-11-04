package nl.fah.monitor;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableModel;
import java.awt.*;

/**
 * Created by Haulussy on 3-11-2014.
 */
public class MyEnumCellEditor extends AbstractCellEditor implements TableCellEditor
{
    private JComboBox editor;

    public MyEnumCellEditor(String[] values)
    {
        // Create a new Combobox with the array of values.
        editor = new JComboBox(values);
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int rowIndex, int colIndex)
    {

        // Set the model data of the table
        if(isSelected)
        {
            editor.setSelectedItem(value);
            TableModel model = table.getModel();
            model.setValueAt(value, rowIndex, colIndex);
        }

        return editor;
    }

    @Override
    public Object getCellEditorValue()
    {
        return editor.getSelectedItem();
    }
}