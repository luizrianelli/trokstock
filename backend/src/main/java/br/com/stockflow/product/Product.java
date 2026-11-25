package br.com.stockflow.product;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "product")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    public String sku;
    public String name;
    public BigDecimal price;
    public int stock;
}
