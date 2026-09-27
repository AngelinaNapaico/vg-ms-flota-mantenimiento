package vg_ms.flota.shared.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "BearerAuth";

        return new OpenAPI()
                .info(new Info()
                        .title("API de Gestión de Siniestros y Mantenimiento de Flota")
                        .version("1.0.0")
                        .description("Microservicio transaccional para la gestión del ciclo de vida, talleres, siniestros y altas operativas de vehículos de emergencia (Cañete Seguro).")
                        .contact(new Contact()
                                .name("Equipo Cañete Seguro")
                                .email("soporte@canete-seguro.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")))
                .servers(List.of(
                        new Server().url("http://localhost:9012").description("Servidor Local"),
                        new Server().url("http://localhost:9000/api/flota-mantenimiento").description("Vía API Gateway")
                ))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .schemaRequirement(securitySchemeName, new SecurityScheme()
                        .name(securitySchemeName)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT"));
    }
}
