package nl.fah.persistence;

import java.io.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.Set;

/**
 * Created by Haulussy on 19-11-2014.
 *
 * Data is stored persistently on disk
 */
public class DaemonStorageImpl implements DaemonStorage {
    String FileLocation;
    HashMap<String, String> xmlHashMap;

    boolean save() {
        FileOutputStream fs = null;
        try {
            fs = new FileOutputStream(new File(FileLocation));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        ObjectOutputStream oos = null;
        try {
            oos = new ObjectOutputStream(fs);
            oos.writeObject(xmlHashMap);
            oos.close();
            fs.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;
    }

    public boolean load() {
        FileInputStream is = null;
        try {
            is = new FileInputStream(new File(FileLocation));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        ObjectInputStream ois = null;
        try {
            ois = new ObjectInputStream(is);
            xmlHashMap = (HashMap<String, String>) ois.readObject();

            ois.close();
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        return false;
    }


    @Override
    public boolean SetFileLocation(String location) {
        FileLocation = location;
        File file = new File(FileLocation);
        return file.exists();
    }

    public void clear() { xmlHashMap.clear(); }
    public Set<String> getList() { return xmlHashMap.keySet(); }
    public String get(String key) { return xmlHashMap.get(key); }

    public void put(String key, String data) {
        xmlHashMap.put(key, data);
        save();
    }

    public Collection<String> getAll() { return xmlHashMap.values(); }
}
