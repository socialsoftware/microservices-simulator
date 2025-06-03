/**
 * Generates a Java service class for an aggregate.
 * 
 * @param aggregate The aggregate to generate a service for
 * @param projectName The project name
 * @returns The generated service class code as a string
 */
export function generateServiceCode(aggregate: any, projectName: string): string {
    const packageName = `pt.ulisboa.tecnico.socialsoftware.${projectName.toLowerCase()}.microservices.${aggregate.name.toLowerCase()}.service`;
    const entityName = aggregate.entities.find((e: any) => e.isRoot)?.name || `${aggregate.name}`;

    return `package ${packageName};

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.AggregateIdGeneratorService;
import pt.ulisboa.tecnico.socialsoftware.${projectName.toLowerCase()}.microservices.exception.QuizzesException;
import pt.ulisboa.tecnico.socialsoftware.${projectName.toLowerCase()}.microservices.${aggregate.name.toLowerCase()}.aggregate.*;

@Service
public class ${aggregate.name}Service {
    @Autowired
    private AggregateIdGeneratorService aggregateIdGeneratorService;

    private final UnitOfWorkService unitOfWorkService;
    private final ${aggregate.name}CustomRepository ${aggregate.name.toLowerCase()}Repository;
    
    @Autowired
    private ${aggregate.name}Factory ${aggregate.name.toLowerCase()}Factory;

    public ${aggregate.name}Service(UnitOfWorkService unitOfWorkService, ${aggregate.name}CustomRepository ${aggregate.name.toLowerCase()}Repository) {
        this.unitOfWorkService = unitOfWorkService;
        this.${aggregate.name.toLowerCase()}Repository = ${aggregate.name.toLowerCase()}Repository;
    }

    @Retryable(
            value = { SQLException.class },
            backoff = @Backoff(delay = 5000))
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public ${entityName}Dto get${entityName}ById(Integer aggregateId, UnitOfWork unitOfWork) {
        return ${aggregate.name.toLowerCase()}Factory.create${entityName}Dto((${entityName}) unitOfWorkService.aggregateLoadAndRegisterRead(aggregateId, unitOfWork));
    }

    @Retryable(
            value = { SQLException.class },
            backoff = @Backoff(delay = 5000))
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public ${entityName}Dto create${entityName}(${entityName}Dto ${entityName.toLowerCase()}Dto, UnitOfWork unitOfWork) {
        Integer aggregateId = aggregateIdGeneratorService.getNewAggregateId();
        
        ${entityName} ${entityName.toLowerCase()} = ${aggregate.name.toLowerCase()}Factory.create${entityName}(aggregateId, ${entityName.toLowerCase()}Dto);

        unitOfWorkService.registerChanged(${entityName.toLowerCase()}, unitOfWork);
        return ${aggregate.name.toLowerCase()}Factory.create${entityName}Dto(${entityName.toLowerCase()});
    }

    @Retryable(
            value = { SQLException.class },
            backoff = @Backoff(delay = 5000))
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public ${entityName}Dto update${entityName}(${entityName}Dto ${entityName.toLowerCase()}Dto, UnitOfWork unitOfWork) {
        ${entityName} old${entityName} = (${entityName}) unitOfWorkService.aggregateLoadAndRegisterRead(${entityName.toLowerCase()}Dto.getAggregateId(), unitOfWork);
        ${entityName} new${entityName} = ${aggregate.name.toLowerCase()}Factory.create${entityName}FromExisting(old${entityName});

        // TODO: Implement update logic based on DTO fields

        unitOfWorkService.registerChanged(new${entityName}, unitOfWork);
        return ${aggregate.name.toLowerCase()}Factory.create${entityName}Dto(new${entityName});
    }

    @Retryable(
            value = { SQLException.class },
            backoff = @Backoff(delay = 5000))
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void remove${entityName}(Integer aggregateId, UnitOfWork unitOfWork) {
        ${entityName} old${entityName} = (${entityName}) unitOfWorkService.aggregateLoadAndRegisterRead(aggregateId, unitOfWork);
        ${entityName} new${entityName} = ${aggregate.name.toLowerCase()}Factory.create${entityName}FromExisting(old${entityName});
        new${entityName}.remove();
        unitOfWorkService.registerChanged(new${entityName}, unitOfWork);
    }

    /******************************************* EVENT PROCESSING SERVICES ********************************************/

    @Retryable(
            value = { SQLException.class },
            backoff = @Backoff(delay = 5000))
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public ${entityName} anonymizeUser(Integer aggregateId, Integer userAggregateId, String name, String username, Integer eventVersion, UnitOfWork unitOfWork) {
        ${entityName} old${entityName} = (${entityName}) unitOfWorkService.aggregateLoadAndRegisterRead(aggregateId, unitOfWork);
        ${entityName} new${entityName} = ${aggregate.name.toLowerCase()}Factory.create${entityName}FromExisting(old${entityName});

        // TODO: Implement anonymization logic

        unitOfWorkService.registerChanged(new${entityName}, unitOfWork);
        return new${entityName};
    }

    @Retryable(
            value = { SQLException.class },
            backoff = @Backoff(delay = 5000))
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public ${entityName} removeUser(Integer aggregateId, Integer userAggregateId, Integer eventVersion, UnitOfWork unitOfWork) {
        ${entityName} old${entityName} = (${entityName}) unitOfWorkService.aggregateLoadAndRegisterRead(aggregateId, unitOfWork);
        ${entityName} new${entityName} = ${aggregate.name.toLowerCase()}Factory.create${entityName}FromExisting(old${entityName});

        // TODO: Implement user removal logic
        
        unitOfWorkService.registerChanged(new${entityName}, unitOfWork);
        return new${entityName};
    }
}`;
} 