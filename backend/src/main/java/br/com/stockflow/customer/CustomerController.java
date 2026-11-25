package br.com.stockflow.customer;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {
    private final CustomerRepository repository;

    public CustomerController(CustomerRepository repository) {
        this.repository = repository;
    }

    public record Input(@NotBlank @Size(max = 120) String name,
                        @NotBlank @Email @Size(max = 180) String email) {}

    @GetMapping
    public List<Customer> list() {
        return repository.findAll(Sort.by("name", "id"));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Customer create(@Valid @RequestBody Input input) {
        var entity = new Customer();
        entity.name = input.name().strip();
        entity.email = input.email().strip().toLowerCase(Locale.ROOT);
        return repository.save(entity);
    }
}
