package br.com.stockflow.common;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.parameters.HeaderParameter;
import io.swagger.v3.oas.models.media.StringSchema;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    OpenAPI stockFlowApi() {
        return new OpenAPI().info(new Info()
                .title("Trok&Stock API")
                .version("0.1.0")
                .description("Entre pelo Trok&Stock e obtenha o token em GET /api/auth/csrf antes de enviar POST."));
    }

    @Bean
    OpenApiCustomizer postCsrfHeader() {
        return api -> api.getPaths().values().forEach(path -> {
            if (path.getPost() != null) {
                path.getPost().addParametersItem(new HeaderParameter()
                        .name("X-CSRF-TOKEN")
                        .description("Token da sessão retornado por GET /api/auth/csrf")
                        .required(true).schema(new StringSchema()));
            }
        });
    }
}
