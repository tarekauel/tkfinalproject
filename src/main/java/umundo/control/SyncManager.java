package umundo.control;

import helper.Database;
import org.apache.log4j.Logger;
import org.umundo.core.Message;
import umundo.model.Match;
import umundo.model.Player;
import umundo.model.ScoreSyncMessage;
import umundo.model.Welcome;

import javax.xml.bind.DatatypeConverter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * The SyncManager is responsible for synchronizing the Scoreboard of all connected peers.
 * This is made more efficient through the use of hashes to determine which part of the database is
 * not identical.
 * The system computes a hash over a sorted list of all match UUIDs in our local scoreboard history. It also
 * computes hashes over 16 partitions of the database, based on the first character of the match UUID (0...9a...f).
 * These 17 hashes are exchanged with the peers, and allow the peers to determine if the database is in sync and, if not,
 * which prefixes need to be exchanged. Using this method, the sync overhead can be reduced, as we no longer need to
 * transmit the entire database all the time.
 * The process is repeated in the other direction to ensure that the databases are reconciled in the end.
 */
public class SyncManager {
    private enum STATE {
        HASH_COMPARE,
        RECONCILIATION,
        SYNCED
    }

    private static Logger log = Logger.getLogger(SyncManager.class.getName());

    // Keep track of the state of individual peers
    private HashMap<String, STATE> clientstate;

    // Make space for 17 hashes
    private byte[][] hashes = new byte[17][];

    // Cache for database information
    private HashMap<String, String> matches;
    private HashMap<String, String> players;
    private HashMap<String, List<String>> prefixMap;

    // Singleton instance
    private static SyncManager instance;

    private SyncManager() {
        // private constructor (singleton)
        clientstate = new HashMap<>();
        players = new HashMap<>();
        prefixMap = new HashMap<>();
        loadDatabaseCache();
    }

    public static SyncManager getInstance() {
        if (instance == null) instance = new SyncManager();
        return instance;
    }

    /**
     * Handles a sync message of the type "Welcome Message", processing the contained hashes and generating a reply,
     * if necessary.
     * @param msg The Welcome Message
     * @return A reply Message, or null if none is needed
     */
    public Message handleSyncMessage(Welcome msg) {
        String uuid = msg.getUUID();
        // Ignore our own messages
        if (uuid.equals(Database.getMyUID())) return null;
        // Check if this is a new peer
        log.info("Got Welcome Message, ignoring common sense and replying");
        return processHashes(uuid, msg.getHashes(), false);
        /*
        if (!clientstate.containsKey(uuid)) {
            log.info("New client: " + uuid + ", starting sync");
            clientstate.put(uuid, STATE.HASH_COMPARE);
            return processHashes(uuid, msg.getHashes(), false);
        } else {
            // Not a new peer, so this is a redundant welcome message, ignore
            log.info("Ingoring redundant welcome msg from " + uuid);
            return null;
        }*/
    }

    public Message handleSyncMessage(ScoreSyncMessage msg) {
        String uuid = msg.getSenderUID();
        log.info("Received ScoreSyncMessage from " + uuid);
        if (!clientstate.containsKey(uuid)) {
            log.warn("Received unexpected ScoreSyncMessage, ignoring");
            return null; // wtf
        } else if (clientstate.get(uuid) == STATE.SYNCED && !msg.isInSync()) {
            log.error("We think that we are in sync, but partner disagrees - wtf?");
            return null; // wtf
        } else if (clientstate.get(uuid) == STATE.SYNCED && msg.isInSync()) {
            log.info("Synchronization with peer " + uuid + " completed");
            return null;
        }
        // Merge into DB
        log.info("Inserting received matches into DB");
        Map<String, String> syncedMatches = msg.getMatches();
        Map<String, String> syncedPlayers = msg.getPlayers();
        if (syncedPlayers == null) log.warn("Syncedplayers == null!");
        for (String matchUUID : syncedMatches.keySet()) {
            String winnerUUID = syncedMatches.get(matchUUID);
            if (winnerUUID == null) log.warn("WinnerUUID == null!");
            if (syncedPlayers.get(winnerUUID) == null) log.warn("Winner name == null!");
            log.info("Inserting " + matchUUID + ":" + winnerUUID + ":" + syncedPlayers.get(winnerUUID));
            Database.insertMatch(new Match(matchUUID, new Player(syncedPlayers.get(winnerUUID), winnerUUID)));
        }
        // Reload hashes
        loadDatabaseCache();
        // Compare with received hashes and send reply
        return processHashes(uuid, msg.getHashes(), msg.isInSync());
    }

    /**
     * Load the contents of the database into the cache and compute all required hashes
     */
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
            log.error("MD5 algorithm not available - wtf?");
            System.exit(1);
            return;
        }
        prefixMap.put(currentPrefix, new ArrayList<>());
        // Iterate over all known match UUIDs
        log.info("Computing hashes...");
        for (String uuid : uuidset) {
            if (uuid.startsWith(currentPrefix)) {
                prefix.append(uuid);
                prefixMap.get(currentPrefix).add(uuid);
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
                prefix = new StringBuilder();
                prefix.append(uuid);
                // Check if current prefix is next or if prefix was skipped
                String nextPrefix = uuid.substring(0,1);
                // Create new List in prefixMap
                prefixMap.put(nextPrefix, new ArrayList<>());
                prefixMap.get(nextPrefix).add(uuid);
                int nextIndex = Integer.parseInt(nextPrefix, 16);
                // Fill up skipped prefixes with hash of empty string, if applicable
                if (nextIndex - currentIndex > 1) {
                    for (int i = currentIndex+1; i < nextIndex; i++) {
                        hashes[i] = md.digest();
                        prefixMap.put(Integer.toHexString(i), new ArrayList<>());
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
            for (int i = currentIndex+1; i < 16; i++) {
                hashes[i] = md.digest();
                md.reset();
                prefixMap.put(Integer.toHexString(i), new ArrayList<>());
            }
        }
        // Compute overall hash
        md.update(all.toString().getBytes());
        hashes[16] = md.digest();

        log.info("Computed all hashes");
    }

    /**
     * Given a list of hashes, compare these with our own hashes and generate a ScoreSyncMessage with the information
     * required for reconciliation.
     * @param uuid UUID of the peer we are synchronizing with
     * @param hashes byte[][] of the hashes
     * @return A ScoreSyncMessage with reconciliation information, or null if none is required or an error is encountered
     */
    private Message processHashes(String uuid, byte[][] hashes, boolean otherInSync) {
        if (hashes.length != 17) {
            log.error("Malformed hash list received");
            return null;
        }
        if (Arrays.equals(hashes[16], this.hashes[16])) {
            // Last hash = overall hash
            // If it matches, we are in sync
            clientstate.put(uuid, STATE.SYNCED);
            log.info("We ARE in sync with peer " + uuid);
            if (!otherInSync) {
                return new ScoreSyncMessage(getHashes(), true).get();
            } else {
                return null;
            }
        } else {
            clientstate.put(uuid, STATE.RECONCILIATION);
            log.info("We are NOT in sync with peer " + uuid + ", starting reconciliation process");
            // find out which prefixes do not match
            List<String> doesNotMatch = new ArrayList<>();
            for (int i = 0; i < 16; i++) {
                log.info(Integer.toHexString(i) + ":" + DatatypeConverter.printHexBinary(hashes[i]) + ":" + DatatypeConverter.printHexBinary(this.hashes[i]));
                if (!Arrays.equals(hashes[i], this.hashes[i])) {
                    doesNotMatch.add(Integer.toHexString(i));
                }
            }
            HashMap<String, String> matches = new HashMap<>();
            HashMap<String, String> players = new HashMap<>();
            for (String prefix : doesNotMatch) {
                for (String matchuuid : prefixMap.get(prefix)) {
                    String winner = this.matches.get(matchuuid);
                    matches.put(matchuuid, winner);
                    players.put(winner, this.players.get(winner));
                    log.info(matchuuid + ":" + winner + ":" + this.players.get(winner));
                }
            }
            log.info("Sending ScoreSyncMessage with PlayerInfo");
            return new ScoreSyncMessage(matches, players, getHashes()).get();
        }
    }

    /**
     * Getter for the list of hashes
     * @return A byte[][] containing the list of our local hashes
     */
    public byte[][] getHashes() {
        return hashes;
    }

}
