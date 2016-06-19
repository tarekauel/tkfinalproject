package umundo.model;

public class Question implements OutMessage {

  private final String type = "question";

  private int questionId;
  private String question;
  private String answerA;
  private String answerB;
  private String answerC;
  private String answerD;
  private int correctAnswer;
  private InMessage.Pos pos;

  public static Question getWithId(Question q, int id) {
    return new Question(
      id,
      q.getQuestion(),
      q.getAnswerA(),
      q.getAnswerB(),
      q.getAnswerC(),
      q.getAnswerD(),
      q.getCorrectAnswer(),
      q.getPos()
    );
  }

  public Question(String question, String answerA, String answerB, String answerC, String answerD, int correctAnswer, InMessage.Pos pos) {
    this.question = question;
    this.answerA = answerA;
    this.answerB = answerB;
    this.answerC = answerC;
    this.answerD = answerD;
    this.correctAnswer = correctAnswer;
    this.pos = pos;
  }

  private Question(int questionId, String question, String answerA, String answerB, String answerC, String answerD, int correctAnswer, InMessage.Pos pos) {
    this.questionId = questionId;
    this.question = question;
    this.answerA = answerA;
    this.answerB = answerB;
    this.answerC = answerC;
    this.answerD = answerD;
    this.correctAnswer = correctAnswer;
    this.pos = pos;
  }

  public static Question fromMessage(org.umundo.core.Message m) {
    return new Question(
        Integer.parseInt(m.getMeta("id")),
        m.getMeta("question"),
        m.getMeta("answerA"),
        m.getMeta("answerB"),
        m.getMeta("answerC"),
        m.getMeta("answerD"),
        Integer.parseInt(m.getMeta("correctAnswer")),
        InMessage.Pos.fromJson(m.getMeta("pos"))
    );
  };

  public org.umundo.core.Message get() {
    org.umundo.core.Message m = new org.umundo.core.Message();
    m.putMeta("type", "question");
    m.putMeta("id", questionId + "");
    m.putMeta("question", question);
    m.putMeta("answerA", answerA);
    m.putMeta("answerB", answerB);
    m.putMeta("answerC", answerC);
    m.putMeta("answerD", answerD);
    m.putMeta("correctAnswer", correctAnswer + "");
    m.putMeta("pos", pos.toJson());
    return m;
  }



  public int getQuestionId() {
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

  public InMessage.Pos getPos() {
    return pos;
  }
}
