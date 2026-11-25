package br.com.stockflow.auth;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class OperatorAccount {
    @Id
    private String username;
    private String passwordHash;

    protected OperatorAccount() {}

    OperatorAccount(String username, String passwordHash) {
        this.username = username;
        this.passwordHash = passwordHash;
    }

    String passwordHash() {
        return passwordHash;
    }
}
