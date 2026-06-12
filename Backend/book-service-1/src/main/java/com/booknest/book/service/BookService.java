package com.booknest.book.service;

import com.booknest.book.document.BookDocument;
import com.booknest.book.dto.BookRequest;
import com.booknest.book.dto.BookResponse;
import com.booknest.book.entity.Book;
import com.booknest.book.exception.ResourceNotFoundException;
import com.booknest.book.repository.BookRepository;
import com.booknest.book.repository.BookSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

// This service implements the central business logic for catalog book operations in BookNest.
@Service
@RequiredArgsConstructor
@Slf4j
public class BookService {

    /**
     * Repository used to perform SQL CRUD actions on relational Book entities.
     */
    private final BookRepository bookRepository;

    /**
     * Elasticsearch repository used to perform high-speed fuzzy search lookups.
     */
    private final BookSearchRepository bookSearchRepository;

    /**
     * Utility service to upload, persist, and delete physical book cover images.
     */
    private final ImageStorageService imageStorageService;

    // Converts a database Book entity into a BookResponse data transfer object (DTO).
    private BookResponse convertToResponse(Book book) {
        BookResponse response = new BookResponse();
        response.setBookId(book.getBookId());
        response.setTitle(book.getTitle());
        response.setAuthor(book.getAuthor());
        response.setIsbn(book.getIsbn());
        response.setGenre(book.getGenre());
        response.setPublisher(book.getPublisher());
        response.setPrice(book.getPrice());
        response.setStock(book.getStock());
        response.setRating(book.getRating());
        response.setDescription(book.getDescription());
        response.setCoverImageUrl(book.getCoverImageUrl());
        response.setPublishedDate(book.getPublishedDate());
        response.setFeatured(book.getFeatured());
        response.setActive(book.getActive());
        response.setCreatedAt(book.getCreatedAt());
        response.setInStock(book.getStock() > 0);
        return response;
    }

    // Converts a database Book entity into an Elasticsearch BookDocument index model.
    private BookDocument convertToDocument(Book book) {
        BookDocument doc = new BookDocument();
        doc.setBookId(String.valueOf(book.getBookId()));
        doc.setTitle(book.getTitle());
        doc.setAuthor(book.getAuthor());
        doc.setGenre(book.getGenre());
        doc.setPublisher(book.getPublisher());
        doc.setPrice(book.getPrice());
        doc.setStock(book.getStock());
        doc.setRating(book.getRating());
        doc.setDescription(book.getDescription());
        doc.setCoverImageUrl(book.getCoverImageUrl());
        doc.setFeatured(book.getFeatured());
        doc.setActive(book.getActive());
        return doc;
    }

    // Converts an Elasticsearch BookDocument into a standard BookResponse DTO.
    private BookResponse convertDocToResponse(BookDocument doc) {
        BookResponse resp = new BookResponse();
        resp.setBookId(Long.parseLong(doc.getBookId()));
        resp.setTitle(doc.getTitle());
        resp.setAuthor(doc.getAuthor());
        resp.setGenre(doc.getGenre());
        resp.setPublisher(doc.getPublisher());
        resp.setPrice(doc.getPrice());
        resp.setStock(doc.getStock());
        resp.setRating(doc.getRating());
        resp.setDescription(doc.getDescription());
        resp.setCoverImageUrl(doc.getCoverImageUrl());
        resp.setFeatured(doc.getFeatured());
        resp.setActive(doc.getActive());
        resp.setInStock(doc.getStock() != null && doc.getStock() > 0);
        return resp;
    }

    // Safely synchronizes a Book entity state into the Elasticsearch search index.
    private void syncToElasticsearch(Book book) {
        try {
            bookSearchRepository.save(convertToDocument(book));
            log.info("Book {} synced to Elasticsearch", book.getBookId());
        } catch (Exception e) {
            log.warn("Failed to sync book {} to Elasticsearch: {}", book.getBookId(), e.getMessage());
        }
    }

    // Checks if the caller has the required ADMIN privileges.
    private void verifyAdmin(String role) {
        if (!"ADMIN".equals(role)) {
            throw new RuntimeException("Access denied. Admin privileges required.");
        }
    }

    // Registers a new book in the catalog.
    @Transactional
    public BookResponse addBook(BookRequest request, String role) {
        log.info("Adding new book: {}", request.getTitle());
        verifyAdmin(role);
        
        // Prevent duplicate ISBN entries to maintain unique catalog indexing.
        if (request.getIsbn() != null && !request.getIsbn().isBlank() && bookRepository.existsByIsbn(request.getIsbn())) {
            throw new RuntimeException("ISBN already exists: " + request.getIsbn());
        }
        
        Book book = new Book();
        book.setTitle(request.getTitle());
        book.setAuthor(request.getAuthor());
        book.setIsbn(request.getIsbn());
        book.setGenre(request.getGenre());
        book.setPublisher(request.getPublisher());
        book.setPrice(request.getPrice());
        book.setStock(request.getStock());
        book.setDescription(request.getDescription());
        
        if (request.getPublishedDate() != null && !request.getPublishedDate().isBlank()) {
            book.setPublishedDate(LocalDate.parse(request.getPublishedDate()));
        }
        // Explicitly set active=true so new books are visible in the catalog.
        // Do NOT rely on the entity field default — JPA/Lombok NoArgsConstructor
        // can bypass field initializers leading to active=false in the DB.
        book.setActive(true);
        book.setFeatured(request.getFeatured() != null ? request.getFeatured() : false);
        
        Book saved = bookRepository.save(book);
        
        // Push newly saved record to search index.
        syncToElasticsearch(saved);
        return convertToResponse(saved);
    }

    // Updates structural information of an existing book.
    @Transactional
    public BookResponse updateBook(Long bookId, BookRequest request, String role) {
        log.info("Updating book: {}", bookId);
        verifyAdmin(role);
        
        Book book = bookRepository.findByBookIdAndActiveTrue(bookId)
            .orElseThrow(() -> new ResourceNotFoundException("Book not found id: " + bookId));
            
        book.setTitle(request.getTitle());
        book.setAuthor(request.getAuthor());
        book.setGenre(request.getGenre());
        book.setPublisher(request.getPublisher());
        book.setPrice(request.getPrice());
        book.setStock(request.getStock());
        book.setDescription(request.getDescription());
        book.setUpdatedAt(LocalDateTime.now());
        
        if (request.getPublishedDate() != null && !request.getPublishedDate().isBlank()) {
            book.setPublishedDate(LocalDate.parse(request.getPublishedDate()));
        }
        if (request.getFeatured() != null) {
            book.setFeatured(request.getFeatured());
        }
        
        Book updated = bookRepository.save(book);
        syncToElasticsearch(updated);
        return convertToResponse(updated);
    }

    // Soft-deletes a book from the catalog by marking its active flag as false.
    @Transactional
    public void deleteBook(Long bookId, String role) {
        log.info("Deleting book: {}", bookId);
        verifyAdmin(role);
        
        Book book = bookRepository.findById(bookId)
            .orElseThrow(() -> new ResourceNotFoundException("Book not found id: " + bookId));
            
        book.setActive(false);
        book.setUpdatedAt(LocalDateTime.now());
        bookRepository.save(book);
        
        // Remove document from Elasticsearch index so it immediately disappears from search queries.
        try {
            bookSearchRepository.deleteById(String.valueOf(bookId));
        } catch (Exception e) {
            log.warn("Could not remove book from Elasticsearch: {}", e.getMessage());
        }
    }

    // Explicitly sets a book's physical inventory stock quantity.
    @Transactional
    public BookResponse updateStock(Long bookId, Integer quantity, String role) {
        log.info("Updating stock for book {} to {}", bookId, quantity);
        verifyAdmin(role);
        
        Book book = bookRepository.findById(bookId)
            .orElseThrow(() -> new ResourceNotFoundException("Book not found id: " + bookId));
            
        book.setStock(quantity);
        book.setUpdatedAt(LocalDateTime.now());
        Book updated = bookRepository.save(book);
        syncToElasticsearch(updated);
        return convertToResponse(updated);
    }

    // Toggles whether a book is featured on the application's homepage.
    @Transactional
    public BookResponse toggleFeatured(Long bookId, String role) {
        log.info("Toggling featured status for book: {}", bookId);
        verifyAdmin(role);
        
        Book book = bookRepository.findById(bookId)
            .orElseThrow(() -> new RuntimeException("Book not found id: " + bookId));
            
        book.setFeatured(!book.getFeatured());
        book.setUpdatedAt(LocalDateTime.now());
        Book updated = bookRepository.save(book);
        syncToElasticsearch(updated);
        return convertToResponse(updated);
    }

    // Uploads and configures a cover image for a book.
    @Transactional
    public BookResponse uploadCoverImage(Long bookId, MultipartFile file, String role) {
        log.info("Uploading cover image for book: {}", bookId);
        verifyAdmin(role);
        
        Book book = bookRepository.findById(bookId)
            .orElseThrow(() -> new RuntimeException("Book not found id: " + bookId));
            
        // Delete old cover image if present to free disk space.
        if (book.getCoverImageUrl() != null) {
            imageStorageService.deleteImage(book.getCoverImageUrl());
        }
        
        String imagePath = imageStorageService.saveImage(file);
        book.setCoverImageUrl(imagePath);
        book.setUpdatedAt(LocalDateTime.now());
        Book updated = bookRepository.save(book);
        syncToElasticsearch(updated);
        return convertToResponse(updated);
    }

    // Retrieves all active catalog book entities.
    public List<BookResponse> getAllBooks() {
        log.info("Fetching all active books");
        return bookRepository.findByActiveTrue().stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
    }

    // Fetches a single book by database ID.
    public BookResponse getBookById(Long bookId) {
        log.info("Fetching book by ID: {}", bookId);
        Book book = bookRepository.findByBookIdAndActiveTrue(bookId)
            .orElseThrow(() -> new RuntimeException("Book not found id: " + bookId));
        return convertToResponse(book);
    }

    // Implements resilient fuzzy searching on the book catalog.
    public List<BookResponse> searchBooks(String keyword) {
        log.info("Searching books with keyword: {}", keyword);
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllBooks();
        }
        
        // Attempt search in Elasticsearch first.
        try {
            List<BookDocument> docs = bookSearchRepository.fuzzySearch(keyword);
            if (docs != null && !docs.isEmpty()) {
                return docs.stream().map(this::convertDocToResponse).collect(Collectors.toList());
            }
            log.info("Elasticsearch index empty for keyword '{}', falling back to MySQL", keyword);
        } catch (Exception e) {
            log.warn("Elasticsearch search failed for '{}', falling back to MySQL: {}", keyword, e.getMessage());
        }
        
        // Fallback to MySQL query.
        return bookRepository.searchBooks(keyword).stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
    }

    // Retrieves all active books matching a specific genre.
    public List<BookResponse> getBooksByGenre(String genre) {
        log.info("Fetching books by genre: {}", genre);
        try {
            List<BookDocument> docs = bookSearchRepository.findByGenreAndActiveTrue(genre);
            if (docs != null && !docs.isEmpty()) {
                return docs.stream().map(this::convertDocToResponse).collect(Collectors.toList());
            }
            log.info("Elasticsearch index empty for genre '{}', falling back to MySQL", genre);
        } catch (Exception e) {
            log.warn("Elasticsearch not available for genre '{}', falling back to MySQL", genre);
        }
        
        // Fallback to MySQL query.
        return bookRepository.findByGenreIgnoreCaseAndActiveTrue(genre).stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
    }

    // Retrieves all promoted featured books.
    public List<BookResponse> getFeaturedBooks() {
        log.info("Fetching featured books");
        try {
            List<BookDocument> docs = bookSearchRepository.findByFeaturedTrueAndActiveTrue();
            if (docs != null && !docs.isEmpty()) {
                return docs.stream().map(this::convertDocToResponse).collect(Collectors.toList());
            }
            log.info("Elasticsearch index is empty for featured books, falling back to MySQL");
        } catch (Exception e) {
            log.warn("Elasticsearch not available for featured books, falling back to MySQL: {}", e.getMessage());
        }
        
        // Fallback to MySQL query.
        return bookRepository.findByFeaturedTrueAndActiveTrue().stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
    }

    // Filters catalog items within a specified price range.
    public List<BookResponse> getBooksByPriceRange(Double minPrice, Double maxPrice) {
        log.info("Fetching books in price range: {} - {}", minPrice, maxPrice);
        if (minPrice < 0 || maxPrice < minPrice) {
            throw new RuntimeException("Invalid price range");
        }
        try {
            List<BookDocument> docs = bookSearchRepository.findByPriceBetweenAndActiveTrue(minPrice, maxPrice);
            if (!docs.isEmpty()) {
                return docs.stream().map(this::convertDocToResponse).collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.warn("Elasticsearch not available for price filter");
        }
        
        // Fallback to MySQL query.
        return bookRepository.findByPriceBetweenAndActiveTrue(minPrice, maxPrice).stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
    }

    // Checks if sufficient stock is available for a book order.
    public boolean checkStock(Long bookId, Integer quantity) {
        Book book = bookRepository.findById(bookId).orElseThrow(() -> new ResourceNotFoundException("Book not found"));
        return book.getStock() >= quantity;
    }

    // Reserves and decrements inventory stock for a new order.
    @Transactional
    public boolean reserveStock(Long bookId, Integer quantity) {
        Book book = bookRepository.findById(bookId).orElseThrow(() -> new ResourceNotFoundException("Book not found"));
        if (book.getStock() < quantity) return false;
        
        book.setStock(book.getStock() - quantity);
        book.setUpdatedAt(LocalDateTime.now());
        bookRepository.save(book);
        syncToElasticsearch(book);
        return true;
    }

    // Releases and increments inventory stock when an order is cancelled or falls back.
    @Transactional
    public void releaseStock(Long bookId, Integer quantity) {
        Book book = bookRepository.findById(bookId).orElseThrow(() -> new ResourceNotFoundException("Book not found"));
        book.setStock(book.getStock() + quantity);
        book.setUpdatedAt(LocalDateTime.now());
        bookRepository.save(book);
        syncToElasticsearch(book);
    }

    // Updates a book's average star rating.
    public void updateRating(Long bookId, Double rating) {
        Book book = bookRepository.findById(bookId).orElseThrow(() -> new RuntimeException("Book not found id=" + bookId));
        double rounded = Math.round(rating * 10.0) / 10.0;
        book.setRating(rounded);
        book.setUpdatedAt(LocalDateTime.now());
        Book updated = bookRepository.save(book);
        syncToElasticsearch(updated);
    }

    // Forces full synchronization of all catalog database entries into the Elasticsearch index.
    @Transactional
    public String syncAllBooksToElasticsearch(String role) {
        verifyAdmin(role);
        List<Book> books = bookRepository.findAll();
        int successCount = 0;
        for (Book book : books) {
            try {
                syncToElasticsearch(book);
                successCount++;
            } catch (Exception e) {
                log.error("Failed to sync book {}", book.getBookId());
            }
        }
        return "Synced " + successCount + " books to Elasticsearch";
    }
}
