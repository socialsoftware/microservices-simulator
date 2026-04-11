package pt.ulisboa.tecnico.socialsoftware.ms

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.PropertySource
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.AggregateIdGeneratorService
import pt.ulisboa.tecnico.socialsoftware.ms.notification.EventApplicationService
import pt.ulisboa.tecnico.socialsoftware.ms.notification.EventService
import pt.ulisboa.tecnico.socialsoftware.ms.transactional.sagas.unitOfWork.SagaUnitOfWorkService
import pt.ulisboa.tecnico.socialsoftware.ms.versioning.CentralizedVersionService
import pt.ulisboa.tecnico.socialsoftware.ms.versioning.IVersionService

@TestConfiguration
@PropertySource("classpath:application-test.properties")
class BeanConfigurationSagas {
    @Bean
    AggregateIdGeneratorService aggregateIdGeneratorService() {
        return new AggregateIdGeneratorService();
    }

    @Bean
    IVersionService versionService() {
        return new CentralizedVersionService();
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
    SagaUnitOfWorkService unitOfWorkService() {
        return new SagaUnitOfWorkService();
    }
}