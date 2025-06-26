package io.github.egorkor.repository;

import io.github.egorkor.model.Order;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Profile("test")
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
}
