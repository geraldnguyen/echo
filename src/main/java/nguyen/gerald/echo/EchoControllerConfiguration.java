package nguyen.gerald.echo;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EchoControllerConfiguration {
    @Bean
    public EchoController echoController(ObjectMapper objectMapper) {
        return new EchoController(objectMapper);
    }
}
