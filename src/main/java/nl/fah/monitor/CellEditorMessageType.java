package nl.fah.monitor;

import nl.fah.common.Types;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableModel;
import java.awt.*;

/**
 * Created by Haulussy on 4-12-2014.
 */
public class CellEditorMessageType extends AbstractCellEditor implements TableCellEditor
{
    private JComboBox editor;
    private String [] values = {Types.MSG_TYPE_EVENT, Types.MSG_TYPE_CONTEXT, Types.MSG_TYPE_PERSISTENT};

    public CellEditorMessageType()
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
