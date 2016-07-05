package umundo.model;

import org.umundo.core.Message;

public class Welcome {

  private String username;
  private String uuid;

  public Welcome(String username, String uuid) {
    this.uuid = uuid;
    this.username = username;
  }

  public String getUsername() {
    return username;
  }

  public String getUUID() {
    return uuid;
  }

  public Message get() {
    Message m = new Message();
    m.putMeta("type", "welcome");
    m.putMeta("username", username);
    m.putMeta("uuid", uuid);
    return m;
  }

  public static Welcome fromMessage(Message m) {
    return new Welcome(m.getMeta("username"), m.getMeta("uuid"));
  }
}
