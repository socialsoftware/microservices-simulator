package pt.ulisboa.tecnico.socialsoftware.quizzes;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventService;

@PropertySource({"classpath:application.yaml"})
@EnableJpaRepositories(basePackages = {"pt.ulisboa.tecnico.socialsoftware.ms.*", "pt.ulisboa.tecnico.socialsoftware.quizzes.*"})
@EntityScan(basePackages = {"pt.ulisboa.tecnico.socialsoftware.ms.*", "pt.ulisboa.tecnico.socialsoftware.quizzes.*"})
@EnableTransactionManagement
@EnableJpaAuditing
@EnableScheduling
@EnableRetry
@SpringBootApplication(scanBasePackages = {"pt.ulisboa.tecnico.socialsoftware.ms.*", "pt.ulisboa.tecnico.socialsoftware.quizzes.*"})
public class QuizzesSimulator implements InitializingBean {
	@Autowired
	private EventService eventService;

	public static void main(String[] args) {
		SpringApplication.run(QuizzesSimulator.class, args);
	}

	@Override
	public void afterPropertiesSet() {
		// Run on startup
		eventService.clearEventsAtApplicationStartUp();
	}

}
