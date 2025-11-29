import type { ValidationAcceptor } from "langium";
import type { Entity, Model, Property } from "../generated/ast.js";
import { getEntities } from "../../cli/utils/aggregate-helpers.js";
import { NamingValidator } from "./naming-validator.js";
import type { NebulaServices } from "../nebula-module.js";

export class EntityValidator {
    constructor(
        private readonly namingValidator: NamingValidator,
        private readonly services?: NebulaServices
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
            const hasId = entity.properties.some(p => p.name && p.name.toLowerCase() === 'id');
            if (!hasId) {
                accept("warning", "Root entity should have an 'id' property", {
                    node: entity,
                });
            }

            for (const property of entity.properties) {
                if (property.dtoExclude && property.name && property.name.toLowerCase() === 'id') {
                    accept("error", "Root entity id property cannot be marked with 'dto-exclude'.", {
                        node: property as any,
                        property: "name",
                    });
                }
            }
        }

        const entityAny = entity as any;
        if (entity.generateDto && entityAny.dtoMapping) {
            accept("error", "Entities marked with 'Dto' cannot declare a DTO mapping block.", {
                node: entityAny.dtoMapping,
            });
        }
        if (entityAny.dtoMapping?.fieldMappings) {
            this.validateEntityDtoMapping(entity, entityAny.dtoMapping.fieldMappings, accept);
        }

        if (!entity.isRoot && entity.invariants && entity.invariants.length > 0) {
            accept("error", "Only root entities can have invariants. Non-root entities should not define invariant blocks.", {
                node: entity,
                property: "invariants",
            });
        }
    }

    private collectAllDtoEnabledEntities(model: Model): Map<string, Entity> {
        const collected = new Map<string, Entity>();

        const collectFromModel = (target?: Model) => {
            if (!target) return;
            for (const aggregate of target.aggregates || []) {
                for (const entity of getEntities(aggregate)) {
                    const generateDto = (entity as any)?.generateDto;
                    if ((entity.isRoot || generateDto) && entity.name && !collected.has(entity.name)) {
                        collected.set(entity.name, entity);
                    }
                }
            }
        };

        collectFromModel(model);

        const documentsService: any = (this.services as any)?.shared?.workspace?.LangiumDocuments;
        const documentStream = documentsService?.all;

        const iterateDocuments = (documents: any) => {
            if (!documents) return;
            if (typeof documents.forEach === 'function') {
                documents.forEach((doc: any) => {
                    const root = doc?.parseResult?.value;
                    if (root && root.$type === 'Model') {
                        collectFromModel(root as Model);
                    }
                });
                return;
            }
            if (typeof documents[Symbol.iterator] === 'function') {
                for (const doc of documents as any) {
                    const root = doc?.parseResult?.value;
                    if (root && root.$type === 'Model') {
                        collectFromModel(root as Model);
                    }
                }
            }
        };

        iterateDocuments(documentStream);

        return collected;
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
        const entityFields = entity.properties.map(p => p.name);

        const entityAny = entity as any;
        const dtoType = entityAny.dtoType;
        const model = entity.$container?.$container as Model | undefined;

        let targetDtoFields: Set<string> | undefined;
        if (model && dtoType) {
            const dtoName = dtoType;
            const dtoEnabledEntities = this.collectAllDtoEnabledEntities(model);
            if (dtoName.endsWith('Dto')) {
                const targetEntityName = dtoName.slice(0, -3);
                const targetEntity = dtoEnabledEntities.get(targetEntityName);
                if (!targetEntity) {
                    accept("error", `DTO '${dtoName}' must correspond to entity '${targetEntityName}' that is marked as 'Root' or 'Dto', but none was found.`, {
                        node: entity,
                        property: "dtoType",
                    });
                } else {
                    targetDtoFields = this.getDtoFieldsForEntity(targetEntity);
                }
            } else {
                accept("error", `DTO reference '${dtoName}' must end with 'Dto' to correspond to a generated DTO.`, {
                    node: entity,
                    property: "dtoType",
                });
            }
        }

        for (const mapping of fieldMappings) {
            if (!entityFields.includes(mapping.entityField)) {
                accept("error", `Entity field '${mapping.entityField}' does not exist in entity '${entity.name}'. Available fields: ${entityFields.join(', ')}`, {
                    node: mapping,
                    property: "entityField",
                });
            }

            if (targetDtoFields && !targetDtoFields.has(mapping.dtoField)) {
                accept("error", `DTO field '${mapping.dtoField}' is not available on the target DTO. Available fields: ${Array.from(targetDtoFields).join(', ')}`, {
                    node: mapping,
                    property: "dtoField",
                });
            }
        }
    }
}

