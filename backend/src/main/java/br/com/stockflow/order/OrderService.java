package br.com.stockflow.order;

import br.com.stockflow.customer.CustomerRepository;
import br.com.stockflow.inventory.*;
import br.com.stockflow.product.ProductRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static br.com.stockflow.order.SalesOrder.Status.*;

@Service
public class OrderService {
    private final SalesOrderRepository orders;
    private final OrderLineRepository lines;
    private final ProductRepository products;
    private final CustomerRepository customers;
    private final InventoryService inventory;

    public OrderService(SalesOrderRepository orders, OrderLineRepository lines, ProductRepository products,
                        CustomerRepository customers, InventoryService inventory) {
        this.orders = orders;
        this.lines = lines;
        this.products = products;
        this.customers = customers;
        this.inventory = inventory;
    }

    public record Item(@NotNull @Positive Long productId, @NotNull @Positive @Max(1_000_000) Integer quantity) {}
    public record Input(@NotNull @Positive Long customerId,
                        @NotEmpty @Size(max = 100) List<@NotNull @Valid Item> items) {}
    public record Details(SalesOrder order, List<OrderLine> items) {}

    @Transactional
    public Details create(Input input) {
        if (!customers.existsById(input.customerId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente não encontrado.");
        }
        var order = new SalesOrder();
        order.customerId = input.customerId();
        order.status = DRAFT;
        order.createdAt = Instant.now();
        order.total = BigDecimal.ZERO;
        var uniqueProducts = new HashSet<Long>();
        var items = input.items().stream().map(item -> {
            if (!uniqueProducts.add(item.productId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cada produto deve aparecer uma vez.");
            }
            var product = products.findById(item.productId()).orElseThrow(() ->
                    new ResponseStatusException(HttpStatus.NOT_FOUND, "Produto não encontrado."));
            var line = new OrderLine();
            line.productId = product.id;
            line.quantity = item.quantity();
            line.unitPrice = product.price;
            return line;
        }).toList();
        for (var line : items) {
            order.total = order.total.add(line.unitPrice.multiply(BigDecimal.valueOf(line.quantity)));
        }
        orders.save(order);
        items.forEach(line -> line.orderId = order.id);
        return new Details(order, lines.saveAll(items));
    }

    public List<SalesOrder> list() {
        return orders.findAll(Sort.by(Sort.Direction.DESC, "id"));
    }

    @Transactional(readOnly = true)
    public Details detail(long id) {
        return details(orders.findById(id).orElseThrow(OrderService::missing));
    }

    @Transactional
    public Details confirm(long id) {
        var order = orders.lockById(id).orElseThrow(OrderService::missing);
        var details = details(order);
        if (order.status == CONFIRMED) {
            return details;
        }
        if (order.status != DRAFT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Apenas rascunhos podem ser confirmados.");
        }
        changeStock(details, Movement.Kind.SALE);
        order.status = CONFIRMED;
        return details;
    }

    @Transactional
    public Details cancel(long id) {
        var order = orders.lockById(id).orElseThrow(OrderService::missing);
        var details = details(order);
        if (order.status == CONFIRMED) {
            changeStock(details, Movement.Kind.RETURN);
        }
        order.status = CANCELED;
        return details;
    }

    private void changeStock(Details details, Movement.Kind kind) {
        // Os itens vêm em ordem de produto para que pedidos concorrentes bloqueiem na mesma sequência.
        for (var item : details.items()) {
            inventory.move(item.productId, item.quantity, kind, details.order().id);
        }
    }

    private Details details(SalesOrder order) {
        return new Details(order, lines.findByOrderIdOrderByProductId(order.id));
    }

    private static ResponseStatusException missing() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido não encontrado.");
    }
}
