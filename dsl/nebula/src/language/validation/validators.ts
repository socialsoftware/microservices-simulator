import type { ValidationAcceptor } from "langium";
import type { Model, Aggregate, Entity, Property, Method, Invariant, RepositoryMethod, SubscribedEvent } from "../generated/ast.js";
import type { NebulaServices } from "../nebula-module.js";
import { NamingValidator } from "./naming-validator.js";
import { ModelValidator } from "./model-validator.js";
import { EntityValidator } from "./entity-validator.js";
import { PropertyValidator } from "./property-validator.js";
import { MethodValidator } from "./method-validator.js";
import { InvariantValidator } from "./invariant-validator.js";
import { RepositoryValidator } from "./repository-validator.js";
import { EventValidator } from "./validation/event-validator.js";

export class NebulaValidator {
    private readonly namingValidator: NamingValidator;
    private readonly modelValidator: ModelValidator;
    private readonly entityValidator: EntityValidator;
    private readonly propertyValidator: PropertyValidator;
    private readonly methodValidator: MethodValidator;
    private readonly invariantValidator: InvariantValidator;
    private readonly repositoryValidator: RepositoryValidator;
    private readonly eventValidator: EventValidator;

    constructor(private readonly services?: NebulaServices) {
        this.namingValidator = new NamingValidator();
        this.modelValidator = new ModelValidator(this.namingValidator);
        this.entityValidator = new EntityValidator(this.namingValidator, this.services);
        this.propertyValidator = new PropertyValidator(this.namingValidator);
        this.methodValidator = new MethodValidator(this.namingValidator);
        this.invariantValidator = new InvariantValidator(this.namingValidator);
        this.repositoryValidator = new RepositoryValidator();
        this.eventValidator = new EventValidator();
    }

    checkModel(model: Model, accept: ValidationAcceptor): void {
        this.modelValidator.checkModel(model, accept);
    }

    checkAggregate(aggregate: Aggregate, accept: ValidationAcceptor): void {
        this.modelValidator.checkAggregate(aggregate, accept);
    }

    checkEntity(entity: Entity, accept: ValidationAcceptor): void {
        this.entityValidator.checkEntity(entity, accept);
    }

    checkProperty(property: Property, accept: ValidationAcceptor): void {
        this.propertyValidator.checkProperty(property, accept);
    }

    checkMethod(method: Method, accept: ValidationAcceptor): void {
        this.methodValidator.checkMethod(method, accept);
    }

    checkInvariant(invariant: Invariant, accept: ValidationAcceptor): void {
        this.invariantValidator.checkInvariant(invariant, accept);
    }

    checkRepositoryMethod(method: RepositoryMethod, accept: ValidationAcceptor): void {
        this.repositoryValidator.checkRepositoryMethod(method, accept);
    }

    checkSubscribedEvent(event: SubscribedEvent, accept: ValidationAcceptor): void {
        this.eventValidator.checkSubscribedEvent(event, accept);
    }
}