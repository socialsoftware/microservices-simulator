package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;
import pt.ulisboa.tecnico.socialsoftware.ms.notification.EventService;

@Profile("route-service")
@SpringBootApplication(scanBasePackages = {
        "pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route",
        "pt.ulisboa.tecnico.socialsoftware.ms"})
@EnableJpaRepositories(basePackages = {
        "pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route",
        "pt.ulisboa.tecnico.socialsoftware.ms"})
@EntityScan(basePackages = {
        "pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route",
        "pt.ulisboa.tecnico.socialsoftware.ms"})
@EnableScheduling
public class RouteServiceApplication implements InitializingBean {
    @Autowired
    private EventService eventService;

    public static void main(String[] args) {
        SpringApplication.run(RouteServiceApplication.class, args);
    }

    @Override
    public void afterPropertiesSet() {
        eventService.clearEventsAtApplicationStartUp();
    }
}
