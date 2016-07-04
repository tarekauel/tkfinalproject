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
         * http://stackoverflow.com/questions/3695224/sqlite-getting-nearest-locations-with-latitude-and-longitude
         *
         * @param range
         *            Range in meters
         * @param bearing
         *            Bearing in degrees, (N = 90, E = 0, S = 270, W = 180
         * @return End-point from the source given the desired range and bearing.
         */
        public Pos calculateDerivedPosition(double range, double bearing)
        {
            double EarthRadius = 6371000; // m
            double angularDistance = range / EarthRadius;
            double latA = Math.toRadians(this.latitude);
            double lonA = Math.toRadians(this.longitude);
            double trueCourse = Math.toRadians(bearing);

            double lat = Math.asin(
                    Math.sin(latA) * Math.cos(angularDistance) +
                            Math.cos(latA) * Math.sin(angularDistance) * Math.cos(trueCourse));

            double dlon = Math.atan2(
                    Math.sin(trueCourse) * Math.sin(angularDistance) * Math.cos(latA),
                    Math.cos(angularDistance) - Math.sin(latA) * Math.sin(lat));

            double lon = ((lonA + dlon + Math.PI) % (Math.PI * 2)) - Math.PI;

            lat = Math.toDegrees(lat);
            lon = Math.toDegrees(lon);

            return new Pos(lon, lat);
        }
    }
}
