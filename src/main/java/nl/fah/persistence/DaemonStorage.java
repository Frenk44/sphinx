package nl.fah.persistence;

import java.util.Collection;
import java.util.Set;

/**
 * Created by Haulussy on 4-11-2014.
 *
 * - Monitors UDP traffic and stores all PERSISTENT data.
 * - Data is persisted as XML strings in on disk (REST).
 *
 */
public interface DaemonStorage {

    public boolean SetFileLocation(String location);
    /*
     * returns a list of key identifiers
     */
    public Set<String> getList();

    /*
     * returns an xml value string
     */
    public String get(String key);

    /*
     * stores an xml value string with a certain key
     * overwrites any previous stored string
     * overwrites any previous stored string
     */
    public void put(String key, String data);

    public void clear();

    public Collection<String> getAll();
}
