package skadi.api.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Comparator;
import java.util.List;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Skadi API",
                version = "1.0.0",
                description = "API do Skadi para gerenciamento e monitoramento de centros de distribuição e câmaras frigoríficas."
        )
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)
public class OpenApiConfiguration {

    @Bean
    public OpenApiCustomizer operationSecurity() {
        return openApi -> {
            if (openApi.getTags() != null) {
                openApi.getTags().sort(Comparator.comparing(tag -> !"Acesso e autenticação".equals(tag.getName())));
            }
            openApi.setSecurity(null);
            openApi.getPaths().forEach((path, pathItem) ->
                    pathItem.readOperations().forEach(operation -> {
                        if ("/auth/login".equals(path)) {
                            operation.setSecurity(List.of());
                        } else {
                            operation.setSecurity(List.of(
                                    new io.swagger.v3.oas.models.security.SecurityRequirement()
                                            .addList("bearerAuth")
                            ));
                        }
                    })
            );
        };
    }
}