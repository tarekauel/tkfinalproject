package umundo.model;


import javafx.beans.binding.StringBinding;
import org.umundo.core.Message;

import javax.xml.bind.DatatypeConverter;
import java.util.HashMap;
import java.util.Map;

public class ScoreSyncMessage {

    private Map<String, String> matches; // contains machUID -> winnerUID mapping
    private Map<String, String> players; // contains playerUID -> playerName mapping for players contained in matches
    private byte[][] hashes; // optional, for piggybacking hashes of prefixed areas

    private String prefix;

    public ScoreSyncMessage(String prefix, Map<String, String> matches, Map<String, String> players, byte[][] hashes) {
        this.prefix = prefix;
        this.matches = matches;
        this.players = players;
        this.hashes = hashes;
    }

    public Message get() {
        Message m = new Message();
        m.putMeta("type", "sync");
        m.putMeta("prefix", prefix);

        // build matches string
        if (matches == null || matches.isEmpty()) {
            return null; // wtf
        }

        StringBuilder sb = new StringBuilder();
        for (String match : matches.keySet()) {
            sb.append(match + ',' + matches.get(match));
            sb.append(';');
        }
        m.putMeta("matches", sb.toString().substring(0, sb.length() - 2));

        // build players string
        if (players == null || players.isEmpty()) {
            return null; // wtf
        }

        sb.setLength(0);
        for (String playerUID : players.keySet()) {
            sb.append(playerUID + ',' + players.get(playerUID));
            sb.append(';');
        }
        m.putMeta("players", sb.toString().substring(0, sb.length() - 2));

        // build hashes string
        if(hashes == null) {
            return m;
        }

        sb.setLength(0);
        for (int i = 0; i < 17; i++) {
            sb.append(DatatypeConverter.printHexBinary(hashes[i]));
            sb.append(",");
        }
        m.putMeta("hashes", sb.toString().substring(0, sb.length() - 2));


        return m;
    }

    public static ScoreSyncMessage fromMessage(Message m) {

        // parse matches
        Map<String, String> matches = new HashMap<String, String>();
        String[] matchStrings = m.getMeta("matches").split(";");

        for (String s : matchStrings) {
            String[] data = s.split(",");
            matches.put(data[0], data[1]);
        }

        // parse players
        Map<String, String> players = new HashMap<String, String>();
        String[] playerStrings = m.getMeta("matches").split(";");

        for (String s : playerStrings) {
            String[] data = s.split(",");
            players.put(data[0], data[1]);
        }

        // parse hashes
        String hashesString = m.getMeta("hashes");
        if(hashesString == null) {
            return new ScoreSyncMessage(m.getMeta("prefix"), matches, players, null);
        }

        String[] hashesStringArray = m.getMeta("hashes").split(",");
        byte[][] hashes = new byte[17][];
        if (hashesStringArray.length != 18) return null;
        for (int i = 0; i < 17; i++) {
            hashes[i] = DatatypeConverter.parseHexBinary(hashesStringArray[i]);
        }

        return new ScoreSyncMessage(m.getMeta("prefix"), matches, players, hashes);

    }
}


