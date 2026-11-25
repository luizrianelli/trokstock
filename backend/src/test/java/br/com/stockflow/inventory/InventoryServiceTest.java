package br.com.stockflow.inventory;

import br.com.stockflow.product.Product;
import br.com.stockflow.product.ProductRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class InventoryServiceTest {
    @Test
    void rejectsSaleAboveBalanceWithoutWritingMovement() {
        var products = mock(ProductRepository.class);
        var movements = mock(MovementRepository.class);
        var product = new Product();
        product.sku = "SKU-1";
        product.stock = 2;
        when(products.lockById(1L)).thenReturn(Optional.of(product));

        var inventory = new InventoryService(products, movements);
        assertThatThrownBy(() -> inventory.move(1, 3, Movement.Kind.SALE, 1L))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("Estoque insuficiente");
        assertThat(product.stock).isEqualTo(2);
        verifyNoInteractions(movements);
    }
}
