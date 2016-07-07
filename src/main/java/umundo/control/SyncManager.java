package umundo.control;

import helper.Database;
import org.apache.log4j.Logger;
import umundo.model.Welcome;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class SyncManager {
    private enum STATE {
        HASH_COMPARE,
        RECONCILIATION,
        SYNCED
    }

    private static Logger log = Logger.getLogger(Client.class.getName());


    private HashMap<String, STATE> clientstate;

    private HashMap<String, String> matches;
    private HashMap<String, String> players;
    // Make space for 17 hashes
    private byte[][] hashes = new byte[17][];
    // SMASH THE STATE!

    private static SyncManager instance;

    private SyncManager(){
        clientstate = new HashMap<>();
        loadDatabaseCache();
    }

    public static SyncManager getInstance() {
        if (instance == null) instance = new SyncManager();
        return instance;
    }

    public void handleSyncMessage(Welcome msg) {
        String uuid = msg.getUUID();
        if (!clientstate.containsKey(uuid)) {
            clientstate.put(uuid, STATE.HASH_COMPARE);
            // TODO Get and send hashes
        }
    }

    private void loadDatabaseCache() {
        matches = Database.getMatchDatabase();
        players = Database.getPlayerDatabase();

        log.info("Loaded information from database");

        // Get List of Match UUIDs
        List<String> uuidset = new ArrayList<>(matches.keySet());
        Collections.sort(uuidset);

        // Compute prefix and complete hashes
        String currentPrefix = "0";
        StringBuilder prefix = new StringBuilder();
        StringBuilder all = new StringBuilder();
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            // Fuck this shit, what the hell kind of platform is this?
            log.error("MD5 algorithm not available - the fuck?");
            System.exit(1);
            return;
        }
        // Iterate over all known match UUIDs
        for (String uuid : uuidset) {
            if (uuid.startsWith(currentPrefix)) {
                prefix.append(uuid);
            } else {
                // Finalize string
                String toHash = prefix.toString();
                // Hash
                md.update(toHash.getBytes());
                // Save result
                int currentIndex = Integer.parseInt(currentPrefix, 16);
                hashes[currentIndex] = md.digest();
                // Reset state of hash
                md.reset();
                // New Stringbuilder
                prefix = new StringBuilder(uuid);
                // Check if current prefix is next or if prefix was skipped
                String nextPrefix = uuid.substring(0,1);
                int nextIndex = Integer.parseInt(nextPrefix, 16);
                // Fill up skipped prefixes with hash of empty string, if applicable
                if (nextIndex - currentIndex > 1) {
                    for (int i = currentIndex+1; i < nextIndex; i++) {
                        hashes[i] = md.digest();
                        md.reset();
                    }
                }
                // Update current Prefix
                currentPrefix = nextPrefix;
            }
            all.append(uuid);
        }
        // Finish up current prefix
        int currentIndex = Integer.parseInt(currentPrefix, 16);
        md.update(prefix.toString().getBytes());
        hashes[currentIndex] = md.digest();
        // Reset hash
        md.reset();

        // Check if we are missing prefixes and fill them up
        if (!currentPrefix.equals("f")) {
            currentIndex = Integer.parseInt(currentPrefix, 16);
            for (int i = currentIndex; i < 16; i++) {
                hashes[i] = md.digest();
                md.reset();
            }
        }
        // Compute overall hash
        md.update(all.toString().getBytes());
        hashes[16] = md.digest();

        log.info("Computed all hashes");
    }

}
