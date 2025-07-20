package io.github.egorkor.model;

import jakarta.persistence.*;
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

    @ManyToOne(fetch = FetchType.LAZY)
    private User user;
}
