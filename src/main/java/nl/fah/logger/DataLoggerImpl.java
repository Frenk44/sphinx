package nl.fah.logger;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

/**
 * Created by Haulussy on 16-11-2014.
 *
 */
public class DataLoggerImpl implements DataLogger {
    class DataItem {
        public Date receivedTime;
        public String sender;
        public String key;
        public String dataName;
        public String dataType;
        public String dataPayload;
    }
    List<DataItem> dataLog = new ArrayList<>();

    @Override
    public boolean saveLog(String name) {
        // stringefy dataLog and save to file in readable
        // xml format
        Iterator<DataItem> it=dataLog.iterator();

        while(it.hasNext()){
            DataItem i = it.next();
        }
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
    public void log(String sender, String key, String dataName, String dataType, String dataPayload){
        DataItem data = new DataItem();

        data.dataName = dataName;
        data.dataPayload = dataPayload;
        data.dataType = dataType;
        data.key = key;
        data.sender = sender;
        data.receivedTime = new Date();

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
