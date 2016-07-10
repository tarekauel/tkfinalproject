package umundo.model;

import org.umundo.core.Message;

public class Player {
    private String playerName;
    private String playerUid;

    public Player(String name, String uid) {
        playerName = name;
        playerUid = uid;
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getPlayerUid() {
        return playerUid;
    }

    public Message get() {
        Message m = new Message();
        m.putMeta("name", playerName);
        m.putMeta("uid", playerUid);
        return m;
    }

    public static Player fromMessage(Message m) {
        return new Player(
                m.getMeta("name"),
                m.getMeta("uid")
        );
    }
}
