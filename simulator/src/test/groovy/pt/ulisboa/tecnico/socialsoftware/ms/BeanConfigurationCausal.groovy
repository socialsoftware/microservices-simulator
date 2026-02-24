package pt.ulisboa.tecnico.socialsoftware.ms

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.PropertySource
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.CausalUnitOfWorkService
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.AggregateIdGeneratorService
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventApplicationService
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventService
import pt.ulisboa.tecnico.socialsoftware.ms.domain.version.IVersionService
import pt.ulisboa.tecnico.socialsoftware.ms.domain.version.VersionService

@TestConfiguration
@PropertySource("classpath:application-test.properties")
class BeanConfigurationCausal {
    @Bean
    AggregateIdGeneratorService aggregateIdGeneratorService() {
        return new AggregateIdGeneratorService();
    }

    @Bean
    IVersionService versionService() {
        return new VersionService();
    }

    @Bean
    EventApplicationService eventApplicationService() {
        return new EventApplicationService();
    }

    @Bean
    EventService eventService() {
        return new EventService();
    }

    @Bean
    CausalUnitOfWorkService unitOfWorkService() {
        return new CausalUnitOfWorkService();
    }
}