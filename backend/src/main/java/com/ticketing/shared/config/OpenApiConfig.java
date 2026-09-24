package com.ticketing.shared.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI ticketingOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Support Ticket API")
                        .version("1.0.0")
                        .description("Ticket CRUD, comments, search, and lifecycle transitions."))
                .addServersItem(new Server().url("/api/v1"));
    }
}
