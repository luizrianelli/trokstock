package br.com.stockflow.product;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    private final ProductRepository repository;

    public ProductController(ProductRepository repository) {
        this.repository = repository;
    }

    public record Input(@NotBlank @Size(max = 40) String sku, @NotBlank @Size(max = 120) String name,
                        @NotNull @PositiveOrZero @Digits(integer = 10, fraction = 2) BigDecimal price) {}

    @GetMapping
    public List<Product> list() {
        return repository.findAll(Sort.by("name", "id"));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Product create(@Valid @RequestBody Input input) {
        var entity = new Product();
        entity.name = input.name().strip();
        entity.sku = input.sku().strip().toUpperCase(Locale.ROOT);
        entity.price = input.price();
        return repository.save(entity);
    }
}
