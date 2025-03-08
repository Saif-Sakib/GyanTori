package com.services;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.models.Book;

public class BookAPIService {
    private final HttpClient httpClient;
    private final String apiBaseUrl = "https://openlibrary.org/api";
    private final String searchBaseUrl = "https://openlibrary.org/search.json";

    /**
     * Constructor for BookAPIService.
     */
    public BookAPIService() {
        this.httpClient = HttpClient.newHttpClient();
    }

    /**
     * Fetches book details from the Open Library API using ISBN.
     *
     * @param isbn The ISBN (International Standard Book Number) to look up
     * @return Book object containing the book's details
     * @throws BookServiceException if there's an error fetching or parsing the book
     *                              data
     */
    public Book fetchBookByISBN(String isbn) throws BookServiceException {
        if (isbn == null || isbn.trim().isEmpty()) {
            throw new IllegalArgumentException("ISBN cannot be null or empty");
        }

        try {
            // Build the Open Library API request URL with the ISBN
            // Format:
            // https://openlibrary.org/api/books?bibkeys=ISBN:9780151010264&format=json&jscmd=data
            String requestUrl = apiBaseUrl + "/books?bibkeys=ISBN:" +
                    URLEncoder.encode(isbn, StandardCharsets.UTF_8) +
                    "&format=json&jscmd=data";

            // Create HTTP request
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(requestUrl))
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            // Send the request and get the response
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            // Check if the request was successful (status code 200)
            if (response.statusCode() == 200) {
                String jsonResponse = response.body();
                // Check if any books were found
                if (jsonResponse.equals("{}") || jsonResponse.trim().isEmpty()) {
                    throw new BookNotFoundException("Book with ISBN " + isbn + " not found");
                }

                // Parse the JSON response to a Book object
                return parseBookFromJson(jsonResponse);
            } else if (response.statusCode() == 404) {
                throw new BookNotFoundException("Book with ISBN " + isbn + " not found");
            } else {
                throw new BookServiceException("API returned status code: " + response.statusCode());
            }
        } catch (IOException e) {
            throw new BookServiceException("Error connecting to book API: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BookServiceException("API request was interrupted", e);
        }
    }

    /**
     * Searches for books by title, author, or keywords.
     * 
     * @param query The search query
     * @param limit Maximum number of results to return
     * @return List of Book objects matching the search criteria
     * @throws BookServiceException if there's an error fetching or parsing the book
     *                              data
     */
    public List<Book> searchBooks(String query, int limit) throws BookServiceException {
        if (query == null || query.trim().isEmpty()) {
            throw new IllegalArgumentException("Search query cannot be null or empty");
        }

        try {
            // Build the API request URL for search
            // Format: https://openlibrary.org/search.json?q=the+lord+of+the+rings&limit=10
            String requestUrl = searchBaseUrl + "?q=" +
                    URLEncoder.encode(query, StandardCharsets.UTF_8) +
                    "&limit=" + limit;

            // Create HTTP request
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(requestUrl))
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            // Send the request and get the response
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            // Check if the request was successful (status code 200)
            if (response.statusCode() == 200) {
                // Parse the JSON response to a list of Book objects
                return parseBooksFromJson(response.body());
            } else if (response.statusCode() == 404) {
                // Return empty list if no results found
                return new ArrayList<>();
            } else {
                throw new BookServiceException("API returned status code: " + response.statusCode());
            }
        } catch (IOException e) {
            throw new BookServiceException("Error connecting to book API: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BookServiceException("API request was interrupted", e);
        }
    }

    /**
     * Parse a single book from JSON string using simple regex-based parsing
     * 
     * @param json The JSON string to parse
     * @return A Book object
     */
    private Book parseBookFromJson(String json) throws BookServiceException {
        try {
            // Create a book builder to construct the book
            Book.Builder bookBuilder = new Book.Builder();

            // Extract the ISBN key from response
            Pattern isbnKeyPattern = Pattern.compile("\"ISBN:(\\d+)\"");
            Matcher isbnKeyMatcher = isbnKeyPattern.matcher(json);
            String isbn = isbnKeyMatcher.find() ? isbnKeyMatcher.group(1) : null;
            bookBuilder.isbn(isbn);

            // Parse book ID - using Open Library ID if available
            String id = extractStringValue(json, "key");
            if (id != null) {
                // Remove '/books/' prefix if present
                id = id.replace("/books/", "");
                bookBuilder.id(id);
                bookBuilder.bookHubId(id);
            }

            // Parse title
            bookBuilder.title(extractStringValue(json, "title"));

            // Parse author - Open Library provides authors as an array
            String authorName = extractAuthorName(json);
            bookBuilder.author(authorName);

            // Parse publisher - typically the first one in the array
            String publisher = extractFirstPublisher(json);
            bookBuilder.publisher(publisher);

            // Parse publication date
            String publishDate = extractStringValue(json, "publish_date");
            bookBuilder.publicationDate(publishDate);

            // Parse number of pages
            String pagesStr = extractNumberValue(json, "number_of_pages");
            if (pagesStr != null && !pagesStr.isEmpty()) {
                bookBuilder.pages(Integer.parseInt(pagesStr));
            }

            // Parse language - Open Library provides language codes
            String language = extractLanguage(json);
            bookBuilder.language(language);

            // Parse cover image URL
            String coverUrl = extractCoverUrl(json);
            bookBuilder.imageUrl(coverUrl);

            // Parse description - Open Library may provide description in different formats
            String description = extractDescription(json);
            bookBuilder.description(description);

            // Parse subjects/categories
            String[] categories = extractCategories(json);
            if (categories != null) {
                bookBuilder.categories(categories);
            }

            // Other fields will be left as defaults or null
            // since Open Library API doesn't provide information about:
            // - pricing
            // - ratings/reviews
            // - purchase information
            // - borrower information

            return bookBuilder.build();
        } catch (Exception e) {
            throw new BookServiceException("Error parsing book JSON: " + e.getMessage(), e);
        }
    }

    /**
     * Parse a list of books from JSON string
     * 
     * @param json The JSON string to parse
     * @return A list of Book objects
     */
    private List<Book> parseBooksFromJson(String json) throws BookServiceException {
        List<Book> books = new ArrayList<>();

        try {
            // Extract the number of results
            Pattern numFoundPattern = Pattern.compile("\"numFound\"\\s*:\\s*(\\d+)");
            Matcher numFoundMatcher = numFoundPattern.matcher(json);
            int numFound = numFoundMatcher.find() ? Integer.parseInt(numFoundMatcher.group(1)) : 0;

            if (numFound == 0) {
                return books; // Empty list, no results found
            }

            // Check if docs array exists in response
            Pattern docsPattern = Pattern.compile("\"docs\"\\s*:\\s*\\[(.*?)\\]", Pattern.DOTALL);
            Matcher docsMatcher = docsPattern.matcher(json);

            if (docsMatcher.find()) {
                String docsJson = docsMatcher.group(1);

                // Find all book objects in the JSON array
                Pattern bookPattern = Pattern.compile("\\{[^\\{\\}]*((\\{[^\\{\\}]*\\})[^\\{\\}]*)*\\}",
                        Pattern.DOTALL);
                Matcher bookMatcher = bookPattern.matcher(docsJson);

                while (bookMatcher.find()) {
                    String bookJson = bookMatcher.group();
                    // Create a book from the search result data
                    books.add(parseSearchResultToBook(bookJson));
                }
            }

            return books;
        } catch (Exception e) {
            throw new BookServiceException("Error parsing books JSON: " + e.getMessage(), e);
        }
    }

    /**
     * Parse a book from search result JSON
     * The search API returns different JSON structure compared to the books API
     */
    private Book parseSearchResultToBook(String json) {
        Book.Builder bookBuilder = new Book.Builder();

        // Extract basic book information
        bookBuilder.id(extractStringValue(json, "key"));
        bookBuilder.title(extractStringValue(json, "title"));

        // Extract ISBN
        String[] isbns = extractStringArray(json, "isbn");
        if (isbns != null && isbns.length > 0) {
            bookBuilder.isbn(isbns[0]); // Use the first ISBN
        }

        // Extract author name
        String[] authorNames = extractStringArray(json, "author_name");
        if (authorNames != null && authorNames.length > 0) {
            bookBuilder.author(authorNames[0]); // Use the first author
        }

        // Extract publisher
        String[] publishers = extractStringArray(json, "publisher");
        if (publishers != null && publishers.length > 0) {
            bookBuilder.publisher(publishers[0]); // Use the first publisher
        }

        // Extract publication year
        String firstPubYear = extractStringValue(json, "first_publish_year");
        if (firstPubYear != null) {
            bookBuilder.publicationDate(firstPubYear);
        }

        // Extract number of pages
        String pagesStr = extractNumberValue(json, "number_of_pages_median");
        if (pagesStr != null && !pagesStr.isEmpty()) {
            try {
                bookBuilder.pages(Integer.parseInt(pagesStr));
            } catch (NumberFormatException e) {
                // Ignore if not a valid number
            }
        }

        // Extract language
        String[] languages = extractStringArray(json, "language");
        if (languages != null && languages.length > 0) {
            bookBuilder.language(languages[0]); // Use the first language
        }

        // Extract cover ID and create image URL
        String coverIdStr = extractNumberValue(json, "cover_i");
        if (coverIdStr != null && !coverIdStr.isEmpty()) {
            try {
                int coverId = Integer.parseInt(coverIdStr);
                bookBuilder.imageUrl("https://covers.openlibrary.org/b/id/" + coverId + "-L.jpg");
            } catch (NumberFormatException e) {
                // Ignore if not a valid number
            }
        }

        // Extract subjects/categories
        String[] subjects = extractStringArray(json, "subject");
        if (subjects != null) {
            bookBuilder.categories(subjects);
        }

        return bookBuilder.build();
    }

    // Additional helper methods for Open Library specific parsing

    private String extractAuthorName(String json) {
        Pattern pattern = Pattern.compile("\"authors\"\\s*:\\s*\\[(.*?)\\]", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(json);

        if (matcher.find()) {
            String authorsJson = matcher.group(1);
            Pattern namePattern = Pattern.compile("\"name\"\\s*:\\s*\"([^\"]*)\"");
            Matcher nameMatcher = namePattern.matcher(authorsJson);

            if (nameMatcher.find()) {
                return nameMatcher.group(1);
            }
        }

        return null;
    }

    private String extractFirstPublisher(String json) {
        Pattern pattern = Pattern.compile("\"publishers\"\\s*:\\s*\\[(.*?)\\]", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(json);

        if (matcher.find()) {
            String publishersJson = matcher.group(1);
            Pattern namePattern = Pattern.compile("\"name\"\\s*:\\s*\"([^\"]*)\"");
            Matcher nameMatcher = namePattern.matcher(publishersJson);

            if (nameMatcher.find()) {
                return nameMatcher.group(1);
            } else {
                // Try direct string extraction if publisher is just a string in array
                Pattern strPattern = Pattern.compile("\"([^\"]*)\"");
                Matcher strMatcher = strPattern.matcher(publishersJson);
                if (strMatcher.find()) {
                    return strMatcher.group(1);
                }
            }
        }

        return null;
    }

    private String extractLanguage(String json) {
        Pattern pattern = Pattern.compile("\"languages\"\\s*:\\s*\\[(.*?)\\]", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(json);

        if (matcher.find()) {
            String languagesJson = matcher.group(1);
            // Extract language key, typically in format "/languages/eng"
            Pattern keyPattern = Pattern.compile("\"key\"\\s*:\\s*\"(/languages/[^\"]*)\"");
            Matcher keyMatcher = keyPattern.matcher(languagesJson);

            if (keyMatcher.find()) {
                String langKey = keyMatcher.group(1);
                // Extract the language code from the key
                return langKey.substring(langKey.lastIndexOf('/') + 1);
            }
        }

        return null;
    }

    private String extractCoverUrl(String json) {
        // Try to find cover ID
        Pattern pattern = Pattern.compile("\"cover\"\\s*:\\s*\\{(.*?)\\}", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(json);

        if (matcher.find()) {
            String coverJson = matcher.group(1);
            Pattern largePattern = Pattern.compile("\"large\"\\s*:\\s*\"([^\"]*)\"");
            Matcher largeMatcher = largePattern.matcher(coverJson);

            if (largeMatcher.find()) {
                return largeMatcher.group(1);
            }

            // If large is not available, try medium
            Pattern mediumPattern = Pattern.compile("\"medium\"\\s*:\\s*\"([^\"]*)\"");
            Matcher mediumMatcher = mediumPattern.matcher(coverJson);

            if (mediumMatcher.find()) {
                return mediumMatcher.group(1);
            }
        }

        // Alternative: check for cover_i field and construct URL
        String coverId = extractNumberValue(json, "cover_i");
        if (coverId != null && !coverId.isEmpty()) {
            return "https://covers.openlibrary.org/b/id/" + coverId + "-L.jpg";
        }

        return null;
    }

    private String extractDescription(String json) {
        // Try value as direct string
        String description = extractStringValue(json, "description");
        if (description != null) {
            return description;
        }

        // Try as object with "value" field
        Pattern pattern = Pattern.compile("\"description\"\\s*:\\s*\\{(.*?)\\}", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(json);

        if (matcher.find()) {
            String descJson = matcher.group(1);
            Pattern valuePattern = Pattern.compile("\"value\"\\s*:\\s*\"([^\"]*)\"");
            Matcher valueMatcher = valuePattern.matcher(descJson);

            if (valueMatcher.find()) {
                return valueMatcher.group(1);
            }
        }

        return null;
    }

    private String[] extractCategories(String json) {
        // Try to extract subjects
        String[] subjects = extractStringArray(json, "subjects");
        if (subjects != null && subjects.length > 0) {
            return subjects;
        }

        // Try to extract subject_places as fallback
        return extractStringArray(json, "subject_places");
    }

    // Helper methods for JSON parsing

    private String extractStringValue(String json, String key) {
        Pattern pattern = Pattern.compile("\"" + key + "\"\\s*:\\s*\"([^\"]*)\"");
        Matcher matcher = pattern.matcher(json);
        return matcher.find() ? matcher.group(1) : null;
    }

    private String extractNumberValue(String json, String key) {
        Pattern pattern = Pattern.compile("\"" + key + "\"\\s*:\\s*([0-9.]+)");
        Matcher matcher = pattern.matcher(json);
        return matcher.find() ? matcher.group(1) : null;
    }

    private String extractBooleanValue(String json, String key) {
        Pattern pattern = Pattern.compile("\"" + key + "\"\\s*:\\s*(true|false)");
        Matcher matcher = pattern.matcher(json);
        return matcher.find() ? matcher.group(1) : null;
    }

    private String[] extractStringArray(String json, String key) {
        Pattern pattern = Pattern.compile("\"" + key + "\"\\s*:\\s*\\[(.*?)\\]", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(json);

        if (matcher.find()) {
            String arrayContent = matcher.group(1);
            if (arrayContent.trim().isEmpty()) {
                return new String[0];
            }

            // Extract individual strings from the array
            Pattern itemPattern = Pattern.compile("\"([^\"]*)\"");
            Matcher itemMatcher = itemPattern.matcher(arrayContent);
            List<String> items = new ArrayList<>();

            while (itemMatcher.find()) {
                items.add(itemMatcher.group(1));
            }

            return items.toArray(new String[0]);
        }

        return null;
    }

    /**
     * Exception for book service related errors.
     */
    public static class BookServiceException extends Exception {
        public BookServiceException(String message) {
            super(message);
        }

        public BookServiceException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Exception for when a book is not found.
     */
    public static class BookNotFoundException extends BookServiceException {
        public BookNotFoundException(String message) {
            super(message);
        }
    }
}