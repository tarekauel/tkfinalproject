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

        public double getLatitude() {
            return latitude;
        }

        public double getLongitude() {
            return longitude;
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

        /**
         * Calculates the end-point from a given source at a given range (meters)
         * and bearing (degrees). This methods uses simple geometry equations to
         * calculate the end-point.
         * http://www.movable-type.co.uk/scripts/latlong.html
         *
         * @param range
         *            Range in meters
         * @param bearing
         *            Bearing in degrees, (N = 90, E = 0, S = 270, W = 180
         * @return End-point from the source given the desired range and bearing.
         */
        public Pos calculateDerivedPosition(double range, double bearing)
        {
            double earthRadius = 6371E3; // m
            double phi1 = Math.toRadians(this.latitude);
            double lambda1 = Math.toRadians(this.longitude);
            double theta = Math.toRadians(bearing);
            double delta = range / earthRadius;

            double phi2 = Math.asin(
                    Math.sin(phi1) * Math.cos(delta) +
                    Math.cos(phi1) * Math.sin(delta) * Math.cos(theta));
            double lamda2 = lambda1 + Math.atan2(Math.sin(theta) * Math.sin(delta) * Math.cos(phi1),
                    Math.cos(delta) - Math.sin(phi1) * Math.sin(phi2));

            return new Pos(Math.toDegrees(lamda2), Math.toDegrees(phi2));
        }

        public PosBoundary rangeBoundaries(double range) {
            Pos north = calculateDerivedPosition(range, 90);
            Pos east = calculateDerivedPosition(range, 0);
            Pos south = calculateDerivedPosition(range, 270);
            Pos west = calculateDerivedPosition(range, 180);

            return new PosBoundary(
                    new Pos(north.getLongitude(), west.getLatitude()),
                    new Pos(south.getLongitude(), east.getLatitude())
            );
        }
    }

    public static class PosBoundary {
        private Pos northWest;
        private Pos southEast;

        public PosBoundary(Pos northWest, Pos southEast) {
            this.northWest = northWest;
            this.southEast = southEast;
        }

        public Pos getNorthWest() {
            return northWest;
        }

        public Pos getSouthEast() {
            return southEast;
        }
    }
}
