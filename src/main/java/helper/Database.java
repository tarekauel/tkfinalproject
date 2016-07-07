package helper;

import org.apache.log4j.Logger;
import umundo.model.*;

import java.sql.*;
import java.util.*;


public class Database {
    private static Logger log = Logger.getLogger(Database.class.getName());
    private static Connection db;


    public static Connection getConnection() {
        try {
            Class.forName("org.sqlite.JDBC");
            db = DriverManager.getConnection("jdbc:sqlite:storage.db");
            setupDatabase();
        } catch (Exception e) {
            log.error("Failed to create sqlite database: " + e.getMessage());
            System.exit(1);
        }

        return db;
    }

    private static void setupDatabase() {
        assert isOpen();
        try (Statement s = db.createStatement()) {
            s.executeUpdate("CREATE TABLE IF NOT EXISTS `question` (" +
                    "`uuid` CHAR(36) PRIMARY KEY NOT NULL," +
                    "`question` TEXT NOT NULL," +
                    "`answerA` TEXT NOT NULL," +
                    "`answerB` TEXT NOT NULL," +
                    "`answerC` TEXT NOT NULL," +
                    "`answerD` TEXT NOT NULL," +
                    "`correct` INTEGER NOT NULL," +
                    "`longitude` DECIMAL(9,6)," +
                    "`latitude` DECIMAL(9,6)" +
                    ")");

            s.close();
        } catch (SQLException e) {
            log.error("Failed to create question table: " + e.getMessage());
            System.exit(1);
        }
        try (Statement s = db.createStatement()) {
            s.executeUpdate("CREATE TABLE IF NOT EXISTS `localPlayerInfo` (" +
                    "`uuid` CHAR(36) PRIMARY KEY NOT NULL" +
                    ")");

            s.close();
        } catch (SQLException e) {
            log.error("Failed to create localPlayerInfo table: " + e.getMessage());
            System.exit(1);
        }
        try (Statement s = db.createStatement()) {
            s.executeUpdate("CREATE TABLE IF NOT EXISTS `player` (" +
                    "`uuid` CHAR(36) PRIMARY KEY NOT NULL," +
                    "`name` TEXT NOT NULL" +
                    ")");

            s.close();
        } catch (SQLException e) {
            log.error("Failed to create player table: " + e.getMessage());
            System.exit(1);
        }
        try (Statement s = db.createStatement()) {
            s.executeUpdate("CREATE TABLE IF NOT EXISTS `match` (" +
                    "`uuid` CHAR(36) PRIMARY KEY NOT NULL," +
                    "`winner_uuid` CHAR(36) NOT NULL" +
                    ")");

            s.close();
        } catch (SQLException e) {
            log.error("Failed to create question table: " + e.getMessage());
            System.exit(1);
        }

        // Own UID
        // TODO Also insert into player database with name, once set
        if (getMyUID() == null) {
            String uid = UUID.randomUUID().toString();

            try (PreparedStatement s = db.prepareStatement("INSERT INTO localPlayerInfo (uuid) VALUES (?);")) {
                s.setString(1, uid);

                int rv = s.executeUpdate();
                s.close();
                if (rv != 1) {
                    log.error("Insert failed - wtf?");
                }
            } catch (SQLException e) {
                log.error(e);
            }
        }
        log.info("Database setup completed");
    }

    public static String getMyUID() {
        assert isOpen();
        log.info("getMyUID: UID retrieved");
        String uid = null;
        try (PreparedStatement s = db.prepareStatement("SELECT uuid FROM `localPlayerInfo` LIMIT 1")) {
            ResultSet rs = s.executeQuery();

            // This should always be true, as we generated a UID in the beginning
            if (rs.next()) {
                uid = rs.getString(1);
            }

            s.close();
        } catch (SQLException e) {
            log.error("Failed to execute SQL statement: " + e.getMessage());
        }
        return uid;
    }

    public static void insertMatch(Match match) {
        assert isOpen();
        log.info("Add to database: Match " + match.getMatchUID() + " with winner " + match.getWinner().getPlayerUid());
        try (PreparedStatement s = db.prepareStatement("INSERT INTO `match` " +
                "(`uuid`, `winner_uuid`)" +
                "VALUES(?, ?)"))
        {
            s.setString(1, match.getMatchUID());
            s.setString(2, match.getWinner().getPlayerUid());

            s.executeUpdate();
            s.close();

            insertPlayerIfNotExists(match.getWinner());

        } catch(SQLException e) {
            log.error("Failed to add match: " + e.getMessage());
        }
    }

    public static void insertPlayerIfNotExists(Player player) {
        assert isOpen();

        if (getPlayerByUUID(player.getPlayerUid()) != null) {
            log.info("Attempted to insert player " + player.getPlayerUid() + ", but UUID already known");
            return;
        }
        log.info("Inserting player " + player.getPlayerName() + ", uuid = " + player.getPlayerUid());
        try (PreparedStatement s = db.prepareStatement("INSERT INTO `player` " +
                "(`uuid`, `name`)" +
                "VALUES(?, ?)"))
        {
            s.setString(1, player.getPlayerUid());
            s.setString(2, player.getPlayerName());

            s.executeUpdate();
            s.close();

        } catch(SQLException e) {
            log.error("Failed to add player: " + e.getMessage());
        }
    }

    public static Player getPlayerByUUID(String uuid) {
        assert isOpen();
        log.info("Looking for player with uuid " + uuid);
        Player rv = null;
        try (PreparedStatement s = db.prepareStatement("SELECT uuid, name FROM `player` WHERE uuid = ? LIMIT 1")) {
            s.setString(1, uuid);

            ResultSet rs = s.executeQuery();

            if (rs.next()) {
                rv = new Player(rs.getString(1), rs.getString(2));
                log.info("Player found: " + rv.getPlayerName());
            }

            s.close();
        } catch (SQLException e) {
            log.error("Failed to execute SQL statement: " + e.getMessage());
        }
        return rv;
    }

    public static GlobalScoreboard getGlobalScores() {
        assert isOpen();
        log.info("Retrieving global scoreboard");

        List<Score> score = new LinkedList<>();
        try (PreparedStatement s = db.prepareStatement("SELECT player.name, COUNT(match.uuid) FROM player INNER JOIN match ON match.winner_uuid LIKE player.uuid GROUP BY player.name;")) {
            ResultSet rs = s.executeQuery();
            while (rs.next()) {
                score.add(new Score(rs.getString(1), rs.getInt(2)));
            }
            s.close();
        } catch (SQLException e) {
            log.error(e);
        }
        // Fuck java.
        Score[] scorearray = new Score[score.size()];
        for (int i = 0; i < score.size(); i++) {
            scorearray[i] = score.get(i);
        }
        return new GlobalScoreboard(scorearray);
    }

    public static Set<String> getKnownMatchUUIDs() {
        // Ensure connection is sane
        assert isOpen();
        log.info("Retrieving list of known Match UUIDs");
        // Prepare result set
        Set<String> rv = new HashSet<>();
        // Query for all UUIDs
        try (PreparedStatement s = db.prepareStatement("SELECT uuid FROM `match`")) {
            ResultSet rs = s.executeQuery();
            // Add all to return value
            while (rs.next()) {
                rv.add(rs.getString(1));
            }
            // Close
            rs.close();
        } catch (SQLException e) {
            log.error(e);
        }
        return rv;
    }

    public static Match getMatchByUUID(String uuid) {
        // Database connection sane?
        assert isOpen();
        log.info("Looking for match with UUID " + uuid);
        // Prepare return value
        Match rv = null;
        // TODO Refactor to join with players database?
        try (PreparedStatement s = db.prepareStatement("SELECT uuid, winner_uuid FROM `match` WHERE uuid = ? LIMIT 1")) {
            // Insert placeholders
            s.setString(1, uuid);

            // Query
            ResultSet rs = s.executeQuery();

            // Retrieve result
            if (rs.next()) {
                rv = new Match(rs.getString(1), getPlayerByUUID(rs.getString(2)));
            }

            // Close resultset
            s.close();
        } catch (SQLException e) {
            log.error("Failed to execute SQL statement: " + e.getMessage());
        }
        return rv;
    }



    private static boolean isOpen() {
        return db != null;
    }
}
