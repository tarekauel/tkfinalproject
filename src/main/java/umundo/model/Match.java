package umundo.model;

import org.umundo.core.Message;

import java.util.UUID;

public class Match {
    private String matchUID;
    private Player winner;

    private Match(String match, Player win) {
        winner = win;
        matchUID = match;
    }

    public Match(Player Winner) {
        this(UUID.randomUUID().toString(), Winner);
    }

    public String getMatchUID() {
        return matchUID;
    }

    public Player getWinner() {
        return winner;
    }

    public Message get() {
        Message m = new Message();
        m.putMeta("match", matchUID);
        m.putMeta("winner_name", winner.getPlayerName());
        m.putMeta("winner_uid", winner.getPlayerUid());
        return m;
    }

    public static Match fromMessage(Message m) {
        return new Match(
                m.getMeta("match"),
                new Player(m.getMeta("winner_name"),
                           m.getMeta("winner_uid")
                )
        );
    }
}
