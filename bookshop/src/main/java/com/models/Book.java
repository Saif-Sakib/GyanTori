package com.models;

public class Book {
    private String Id;
    private String title;
    private String author;
    private String publisher;
    private String publicationDate;
    private String language;
    private int pages;
    private String isbn;
    private String[] categories;
    private double originalPrice;
    private double currentPrice;
    private double discount;
    private double rating;
    private int reviewCount;
    private String description;
    private String imageUrl;

    public Book(){

    }

    // Constructor
    public Book(String title, String author, String publisher, String publicationDate, String language, int pages,
            String isbn, String[] categories, double originalPrice, double currentPrice, double discount,
            double rating, int reviewCount, String description) {
        this.title = title;
        this.author = author;
        this.publisher = publisher;
        this.publicationDate = publicationDate;
        this.language = language;
        this.pages = pages;
        this.isbn = isbn;
        this.categories = categories;
        this.originalPrice = originalPrice;
        this.currentPrice = currentPrice;
        this.discount = discount;
        this.rating = rating;
        this.reviewCount = reviewCount;
        this.description = description;
    }

    public Book(String Id, String title, String author, String[] categories, double rating, double price, String imageUrl) {
        this.Id = Id;
        this.title = title;
        this.author = author;
        this.categories = categories;
        this.rating = rating;
        this.currentPrice = price;
        this.imageUrl = imageUrl;
    }

    // Getters and setters
    public String getId() {
        return Id;
    }

    public void setId(String Id) {
        this.Id = Id;
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
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    public int getReviewCount() {
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

    // toString method for easy printing
    @Override
    public String toString() {
        return "Book{" +
                "Id='" + Id + '\'' +
                ", title='" + title + '\'' +
                ", author='" + author + '\'' +
                ", publisher='" + publisher + '\'' +
                ", publicationDate='" + publicationDate + '\'' +
                ", language='" + language + '\'' +
                ", pages=" + pages +
                ", isbn='" + isbn + '\'' +
                ", categories=" + (categories != null ? String.join(", ", categories) : "null") +
                ", originalPrice=" + originalPrice +
                ", currentPrice=" + currentPrice +
                ", discount=" + discount +
                ", rating=" + rating +
                ", reviewCount=" + reviewCount +
                ", description='" + description + '\'' +
                ", imageUrl='" + imageUrl + '\'' +
                '}';
    }
}