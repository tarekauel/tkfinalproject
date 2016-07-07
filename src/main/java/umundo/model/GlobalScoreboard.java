package umundo.model;

public class GlobalScoreboard implements OutMessage {
    private final String type = "global-scoreboard";

    private Score[] scores;

    public GlobalScoreboard(Score[] scores) {
        this.scores = scores;
    }
}
