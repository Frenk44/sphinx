package nl.fah.context;

import java.util.Collection;
import java.util.Set;

/**
 * Created by Haulussy on 4-11-2014.
 *
 * - Monitors UDP traffic and stores all CONTEXT data.
 * - Offers a webservice interface to retrieve context data
 * - Data is persisted as XML strings in a DB.
 *
 */
public interface DaemonStorage {
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
