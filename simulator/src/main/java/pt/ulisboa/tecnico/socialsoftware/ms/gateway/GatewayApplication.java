package pt.ulisboa.tecnico.socialsoftware.ms.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Profile;

@Profile("gateway")
@SpringBootApplication(scanBasePackages = "pt.ulisboa.tecnico.socialsoftware.ms.gateway")
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }

}
