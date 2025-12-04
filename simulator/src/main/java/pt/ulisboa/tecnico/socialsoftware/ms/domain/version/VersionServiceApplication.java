package pt.ulisboa.tecnico.socialsoftware.ms.domain.version;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@EnableJpaRepositories(basePackages = { "pt.ulisboa.tecnico.socialsoftware.ms.domain.version" })
@EntityScan(basePackages = { "pt.ulisboa.tecnico.socialsoftware.ms.domain.version" })
@EnableTransactionManagement
@SpringBootApplication(scanBasePackages = {
        "pt.ulisboa.tecnico.socialsoftware.ms.domain.version",
        "pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.stream",
        "pt.ulisboa.tecnico.socialsoftware.ms.exception"
})
public class VersionServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(VersionServiceApplication.class, args);
    }
}
