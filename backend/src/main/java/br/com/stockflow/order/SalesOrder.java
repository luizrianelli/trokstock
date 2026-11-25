package br.com.stockflow.order;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "sales_order")
public class SalesOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    public enum Status { DRAFT, CONFIRMED, CANCELED }

    public Long customerId;
    @Enumerated(EnumType.STRING)
    public Status status;
    public Instant createdAt;
    public BigDecimal total;
}
