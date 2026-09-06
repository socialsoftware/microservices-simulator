package pt.ulisboa.tecnico.socialsoftware.ms.monitoring.impact;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Table;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventSubscription;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = PersistentStateObserverDatabaseTest.TestApplication.class, properties = {
        "spring.main.web-application-type=none",
        "spring.profiles.active=test",
        "spring.datasource.url=jdbc:h2:mem:impact-observer;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class PersistentStateObserverDatabaseTest {
    @Autowired private EntityManager entityManager;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private PersistentStateObserver observer;

    @Test
    void committedRevisionProjectionUsesDatabaseTimestampPrecision() {
        LocalDateTime callerValue = LocalDateTime.of(2026, 9, 6, 1, 2, 3, 123_456_789);
        ImpactEvidence.AggregateIdentity identity =
                new ImpactEvidence.AggregateIdentity("PrecisionAggregate", 7001);
        AtomicReference<ImpactEvidence.Projection> committedProjection = new AtomicReference<>();
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        transaction.executeWithoutResult(status -> {
            PrecisionAggregate aggregate = new PrecisionAggregate(7001, callerValue);
            aggregate.setVersion(31L);
            aggregate.setCreationTs(callerValue);
            entityManager.persist(aggregate);
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() {
                    committedProjection.set(observer.snapshotVersion(identity, 31L, "WRITE"));
                }
            });
        });

        ImpactEvidence.Projection projection = committedProjection.get();

        assertThat(projection).isNotNull();
        assertThat(projection.gaps()).isEmpty();
        assertThat(projection.snapshot().applicationData().get("lastModifiedTime"))
                .isEqualTo("2026-09-06T01:02:03.123457")
                .isNotEqualTo(callerValue.toString());
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = PrecisionAggregate.class)
    @Import(PersistentStateObserver.class)
    static class TestApplication {
    }

    @Entity(name = "ImpactPrecisionAggregate")
    @Table(name = "impact_precision_aggregate")
    static class PrecisionAggregate extends Aggregate {
        @Column(columnDefinition = "timestamp(6)")
        private LocalDateTime lastModifiedTime;

        protected PrecisionAggregate() {
        }

        PrecisionAggregate(Integer aggregateId, LocalDateTime lastModifiedTime) {
            super(aggregateId);
            setAggregateType("PrecisionAggregate");
            this.lastModifiedTime = lastModifiedTime;
        }

        @Override public void verifyInvariants() {
        }

        @Override public Set<EventSubscription> getEventSubscriptions() {
            return Set.of();
        }
    }
}
