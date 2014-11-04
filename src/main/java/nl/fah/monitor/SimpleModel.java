package nl.fah.monitor;

import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import java.util.Vector;

public class SimpleModel extends AbstractTableModel {

    String[] columnNames = {"received",
            "name",
            "sequence"};

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
    public int getColumnCount() { return 3; }
    public Object getValueAt(int row, int column){ return ((Vector)(listData.elementAt(row))).elementAt(column); }

    public void clearData(){
        listData.clear();
        fireTableDataChanged();
    }

}
