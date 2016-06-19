package umundo.model;

import com.google.gson.Gson;

public abstract class InMessage {

    private Pos _pos;
    private static final Gson gson = new Gson();

    public Pos getPos() {
        return _pos;
    }

    public static class Pos {
        private double longitude;
        private double latitude;

        public Pos(double longitude, double latitude) {
            this.longitude = longitude;
            this.latitude = latitude;
        }

        public static Pos fromJson(String json) {
            return gson.fromJson(json, Pos.class);
        }

        public String toJson() {
            return gson.toJson(this);
        }

        public double distance(Pos b) {
            return Math.sqrt(Math.pow(this.longitude - b.longitude, 2) + Math.pow(this.latitude - b.latitude, 2));
        }
    }
}
