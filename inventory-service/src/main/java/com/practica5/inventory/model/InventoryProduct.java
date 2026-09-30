package com.practica5.inventory.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "inventory_products")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InventoryProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_code", nullable = false, unique = true)
    private String productCode;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Integer stock;

    @Column(name = "fail_mode", nullable = false)
    private Boolean failMode = false;
}
