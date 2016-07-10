package umundo.model;

public class Score implements OutMessage {
    private final String type = "scoreboard-score";

    private String user;
    private int score;

    public Score(String user, int score) {
        this.user = user;
        this.score = score;
    }
}
