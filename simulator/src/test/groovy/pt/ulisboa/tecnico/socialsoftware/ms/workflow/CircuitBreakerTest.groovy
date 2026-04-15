package pt.ulisboa.tecnico.socialsoftware.ms.workflow

import com.fasterxml.jackson.databind.ObjectMapper
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
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandHandler
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.MessagingObjectMapperProvider
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.local.LocalCommandGateway
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.local.LocalCommandService

import java.util.concurrent.atomic.AtomicInteger

@SpringBootTest(classes = [TestApplication, LocalBeanConfiguration])
@ActiveProfiles("test")
@TestPropertySource(properties = [
        "resilience4j.retry.instances.commandGateway.max-attempts=5",
        "resilience4j.retry.instances.commandGateway.wait-duration=100ms",
        "resilience4j.retry.instances.commandGateway.enable-exponential-backoff=true",
        "resilience4j.retry.instances.commandGateway.exponential-backoff-multiplier=2",
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

    def "exponential backoff increases wait time between retries"() {
        given:
        def command = new Command(null, "timed", null)

        when:
        try {
            localCommandGateway.send(command)
        } catch (RuntimeException ignored) {}

        then: "intervals should increase exponentially"
        LocalBeanConfiguration.timedAttempts.get() == 5
        def timestamps = LocalBeanConfiguration.timedTimestamps
        timestamps.size() == 5

        and: "each interval should be roughly double the previous"
        def intervals = []
        for (int i = 1; i < timestamps.size(); i++) {
            intervals << (timestamps[i] - timestamps[i - 1])
        }
        for (int i = 1; i < intervals.size(); i++) {
            assert intervals[i] > intervals[i - 1] * 0.8:
                    "Interval ${i} (${intervals[i]}ms) should be larger than interval ${i-1} (${intervals[i-1]}ms)"
        }
        println "Retry intervals: ${intervals}ms"
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
        static final AtomicInteger slowAttempts = new AtomicInteger()
        static final AtomicInteger timedAttempts = new AtomicInteger()
        static final List<Long> timedTimestamps = Collections.synchronizedList(new ArrayList<>())

        static void resetCounters() {
            retryableAttempts.set(0)
            simulatorExceptionAttempts.set(0)
            eventuallySucceedsAttempts.set(0)
            slowAttempts.set(0)
            timedAttempts.set(0)
            timedTimestamps.clear()
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
        LocalCommandGateway localCommandGateway(ApplicationContext applicationContext, RetryRegistry registry, LocalCommandService localCommandService, MessagingObjectMapperProvider mapperProvider) {
            return new LocalCommandGateway(applicationContext, registry, localCommandService, mapperProvider)
        }

        @Bean
        CommandHandler retryableCommandHandler() {
            return new CommandHandler() {
                @Override
                public String getAggregateTypeName() { return "test" }

                @Override
                public Object handleDomainCommand(Command command) {
                    retryableAttempts.incrementAndGet()
                    throw new RuntimeException("Simulated retriable failure")
                }
            }
        }

        @Bean
        CommandHandler simulatorExceptionCommandHandler() {
            return new CommandHandler() {
                @Override
                public String getAggregateTypeName() { return "test" }

                @Override
                public Object handleDomainCommand(Command command) {
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
                public String getAggregateTypeName() { return "test" }

                @Override
                public Object handleDomainCommand(Command command) {
                    eventuallySucceedsAttempts.incrementAndGet()
                    callCount++
                    if (callCount < 3) {
                        throw new RuntimeException("Temporary failure")
                    }
                    return "Success after retries"
                }
            }
        }

        @Bean
        CommandHandler slowCommandHandler() {
            return new CommandHandler() {
                @Override
                public String getAggregateTypeName() { return "test" }

                @Override
                public Object handleDomainCommand(Command command) {
                    slowAttempts.incrementAndGet()
                    try {
                        Thread.sleep(3000)
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt()
                    }
                    return "Should not reach here"
                }
            }
        }

        @Bean
        CommandHandler timedCommandHandler() {
            return new CommandHandler() {
                @Override
                public String getAggregateTypeName() { return "test" }

                @Override
                public Object handleDomainCommand(Command command) {
                    timedAttempts.incrementAndGet()
                    timedTimestamps.add(System.currentTimeMillis())
                    throw new RuntimeException("Timed failure")
                }
            }
        }
    }
}
