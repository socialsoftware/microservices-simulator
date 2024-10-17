package pt.ulisboa.tecnico.socialsoftware.ms;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventService;

@PropertySource({"classpath:application.properties"})
@EnableJpaRepositories
@EnableTransactionManagement
@EnableJpaAuditing
@SpringBootApplication
@EnableScheduling
public class MicroservicesSimulator implements InitializingBean {
	public enum TransactionalModel {
		SAGAS("sagas"),
		TCC("tcc");

		private String value;

		TransactionalModel(String value) {
			this.value = value;
		}

		public String getValue() {
			return this.value;
		}

		public void setValue(String value) {
			this.value = value;
		}
	}

	@Autowired
	private EventService eventService;

	public static void main(String[] args) {
		SpringApplication.run(MicroservicesSimulator.class, args);
	}

	@Override
	public void afterPropertiesSet() {
		// Run on startup
		eventService.clearEventsAtApplicationStartUp();
	}

}
