package br.com.stockflow.auth;

import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    public record Session(String username, List<String> roles) {}
    public record Token(String headerName, String token) {}

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
