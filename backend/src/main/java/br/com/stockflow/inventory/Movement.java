package br.com.stockflow.inventory;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "movement")
public class Movement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    public enum Kind { ENTRY, SALE, RETURN }

    public Long productId;
    public Long orderId;
    @Enumerated(EnumType.STRING)
    public Kind kind;
    public int quantity;
    public Instant createdAt;
}
