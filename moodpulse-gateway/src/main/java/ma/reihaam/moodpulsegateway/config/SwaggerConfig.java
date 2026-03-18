package ma.reihaam.moodpulsegateway.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("MoodPulse API")
                        .version("1.0.0")
                        .description("Spring Boot gateway for MoodPulse"))
                .servers(List.of(
                        new Server()
                                .url("https://moodpulse-production.up.railway.app")
                                .description("Production"),
                        new Server()
                                .url("http://127.0.0.1:8080")
                                .description("Local")
                ));
    }
}