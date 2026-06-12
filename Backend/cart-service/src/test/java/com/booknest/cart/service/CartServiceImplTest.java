package com.booknest.cart.service;

import com.booknest.cart.client.BookClient;
import com.booknest.cart.dto.AddToCartRequest;
import com.booknest.cart.dto.BookResponse;
import com.booknest.cart.dto.CartResponse;
import com.booknest.cart.entity.Cart;
import com.booknest.cart.entity.CartItem;
import com.booknest.cart.repository.CartItemRepository;
import com.booknest.cart.repository.CartRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CartServiceImpl Unit Tests")
class CartServiceImplTest {

    @Mock
    private CartRepository cartRepository;
    @Mock
    private CartItemRepository cartItemRepository;
    @Mock
    private BookClient bookClient;

    @InjectMocks
    private CartServiceImpl cartService;

    private Cart emptyCart;
    private BookResponse availableBook;

    @BeforeEach
    void setUp() {
        emptyCart = new Cart();
        emptyCart.setCartId(1L);
        emptyCart.setUserId(10L);
        emptyCart.setItems(new ArrayList<>());
        emptyCart.setTotalPrice(0.0);

        availableBook = new BookResponse();
        availableBook.setBookId(100L);
        availableBook.setTitle("Clean Code");
        availableBook.setPrice(499.0);
        availableBook.setStock(10);
        availableBook.setActive(true);
        availableBook.setCoverImageUrl("cover.jpg");
    }

    // GET CART

    @Test
    @DisplayName("getCartByUser: existing cart → returns CartResponse")
    void getCartByUser_existingCart_returnsCart() {
        when(cartRepository.findByUserId(10L)).thenReturn(Optional.of(emptyCart));

        CartResponse response = cartService.getCartByUser(10L);

        assertThat(response.getUserId()).isEqualTo(10L);
        assertThat(response.getTotalItems()).isEqualTo(0);
    }

    @Test
    @DisplayName("getCartByUser: no cart → creates new cart and returns it")
    void getCartByUser_noCart_createsAndReturns() {
        when(cartRepository.findByUserId(10L)).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenReturn(emptyCart);

        CartResponse response = cartService.getCartByUser(10L);

        assertThat(response).isNotNull();
        verify(cartRepository).save(any(Cart.class));
    }

    // ADD ITEM

    @Test
    @DisplayName("addItem: new item, book in stock → item added, total recalculated")
    void addItem_newItem_success() {
        AddToCartRequest request = new AddToCartRequest();
        request.setBookId(100L);
        request.setQuantity(2);

        when(cartRepository.findByUserId(10L)).thenReturn(Optional.of(emptyCart));
        when(bookClient.getBookById(100L)).thenReturn(availableBook);
        when(cartRepository.save(any(Cart.class))).thenReturn(emptyCart);

        CartResponse response = cartService.addItem(10L, request);

        assertThat(response).isNotNull();
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    @DisplayName("addItem: book inactive → throws RuntimeException")
    void addItem_bookInactive_throws() {
        availableBook.setActive(false);
        AddToCartRequest request = new AddToCartRequest();
        request.setBookId(100L);
        request.setQuantity(1);

        when(cartRepository.findByUserId(10L)).thenReturn(Optional.of(emptyCart));
        when(bookClient.getBookById(100L)).thenReturn(availableBook);

        assertThatThrownBy(() -> cartService.addItem(10L, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("no longer available");
    }

    @Test
    @DisplayName("addItem: out of stock → throws RuntimeException")
    void addItem_outOfStock_throws() {
        availableBook.setStock(0);
        AddToCartRequest request = new AddToCartRequest();
        request.setBookId(100L);
        request.setQuantity(1);

        when(cartRepository.findByUserId(10L)).thenReturn(Optional.of(emptyCart));
        when(bookClient.getBookById(100L)).thenReturn(availableBook);

        assertThatThrownBy(() -> cartService.addItem(10L, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("out of stock");
    }

    @Test
    @DisplayName("addItem: quantity exceeds stock → throws RuntimeException")
    void addItem_exceedsStock_throws() {
        AddToCartRequest request = new AddToCartRequest();
        request.setBookId(100L);
        request.setQuantity(15); // stock is 10

        when(cartRepository.findByUserId(10L)).thenReturn(Optional.of(emptyCart));
        when(bookClient.getBookById(100L)).thenReturn(availableBook);

        assertThatThrownBy(() -> cartService.addItem(10L, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("exceeds available stock");
    }

    @Test
    @DisplayName("addItem: book already in cart → increases quantity")
    void addItem_bookAlreadyInCart_increasesQuantity() {
        CartItem existingItem = new CartItem();
        existingItem.setItemId(1L);
        existingItem.setBookId(100L);
        existingItem.setQuantity(3);
        existingItem.setPrice(499.0);
        existingItem.setCart(emptyCart);
        emptyCart.getItems().add(existingItem);

        AddToCartRequest request = new AddToCartRequest();
        request.setBookId(100L);
        request.setQuantity(2);

        when(cartRepository.findByUserId(10L)).thenReturn(Optional.of(emptyCart));
        when(bookClient.getBookById(100L)).thenReturn(availableBook);
        when(cartRepository.save(any(Cart.class))).thenReturn(emptyCart);

        cartService.addItem(10L, request);

        assertThat(existingItem.getQuantity()).isEqualTo(5); // 3 + 2
    }

    // REMOVE ITEM

    @Test
    @DisplayName("removeItem: item found in cart → removed")
    void removeItem_found_removed() {
        CartItem item = new CartItem();
        item.setItemId(1L);
        item.setBookId(100L);
        item.setQuantity(2);
        item.setPrice(499.0);
        emptyCart.getItems().add(item);

        when(cartRepository.findByUserId(10L)).thenReturn(Optional.of(emptyCart));
        when(cartRepository.save(any(Cart.class))).thenReturn(emptyCart);

        CartResponse response = cartService.removeItem(10L, 1L);

        assertThat(response).isNotNull();
        assertThat(emptyCart.getItems()).isEmpty();
    }

    @Test
    @DisplayName("removeItem: item not in cart → throws RuntimeException")
    void removeItem_notFound_throws() {
        when(cartRepository.findByUserId(10L)).thenReturn(Optional.of(emptyCart));

        assertThatThrownBy(() -> cartService.removeItem(10L, 999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Item not found");
    }

    // UPDATE QUANTITY

    @Test
    @DisplayName("updateQuantity: valid quantity → updated and saved")
    void updateQuantity_success() {
        CartItem item = new CartItem();
        item.setItemId(1L);
        item.setBookId(100L);
        item.setQuantity(2);
        item.setPrice(499.0);
        emptyCart.getItems().add(item);

        when(cartRepository.findByUserId(10L)).thenReturn(Optional.of(emptyCart));
        when(cartRepository.save(any(Cart.class))).thenReturn(emptyCart);

        CartResponse response = cartService.updateQuantity(10L, 1L, 5);

        assertThat(item.getQuantity()).isEqualTo(5);
    }

    @Test
    @DisplayName("updateQuantity: quantity < 1 → throws RuntimeException")
    void updateQuantity_lessThanOne_throws() {
        assertThatThrownBy(() -> cartService.updateQuantity(10L, 1L, 0))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("at least 1");
    }

    // CLEAR CART

    @Test
    @DisplayName("clearCart: success → items empty, total 0")
    void clearCart_success_emptyItems() {
        CartItem item = new CartItem();
        item.setItemId(1L);
        item.setPrice(499.0);
        item.setQuantity(1);
        emptyCart.getItems().add(item);

        when(cartRepository.findByUserId(10L)).thenReturn(Optional.of(emptyCart));
        when(cartRepository.save(any(Cart.class))).thenReturn(emptyCart);

        cartService.clearCart(10L);

        assertThat(emptyCart.getItems()).isEmpty();
        assertThat(emptyCart.getTotalPrice()).isEqualTo(0.0);
    }

    // GET ITEM COUNT

    @Test
    @DisplayName("getItemCount: existing cart with items → returns correct size")
    void getItemCount_existingCart_returnsSize() {
        CartItem item = new CartItem();
        item.setItemId(1L);
        emptyCart.getItems().add(item);

        when(cartRepository.findByUserId(10L)).thenReturn(Optional.of(emptyCart));

        Integer count = cartService.getItemCount(10L);

        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("getItemCount: no cart → returns 0")
    void getItemCount_noCart_returnsZero() {
        when(cartRepository.findByUserId(10L)).thenReturn(Optional.empty());

        Integer count = cartService.getItemCount(10L);

        assertThat(count).isEqualTo(0);
    }
}
