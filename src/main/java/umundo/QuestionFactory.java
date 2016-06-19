package umundo;

import umundo.model.InMessage.Pos;
import umundo.model.Question;

import java.util.*;


public class QuestionFactory {

  public static final Pos mannheim  = new Pos(49.486468, 8.469552);
  public static final Pos darmstadt = new Pos(49.877658, 8.655008);
  public static final Pos frankfurt = new Pos(50.048403, 8.573321);


  private final static List<Question> questions = new ArrayList<>();

  static {
    for(int i=0; i < 10; i++) {
      questions.add(new Question("Mannheim" + i, "A", "B", "C", "D", 0, mannheim));
    }
    for(int i=0; i < 10; i++) {
      questions.add(new Question("Darmstadt" + i, "A", "B", "C", "D", 0, darmstadt));
    }
    for(int i=0; i < 10; i++) {
      questions.add(new Question("Frankfurt" + i, "A", "B", "C", "D", 0, frankfurt));
    }
  }

  private static Random r = new Random();

  public static Question getQuestionLoc(int id, Pos where) {
    synchronized (questions) {
      questions.sort((o1, o2) -> Double.compare(o1.getPos().distance(where), o2.getPos().distance(where)));
      // return one of the 20 closest questions
      return Question.getWithId(questions.get(Math.min(r.nextInt(20), questions.size() -  1)), id);
    }
  }
}
