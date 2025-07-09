package io.github.egorkor.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "'orders'")
public class Order {
    @Id
    private Long id;

    private String name;

    private Double cost;

    @ManyToOne
    private User user;
}
