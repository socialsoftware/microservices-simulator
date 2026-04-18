package pt.ulisboa.tecnico.socialsoftware.ms.workflow

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.resilience4j.retry.RetryRegistry
import io.github.resilience4j.springboot3.circuitbreaker.autoconfigure.CircuitBreakerAutoConfiguration
import io.github.resilience4j.springboot3.retry.autoconfigure.RetryAutoConfiguration
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.datasource.DataSourceTransactionManager
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import org.springframework.test.util.ReflectionTestUtils
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.UnexpectedRollbackException
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionSynchronizationManager
import pt.ulisboa.tecnico.socialsoftware.ms.SpockTest
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandHandler
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.MessagingObjectMapperProvider
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.local.LocalCommandGateway
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.local.LocalCommandService
import pt.ulisboa.tecnico.socialsoftware.ms.transactional.sagas.unitOfWork.SagaUnitOfWork
import pt.ulisboa.tecnico.socialsoftware.ms.transactional.sagas.unitOfWork.SagaUnitOfWorkService
import pt.ulisboa.tecnico.socialsoftware.ms.transactional.sagas.unitOfWork.command.CommitSagaCommand
import pt.ulisboa.tecnico.socialsoftware.ms.versioning.VersionServiceClient
import pt.ulisboa.tecnico.socialsoftware.ms.versioning.command.IncrementVersionCommand

import javax.sql.DataSource
import java.util.concurrent.atomic.AtomicInteger

@SpringBootTest(classes = [TestApplication, TransactionalRetryConfig])
@ActiveProfiles("test")
@TestPropertySource(properties = [
        "resilience4j.retry.instances.commandGateway.max-attempts=2",
        "resilience4j.retry.instances.commandGateway.wait-duration=10ms",
        "resilience4j.retry.instances.commandGateway.enable-exponential-backoff=false",
        "resilience4j.retry.instances.commandGateway.retry-exceptions[0]=java.lang.RuntimeException",
        "resilience4j.retry.instances.commandGateway.ignore-exceptions[0]=pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException"
])
class CircuitBreakerTransactionalRetryTest extends SpockTest {

    @Autowired
    TransactionalCallerService transactionalCallerService

    def setup() {
        TransactionalRetryConfig.resetCounters()
    }

    def "merged transaction retry should mark outer transaction rollback-only for command handler path"() {
        when:
        transactionalCallerService.invokeHandlerWithinTransaction()

        then:
        thrown(UnexpectedRollbackException)
        TransactionalRetryConfig.txBoundaryAttempts.get() == 2
        TransactionalRetryConfig.txBoundaryTransactionActivePerAttempt == [true, true]
    }

    def "merged transaction retry should mark outer transaction rollback-only for version increment path"() {
        when:
        transactionalCallerService.invokeVersionIncrementWithinTransaction()

        then:
        thrown(UnexpectedRollbackException)
        TransactionalRetryConfig.versionIncrementAttempts.get() == 2
    }

    def "merged transaction retry should mark outer transaction rollback-only for saga commit path"() {
        when:
        transactionalCallerService.invokeSagaCommitWithinTransaction()

        then:
        thrown(UnexpectedRollbackException)
        TransactionalRetryConfig.sagaCommitAttempts.get() == 2
        TransactionalRetryConfig.sagaCommitTransactionActivePerAttempt == [true, true]
    }

    def "retry should still succeed when no outer transaction exists"() {
        when:
        def result = transactionalCallerService.invokeHandlerWithoutOuterTransaction()

        then:
        result == "tx-ok"
        TransactionalRetryConfig.txBoundaryAttempts.get() == 2
        TransactionalRetryConfig.txBoundaryTransactionActivePerAttempt == [true, true]
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = [HibernateJpaAutoConfiguration, JpaRepositoriesAutoConfiguration])
    @ImportAutoConfiguration([RetryAutoConfiguration, CircuitBreakerAutoConfiguration])
    static class TestApplication {
    }

    @Configuration
    static class TransactionalRetryConfig {
        static final AtomicInteger txBoundaryAttempts = new AtomicInteger(0)
        static final AtomicInteger versionIncrementAttempts = new AtomicInteger(0)
        static final AtomicInteger sagaCommitAttempts = new AtomicInteger(0)

        static final List<Boolean> txBoundaryTransactionActivePerAttempt = Collections.synchronizedList(new ArrayList<>())
        static final List<Boolean> sagaCommitTransactionActivePerAttempt = Collections.synchronizedList(new ArrayList<>())

        static void resetCounters() {
            txBoundaryAttempts.set(0)
            versionIncrementAttempts.set(0)
            sagaCommitAttempts.set(0)
            txBoundaryTransactionActivePerAttempt.clear()
            sagaCommitTransactionActivePerAttempt.clear()
        }

        @Bean
        DataSource dataSource() {
            def dataSource = new DriverManagerDataSource()
            dataSource.setDriverClassName("org.h2.Driver")
            dataSource.setUrl("jdbc:h2:mem:cb-retry-tx;DB_CLOSE_DELAY=-1")
            dataSource.setUsername("sa")
            dataSource.setPassword("sa")
            return dataSource
        }

        @Bean
        PlatformTransactionManager transactionManager(DataSource dataSource) {
            return new DataSourceTransactionManager(dataSource)
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
        LocalCommandGateway localCommandGateway(
                ApplicationContext applicationContext,
                RetryRegistry retryRegistry,
                LocalCommandService localCommandService,
                MessagingObjectMapperProvider mapperProvider) {
            return new LocalCommandGateway(applicationContext, retryRegistry, localCommandService, mapperProvider)
        }

        @Bean
        VersionServiceClient versionServiceClient(LocalCommandGateway localCommandGateway) {
            return new VersionServiceClient(localCommandGateway)
        }

        @Bean
        TransactionalCallerService transactionalCallerService(
                LocalCommandGateway localCommandGateway,
                VersionServiceClient versionServiceClient) {
            def sagaUnitOfWorkService = new SagaUnitOfWorkService()
            ReflectionTestUtils.setField(sagaUnitOfWorkService, "commandGateway", localCommandGateway)
            return new TransactionalCallerService(localCommandGateway, versionServiceClient, sagaUnitOfWorkService)
        }

        @Bean
        CommandHandler txBoundaryCommandHandler() {
            return new CommandHandler() {
                @Override
                String getAggregateTypeName() {
                    return "test"
                }

                @Override
                Object handleDomainCommand(Command command) {
                    txBoundaryTransactionActivePerAttempt.add(TransactionSynchronizationManager.isActualTransactionActive())
                    def attempt = txBoundaryAttempts.incrementAndGet()
                    if (attempt == 1) {
                        throw new RuntimeException("transient handler failure")
                    }
                    return "tx-ok"
                }
            }
        }

        @Bean
        CommandHandler versionCommandHandler() {
            return new CommandHandler() {
                @Override
                String getAggregateTypeName() {
                    return "Version"
                }

                @Override
                Object handleDomainCommand(Command command) {
                    if (command instanceof IncrementVersionCommand) {
                        def attempt = versionIncrementAttempts.incrementAndGet()
                        if (attempt == 1) {
                            throw new RuntimeException("transient version failure")
                        }
                        return 101L
                    }
                    throw new RuntimeException("Unsupported version command in test: " + command.getClass().getSimpleName())
                }
            }
        }

        @Bean
        CommandHandler executionCommandHandler() {
            return new CommandHandler() {
                @Override
                String getAggregateTypeName() {
                    return "SagaExecution"
                }

                @Override
                Object handleDomainCommand(Command command) {
                    if (command instanceof CommitSagaCommand) {
                        sagaCommitTransactionActivePerAttempt.add(TransactionSynchronizationManager.isActualTransactionActive())
                        def attempt = sagaCommitAttempts.incrementAndGet()
                        if (attempt == 1) {
                            throw new RuntimeException("transient commit failure")
                        }
                        return null
                    }
                    throw new RuntimeException("Unsupported execution command in test: " + command.getClass().getSimpleName())
                }
            }
        }
    }

    static class TransactionalCallerService {
        private final LocalCommandGateway localCommandGateway
        private final VersionServiceClient versionServiceClient
        private final SagaUnitOfWorkService sagaUnitOfWorkService

        TransactionalCallerService(LocalCommandGateway localCommandGateway,
                                   VersionServiceClient versionServiceClient,
                                   SagaUnitOfWorkService sagaUnitOfWorkService) {
            this.localCommandGateway = localCommandGateway
            this.versionServiceClient = versionServiceClient
            this.sagaUnitOfWorkService = sagaUnitOfWorkService
        }

        @Transactional(isolation = Isolation.SERIALIZABLE)
        Object invokeHandlerWithinTransaction() {
            return localCommandGateway.send(new Command(null, "txBoundary", null))
        }

        Object invokeHandlerWithoutOuterTransaction() {
            return localCommandGateway.send(new Command(null, "txBoundary", null))
        }

        @Transactional(isolation = Isolation.SERIALIZABLE)
        Long invokeVersionIncrementWithinTransaction() {
            return versionServiceClient.incrementAndGetVersionNumber()
        }

        @Transactional(isolation = Isolation.SERIALIZABLE)
        void invokeSagaCommitWithinTransaction() {
            def unitOfWork = new SagaUnitOfWork(1L, "commitFunctionality")
            unitOfWork.addToAggregatesInSaga(2, "SagaExecution")
            sagaUnitOfWorkService.commit(unitOfWork)
        }
    }
}
