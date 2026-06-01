package com.tenda.coupon.infrastructure.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI couponOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Coupon API")
                .description("""
                    API REST de gerenciamento de cupons (Desafio Técnico Tenda).

                    Regras de negócio principais:
                    - **code**: alfanumérico, 6 caracteres após sanitização. Caracteres especiais são removidos pela aplicação.
                    - **discountValue**: valor absoluto, mínimo 0,5; sem máximo.
                    - **expirationDate**: nunca pode estar no passado.
                    - **published**: opcional; permite criar o cupom já publicado.
                    - **delete**: soft delete — não pode deletar um cupom já deletado.
                    """)
                .version("v1")
                .contact(new Contact().name("Tenda").url("https://www.tenda.com"))
                .license(new License().name("MIT")))
            .servers(List.of(
                new Server().url("http://localhost:8080").description("Local"),
                new Server().url("/").description("Same host")
            ))
            .externalDocs(new ExternalDocumentation()
                .description("README do projeto")
                .url("https://github.com/"))
            .tags(List.of(
                new Tag().name("Coupon").description("Operações de cupons")
            ));
    }
}
