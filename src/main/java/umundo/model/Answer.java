package umundo.model;

import org.umundo.core.Message;

public class Answer extends InMessage {

  private String username;
  private String questionId;
  private int answer;

  public Answer(String username, String questionId, int answer) {
    this.username = username;
    this.questionId = questionId;
    this.answer = answer;
  }

  public static Answer fromMessage(Message m) {
    return new Answer(
        m.getMeta("username"),
        m.getMeta("questionId"),
        Integer.parseInt(m.getMeta("answer"))
    );
  }

  public Message get() {
    Message m = new Message();
    m.putMeta("type", "answer");
    m.putMeta("username", username);
    m.putMeta("questionId", questionId);
    m.putMeta("answer", answer + "");
    return m;
  }

  public String getUsername() {
    return username;
  }

  public String getQuestionId() {
    return questionId;
  }

  public int getAnswer() {
    return answer;
  }
}
