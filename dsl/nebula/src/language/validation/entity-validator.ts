import type { ValidationAcceptor } from "langium";
import type { Entity, Model, Property } from "../generated/ast.js";
import { isEntity } from "../generated/ast.js";
import { NamingValidator } from "./naming-validator.js";

export class EntityValidator {
    constructor(
        private readonly namingValidator: NamingValidator
    ) { }

    checkEntity(entity: Entity, accept: ValidationAcceptor): void {
        this.namingValidator.validateName(entity.name, "entity", entity, accept);

        const propertyNames = new Set<string>();
        for (const property of entity.properties) {
            if (!property.name) {
                accept("error", "Property name cannot be empty", {
                    node: property,
                    property: "name",
                });
                continue;
            }
            if (propertyNames.has(property.name.toLowerCase())) {
                accept("error", `Duplicate property name: ${property.name}`, {
                    node: property,
                    property: "name",
                });
            } else {
                propertyNames.add(property.name.toLowerCase());
            }
        }

        if (entity.isRoot) {
            // Root entities inherit 'id' from Aggregate base class, so they shouldn't define it explicitly
            for (const property of entity.properties) {
                if (property.name && property.name.toLowerCase() === 'id') {
                    accept("error", "Root entities inherit 'id' from Aggregate base class and should not define it explicitly.", {
                        node: property as any,
                        property: "name",
                    });
                }
            }
        }

        const entityAny = entity as any;
        if (entityAny.fieldMappings && entityAny.fieldMappings.length > 0) {
            this.validateEntityDtoMapping(entity, entityAny.fieldMappings, accept);
        }

        if (!entity.isRoot && entity.invariants && entity.invariants.length > 0) {
            accept("error", "Only root entities can have invariants. Non-root entities should not define invariant blocks.", {
                node: entity,
                property: "invariants",
            });
        }
    }


    private getDtoFieldsForEntity(entity: Entity): Set<string> {
        const fields = new Set<string>();
        if (entity.isRoot) {
            fields.add('aggregateId');
            fields.add('version');
            fields.add('state');
        }

        for (const prop of entity.properties || []) {
            if (!prop?.name) continue;
            if (prop.dtoExclude) continue;

            const propertyNames = [prop.name, ...(prop.names || [])];
            for (const propName of propertyNames) {
                if (!propName) continue;
                fields.add(propName);

                if (this.isEntityReferenceProperty(prop)) {
                    fields.add(`${propName}AggregateId`);
                }
            }
        }
        return fields;
    }

    private isEntityReferenceProperty(property: Property): boolean {
        if (!property?.type) {
            return false;
        }
        return (property.type as any).$type === 'EntityType';
    }

    private validateEntityDtoMapping(entity: Entity, fieldMappings: any[], accept: ValidationAcceptor): void {
        // Mappings create entity fields automatically (map dtoField as entityField)
        // We only need to validate that DTO fields exist on the target aggregate

        const entityAny = entity as any;
        const aggregateRef = entityAny.aggregateRef;
        const model = entity.$container?.$container as Model | undefined;

        let targetDtoFields: Set<string> | undefined;
        if (model && aggregateRef) {
            // aggregateRef is the aggregate name (e.g., "Topic"), derive DTO name (e.g., "TopicDto")
            const aggregateName = typeof aggregateRef === 'string' ? aggregateRef : (aggregateRef.ref?.name || aggregateRef.$refText || '');
            if (!aggregateName) {
                return; // Skip validation if aggregate name can't be determined
            }
            // Find the root entity of the referenced aggregate
            // Note: Only check in current model - cross-file aggregates will be validated during code generation
            const targetAggregate = model.aggregates?.find(agg => agg.name === aggregateName);
            if (!targetAggregate) {
                // Aggregate not found in current model - skip validation (may be in another file)
                // Code generation will validate cross-file references
                return;
            }

            // Get entities from aggregateElements (entities are mixed with other aggregate elements)
            const entities = targetAggregate.aggregateElements?.filter(el => isEntity(el)) as Entity[] || [];
            const rootEntity = entities.find((e: Entity) => e.isRoot);

            if (!rootEntity) {
                accept("error", `Aggregate '${aggregateName}' does not have a root entity.`, {
                    node: entity,
                    property: "aggregateRef",
                });
                return;
            }
            targetDtoFields = this.getDtoFieldsForEntity(rootEntity);
        }

        for (const mapping of fieldMappings) {
            // Validate that DTO field exists on the target aggregate's DTO
            if (targetDtoFields && !targetDtoFields.has(mapping.dtoField)) {
                accept("error", `DTO field '${mapping.dtoField}' is not available on the target DTO. Available fields: ${Array.from(targetDtoFields).join(', ')}`, {
                    node: mapping,
                    property: "dtoField",
                });
            }
        }
    }
}

