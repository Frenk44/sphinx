package nl.fah.monitor.data;

import javax.swing.table.AbstractTableModel;
import java.util.Vector;

public class SimpleModel extends AbstractTableModel {

    String[] columnNames = {"received",
            "name",
            "type",
    "key"};

    protected Vector listData = new Vector();


    public void addText(Vector data) {
        listData.addElement(new Vector(data));
        fireTableDataChanged();
    }

    @Override
    public String getColumnName(int index) {
        return columnNames[index];
    }

    public int getRowCount() { return listData.size(); }
    public int getColumnCount() { return 4; }
    public Object getValueAt(int row, int column){ return ((Vector)(listData.elementAt(row))).elementAt(column); }

    public void clearData(){
        listData.clear();
        fireTableDataChanged();
    }

}
