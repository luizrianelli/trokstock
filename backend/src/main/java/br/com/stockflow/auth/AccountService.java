package br.com.stockflow.auth;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
class AccountService {
    private static final Set<String> RESERVED = Set.of("admin", "operador");

    private final OperatorAccountRepository accounts;
    private final PasswordEncoder encoder;

    AccountService(OperatorAccountRepository accounts, PasswordEncoder encoder) {
        this.accounts = accounts;
        this.encoder = encoder;
    }

    static String normalize(String username) {
        return username.toLowerCase(Locale.ROOT);
    }

    @Transactional
    public void register(String username, String password) {
        String normalized = normalize(username);
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A senha ultrapassa o limite de 72 bytes.");
        }
        if (RESERVED.contains(normalized) || accounts.insert(normalized, encoder.encode(password)) == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Este nome de usuário já está em uso.");
        }
    }
}
