package pt.ulisboa.tecnico.socialsoftware.quizzes;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Profile("course-service")
@SpringBootApplication
@EnableRetry
@EnableTransactionManagement
@EnableJpaAuditing
@EnableScheduling
@ComponentScan(basePackages = {
                "pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.course",
                "pt.ulisboa.tecnico.socialsoftware.quizzes.coordination.webapi",
                "pt.ulisboa.tecnico.socialsoftware.quizzes.coordination.functionalities",
                "pt.ulisboa.tecnico.socialsoftware.ms"
}, excludeFilters = {
                @ComponentScan.Filter(type = FilterType.REGEX, pattern = "pt\\.ulisboa\\.tecnico\\.socialsoftware\\.quizzes\\.microservices\\.(?!course).*"),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = QuizzesSimulator.class)
})
@EntityScan(basePackages = {
                "pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.course.aggregate",
                "pt.ulisboa.tecnico.socialsoftware.ms.*"
})
@EnableJpaRepositories(basePackages = {
                "pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.course.aggregate",
                "pt.ulisboa.tecnico.socialsoftware.ms.*"
})
public class CourseServiceApplication {

        public static void main(String[] args) {
                SpringApplication.run(CourseServiceApplication.class, args);
        }
}