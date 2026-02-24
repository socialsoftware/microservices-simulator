package pt.ulisboa.tecnico.socialsoftware.ms.domain.version;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Profile;

@Profile("version-service")
@SpringBootApplication(scanBasePackages = {
        "pt.ulisboa.tecnico.socialsoftware.ms.domain.version",
        "pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command",
        "pt.ulisboa.tecnico.socialsoftware.ms.exception"
})
public class VersionServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(VersionServiceApplication.class, args);
    }
}
