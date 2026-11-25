package br.com.stockflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@AutoConfigureMockMvc
@SpringBootTest(properties = {"stockflow.admin-password=test-admin", "stockflow.operator-password=test-operator"})
class StockFlowIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17.10-alpine");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    @Test
    void registersAnOperatorAndReservesExistingUsernames() throws Exception {
        var guest = (MockHttpSession) mvc.perform(get("/api/auth/csrf")).andReturn().getRequest().getSession(false);
        mvc.perform(post("/api/auth/register").session(guest).contentType("application/json").content("{}"))
                .andExpect(status().isForbidden());
        postJson(guest, "/api/auth/register", """
                {"username":"Admin","password":"minha-senha-teste"}
                """, 409);
        postJson(guest, "/api/auth/register", """
                {"username":"Novo.Operador","password":"curta"}
                """, 400);
        postJson(guest, "/api/auth/register", """
                {"username":"Novo.Operador","password":"minha-senha-teste","role":"ADMIN"}
                """, 204);
        postJson(guest, "/api/auth/register", """
                {"username":"novo.operador","password":"outra-senha-teste"}
                """, 409);
        var operator = login("Novo.Operador", "minha-senha-teste");
        mvc.perform(get("/api/auth/me").session(operator))
                .andExpect(jsonPath("$.username").value("novo.operador"))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_OPERATOR"));
        postJson(operator, "/api/inventory/entries", entry(1, 5), 403);
    }

    @Test
    void orderUpdatesStockAtomicallyAndEnforcesPermissions() throws Exception {
        mvc.perform(get("/api/products")).andExpect(status().isUnauthorized());
        var admin = login("admin", "test-admin");
        var operator = login("operador", "test-operator");
        long first = postJson(operator, "/api/products", """
                {"sku":"A","name":"Caneca","price":12.50}
                """, 201).path("id").asLong();
        long second = postJson(operator, "/api/products", """
                {"sku":"B","name":"Caderno","price":10.00}
                """, 201).path("id").asLong();
        long client = postJson(operator, "/api/customers", """
                {"name":"Ana","email":"ana@example.com"}
                """, 201).path("id").asLong();
        postJson(operator, "/api/inventory/entries", entry(first, 5), 403);
        postJson(admin, "/api/inventory/entries", entry(first, 5), 201);
        long order = postJson(operator, "/api/orders", """
                {"customerId":%s,"items":[{"productId":%s,"quantity":2},{"productId":%s,"quantity":1}]}
                """.formatted(client, first, second), 201).path("order").path("id").asLong();
        String path = "/api/orders/" + order;

        postJson(operator, path + "/confirm", "{}", 409);
        assertThat(balance(operator, first)).isEqualTo(5);
        mvc.perform(get("/api/inventory/movements").session(operator))
                .andExpect(jsonPath("$.length()").value(1));
        postJson(admin, "/api/inventory/entries", entry(second, 2), 201);
        postJson(operator, path + "/confirm", "{}", 200);
        postJson(operator, path + "/confirm", "{}", 200);
        assertThat(balance(operator, first)).isEqualTo(3);
        assertThat(balance(operator, second)).isEqualTo(1);
        mvc.perform(get("/api/dashboard").session(operator))
                .andExpect(jsonPath("$.confirmedTotal").value(35.0));
        postJson(operator, path + "/cancel", "{}", 403);
        postJson(admin, path + "/cancel", "{}", 200);
        postJson(admin, path + "/cancel", "{}", 200);
        assertThat(balance(admin, first)).isEqualTo(5);
        assertThat(balance(admin, second)).isEqualTo(2);
        mvc.perform(get("/api/inventory/movements").session(admin))
                .andExpect(jsonPath("$.length()").value(6));
        mvc.perform(post(path + "/confirm").session(operator)).andExpect(status().isForbidden());
    }

    private MockHttpSession login(String username, String password) throws Exception {
        var response = mvc.perform(get("/api/auth/csrf")).andReturn();
        var session = (MockHttpSession) response.getRequest().getSession(false);
        var token = json.readTree(response.getResponse().getContentAsString());
        return (MockHttpSession) mvc.perform(post("/api/auth/login").session(session)
                        .header(token.path("headerName").asText(), token.path("token").asText())
                        .param("username", username).param("password", password))
                .andExpect(status().isNoContent()).andReturn().getRequest().getSession(false);
    }

    private JsonNode postJson(MockHttpSession session, String path, String payload, int expected) throws Exception {
        var csrf = json.readTree(mvc.perform(get("/api/auth/csrf").session(session))
                .andReturn().getResponse().getContentAsString());
        var body = mvc.perform(post(path).session(session)
                .header(csrf.path("headerName").asText(), csrf.path("token").asText())
                .contentType("application/json").content(payload)).andExpect(status().is(expected))
                .andReturn().getResponse().getContentAsString();
        return json.readTree(body);
    }

    private int balance(MockHttpSession session, long id) throws Exception {
        var list = json.readTree(mvc.perform(get("/api/products").session(session))
                .andReturn().getResponse().getContentAsString());
        for (var product : list) {
            if (product.path("id").asLong() == id) return product.path("stock").asInt();
        }
        throw new AssertionError("Produto não encontrado: " + id);
    }

    private String entry(long product, int quantity) {
        return """
                {"productId":%s,"quantity":%s}
                """.formatted(product, quantity);
    }
}
