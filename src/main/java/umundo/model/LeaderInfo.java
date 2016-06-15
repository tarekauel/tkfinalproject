package umundo.model;

public class LeaderInfo implements OutMessage {

    private final boolean leader;
    private final String type = "leader";

    public LeaderInfo(boolean leader) {
        this.leader = leader;
    }
}
