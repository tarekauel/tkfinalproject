package umundo.model;

import helper.Database;
import org.apache.log4j.Logger;

import java.sql.*;
import java.util.Optional;
import java.util.UUID;

public class Question implements OutMessage, PersistedModel {

  private static Logger log = Logger.getLogger(Question.class.getName());
  private final String type = "question";

  private String questionId;
  private String question;
  private String answerA;
  private String answerB;
  private String answerC;
  private String answerD;
  private int correctAnswer;
  private Optional<InMessage.Pos> pos;


  public Question(String question, String answerA, String answerB, String answerC, String answerD, int correctAnswer) {
    this(question, answerA, answerB, answerC, answerD, correctAnswer, Optional.empty());
  }

  public Question(String question, String answerA, String answerB, String answerC, String answerD, int correctAnswer, Optional<InMessage.Pos> pos) {
    this(null, question, answerA, answerB, answerC, answerD, correctAnswer, pos);
  }

  private Question(String questionId, String question, String answerA, String answerB, String answerC, String answerD, int correctAnswer, Optional<InMessage.Pos> pos) {
    this.questionId = questionId;
    this.question = question.trim();
    this.answerA = answerA.trim();
    this.answerB = answerB.trim();
    this.answerC = answerC.trim();
    this.answerD = answerD.trim();
    this.correctAnswer = correctAnswer;
    this.pos = pos;
  }

  private Question(String questionId) {
    this.questionId = questionId;
  }


  public static Question loadById(String questionId) {
    Question q = new Question(questionId);

    return q.reload() ? q : null;
  }

  private static boolean exists(String questionId) {
    Connection db = Database.getConnection();

    try (PreparedStatement s = db.prepareStatement("SELECT uuid FROM `question` WHERE `uuid` = ?")) {
      s.setString(1, questionId);
      ResultSet rs = s.executeQuery();

      return rs.next();
    } catch (SQLException e) {
      log.error("Failed to execute SQL statement: " + e.getMessage());
    }

    return false;
  }

  public boolean save() {
    if (this.questionId == null || !Question.exists(this.questionId)) {
      return this.insert();
    } else {
      return this.update();
    }
  }

  public boolean reload() {
    if (questionId == null) {
      return false;
    }

    Connection db = Database.getConnection();

    try(PreparedStatement s = db.prepareStatement("SELECT * FROM `question` WHERE `uuid` = ?")) {
      s.setString(1, questionId);
      ResultSet rs = s.executeQuery();

      if (rs.next()) {
        question = rs.getString("question");
        answerA = rs.getString("answerA");
        answerB = rs.getString("answerB");
        answerC = rs.getString("answerC");
        answerD = rs.getString("answerD");
        correctAnswer = rs.getInt("correct");

        double longitude = rs.getDouble("longitude");
        double latitude = rs.getDouble("latitude");
        if (rs.wasNull()) {
          pos = Optional.empty();
        } else {
          pos = Optional.of(new InMessage.Pos(longitude, latitude));
        }

        s.close();
        return true;
      }
    } catch (SQLException e) {
      log.error("Failed to get question by UUID: " + e.getMessage());
    }

    return false;
  }

  private boolean insert() {
    Connection db = Database.getConnection();

    // Update question, if it exists
    try (PreparedStatement s = db.prepareStatement("SELECT uuid FROM `question` WHERE `question` = ?")) {
      s.setString(1, question);
      ResultSet rs = s.executeQuery();

      if (rs.next()) {
        this.questionId = rs.getString(1);
        s.close();
        return update();
      }

      s.close();
    } catch (SQLException e) {
      log.error("Failed to execute SQL statement: " + e.getMessage());
    }

    // Otherwise: insert
    try (PreparedStatement s = db.prepareStatement("INSERT INTO `question` " +
            "(`uuid`, `question`, `answerA`, `answerB`, `answerC`, `answerD`, `correct`, `longitude`, `latitude`)" +
            "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?)"))
    {
      UUID uuid = this.questionId == null ? UUID.randomUUID() : UUID.fromString(this.questionId);

      s.setString(1, uuid.toString());
      s.setString(2, question);
      s.setString(3, answerA);
      s.setString(4, answerB);
      s.setString(5, answerC);
      s.setString(6, answerD);
      s.setInt(7, correctAnswer);

      if (pos.isPresent()) {
        s.setDouble(8, pos.get().getLongitude());
        s.setDouble(9, pos.get().getLatitude());
      } else {
        s.setNull(8, Types.DECIMAL);
        s.setNull(9, Types.DECIMAL);
      }

      s.executeUpdate();
      s.close();

      questionId = uuid.toString();
      return true;
    } catch(SQLException e) {
      log.error("Failed to add question: " + e.getMessage());
    }

    return false;
  }

  private boolean update() {
    Connection db = Database.getConnection();

    try (PreparedStatement s = db.prepareStatement("UPDATE `question` SET `question` = ?, `answerA` = ?, `answerB` = ?," +
            "`answerC` = ?, `answerD` = ?, `correct` = ?, `longitude` = ?, `latitude` = ? WHERE `uuid` = ?")) {
      s.setString(1, question);
      s.setString(2, answerA);
      s.setString(3, answerB);
      s.setString(4, answerC);
      s.setString(5, answerD);
      s.setInt(6, correctAnswer);
      s.setString(9, questionId);

      if (pos.isPresent()) {
        s.setDouble(7, pos.get().getLongitude());
        s.setDouble(8, pos.get().getLatitude());
      } else {
        s.setNull(7, Types.DECIMAL);
        s.setNull(8, Types.DECIMAL);
      }

      int affectedRows = s.executeUpdate();
      s.close();

      return affectedRows > 0;
    } catch (SQLException e) {
      log.error("Failed to update question: " + e.getMessage());
    }

    return false;
  }


  public static Question fromMessage(org.umundo.core.Message m) {
    Question q = new Question(
        m.getMeta("id"),
        m.getMeta("question"),
        m.getMeta("answerA"),
        m.getMeta("answerB"),
        m.getMeta("answerC"),
        m.getMeta("answerD"),
        Integer.parseInt(m.getMeta("correctAnswer")),
        m.getMeta("pos").isEmpty() ? Optional.empty() : Optional.of(InMessage.Pos.fromJson(m.getMeta("pos")))
    );

    q.save();
    return q;
  };

  public org.umundo.core.Message get() {
    org.umundo.core.Message m = new org.umundo.core.Message();
    m.putMeta("type", "question");
    m.putMeta("id", questionId);
    m.putMeta("question", question);
    m.putMeta("answerA", answerA);
    m.putMeta("answerB", answerB);
    m.putMeta("answerC", answerC);
    m.putMeta("answerD", answerD);
    m.putMeta("correctAnswer", correctAnswer + "");
    if (pos.isPresent()) {
      m.putMeta("pos", pos.get().toJson());
    }
    return m;
  }


  public String getQuestionId() {
    return questionId;
  }

  public String getQuestion() {
    return question;
  }

  public String getAnswerA() {
    return answerA;
  }

  public String getAnswerB() {
    return answerB;
  }

  public String getAnswerC() {
    return answerC;
  }

  public String getAnswerD() {
    return answerD;
  }

  public int getCorrectAnswer() {
    return correctAnswer;
  }

  public InMessage.Pos getPos() { return pos.isPresent() ? pos.get() : null; }


  static {
    Connection db = Database.getConnection();

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
      s.executeUpdate("CREATE INDEX IF NOT EXISTS `questionIdx` ON `question`(question)");
      s.close();
    } catch (SQLException e) {
      log.error("Failed to create question table: " + e.getMessage());
      System.exit(1);
    }
  }
}
