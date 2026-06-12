package com.booknest.book.service;

import com.booknest.book.document.BookDocument;
import com.booknest.book.dto.BookRequest;
import com.booknest.book.dto.BookResponse;
import com.booknest.book.entity.Book;
import com.booknest.book.repository.BookRepository;
import com.booknest.book.repository.BookSearchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BookServiceTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private BookSearchRepository bookSearchRepository;

    @Mock
    private ImageStorageService imageStorageService;

    @InjectMocks
    private BookService bookService;

    private Book sampleBook;
    private BookDocument sampleDoc;

    @BeforeEach
    void setUp() {
        sampleBook = new Book();
        sampleBook.setBookId(1L);
        sampleBook.setTitle("Java Programming");
        sampleBook.setAuthor("John Doe");
        sampleBook.setPrice(500.0);
        sampleBook.setStock(10);
        sampleBook.setGenre("Tech");
        sampleBook.setActive(true);
        sampleBook.setFeatured(false);

        sampleDoc = new BookDocument();
        sampleDoc.setBookId("1");
        sampleDoc.setTitle("Java Programming");
        sampleDoc.setAuthor("John Doe");
        sampleDoc.setPrice(500.0);
        sampleDoc.setStock(10);
        sampleDoc.setGenre("Tech");
        sampleDoc.setActive(true);
        sampleDoc.setFeatured(false);
    }

    @Test
    void testGetAllBooks() {
        when(bookRepository.findByActiveTrue()).thenReturn(Arrays.asList(sampleBook));
        List<BookResponse> books = bookService.getAllBooks();
        assertEquals(1, books.size());
        assertEquals("Java Programming", books.get(0).getTitle());
    }

    @Test
    void testGetBookById_Success() {
        when(bookRepository.findByBookIdAndActiveTrue(1L)).thenReturn(Optional.of(sampleBook));
        BookResponse response = bookService.getBookById(1L);
        assertNotNull(response);
        assertEquals(1L, response.getBookId());
    }

    @Test
    void testGetBookById_NotFound() {
        when(bookRepository.findByBookIdAndActiveTrue(1L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> bookService.getBookById(1L));
    }

    @Test
    void testAddBook_Admin() {
        BookRequest request = new BookRequest();
        request.setTitle("New Book");
        request.setPrice(200.0);
        request.setStock(5);
        request.setPublishedDate("2023-01-01");
        request.setFeatured(true);

        when(bookRepository.save(any(Book.class))).thenAnswer(i -> {
            Book b = i.getArgument(0);
            b.setBookId(2L);
            return b;
        });

        BookResponse response = bookService.addBook(request, "ADMIN");

        assertNotNull(response);
        assertEquals(2L, response.getBookId());
        assertTrue(response.getFeatured());
        verify(bookRepository, times(1)).save(any());
        verify(bookSearchRepository, times(1)).save(any());
    }

    @Test
    void testAddBook_Denied() {
        BookRequest request = new BookRequest();
        assertThrows(RuntimeException.class, () -> bookService.addBook(request, "CUSTOMER"));
    }

    @Test
    void testUpdateBook_Success() {
        BookRequest request = new BookRequest();
        request.setTitle("Updated Title");
        request.setPublishedDate("2023-01-01");
        request.setFeatured(true);
        request.setStock(10);
        request.setPrice(100.0);

        when(bookRepository.findByBookIdAndActiveTrue(1L)).thenReturn(Optional.of(sampleBook));
        when(bookRepository.save(any(Book.class))).thenReturn(sampleBook);

        bookService.updateBook(1L, request, "ADMIN");

        assertEquals("Updated Title", sampleBook.getTitle());
        verify(bookRepository, times(1)).save(sampleBook);
        verify(bookSearchRepository, times(1)).save(any());
    }

    @Test
    void testDeleteBook_Success() {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(sampleBook));

        bookService.deleteBook(1L, "ADMIN");

        assertFalse(sampleBook.getActive());
        verify(bookRepository, times(1)).save(sampleBook);
        verify(bookSearchRepository, times(1)).deleteById("1");
    }

    @Test
    void testUpdateStock() {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(sampleBook));
        when(bookRepository.save(any(Book.class))).thenReturn(sampleBook);

        bookService.updateStock(1L, 50, "ADMIN");

        assertEquals(50, sampleBook.getStock());
        verify(bookRepository, times(1)).save(sampleBook);
    }

    @Test
    void testToggleFeatured() {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(sampleBook));
        // any method specifies that accept the book type object in parameter it should
        // not be void or null
        when(bookRepository.save(any(Book.class))).thenReturn(sampleBook);

        bookService.toggleFeatured(1L, "ADMIN");

        assertTrue(sampleBook.getFeatured());
    }

    @Test
    void testUploadCoverImage() {
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "test data".getBytes());
        when(bookRepository.findById(1L)).thenReturn(Optional.of(sampleBook));
        when(imageStorageService.saveImage(any())).thenReturn("http:// image.url");
        when(bookRepository.save(any(Book.class))).thenReturn(sampleBook);

        bookService.uploadCoverImage(1L, file, "ADMIN");

        assertEquals("http:// image.url", sampleBook.getCoverImageUrl());
    }

    @Test
    void testReserveStock_Success() {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(sampleBook));
        boolean reserved = bookService.reserveStock(1L, 2);
        assertTrue(reserved);
        assertEquals(8, sampleBook.getStock());
    }

    @Test
    void testReserveStock_Insufficient() {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(sampleBook));
        boolean reserved = bookService.reserveStock(1L, 20);
        assertFalse(reserved);
        assertEquals(10, sampleBook.getStock());
    }

    @Test
    void testReleaseStock() {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(sampleBook));
        bookService.releaseStock(1L, 2);
        assertEquals(12, sampleBook.getStock());
    }

    @Test
    void testUpdateRating() {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(sampleBook));
        when(bookRepository.save(any(Book.class))).thenReturn(sampleBook);

        bookService.updateRating(1L, 4.56);
        assertEquals(4.6, sampleBook.getRating());
    }

    @Test
    void testSearchBooks_Elastic() {
        when(bookSearchRepository.fuzzySearch("Java")).thenReturn(Arrays.asList(sampleDoc));
        List<BookResponse> res = bookService.searchBooks("Java");
        assertEquals(1, res.size());
    }

    @Test
    void testSearchBooks_Fallback() {
        when(bookSearchRepository.fuzzySearch("Java")).thenReturn(Collections.emptyList());
        when(bookRepository.searchBooks("Java")).thenReturn(Arrays.asList(sampleBook));
        List<BookResponse> res = bookService.searchBooks("Java");
        assertEquals(1, res.size());
    }

    @Test
    void testGetBooksByGenre_Elastic() {
        when(bookSearchRepository.findByGenreAndActiveTrue("Tech")).thenReturn(Arrays.asList(sampleDoc));
        List<BookResponse> res = bookService.getBooksByGenre("Tech");
        assertEquals(1, res.size());
    }

    @Test
    void testGetBooksByGenre_Fallback() {
        when(bookSearchRepository.findByGenreAndActiveTrue("Tech")).thenThrow(new RuntimeException("Elastic error"));
        when(bookRepository.findByGenreIgnoreCaseAndActiveTrue("Tech")).thenReturn(Arrays.asList(sampleBook));
        List<BookResponse> res = bookService.getBooksByGenre("Tech");
        assertEquals(1, res.size());
    }

    @Test
    void testGetFeaturedBooks_Elastic() {
        when(bookSearchRepository.findByFeaturedTrueAndActiveTrue()).thenReturn(Arrays.asList(sampleDoc));
        List<BookResponse> res = bookService.getFeaturedBooks();
        assertEquals(1, res.size());
    }

    @Test
    void testGetBooksByPriceRange_Elastic() {
        when(bookSearchRepository.findByPriceBetweenAndActiveTrue(100.0, 600.0)).thenReturn(Arrays.asList(sampleDoc));
        List<BookResponse> res = bookService.getBooksByPriceRange(100.0, 600.0);
        assertEquals(1, res.size());
    }

    @Test
    void testCheckStock() {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(sampleBook));
        assertTrue(bookService.checkStock(1L, 5));
        assertFalse(bookService.checkStock(1L, 15));
    }

    @Test
    void testSyncAllBooksToElasticsearch() {
        when(bookRepository.findAll()).thenReturn(Arrays.asList(sampleBook));
        String result = bookService.syncAllBooksToElasticsearch("ADMIN");
        assertEquals("Synced 1 books to Elasticsearch", result);
        verify(bookSearchRepository, times(1)).save(any());
    }
}
