package br.com.stockflow.order;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService service;

    public OrderController(OrderService service) {
        this.service = service;
    }

    @GetMapping
    public List<SalesOrder> list() {
        return service.list();
    }

    @GetMapping("/{id}")
    public OrderService.Details details(@PathVariable long id) {
        return service.detail(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderService.Details create(@Valid @RequestBody OrderService.Input input) {
        return service.create(input);
    }

    @PostMapping("/{id}/confirm")
    public OrderService.Details confirm(@PathVariable long id) {
        return service.confirm(id);
    }

    @PostMapping("/{id}/cancel")
    public OrderService.Details cancel(@PathVariable long id) {
        return service.cancel(id);
    }
}
