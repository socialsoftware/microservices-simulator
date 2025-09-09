/**
 * Common template components shared across generators
 */

export class CommonTemplates {
    /**
     * Get standard entity template
     */
    static getEntityTemplate(): string {
        return `package {{packageName}}.microservices.{{aggregateName}}.aggregate;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;
import {{packageName}}.microservices.utils.DateHandler;

@Entity
@Table(name = "{{tableName}}")
public class {{entityName}} {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    {{#fields}}
    {{#isPrivate}}private{{/isPrivate}} {{#isProtected}}protected{{/isProtected}} {{type}} {{name}};
    {{/fields}}

    // Constructors
    public {{entityName}}() {}

    public {{entityName}}({{#constructorParams}}{{type}} {{name}}{{^last}}, {{/last}}{{/constructorParams}}) {
        {{#constructorAssignments}}
        {{.}}
        {{/constructorAssignments}}
    }

    // Getters and Setters
    {{#fields}}
    public {{type}} get{{capitalizedName}}() {
        return {{name}};
    }

    public void set{{capitalizedName}}({{type}} {{name}}) {
        this.{{name}} = {{name}};
    }
    {{/fields}}

    // Equals and HashCode
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        {{entityName}} that = ({{entityName}}) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}`;
    }

    /**
     * Get standard DTO template
     */
    static getDtoTemplate(): string {
        return `package {{packageName}}.microservices.{{aggregateName}}.aggregate;

import java.time.LocalDateTime;
import java.util.Objects;

public class {{dtoName}} {
    {{#fields}}
    private {{type}} {{name}};
    {{/fields}}

    // Constructors
    public {{dtoName}}() {}

    public {{dtoName}}({{#constructorParams}}{{type}} {{name}}{{^last}}, {{/last}}{{/constructorParams}}) {
        {{#constructorAssignments}}
        this.{{name}} = {{name}};
        {{/constructorAssignments}}
    }

    // Getters and Setters
    {{#fields}}
    public {{type}} get{{capitalizedName}}() {
        return {{name}};
    }

    public void set{{capitalizedName}}({{type}} {{name}}) {
        this.{{name}} = {{name}};
    }
    {{/fields}}

    // Build method
    public {{entityName}} build() {
        return new {{entityName}}({{#constructorParams}}{{name}}{{^last}}, {{/last}}{{/constructorParams}});
    }

    // Equals and HashCode
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        {{dtoName}} that = ({{dtoName}}) o;
        return Objects.equals({{#fields}}{{name}}{{^last}}, that.{{name}}) && {{/last}}{{^last}}that.{{name}}{{/last}}{{/fields}};
    }

    @Override
    public int hashCode() {
        return Objects.hash({{#fields}}{{name}}{{^last}}, {{/last}}{{/fields}});
    }
}`;
    }

    /**
     * Get standard service template
     */
    static getServiceTemplate(): string {
        return `package {{packageName}}.microservices.{{aggregateName}}.service;

import {{packageName}}.microservices.{{aggregateName}}.aggregate.*;
import {{packageName}}.microservices.{{aggregateName}}.repository.{{repositoryName}};
import {{packageName}}.microservices.utils.UnitOfWork;
import {{packageName}}.microservices.utils.UnitOfWorkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class {{serviceName}} {
    @Autowired
    private {{repositoryName}} {{repositoryVariable}};

    @Autowired
    private UnitOfWorkService unitOfWorkService;

    {{#methods}}
    @Transactional
    public {{returnType}} {{methodName}}({{#parameters}}{{type}} {{name}}{{^last}}, {{/last}}{{/parameters}}) {
        UnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork("{{methodName}}");
        try {
            // TODO: Implement {{methodName}} business logic
            return null;
        } finally {
            unitOfWorkService.commit(unitOfWork);
        }
    }
    {{/methods}}
}`;
    }

    /**
     * Get standard repository template
     */
    static getRepositoryTemplate(): string {
        return `package {{packageName}}.microservices.{{aggregateName}}.repository;

import {{packageName}}.microservices.{{aggregateName}}.aggregate.{{entityName}};
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface {{repositoryName}} extends JpaRepository<{{entityName}}, Integer> {
    {{#queries}}
    @Query("{{query}}")
    List<{{entityName}}> {{methodName}}({{#parameters}}@Param("{{name}}") {{type}} {{name}}{{^last}}, {{/last}}{{/parameters}});
    {{/queries}}
}`;
    }

    /**
     * Get standard factory template
     */
    static getFactoryTemplate(): string {
        return `package {{packageName}}.microservices.{{aggregateName}}.aggregate;

import {{packageName}}.microservices.utils.UnitOfWork;
import {{packageName}}.microservices.utils.UnitOfWorkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class {{factoryName}} {
    @Autowired
    private UnitOfWorkService unitOfWorkService;

    public {{entityName}} create{{entityName}}({{#constructorParams}}{{type}} {{name}}{{^last}}, {{/last}}{{/constructorParams}}) {
        UnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork("create{{entityName}}");
        try {
            // TODO: Implement factory method to create {{entityName}} from parameters
            return new {{entityName}}({{#constructorParams}}{{name}}{{^last}}, {{/last}}{{/constructorParams}});
        } finally {
            unitOfWorkService.commit(unitOfWork);
        }
    }

    public {{entityName}} create{{entityName}}FromDto({{dtoName}} dto) {
        UnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork("create{{entityName}}FromDto");
        try {
            // TODO: Implement factory method to create {{entityName}} from DTO
            return dto.build();
        } finally {
            unitOfWorkService.commit(unitOfWork);
        }
    }
}`;
    }

    /**
     * Get standard validation template
     */
    static getValidationTemplate(): string {
        return `package {{packageName}}.microservices.{{aggregateName}}.validation;

import {{packageName}}.microservices.{{aggregateName}}.aggregate.{{entityName}};
import {{packageName}}.microservices.exception.{{projectName}}Exception;
import {{packageName}}.microservices.exception.{{projectName}}ErrorMessage;
import org.springframework.stereotype.Component;

@Component
public class {{validationName}} {
    
    public void validate{{entityName}}({{entityName}} {{entityVariable}}) {
        {{#invariants}}
        if (!{{condition}}) {
            throw new {{projectName}}Exception({{projectName}}ErrorMessage.{{errorCode}});
        }
        {{/invariants}}
    }

    {{#businessRules}}
    public void validate{{ruleName}}({{entityName}} {{entityVariable}}) {
        // TODO: Implement {{ruleName}} validation logic
    }
    {{/businessRules}}
}`;
    }

    /**
     * Get standard web API template
     */
    static getWebApiTemplate(): string {
        return `package {{packageName}}.microservices.{{aggregateName}}.webapi;

import {{packageName}}.microservices.{{aggregateName}}.aggregate.*;
import {{packageName}}.microservices.{{aggregateName}}.service.{{serviceName}};
import {{packageName}}.microservices.utils.UnitOfWork;
import {{packageName}}.microservices.utils.UnitOfWorkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/{{aggregateName}}")
public class {{controllerName}} {
    @Autowired
    private {{serviceName}} {{serviceVariable}};

    @Autowired
    private UnitOfWorkService unitOfWorkService;

    {{#endpoints}}
    @{{httpMethod}}("{{path}}")
    public ResponseEntity<{{returnType}}> {{methodName}}({{#parameters}}{{annotation}} {{type}} {{name}}{{^last}}, {{/last}}{{/parameters}}) {
        UnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork("{{methodName}}");
        try {
            {{returnType}} result = {{serviceCall}};
            return ResponseEntity.ok(result);
        } finally {
            unitOfWorkService.commit(unitOfWork);
        }
    }
    {{/endpoints}}
}`;
    }

    /**
     * Get standard event template
     */
    static getEventTemplate(): string {
        return `package {{packageName}}.microservices.{{aggregateName}}.events;

import {{packageName}}.microservices.{{aggregateName}}.aggregate.{{entityName}};
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class {{eventName}} {
    private final ApplicationEventPublisher eventPublisher;

    public {{eventName}}(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    {{#events}}
    public void publish{{eventType}}({{entityName}} {{entityVariable}}) {
        {{eventType}} event = new {{eventType}}({{entityVariable}});
        eventPublisher.publishEvent(event);
    }
    {{/events}}
}`;
    }

    /**
     * Get standard saga template
     */
    static getSagaTemplate(): string {
        return `package {{packageName}}.microservices.{{aggregateName}}.saga;

import {{packageName}}.microservices.{{aggregateName}}.aggregate.*;
import {{packageName}}.microservices.utils.UnitOfWork;
import {{packageName}}.microservices.utils.UnitOfWorkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class {{sagaName}} {
    @Autowired
    private UnitOfWorkService unitOfWorkService;

    {{#workflows}}
    public {{returnType}} {{workflowName}}({{#parameters}}{{type}} {{name}}{{^last}}, {{/last}}{{/parameters}}) {
        UnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork("{{workflowName}}");
        try {
            // Saga logic implementation
            {{workflowName}}BusinessLogic({{#each parameters}}{{name}}{{#unless @last}}, {{/unless}}{{/each}});
            return null;
        } catch (Exception e) {
            // Compensation logic implementation
            {{workflowName}}CompensationLogic({{#each parameters}}{{name}}{{#unless @last}}, {{/unless}}{{/each}});
            throw e;
        } finally {
            unitOfWorkService.commit(unitOfWork);
        }
    }
    {{/workflows}}
}`;
    }
}
