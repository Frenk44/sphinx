package nl.fah.context;

import java.util.Collection;
import java.util.HashMap;
import java.util.Set;

/**
 * The context daemon continuously listens to an UDP port
 * and stores all context data.
 *
 * UDP Commands are processed to
 */
public class DaemonStorageImpl implements DaemonStorage {
    HashMap<String, String> xmlHashMap;

    public DaemonStorageImpl(){
        xmlHashMap = new HashMap<>();
    }

    public void clear() { xmlHashMap.clear(); }
    public Set<String> getList() { return xmlHashMap.keySet(); }
    public String get(String key) { return xmlHashMap.get(key); }
    public void put(String key, String data) { xmlHashMap.put(key, data);  }
    public Collection<String> getAll() { return xmlHashMap.values(); }
}
