package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.course;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventService;

@Profile("course-service")
@SpringBootApplication(scanBasePackages = {
        "pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.course",
        "pt.ulisboa.tecnico.socialsoftware.quizzes.coordination.webapi",
        "pt.ulisboa.tecnico.socialsoftware.ms",
})
@EnableJpaRepositories(basePackages = {
        "pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.course",
        "pt.ulisboa.tecnico.socialsoftware.ms.domain.event",
        "pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate",
        "pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate",
        "pt.ulisboa.tecnico.socialsoftware.ms.causal.aggregate",
})
@EntityScan(basePackages = {
        "pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.course",
        "pt.ulisboa.tecnico.socialsoftware.quizzes.events",
        "pt.ulisboa.tecnico.socialsoftware.ms"
})
@PropertySource({"classpath:application-course-service.yaml"})
@EnableScheduling
public class CourseServiceApplication implements InitializingBean {
    @Autowired
    private EventService eventService;

    public static void main(String[] args) {
        SpringApplication.run(CourseServiceApplication.class, args);
    }

    @Override
    public void afterPropertiesSet() {
        // Run on startup
        eventService.clearEventsAtApplicationStartUp();
    }
}
