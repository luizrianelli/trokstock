package br.com.stockflow.auth;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AccountService accounts;

    public AuthController(AccountService accounts) {
        this.accounts = accounts;
    }

    public record Registration(
            @NotBlank @Pattern(regexp = "[A-Za-z0-9_.-]{3,32}", message = "Use de 3 a 32 letras, números, pontos, hífens ou sublinhados.") String username,
            @NotBlank @Size(min = 8, max = 72, message = "Use de 8 a 72 caracteres.") String password) {}
    public record Session(String username, List<String> roles) {}
    public record Token(String headerName, String token) {}

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void register(@Valid @RequestBody Registration input) {
        accounts.register(input.username(), input.password());
    }

    @GetMapping("/me")
    public Session me(Authentication authentication) {
        return new Session(authentication.getName(), authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority()).toList());
    }

    @GetMapping("/csrf")
    public Token csrf(CsrfToken token) {
        return new Token(token.getHeaderName(), token.getToken());
    }
}
