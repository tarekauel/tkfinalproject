package umundo;

import helper.Database;
import org.apache.log4j.Logger;
import umundo.model.InMessage.Pos;
import umundo.model.Question;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;


public class QuestionFactory {
  public static final Integer QUESTION_RANGE_METERS = 5000;

  public static final Pos mannheim  = new Pos(49.486468, 8.469552);
  public static final Pos darmstadt = new Pos(49.877658, 8.655008);
  public static final Pos frankfurt = new Pos(50.048403, 8.573321);

  private static Logger log = Logger.getLogger(QuestionFactory.class.getName());


  static {
    Question q;
    for(int i=0; i < 10; i++) {
      q = new Question("Mannheim" + i, "A", "B", "C", "D", 0, Optional.of(mannheim));
      q.save();
    }
    for(int i=0; i < 10; i++) {
      q = new Question("Darmstadt" + i, "A", "B", "C", "D", 0, Optional.of(darmstadt));
      q.save();
    }
    for(int i=0; i < 10; i++) {
      q = new Question("Frankfurt" + i, "A", "B", "C", "D", 0, Optional.of(frankfurt));
      q.save();
    }
  }

  private static Random r = new Random();

  public static Question getQuestionForLocation(Pos where) {
    Connection db = Database.getConnection();
    Pos north = where.calculateDerivedPosition(QUESTION_RANGE_METERS, 90);
    Pos east = where.calculateDerivedPosition(QUESTION_RANGE_METERS, 0);
    Pos south = where.calculateDerivedPosition(QUESTION_RANGE_METERS, 270);
    Pos west = where.calculateDerivedPosition(QUESTION_RANGE_METERS, 180);
    List<Question> questions = new ArrayList<>();

    try (PreparedStatement s = db.prepareStatement("SELECT `uuid` FROM `question` WHERE " +
            "`longitude` BETWEEN ? AND ? AND `latitude` BETWEEN ? AND ?"))
    {
      s.setDouble(1, south.getLongitude());
      s.setDouble(2, north.getLongitude());
      s.setDouble(3, west.getLatitude());
      s.setDouble(4, east.getLatitude());

      /*log.info("SELECT `uuid` FROM `question` WHERE " +
              "`longitude` BETWEEN " + south.getLongitude() + " AND " + north.getLongitude() + " AND " +
              "`latitude` BETWEEN " + west.getLatitude() + " AND " + east.getLatitude());*/

      ResultSet rs = s.executeQuery();
      while(rs.next()) questions.add(Question.loadById(rs.getString("uuid")));
      s.close();
    } catch (SQLException e) {
      log.error("Failed to retrieve question from db: " + e.getMessage());
    }

    return questions.get(r.nextInt(questions.size() - 1));
  }
}
