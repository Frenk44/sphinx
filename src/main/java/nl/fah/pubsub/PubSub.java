package nl.fah.pubsub;

import java.util.List;

/**
 * Created by Haulussy on 29-11-2014.
 */
public interface PubSub {
    boolean Subscribe(String message, String receivingIp, Integer receivingPort);
    List<String> GetSubscribers();
    Boolean ClearSubscriptions();
}
