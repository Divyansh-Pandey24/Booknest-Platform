package com.booknest.auth.config;

import io.swagger.v3.oas.models.servers.Server;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
@OpenAPIDefinition(info = @Info(title = "Auth Service API", description = "Handles User Registration, Login, and JWT generation.", version = "v1"), security = @SecurityRequirement(name = "bearerAuth")
// it tells the swagger ui to use the bearerAuth security scheme for all
// endpoints
// which will add an "Authorize" padlock to the UI
)
public class SwaggerConfig {

    /*
     * This tiny bean is strictly to add the "Authorize" padlock to the UI.
     * It tells Swagger how to pass the JWT token to our endpoints.
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .servers(List.of(new Server().url("http://localhost:8080/api").description("API Gateway")))

                .components(new Components()
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")));
    }
}
