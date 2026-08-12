package com.SSS.SGI.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI sgiOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("SGI - Système de Gestion des Imputations")
                        .description("API de gestion des collaborateurs, projets, imputations, "
                                + "affectations et absences. "
                                + "Authentification par JWT (POST /api/auth/login) : passez le jeton "
                                + "obtenu dans le header Authorization: Bearer {token} sur tous les "
                                + "autres endpoints (voir SECURITY.md).")
                        .version("v1")
                        .contact(new Contact().name("Smart Square Services")));
    }
}