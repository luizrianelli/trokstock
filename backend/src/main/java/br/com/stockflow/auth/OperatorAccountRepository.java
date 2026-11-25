package br.com.stockflow.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

interface OperatorAccountRepository extends JpaRepository<OperatorAccount, String> {
    @Modifying
    @Query(value = """
            insert into operator_account (username, password_hash) values (:username, :passwordHash)
            on conflict (username) do nothing
            """, nativeQuery = true)
    int insert(String username, String passwordHash);
}
