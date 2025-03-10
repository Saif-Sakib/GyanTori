package com.models;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class Book {
    // MongoDB ID
    private String _id;
    // Application ID
    private String id;

    // Book metadata
    private String title;
    private String author;
    private String publisher;
    private String publicationDate;
    private String language;
    private int pages;
    private String isbn;
    private String[] categories;
    private String description;
    private String imageUrl;
    private String bookHubId;

    // Price information
    private double originalPrice;
    private double currentPrice;
    private double discount;

    // Review information
    private double rating;
    private int reviewCount;

    // Transaction information
    private String sellerId;
    private String uploadDate;
    private int totalPurchases;
    private String holderId;
    private String borrowDate;
    private String returnDate;
    private List<Review> buyerReviews = new ArrayList<>();
    private boolean featured;

    // Inner class for buyer reviews
    public static class Review {
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

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            Review review = (Review) o;
            return Double.compare(review.rating, rating) == 0 &&
                    Objects.equals(reviewerId, review.reviewerId) &&
                    Objects.equals(comment, review.comment) &&
                    Objects.equals(reviewDate, review.reviewDate);
        }

        @Override
        public int hashCode() {
            return Objects.hash(reviewerId, comment, rating, reviewDate);
        }
    }

    // Default constructor for serialization
    public Book() {
        // ArrayList already initialized in field declaration
    }

    // Minimal constructor for essential fields
    public Book(String id, String title, String author, String imageUrl, double originalPrice, double currentPrice,
            double rating, int reviewCount) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.imageUrl = imageUrl;
        this.originalPrice = originalPrice;
        this.currentPrice = currentPrice;
        this.rating = rating;
        this.reviewCount = reviewCount;
    }

    // Builder pattern for constructing Book objects
    public static class Builder {
        private final Book book;

        public Builder() {
            book = new Book();
        }

        public Builder id(String id) {
            book.id = id;
            return this;
        }

        public Builder _id(String _id) {
            book._id = _id;
            return this;
        }

        public Builder title(String title) {
            book.title = title;
            return this;
        }

        public Builder author(String author) {
            book.author = author;
            return this;
        }

        public Builder publisher(String publisher) {
            book.publisher = publisher;
            return this;
        }

        public Builder publicationDate(String publicationDate) {
            book.publicationDate = publicationDate;
            return this;
        }

        public Builder language(String language) {
            book.language = language;
            return this;
        }

        public Builder pages(int pages) {
            book.pages = pages;
            return this;
        }

        public Builder isbn(String isbn) {
            book.isbn = isbn;
            return this;
        }

        public Builder categories(String[] categories) {
            book.categories = categories;
            return this;
        }

        public Builder originalPrice(double originalPrice) {
            book.originalPrice = originalPrice;
            return this;
        }

        public Builder currentPrice(double currentPrice) {
            book.currentPrice = currentPrice;
            return this;
        }

        public Builder discount(double discount) {
            book.discount = discount;
            return this;
        }

        public Builder rating(double rating) {
            book.rating = rating;
            return this;
        }

        public Builder reviewCount(int reviewCount) {
            book.reviewCount = reviewCount;
            return this;
        }

        public Builder description(String description) {
            book.description = description;
            return this;
        }

        public Builder imageUrl(String imageUrl) {
            book.imageUrl = imageUrl;
            return this;
        }

        public Builder sellerId(String sellerId) {
            book.sellerId = sellerId;
            return this;
        }

        public Builder bookHubId(String bookHubId) {
            book.bookHubId = bookHubId;
            return this;
        }

        public Builder uploadDate(String uploadDate) {
            book.uploadDate = uploadDate;
            return this;
        }

        public Builder totalPurchases(int totalPurchases) {
            book.totalPurchases = totalPurchases;
            return this;
        }

        public Builder holderId(String holderId) {
            book.holderId = holderId;
            return this;
        }

        public Builder borrowDate(String borrowDate) {
            book.borrowDate = borrowDate;
            return this;
        }

        public Builder returnDate(String returnDate) {
            book.returnDate = returnDate;
            return this;
        }

        public Builder buyerReviews(List<Review> buyerReviews) {
            if (buyerReviews != null) {
                book.buyerReviews = new ArrayList<>(buyerReviews);
                book.recalculateRating();
            }
            return this;
        }

        public Builder featured(boolean featured) {
            book.featured = featured;
            return this;
        }

        public Book build() {
            return book;
        }
    }

    // Common constructor for list views that replaces the one with similar
    // parameters
    public Book(String id, String title, String author, String[] categories, double rating, double price,
            String imageUrl) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.categories = categories;
        this.rating = rating;
        this.currentPrice = price;
        this.imageUrl = imageUrl;
        // ArrayList already initialized in field declaration
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String get_id() {
        return _id;
    }

    public void set_id(String _id) {
        this._id = _id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getPublisher() {
        return publisher;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    public String getPublicationDate() {
        return publicationDate;
    }

    public void setPublicationDate(String publicationDate) {
        this.publicationDate = publicationDate;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public int getPages() {
        return pages;
    }

    public void setPages(int pages) {
        this.pages = pages;
    }

    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    public String[] getCategories() {
        return categories;
    }

    public void setCategories(String[] categories) {
        this.categories = categories;
    }

    public double getOriginalPrice() {
        return originalPrice;
    }

    public void setOriginalPrice(double originalPrice) {
        this.originalPrice = originalPrice;
    }

    public double getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(double currentPrice) {
        this.currentPrice = currentPrice;
    }

    public double getDiscount() {
        return discount;
    }

    public void setDiscount(double discount) {
        this.discount = discount;
    }

    public double getRating() {
        return Math.round(rating * 100.0) / 100.0;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    public Integer getReviewCount() {
        return reviewCount;
    }

    public void setReviewCount(int reviewCount) {
        this.reviewCount = reviewCount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getSellerId() {
        return sellerId;
    }

    public void setSellerId(String sellerId) {
        this.sellerId = sellerId;
    }

    public String getBookHubId() {
        return bookHubId;
    }

    public void setBookHubId(String bookHubId) {
        this.bookHubId = bookHubId;
    }

    public String getUploadDate() {
        return uploadDate;
    }

    public void setUploadDate(String uploadDate) {
        this.uploadDate = uploadDate;
    }

    public int getTotalPurchases() {
        return totalPurchases;
    }

    public void setTotalPurchases(int totalPurchases) {
        this.totalPurchases = totalPurchases;
    }

    public String getHolderId() {
        return holderId;
    }

    public void setHolderId(String holderId) {
        this.holderId = holderId;
    }

    public String getBorrowDate() {
        return borrowDate;
    }

    public void setBorrowDate(String borrowDate) {
        this.borrowDate = borrowDate;
    }

    public String getReturnDate() {
        return returnDate;
    }

    public void setReturnDate(String returnDate) {
        this.returnDate = returnDate;
    }

    public List<Review> getBuyerReviews() {
        return Collections.unmodifiableList(buyerReviews);
    }

    public void setBuyerReviews(List<Review> buyerReviews) {
        this.buyerReviews = Optional.ofNullable(buyerReviews)
                .map(ArrayList::new)
                .orElseGet(ArrayList::new);
        recalculateRating();
    }

    public boolean isFeatured() {
        return featured;
    }

    public void setFeatured(boolean featured) {
        this.featured = featured;
    }

    // Helper methods
    public void addReview(Review review) {
        if (review == null) {
            return;
        }
        this.buyerReviews.add(review);
        recalculateRating();
    }

    public void removeReview(String reviewerId) {
        if (reviewerId == null || reviewerId.isEmpty()) {
            return;
        }
        this.buyerReviews.removeIf(review -> reviewerId.equals(review.getReviewerId()));
        recalculateRating();
    }

    private void recalculateRating() {
        if (buyerReviews.isEmpty()) {
            this.rating = 0;
            this.reviewCount = 0;
            return;
        }

        this.rating = buyerReviews.stream()
                .mapToDouble(Review::getRating)
                .average()
                .orElse(0);
        this.reviewCount = buyerReviews.size();
    }

    @Override
    public String toString() {
        return "Book{" +
                "_id='" + _id + '\'' +
                ", id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", author='" + author + '\'' +
                ", publisher='" + publisher + '\'' +
                ", publicationDate='" + publicationDate + '\'' +
                ", language='" + language + '\'' +
                ", pages=" + pages +
                ", isbn='" + isbn + '\'' +
                ", categories=" + (categories != null ? Arrays.toString(categories) : "null") +
                ", description='" + truncateString(description, 50) + '\'' +
                ", imageUrl='" + imageUrl + '\'' +
                ", originalPrice=" + originalPrice +
                ", currentPrice=" + currentPrice +
                ", discount=" + discount +
                ", rating=" + rating +
                ", reviewCount=" + reviewCount +
                ", sellerId='" + sellerId + '\'' +
                ", uploadDate='" + uploadDate + '\'' +
                ", totalPurchases=" + totalPurchases +
                ", holderId='" + holderId + '\'' +
                ", borrowDate='" + borrowDate + '\'' +
                ", returnDate='" + returnDate + '\'' +
                ", reviewCount=" + buyerReviews.size() +
                ", featured=" + featured +
                '}';
    }

    private String truncateString(String str, int maxLength) {
        if (str == null) {
            return "null";
        }
        return str.length() <= maxLength ? str : str.substring(0, maxLength) + "...";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Book book = (Book) o;
        return pages == book.pages &&
                Double.compare(book.originalPrice, originalPrice) == 0 &&
                Double.compare(book.currentPrice, currentPrice) == 0 &&
                Double.compare(book.discount, discount) == 0 &&
                Double.compare(book.rating, rating) == 0 &&
                reviewCount == book.reviewCount &&
                totalPurchases == book.totalPurchases &&
                featured == book.featured &&
                Objects.equals(_id, book._id) &&
                Objects.equals(id, book.id) &&
                Objects.equals(title, book.title) &&
                Objects.equals(author, book.author) &&
                Objects.equals(publisher, book.publisher) &&
                Objects.equals(publicationDate, book.publicationDate) &&
                Objects.equals(language, book.language) &&
                Objects.equals(isbn, book.isbn) &&
                Arrays.equals(categories, book.categories) &&
                Objects.equals(description, book.description) &&
                Objects.equals(imageUrl, book.imageUrl) &&
                Objects.equals(sellerId, book.sellerId) &&
                Objects.equals(uploadDate, book.uploadDate) &&
                Objects.equals(holderId, book.holderId) &&
                Objects.equals(borrowDate, book.borrowDate) &&
                Objects.equals(returnDate, book.returnDate);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(_id, id, title, author, publisher, publicationDate, language, pages, isbn,
                originalPrice, currentPrice, discount, rating, reviewCount, description, imageUrl,
                sellerId, uploadDate, totalPurchases, holderId, borrowDate, returnDate, featured);
        result = 31 * result + Arrays.hashCode(categories);
        return result;
    }

    // Static factory methods for common book types
    public static Book createMinimalBook(String id, String title, String author) {
        return new Builder()
                .id(id)
                .title(title)
                .author(author)
                .build();
    }

    public static Book createFullBook(String title, String author, String publisher, String publicationDate,
            String language, int pages, String isbn, String[] categories,
            double originalPrice, double currentPrice, double discount,
            double rating, int reviewCount, String description) {
        return new Builder()
                .title(title)
                .author(author)
                .publisher(publisher)
                .publicationDate(publicationDate)
                .language(language)
                .pages(pages)
                .isbn(isbn)
                .categories(categories)
                .originalPrice(originalPrice)
                .currentPrice(currentPrice)
                .discount(discount)
                .rating(rating)
                .reviewCount(reviewCount)
                .description(description)
                .build();
    }
}