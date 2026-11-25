package br.com.stockflow.inventory;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MovementRepository extends JpaRepository<Movement, Long> {
    List<Movement> findTop100ByOrderByIdDesc();
    List<Movement> findTop100ByProductIdOrderByIdDesc(long productId);
}
