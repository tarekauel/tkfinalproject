package helper;

import org.apache.log4j.Logger;
import umundo.model.Match;
import umundo.model.Player;

import java.sql.*;
import java.util.UUID;


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
            s.executeUpdate("CREATE TABLE IF NOT EXISTS `players` (" +
                    "`uuid` CHAR(36) PRIMARY KEY NOT NULL," +
                    "`name` TEXT NOT NULL" +
                    ")");

            s.close();
        } catch (SQLException e) {
            log.error("Failed to create player table: " + e.getMessage());
            System.exit(1);
        }
        try (Statement s = db.createStatement()) {
            s.executeUpdate("CREATE TABLE IF NOT EXISTS `matches` (" +
                    "`uuid` CHAR(36) PRIMARY KEY NOT NULL," +
                    "`winner_uuid` CHAR(36) NOT NULL" +
                    ")");

            s.close();
        } catch (SQLException e) {
            log.error("Failed to create question table: " + e.getMessage());
            System.exit(1);
        }

        // Own UID
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
    }

    public static String getMyUID() {
        assert isOpen();
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

        if (getPlayerByUUID(player.getPlayerUid()) != null) return;

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
        Player rv = null;
        try (PreparedStatement s = db.prepareStatement("SELECT uuid, name FROM `player` WHERE uuid = ? LIMIT 1")) {
            s.setString(1, uuid);

            ResultSet rs = s.executeQuery();

            if (rs.next()) {
                rv = new Player(rs.getString(1), rs.getString(2));
            }

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
