package helper;

import org.apache.log4j.Logger;

import java.sql.Connection;
import java.sql.DriverManager;


public class Database {
    private static Logger log = Logger.getLogger(Database.class.getName());
    private static Connection db;

    public static Connection getConnection() {
        try {
            Class.forName("org.sqlite.JDBC");
            db = DriverManager.getConnection("jdbc:sqlite:storage.db");
        } catch (Exception e) {
            log.error("Failed to create sqlite database: " + e.getMessage());
            System.exit(1);
        }

        return db;
    }
}
