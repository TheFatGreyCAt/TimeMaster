package com.example.timemaster.ml;

import android.graphics.RectF;

public interface SimilarityClassifier {

    class Recognition {

        private final String id;
        private final String title;
        private final Float distance;
        private RectF location;

        public Recognition(final String id, final String title, final Float distance) {
            this.id = id;
            this.title = title;
            this.distance = distance;
            this.location = null;
        }

        public void setLocation(RectF location) {
            this.location = location;
        }

        public String getId() {
            return id;
        }

        public String getTitle() {
            return title;
        }

        public Float getDistance() {
            return distance;
        }

        @Override
        public String toString() {
            String resultString = "";
            if (id != null) {
                resultString += "[" + id + "] ";
            }
            if (title != null) {
                resultString += title + " ";
            }
            if (distance != null) {
                resultString += String.format("(%.1f%%) ", distance * 100.0f);
            }
            return resultString.trim();
        }
    }
}
