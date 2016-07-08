package umundo.model;

import org.umundo.core.Message;

import javax.xml.bind.DatatypeConverter;

public class Welcome {

  private String username;
  private String uuid;
  private byte[][] hashes;

  public Welcome(String username, String uuid, byte[][] hashes) {
    this.uuid = uuid;
    this.username = username;
    this.hashes = hashes;
  }

  public String getUsername() {
    return username;
  }

  public String getUUID() {
    return uuid;
  }

  public byte[][] getHashes() {
    return hashes;
  }

  public Message get() {
    Message m = new Message();
    m.putMeta("type", "welcome");
    m.putMeta("username", username);
    m.putMeta("uuid", uuid);

    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < 17; i++) {
      sb.append(DatatypeConverter.printHexBinary(hashes[i]));
      sb.append(",");
    }
    m.putMeta("hashes", sb.toString());

    return m;
  }

  public static Welcome fromMessage(Message m) {
    String[] split = m.getMeta("hashes").split(",");
    byte[][] hashes = new byte[17][];
    if (split.length != 18) return null;
    for (int i = 0; i < 17; i++) {
      hashes[i] = DatatypeConverter.parseHexBinary(split[i]);
    }
    return new Welcome(m.getMeta("username"), m.getMeta("uuid"), hashes);
  }
}
