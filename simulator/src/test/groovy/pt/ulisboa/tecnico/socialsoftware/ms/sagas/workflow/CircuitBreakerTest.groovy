package pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow

import io.github.resilience4j.retry.Retry
import io.github.resilience4j.retry.RetryRegistry
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
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandHandler
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.local.LocalCommandGateway
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException

import java.util.concurrent.atomic.AtomicInteger

@SpringBootTest(classes = [TestApplication, LocalBeanConfiguration])
@ActiveProfiles("test")
@TestPropertySource(properties = [
        "resilience4j.retry.instances.commandGateway.max-attempts=5",
        "resilience4j.retry.instances.commandGateway.wait-duration=100ms",
        "resilience4j.retry.instances.commandGateway.retry-exceptions[0]=java.lang.RuntimeException",
        "resilience4j.retry.instances.commandGateway.ignore-exceptions[0]=pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException"
])
class CircuitBreakerTest extends SpockTest {

    @Autowired
    LocalCommandGateway localCommandGateway

    @Autowired
    RetryRegistry retryRegistry

    Retry retry

    def setup() {
        LocalBeanConfiguration.resetCounters()
        retry = retryRegistry.retry("commandGateway")
        println "Configured maxAttempts = " + retry.retryConfig.maxAttempts
    }

    def "retry mechanism retries on RuntimeException"() {
        given:
        def command = new Command(null, "retryable", null)

        when:
        def failureCount = 0
        try {
            localCommandGateway.send(command)
        } catch (RuntimeException ignored) {
            failureCount++
        }

        then:
        failureCount == 1
        LocalBeanConfiguration.retryableAttempts.get() == 5
    }

    def "retry mechanism does not retry on SimulatorException"() {
        given:
        def command = new Command(null, "simulatorException", null)

        when:
        def failureCount = 0
        try {
            localCommandGateway.send(command)
        } catch (SimulatorException ignored) {
            failureCount++
        }

        then:
        failureCount == 1
        LocalBeanConfiguration.simulatorExceptionAttempts.get() == 1
    }

    def "retry mechanism succeeds after retries"() {
        given:
        def command = new Command(null, "eventuallySucceeds", null)

        when:
        def result = localCommandGateway.send(command)

        then:
        result == "Success after retries"
        LocalBeanConfiguration.eventuallySucceedsAttempts.get() == 3
    }

    def "retry metrics are recorded correctly"() {
        given:
        def command = new Command(null, "retryable", null)

        when:
        try {
            localCommandGateway.send(command)
        } catch (RuntimeException ignored) {}

        then:
        LocalBeanConfiguration.retryableAttempts.get() == 5
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = [DataSourceAutoConfiguration, HibernateJpaAutoConfiguration])
    @ImportAutoConfiguration([RetryAutoConfiguration, CircuitBreakerAutoConfiguration])
    static class TestApplication {}

    @Configuration
    static class LocalBeanConfiguration {
        static final AtomicInteger retryableAttempts = new AtomicInteger()
        static final AtomicInteger simulatorExceptionAttempts = new AtomicInteger()
        static final AtomicInteger eventuallySucceedsAttempts = new AtomicInteger()

        static void resetCounters() {
            retryableAttempts.set(0)
            simulatorExceptionAttempts.set(0)
            eventuallySucceedsAttempts.set(0)
        }

        @Bean
        LocalCommandGateway localCommandGateway(ApplicationContext applicationContext, RetryRegistry retryRegistry) {
            return new LocalCommandGateway(applicationContext, retryRegistry)
        }

        @Bean
        CommandHandler retryableCommandHandler() {
            return new CommandHandler() {
                @Override
                protected String getAggregateTypeName() { return "test" }

                @Override
                protected Object handleDomainCommand(Command command) {
                    retryableAttempts.incrementAndGet()
                    throw new RuntimeException("Simulated retriable failure")
                }
            }
        }

        @Bean
        CommandHandler simulatorExceptionCommandHandler() {
            return new CommandHandler() {
                @Override
                protected String getAggregateTypeName() { return "test" }

                @Override
                protected Object handleDomainCommand(Command command) {
                    simulatorExceptionAttempts.incrementAndGet()
                    throw new SimulatorException("Should not retry")
                }
            }
        }

        @Bean
        CommandHandler eventuallySucceedsCommandHandler() {
            return new CommandHandler() {
                private int callCount = 0

                @Override
                protected String getAggregateTypeName() { return "test" }

                @Override
                protected Object handleDomainCommand(Command command) {
                    eventuallySucceedsAttempts.incrementAndGet()
                    callCount++
                    if (callCount < 3) {
                        throw new RuntimeException("Temporary failure")
                    }
                    return "Success after retries"
                }
            }
        }
    }
}
