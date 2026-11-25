package br.com.stockflow.order;

import org.springframework.data.jpa.repository.JpaRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.math.BigDecimal;

public interface SalesOrderRepository extends JpaRepository<SalesOrder, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from SalesOrder o where o.id = :id")
    Optional<SalesOrder> lockById(@Param("id") long id);

    long countByStatus(SalesOrder.Status status);

    @Query("select sum(o.total) from SalesOrder o where o.status = :status")
    BigDecimal sumByStatus(@Param("status") SalesOrder.Status status);
}
