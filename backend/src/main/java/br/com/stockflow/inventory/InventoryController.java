package br.com.stockflow.inventory;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {
    private final InventoryService service;
    private final MovementRepository movements;

    public InventoryController(InventoryService service, MovementRepository movements) {
        this.service = service;
        this.movements = movements;
    }

    public record Entry(@NotNull @Positive Long productId, @NotNull @Positive Integer quantity) {}

    @PostMapping("/entries")
    @ResponseStatus(HttpStatus.CREATED)
    public Movement enter(@Valid @RequestBody Entry entry) {
        return service.move(entry.productId(), entry.quantity(), Movement.Kind.ENTRY, null);
    }

    @GetMapping("/movements")
    public List<Movement> history(@RequestParam(required = false) Long productId) {
        return productId == null ? movements.findTop100ByOrderByIdDesc()
                : movements.findTop100ByProductIdOrderByIdDesc(productId);
    }
}
