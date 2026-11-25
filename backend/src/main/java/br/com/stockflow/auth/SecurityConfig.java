package br.com.stockflow.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.http.HttpStatus;

@Configuration
public class SecurityConfig {
    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    UserDetailsService users(@Value("${stockflow.admin-password}") String admin,
                             @Value("${stockflow.operator-password}") String operator,
                             PasswordEncoder encoder, OperatorAccountRepository accounts) {
        var builtInUsers = new InMemoryUserDetailsManager(
                User.withUsername("admin").password(encoder.encode(admin)).roles("ADMIN").build(),
                User.withUsername("operador").password(encoder.encode(operator)).roles("OPERATOR").build());
        return username -> {
            String normalized = AccountService.normalize(username);
            if (builtInUsers.userExists(normalized)) {
                return builtInUsers.loadUserByUsername(normalized);
            }
            var account = accounts.findById(normalized).orElseThrow(() ->
                    new UsernameNotFoundException("Usuário não encontrado."));
            return User.withUsername(normalized).password(account.passwordHash()).roles("OPERATOR").build();
        };
    }

    @Bean
    SecurityFilterChain security(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/csrf", "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/register").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/inventory/entries", "/api/orders/*/cancel").hasRole("ADMIN")
                        .anyRequest().hasAnyRole("ADMIN", "OPERATOR"))
                .exceptionHandling(errors -> errors.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .formLogin(login -> login.loginProcessingUrl("/api/auth/login").permitAll()
                        .successHandler((request, response, auth) -> response.setStatus(204))
                        .failureHandler((request, response, failure) -> response.setStatus(401)))
                .logout(logout -> logout.logoutUrl("/api/auth/logout")
                        .logoutSuccessHandler((request, response, auth) -> response.setStatus(204)))
                .build();
    }
}
