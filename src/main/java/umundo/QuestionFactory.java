package umundo;

import helper.Database;
import org.apache.log4j.Logger;
import umundo.model.InMessage;
import umundo.model.InMessage.Pos;
import umundo.model.Question;

import java.sql.*;
import java.util.*;


public class QuestionFactory {
  public static final Integer QUESTION_RANGE_METERS = 5000;

  public static final Pos mannheim  = new Pos(49.486468, 8.469552);
  public static final Pos darmstadt = new Pos(49.877658, 8.655008);
  public static final Pos frankfurt = new Pos(50.048403, 8.573321);
  public static final Pos seeheim = new Pos(49.771065426760664, 8.644266128540039);

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

    q = new Question("Wofür steht der Begriff Oheim?", "Obdachlosenheim", "Onkel", "Ohnmacht", "Oggersheim", 1);
    q.save();
  }

  private static Random r = new Random();

  public static Question getQuestionForLocation(Pos location) {
    Connection db = Database.getConnection();
    InMessage.PosBoundary locationBoundary = location.rangeBoundaries(QUESTION_RANGE_METERS);
    Question q = new Question("Dummy Question", "Answer A", "Answer B", "Answer C", "Answer D", 3);

    // count available questions associated for the current location
    int locQuestionCount = 0;
    try (PreparedStatement s = db.prepareStatement("SELECT COUNT(*) FROM `question` WHERE " +
            "`longitude` BETWEEN ? AND ? AND `latitude` BETWEEN ? AND ?"))
    {
      s.setDouble(1, locationBoundary.getSouthEast().getLongitude());
      s.setDouble(2, locationBoundary.getNorthWest().getLongitude());
      s.setDouble(3, locationBoundary.getNorthWest().getLatitude());
      s.setDouble(4, locationBoundary.getSouthEast().getLatitude());

      ResultSet rs = s.executeQuery();
      if (rs.next()) locQuestionCount = rs.getInt(1);
      s.close();
    } catch (SQLException e) {
      log.error("Failed STAT query: " + e.getMessage());
    }

    // count available questions that are NOT associated with a location
    int nolocQuestionCount = 0;
    try (Statement s = db.createStatement()) {
      ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM `question` WHERE `longitude` IS NULL OR `latitude` IS NULL");
      if (rs.next()) nolocQuestionCount = rs.getInt(1);
      s.close();
    } catch (SQLException e) {
      log.error("Failed STAT query: " + e.getMessage());
    }

    int availableQuestionCount = locQuestionCount + nolocQuestionCount;
    if (availableQuestionCount == 0) {
      log.warn("Found no question to draw. Return dummy question");
      return q;
    }

    try {
      PreparedStatement s;
      double locationQuestionDrawChance = (double) locQuestionCount / availableQuestionCount;
      if (r.nextDouble() < locationQuestionDrawChance) {
        s = db.prepareStatement("SELECT `uuid` FROM `question` WHERE " +
                "`longitude` BETWEEN ? AND ? AND `latitude` BETWEEN ? AND ? LIMIT ?, 1");
        InMessage.PosBoundary b = location.rangeBoundaries(QUESTION_RANGE_METERS);
        s.setDouble(1, b.getSouthEast().getLongitude());
        s.setDouble(2, b.getNorthWest().getLongitude());
        s.setDouble(3, b.getNorthWest().getLatitude());
        s.setDouble(4, b.getSouthEast().getLatitude());
        s.setInt(5, r.nextInt(locQuestionCount));
      } else {
        s = db.prepareStatement("SELECT `uuid` FROM `question` WHERE `longitude` IS NULL OR `latitude` IS NULL LIMIT ?, 1");
        s.setInt(1, r.nextInt(nolocQuestionCount));
      }

      ResultSet rs = s.executeQuery();
      if (rs.next()) q = Question.loadById(rs.getString(1));
      s.close();
    } catch (SQLException e) {
      log.error("Failed to query for a question");
    }

    return q;
  }
}
