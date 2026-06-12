package com.booknest.book;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main entry point for the Book Microservice.
 *
 * NOTE: The old WebMvcConfigurer for /uploads/books/** has been removed.
 * Book cover images are now served directly from Cloudinary CDN.
 * The database stores a full HTTPS URL, and the frontend's getImageUrl()
 * already handles full URLs natively (no local path logic needed).
 */
@SpringBootApplication
public class BookService1Application {

    public static void main(String[] args) {
        SpringApplication.run(BookService1Application.class, args);
    }
}