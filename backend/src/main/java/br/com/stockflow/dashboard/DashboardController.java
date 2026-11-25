package br.com.stockflow.dashboard;

import br.com.stockflow.customer.CustomerRepository;
import br.com.stockflow.order.SalesOrderRepository;
import br.com.stockflow.product.ProductRepository;
import java.math.BigDecimal;
import java.util.Objects;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import static br.com.stockflow.order.SalesOrder.Status.*;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    private final ProductRepository products;
    private final CustomerRepository customers;
    private final SalesOrderRepository orders;

    public DashboardController(ProductRepository products, CustomerRepository customers, SalesOrderRepository orders) {
        this.products = products;
        this.customers = customers;
        this.orders = orders;
    }

    public record Indicators(long products, long customers, long lowStock, long drafts,
                             long confirmed, long canceled, BigDecimal confirmedTotal) {}

    @GetMapping
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public Indicators indicators() {
        return new Indicators(products.count(), customers.count(), products.countByStockLessThanEqual(5),
                orders.countByStatus(DRAFT), orders.countByStatus(CONFIRMED), orders.countByStatus(CANCELED),
                Objects.requireNonNullElse(orders.sumByStatus(CONFIRMED), BigDecimal.ZERO));
    }
}
