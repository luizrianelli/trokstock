package br.com.stockflow.inventory;

import br.com.stockflow.product.*;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class InventoryService {
    private final ProductRepository products;
    private final MovementRepository movements;

    public InventoryService(ProductRepository products, MovementRepository movements) {
        this.products = products;
        this.movements = movements;
    }

    @Transactional
    public Movement move(long productId, int quantity, Movement.Kind kind, Long orderId) {
        var product = products.lockById(productId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Produto não encontrado."));
        long balance = (long) product.stock + (kind == Movement.Kind.SALE ? -quantity : quantity);
        if (balance < 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Estoque insuficiente para " + product.sku + ".");
        }
        if (balance > Integer.MAX_VALUE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Limite de saldo excedido.");
        }
        product.stock = (int) balance;
        var movement = new Movement();
        movement.productId = productId;
        movement.orderId = orderId;
        movement.kind = kind;
        movement.quantity = quantity;
        movement.createdAt = Instant.now();
        return movements.save(movement);
    }
}
