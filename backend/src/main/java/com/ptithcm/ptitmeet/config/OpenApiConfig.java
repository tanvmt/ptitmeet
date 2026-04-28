package com.ptithcm.ptitmeet.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI ptitMeetOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("PTITMeet API")
                        .description("API documentation for the PTITMeet backend services.")
                        .version("v1")
                        .contact(new Contact()
                                .name("PTITMeet Team")))
                .addServersItem(new Server()
                        .url("http://localhost:8080")
                        .description("Local development server"));
    }
}
