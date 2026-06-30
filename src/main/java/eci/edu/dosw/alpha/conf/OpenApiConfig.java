package eci.edu.dosw.alpha.conf;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI geoServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("GeoService API")
                        .description("Geolocalización simplificada en campus (RF29): gestión de zonas, preferencias y ubicación en tiempo real vía WebSocket.")
                        .version("1.0.0"));
    }
}
