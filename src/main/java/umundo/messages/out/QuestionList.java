package umundo.messages.out;

import umundo.model.OutMessage;
import umundo.model.Question;

public class QuestionList implements OutMessage {
    private final String type = "question-list";
    private Question[] questions;

    public QuestionList(Question[] questions) {
        this.questions = questions;
    }

    public QuestionList(Question question) {
        this.questions = new Question[1];
        this.questions[0] = question;
    }
}
