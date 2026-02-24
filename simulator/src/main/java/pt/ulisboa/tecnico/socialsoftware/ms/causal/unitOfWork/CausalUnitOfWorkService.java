package pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork;

import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.aggregate.CausalAggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.aggregate.CausalAggregateRepository;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.command.AbortCausalCommand;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.command.CommitCausalCommand;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.command.GetConcurrentAggregateCommand;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.command.PrepareCausalCommand;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventRepository;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.version.IVersionService;
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorErrorMessage;
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException;
import pt.ulisboa.tecnico.socialsoftware.ms.utils.DateHandler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorErrorMessage.*;

@Profile("tcc")
@Service
public class CausalUnitOfWorkService extends UnitOfWorkService<CausalUnitOfWork> {
    private static final Logger logger = LoggerFactory.getLogger(CausalUnitOfWorkService.class);

    @Autowired
    private EntityManager entityManager;
    @Autowired
    private CausalAggregateRepository causalAggregateRepository;
    @Autowired
    private IVersionService versionService;
    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private CommandGateway commandGateway;
    @Autowired
    private Environment environment;

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public CausalUnitOfWork createUnitOfWork(String functionalityName) {
        Integer lastCommittedAggregateVersionNumber = versionService.getVersionNumber();

        CausalUnitOfWork unitOfWork = new CausalUnitOfWork(lastCommittedAggregateVersionNumber + 1, functionalityName);

        logger.info("START EXECUTION FUNCTIONALITY: {} with version {}", functionalityName, unitOfWork.getVersion());

        return unitOfWork;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Aggregate aggregateLoadAndRegisterRead(Integer aggregateId, CausalUnitOfWork unitOfWork) {
        Aggregate aggregate = causalAggregateRepository.findCausal(aggregateId, unitOfWork.getVersion())
                .orElseThrow(() -> new SimulatorException(AGGREGATE_NOT_FOUND, aggregateId));

        if (aggregate.getState() == Aggregate.AggregateState.DELETED) {
            throw new SimulatorException(AGGREGATE_DELETED, aggregate.getAggregateType(), aggregate.getAggregateId());
        }

        List<Event> allEvents = eventRepository.findAll();

        unitOfWork.addToCausalSnapshot(aggregate, allEvents);
        return aggregate;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Aggregate aggregateLoad(Integer aggregateId, CausalUnitOfWork unitOfWork) {
        Aggregate aggregate = causalAggregateRepository.findCausal(aggregateId, unitOfWork.getVersion())
                .orElseThrow(() -> new SimulatorException(AGGREGATE_NOT_FOUND, aggregateId));

        if (aggregate.getState() == Aggregate.AggregateState.DELETED) {
            throw new SimulatorException(AGGREGATE_DELETED, aggregate.getAggregateType(), aggregate.getAggregateId());
        }

        return aggregate;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Aggregate registerRead(Aggregate aggregate, CausalUnitOfWork unitOfWork) {
        List<Event> allEvents = eventRepository.findAll();

        unitOfWork.addToCausalSnapshot(aggregate, allEvents);
        return aggregate;
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void commit(CausalUnitOfWork unitOfWork) {
        boolean concurrentAggregates = true;

        // STEP 1 check whether any of the aggregates to write have concurrent versions
        // STEP 2 if so perform any merges necessary
        // STEP 3 performs steps 1 and 2 until step 1 stops holding
        // STEP 4 perform a commit of the aggregates under SERIALIZABLE isolation

        // may contain merged aggregates we do not want to compare intermediate merged aggregates with concurrent
        // aggregate, so we separate the comparison is always between the original written by the functionality
        // and the concurrent
        List<Aggregate> originalAggregatesToCommit = new ArrayList<>(unitOfWork.getAggregatesToCommit());
        logger.info("Aggregates to commit: {} aggregates", originalAggregatesToCommit.size());
        for (Aggregate agg : originalAggregatesToCommit) {
            logger.info("  - {}: {}", agg.getAggregateType(), agg.getAggregateId());
        }

        // Modified list may have merged versions - start as copy of original
        List<Aggregate> modifiedAggregatesToCommit = new ArrayList<>(originalAggregatesToCommit);

        while (concurrentAggregates) {
            concurrentAggregates = false;
            for (int i = 0; i < originalAggregatesToCommit.size(); i++) {
                Aggregate aggregateToWrite = originalAggregatesToCommit.get(i);
                if (aggregateToWrite.getPrev() != null
                        && aggregateToWrite.getPrev().getState() == Aggregate.AggregateState.INACTIVE) {
                    throw new SimulatorException(CANNOT_MODIFY_INACTIVE_AGGREGATE, aggregateToWrite.getAggregateId());
                }
                aggregateToWrite.verifyInvariants();

                Aggregate concurrentAggregate = null;
                if (aggregateToWrite.getPrev() != null) {
                    String serviceName = this.resolveServiceName(aggregateToWrite.getAggregateType());
                    GetConcurrentAggregateCommand command = new GetConcurrentAggregateCommand(aggregateToWrite.getAggregateId(), serviceName, aggregateToWrite.getPrev().getVersion());
                    concurrentAggregate = (Aggregate) commandGateway.send(command);
                }

                // second condition is necessary for when a concurrent version is detected at
                // first and then in the following detections it will have to do
                // this verification in order to not detect the same as a version as concurrent
                // again
                if (concurrentAggregate != null && unitOfWork.getVersion() <= concurrentAggregate.getVersion()) {
                    concurrentAggregates = true;
                    Aggregate newAggregate = ((CausalAggregate) aggregateToWrite).merge(aggregateToWrite, concurrentAggregate);
                    newAggregate.verifyInvariants();
                    newAggregate.setId(null);
                    modifiedAggregatesToCommit.set(i, newAggregate);
                }
            }

            if (concurrentAggregates) {
                // because there was a concurrent version we need to get a new version
                // the service to get a new version must also increment it to guarantee two
                // transactions do run with the same version number
                // a number must be requested every time a concurrent version is detected
                unitOfWork.setVersion(versionService.incrementAndGetVersionNumber());
            }
        }

        // The commit is done with the last commited version plus one
        Integer commitVersion = versionService.incrementAndGetVersionNumber();
        unitOfWork.setVersion(commitVersion);

        commitAllObjects(commitVersion, modifiedAggregatesToCommit);
        unitOfWork.getEventsToEmit().forEach(e -> {
            /*
             * this is so event detectors can compare this version to those of running
             * transactions
             */
            e.setPublisherAggregateVersion(commitVersion);
            // If running with "local" profile, mark event as published immediately
            if (Arrays.asList(environment.getActiveProfiles()).contains("local")) {
                e.setPublished(true);
            }
            eventRepository.save(e);
        });

        logger.info("END EXECUTION FUNCTIONALITY: {} with version {}", unitOfWork.getFunctionalityName(),
                unitOfWork.getVersion());
    }

    // Must be serializable in order to ensure no other commits are made between the
    // checking of concurrent versions and the actual persist
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void commitAllObjects(Integer commitVersion, List<Aggregate> aggregates) {
        // Phase 1: Prepare
        for (Aggregate aggregateToWrite : aggregates) {
            aggregateToWrite.setVersion(commitVersion);
            aggregateToWrite.setCreationTs(DateHandler.now());

            String serviceName = this.resolveServiceName(aggregateToWrite.getAggregateType());
            PrepareCausalCommand command = new PrepareCausalCommand(aggregateToWrite.getAggregateId(), serviceName,
                    aggregateToWrite);
            try {
                commandGateway.send(command);
            } catch (Exception e) {
                abortAll(aggregates);
                throw new SimulatorException(CANNOT_COMMIT_CAUSAL, e.getMessage());
            }
        }

        // Phase 2: Commit
        for (Aggregate aggregateToWrite : aggregates) {
            String serviceName = this.resolveServiceName(aggregateToWrite.getAggregateType());
            CommitCausalCommand command = new CommitCausalCommand(aggregateToWrite.getAggregateId(), serviceName,
                    aggregateToWrite);
            commandGateway.send(command);
        }
    }

    private void abortAll(List<Aggregate> aggregates) {
        for (Aggregate aggregateToWrite : aggregates) {
            String serviceName = this.resolveServiceName(aggregateToWrite.getAggregateType());
            AbortCausalCommand command = new AbortCausalCommand(aggregateToWrite.getAggregateId(), serviceName);
            try {
                commandGateway.send(command);
            } catch (Exception e) {
                logger.error("Failed to abort causal commit for aggregate " + aggregateToWrite.getAggregateId(), e);
            }
        }
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    @Override
    public void abort(CausalUnitOfWork unitOfWork) {
        // Not needed
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    @Override
    public void registerChanged(Aggregate aggregate, CausalUnitOfWork unitOfWork) {
        // the id set to null to force a new entry in the db
        aggregate.setId(null);
        logger.info("aggregate to commit: {}", aggregate.getClass().getSimpleName());
        unitOfWork.getAggregatesToCommit().add(aggregate);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    @Override
    public void registerEvent(Event event, CausalUnitOfWork unitOfWork) {
        unitOfWork.addEvent(event);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void commitCausal(Aggregate aggregate) {
        entityManager.merge(aggregate);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void prepareCausal(Aggregate aggregate) {
        // TODO what to prepare
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void abortCausal(Integer aggregateId) {
        logger.info("Aborting causal commit for aggregate {}", aggregateId);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Aggregate getConcurrentAggregate(Integer aggregateId, Integer prevVersion, String aggregateType) {
        Aggregate concurrentAggregate = causalAggregateRepository.findConcurrentVersions(aggregateId, prevVersion)
                .orElse(null);

        // if a concurrent version is deleted it means the object has been deleted in
        // the meanwhile
        if (concurrentAggregate != null && (concurrentAggregate.getState() == Aggregate.AggregateState.DELETED
                || concurrentAggregate.getState() == Aggregate.AggregateState.INACTIVE)) {
            throw new SimulatorException(SimulatorErrorMessage.AGGREGATE_DELETED, aggregateType, aggregateId);
        }

        return concurrentAggregate;
    }

}
