package com.models;

import java.time.LocalDate;

public class Review {
        private String reviewerId;
        private String comment;
        private double rating;
        private LocalDate reviewDate;

        // Default constructor for serialization
        public Review() {
        }

        public Review(String reviewerId, String comment, double rating, LocalDate reviewDate) {
            this.reviewerId = reviewerId;
            this.comment = comment;
            this.rating = rating;
            this.reviewDate = reviewDate;
        }

        public String getReviewerId() {
            return reviewerId;
        }

        public void setReviewerId(String reviewerId) {
            this.reviewerId = reviewerId;
        }

        public double getRating() {
            return rating;
        }

        public void setRating(double rating) {
            this.rating = rating;
        }

        public LocalDate getReviewDate() {
            return reviewDate;
        }

        public void setReviewDate(LocalDate reviewDate) {
            this.reviewDate = reviewDate;
        }

        public String getComment() {
            return comment;
        }

        public void setComment(String comment) {
            this.comment = comment;
        }

        @Override
        public String toString() {
            return "Review{" +
                    "reviewerId='" + reviewerId + '\'' +
                    ", comment='" + comment + '\'' +
                    ", rating=" + rating +
                    ", reviewDate=" + reviewDate +
                    '}';
        }
    }
