package umundo.model;

public abstract class InMessage {

    private Pos _pos;

    public Pos getPos() {
        return _pos;
    }

    public static class Pos {
        private double longitude;
        private double latitude;
    }
}
