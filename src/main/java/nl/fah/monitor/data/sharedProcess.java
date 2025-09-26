package nl.fah.monitor.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Time;
import java.sql.Timestamp;

public class sharedProcess extends Thread {
    private static Control stringBuffer;
    private static int index;
    private static Boolean dataAvl;
    Logger logger = LoggerFactory.getLogger(sharedProcess.class);


    public sharedProcess() {
        index = 0;
        stringBuffer = new Control(30);
    }

    public boolean getDataAvl()
    {
        return dataAvl;
    }

    public String getData() {
        if(index==0){
            String tmp = stringBuffer.received[index].read();
            dataAvl = false;
            return tmp;
        }
        else if(index>0)
        {
            String tmp = stringBuffer.received[index].read();
            index--;
            return tmp;
        }
        else return null;

    }

    public void putData(String data, String sender, int port) {
        logger.info("putData[" + index + "] = " + data);
        stringBuffer.received[index++].write(data);
        dataAvl = true;

    }

    Timestamp getTimeValidity()
    {
        return stringBuffer.received[0].timestamp;
    }

    int getSequence() { return stringBuffer.received[0].sequence; }
    void clearSequence() {stringBuffer.received[0].sequence = 0; }

    public void run() {
        logger.info("sharedProcess started");

        try {
            while(true) Thread.sleep(Long.MAX_VALUE);
        } catch (InterruptedException e) {
            logger.error(e.getLocalizedMessage());
        }
    }

}