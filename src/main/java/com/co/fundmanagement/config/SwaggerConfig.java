package com.co.fundmanagement.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title="Fund Management AMV",
                version = "1.0.0",
                description = "This project technical test",
                summary = "Plataforma de gestion de Fondos",
                contact = @Contact(
                name = "Amaris",
                url = "https://amaris.com.co/contactanos/",
                email = "amaris@amaris.com.co"
        ),
                license = @License(
                        name = "propiedad",
                        url = "https://amaris.com.co/"
                )
        )
)
public class SwaggerConfig {
}
