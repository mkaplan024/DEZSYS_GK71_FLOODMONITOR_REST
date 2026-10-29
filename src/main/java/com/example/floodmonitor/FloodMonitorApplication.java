package com.example.floodmonitor;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@OpenAPIDefinition(
        info = @Info(
                title = "Flood Monitor API",
                version = "1.0",
                description = "REST-Schnittstelle für ein Hochwasser-Frühwarnsystem. "
                        + "Alle Antworten gibt es als JSON (Standard) oder XML, gesteuert über den Accept-Header. "
                        + "Bei anderen Formaten kommt 406. Fehler werden immer im gleichen Aufbau zurückgegeben "
                        + "(timestamp, status, error, message, path). Bei unerwarteten Fehlern kommt 500."
        ),
        security = @SecurityRequirement(name = "apiKey")
)
@SecurityScheme(
        name = "apiKey",
        type = SecuritySchemeType.APIKEY,
        in = SecuritySchemeIn.HEADER,
        paramName = "X-API-KEY",
        description = "API-Key im Header X-API-KEY. Fehlt er, kommt 401, ist er falsch, kommt 403."
)
public class FloodMonitorApplication {

    public static void main(String[] args) {
        SpringApplication.run(FloodMonitorApplication.class, args);
    }
}