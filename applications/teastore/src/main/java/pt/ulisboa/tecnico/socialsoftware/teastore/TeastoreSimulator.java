package pt.ulisboa.tecnico.socialsoftware.teastore;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventService;

@PropertySource({"classpath:application.properties"})
@EnableJpaRepositories(basePackages = {".ms.*",
".teastore.*"})
@EntityScan(basePackages = {".ms.*",
".teastore.*"})
@EnableTransactionManagement
@EnableJpaAuditing


@SpringBootApplication(scanBasePackages = {".ms.*",
".teastore.*"})
public class TeastoreSimulator implements InitializingBean {


public static void main(String[] args) {
SpringApplication.run(TeastoreSimulator.class, args);
}

@Override
public void afterPropertiesSet() {
// Run on startup

}
}