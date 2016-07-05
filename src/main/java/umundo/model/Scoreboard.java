package umundo.model;

import org.umundo.core.Message;

import java.util.HashMap;
import java.util.Map;

public class Scoreboard implements OutMessage {

  private static final String type = "score";

  private HashMap<String, Integer> scores;
  private HashMap<String, String> uuidmap;

  public Scoreboard(HashMap<String, Integer> scores, HashMap<String, String> uuidmap) {
    this.scores = scores;
    this.uuidmap = uuidmap;
  }

  public HashMap<String, Integer> getScores() {
    return scores;
  }

  public HashMap<String, String> getUUIDmap() {
    return uuidmap;
  }

  public Message get() {
    Message m = new Message();
    m.putMeta("type", type);
    for(Map.Entry<String, Integer> e : scores.entrySet()) {
      // Format: name => score,uuid
      m.putMeta(e.getKey(), e.getValue().toString() + "," + uuidmap.get(e.getKey()));
    }
    return m;
  }

  public static Scoreboard fromMessage(Message m) {
    HashMap<String, Integer> scores = new HashMap<>();
    HashMap<String, String> uuidmap = new HashMap<>();
    for(int i =0; i < m.getMetaKeys().size(); i++) {
      String key = m.getMetaKeys().get(i);
      if (!key.equals("type") && !key.startsWith("um.")) {
        // Get value for key
        String value = m.getMeta(key);
        // Split on comma
        String[] split = value.split(",");
        // Parse score
        int score = Integer.parseInt(split[0]);
        // Parse UUID
        String uuid = split[1];
        // Add information to variables
        scores.put(key, score);
        uuidmap.put(key, uuid);
      }
    }
    return new Scoreboard(scores, uuidmap);
  }
}
