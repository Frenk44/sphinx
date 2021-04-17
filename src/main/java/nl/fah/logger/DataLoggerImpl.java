package nl.fah.logger;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

/**
 *
 *
 *
 */
public class DataLoggerImpl implements DataLogger {
    class DataItem {
        public Date receivedTime;
        public String sender;
        public String key;
        public String dataName;
        public String dataType;
        public int dataNrOfItems;
        public String dataPayload;
    }
    List<DataItem> dataLog = new ArrayList<>();

    @Override
    public boolean saveLog(String name) {
        // stringefy dataLog and save to file in readable
        // xml format

        String dump = "";
        String ret = "\n";

        dump += dataLog.size() + ret;
        Iterator<DataItem> it=dataLog.iterator();

        while(it.hasNext()){
            DataItem i = it.next();
            dump += i.dataName + ret;
            dump += i.dataType + ret;
            dump += i.receivedTime + ret;
            dump += i.key + ret;
            dump += i.sender + ret;
            dump += i.dataPayload + ret;
        }

        PrintWriter writer = null;
        try {
            writer = new PrintWriter(name, "UTF-8");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        writer.println(dump);
        writer.close();
        return false;
    }

    @Override
    public boolean isEmpty() {
        return dataLog.isEmpty();
    }

    @Override
    public void clear() {
        dataLog.clear();
    }

    @Override
    public String getPayLoad(String key) {
        for(int i=0;i<dataLog.size();i++)
        {
            if(dataLog.get(i).key.equals(key))
            {
                return dataLog.get(i).dataPayload;
            }
        }
        return null;
    }


    @Override
    public void log(String sender, String key, String dataName, String dataType, long tmillis, String dataPayload, int nrOfItems){
        DataItem data = new DataItem();

        data.dataName = dataName;
        data.dataPayload = dataPayload;
        data.dataType = dataType;
        data.key = key;
        data.sender = sender;
        java.util.Date d = new java.util.Date();
        data.receivedTime = new Timestamp(d.getTime());
        data.dataNrOfItems = nrOfItems;

        dataLog.add(data);
    }

    @Override
    public String dumpLog() {
        String dump = "";
        for (DataItem item : dataLog) {
            dump += "\nname=" + item.dataName
                    + "\nsender=" + item.sender
                    + "\ntime=" + item.receivedTime
                    + "\ndataType=" + item.dataType
                    + "\ndataPayload=" + item.dataPayload;
        }
        return dump;
    }

    @Override
    public int getSize() {
        return dataLog.size();
    }
}
