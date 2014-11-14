package nl.fah.monitor.message;

import javax.swing.table.AbstractTableModel;
import java.util.Vector;

public class MessageModel extends AbstractTableModel {

    String[] columnNames = {"name",
            "type",
            "value"};

    protected Vector listData = new Vector();

    public boolean isCellEditable(int row, int col)
    {
        return false;
    }

    public void setValueAt(Object value, int row, int col) {
        Vector v = (Vector)(listData.elementAt(row));
        v.set(2, value);
        fireTableCellUpdated(row, col);
    }

    public void addText(Vector data) {
        listData.addElement(new Vector(data));
        fireTableDataChanged();
    }

    @Override
    public String getColumnName(int index) {
        return columnNames[index];
    }

    public int getRowCount() { return listData.size(); }
    public int getColumnCount() { return columnNames.length; }
    public Object getValueAt(int row, int column){ return ((Vector)(listData.elementAt(row))).elementAt(column); }

    public void clearData(){
        listData.clear();
        fireTableDataChanged();
    }

}
