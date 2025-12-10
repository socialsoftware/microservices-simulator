package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Profile("user-service")
@SpringBootApplication(scanBasePackages = {
        "pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user",
        "pt.ulisboa.tecnico.socialsoftware.ms",
})
@EnableJpaRepositories(basePackages = {
        "pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user",
        "pt.ulisboa.tecnico.socialsoftware.ms.domain.event",
        "pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate",
        "pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate",
        "pt.ulisboa.tecnico.socialsoftware.ms.causal.aggregate",
})
@EntityScan(basePackages = {
        "pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user",
        "pt.ulisboa.tecnico.socialsoftware.ms"
})
@PropertySource({ "classpath:application-user-service.yaml" })
public class UserServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
