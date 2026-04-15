package pt.ulisboa.tecnico.socialsoftware.ms.causal.atomicity

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.resilience4j.springboot3.circuitbreaker.autoconfigure.CircuitBreakerAutoConfiguration
import io.github.resilience4j.springboot3.retry.autoconfigure.RetryAutoConfiguration
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import pt.ulisboa.tecnico.socialsoftware.ms.SpockTest
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandHandler
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.MessagingObjectMapperProvider
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.local.LocalCommandGateway
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.local.LocalCommandService
import pt.ulisboa.tecnico.socialsoftware.ms.notification.EventSubscription
import pt.ulisboa.tecnico.socialsoftware.ms.transactional.causal.aggregate.CausalAggregate
import pt.ulisboa.tecnico.socialsoftware.ms.transactional.causal.unitOfWork.CausalUnitOfWorkService
import pt.ulisboa.tecnico.socialsoftware.ms.transactional.causal.unitOfWork.command.CommitCausalCommand
import pt.ulisboa.tecnico.socialsoftware.ms.transactional.causal.unitOfWork.command.PrepareCausalCommand

import java.util.concurrent.atomic.AtomicInteger

@SpringBootTest(classes = [TestApplication, RetryDuplicateLocalConfig])
@ActiveProfiles('test')
@TestPropertySource(properties = [
        'resilience4j.retry.instances.commandGateway.max-attempts=2',
        'resilience4j.retry.instances.commandGateway.wait-duration=10ms',
        'resilience4j.retry.instances.commandGateway.enable-exponential-backoff=false',
        'resilience4j.retry.instances.commandGateway.retry-exceptions[0]=java.lang.RuntimeException',
        'resilience4j.retry.instances.commandGateway.ignore-exceptions[0]=pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException'
])
class RetryDuplicateCausalCommitTest extends SpockTest {

    private CausalUnitOfWorkService causalUnitOfWorkService

    @Autowired
    private LocalCommandGateway localCommandGateway

    def setup() {
        RetryDuplicateLocalConfig.resetState()
        causalUnitOfWorkService = new CausalUnitOfWorkService()
        setField(causalUnitOfWorkService, 'commandGateway', localCommandGateway)
    }

    def 'retry must not duplicate causal commit side effects'() {
        given:
        def aggregate = new TestCausalAggregate(321, 'ExecutionCausal')

        when:
        causalUnitOfWorkService.commitAllObjects(999L, [aggregate])

        then:
        noExceptionThrown()
        aggregate.version == 999L
        aggregate.creationTs != null
        RetryDuplicateLocalConfig.commitAttempts.get() == 2
        RetryDuplicateLocalConfig.committedAggregateIds == [321]
    }

    private static void setField(Object target, String fieldName, Object value) {
        def field = target.class.getDeclaredField(fieldName)
        field.accessible = true
        field.set(target, value)
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = [DataSourceAutoConfiguration, HibernateJpaAutoConfiguration])
    @ImportAutoConfiguration([RetryAutoConfiguration, CircuitBreakerAutoConfiguration])
    static class TestApplication {}

    @Configuration
    static class RetryDuplicateLocalConfig {
        static final AtomicInteger commitAttempts = new AtomicInteger(0)
        static final List<Integer> committedAggregateIds = Collections.synchronizedList(new ArrayList<>())

        static void resetState() {
            commitAttempts.set(0)
            committedAggregateIds.clear()
        }

        @Bean
        MessagingObjectMapperProvider messagingObjectMapperProvider() {
            return new MessagingObjectMapperProvider(new ObjectMapper().findAndRegisterModules())
        }

        @Bean
        LocalCommandService localCommandService(ApplicationContext applicationContext, MessagingObjectMapperProvider mapperProvider) {
            return new LocalCommandService(applicationContext, mapperProvider)
        }

        @Bean
        LocalCommandGateway localCommandGateway(ApplicationContext applicationContext,
                                                io.github.resilience4j.retry.RetryRegistry retryRegistry,
                                                LocalCommandService localCommandService,
                                                MessagingObjectMapperProvider mapperProvider) {
            return new LocalCommandGateway(applicationContext, retryRegistry, localCommandService, mapperProvider)
        }

        @Bean
        CommandHandler executionCommandHandler() {
            return new CommandHandler() {
                @Override
                String getAggregateTypeName() {
                    return 'ExecutionCausal'
                }

                @Override
                Object handleDomainCommand(Command command) {
                    if (command instanceof PrepareCausalCommand) {
                        return null
                    }

                    if (command instanceof CommitCausalCommand) {
                        def commitCommand = (CommitCausalCommand) command
                        committedAggregateIds.add(commitCommand.rootAggregateId)
                        if (commitAttempts.incrementAndGet() == 1) {
                            // Simulates transport timeout after business side effect already happened.
                            throw new RuntimeException('post-side-effect transport failure')
                        }
                        return null
                    }

                    throw new RuntimeException('Unexpected command type: ' + command.getClass().getSimpleName())
                }
            }
        }
    }

    static class TestCausalAggregate extends Aggregate implements CausalAggregate {
        TestCausalAggregate(Integer aggregateId, String aggregateType) {
            super(aggregateId)
            setAggregateType(aggregateType)
        }

        @Override
        void verifyInvariants() {
            // No-op in this test fixture.
        }

        @Override
        Set<EventSubscription> getEventSubscriptions() {
            return [] as Set
        }

        @Override
        Aggregate mergeFields(Set<String> toCommitVersionChangedFields, Aggregate committedVersion, Set<String> committedVersionChangedFields) {
            return this
        }

        @Override
        Set<String[]> getIntentions() {
            return [] as Set
        }

        @Override
        Set<String> getMutableFields() {
            return [] as Set
        }
    }
}
