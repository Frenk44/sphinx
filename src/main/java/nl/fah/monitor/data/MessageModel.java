package nl.fah.monitor.data;

import javax.swing.table.AbstractTableModel;
import java.util.Vector;

public class MessageModel extends AbstractTableModel {

    String[] columnNames = {"received",
            "name",
            "address",
            "size"};

    protected Vector listData = new Vector();

    @SuppressWarnings("unchecked")
    public void addText(Vector data) {
        listData.addElement(new Vector(data));
        fireTableDataChanged();
    }

    @Override
    @SuppressWarnings("unchecked")
    public String getColumnName(int index) {
        return columnNames[index];
    }

    @Override
    public int getRowCount() { return listData.size(); }

    @Override
    public int getColumnCount() { return 4; }

    public Object getValueAt(int row, int column){
        return ((Vector)(listData.elementAt(row))).elementAt(column);
    }

    public void clearData(){
        listData.clear();
        fireTableDataChanged();
    }

}
