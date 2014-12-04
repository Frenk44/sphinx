package nl.fah.logger;

/**
 * Created by Haulussy on 16-11-2014.
 */
public interface DataLogger {

    public void log(String sender, String key, String dataName, String dataType, String dataPayload, int nrOfItems);
    public int getSize();
    public String dumpLog();
    public boolean saveLog(String name);
    public boolean isEmpty();
    public void clear();
    public String getPayLoad(String key, String dataName);
}
