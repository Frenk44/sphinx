package nl.fah.monitor.message;

import javax.swing.table.AbstractTableModel;
import java.util.Vector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MessageModel extends AbstractTableModel {
    Logger logger = LoggerFactory.getLogger(MessageMonitor.class);

    String[] columnNames = {"name",
            "type",
            "value"};

    protected Vector listData = new Vector();

    public boolean isCellEditable(int row, int col)
    {
        if (row==1 || row > 2 ) return true;
        else return false;
    }

    @SuppressWarnings("unchecked")
    public void setValueAt(Object value, int row, int col) {
        Vector v = (Vector)(listData.elementAt(row));
        v.set(2, value);
        fireTableCellUpdated(row, col);
    }

    @SuppressWarnings("unchecked")
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
    public Object getValueAt(int row, int column){
        Object o = new Object();

        try {
            if(listData!=null){
                Vector vRow = (Vector)(listData.elementAt(row));
                if(vRow!=null && vRow.size()>0){
                    o = vRow.elementAt(column);
                }

                return o;
            }
        }
        catch(ArrayIndexOutOfBoundsException exception) {
            logger.warn("ArrayIndexOutOfBoundsException");
        }

        return new Vector();

    }

    public void clearData(){
        listData.clear();
        fireTableDataChanged();
    }

}
