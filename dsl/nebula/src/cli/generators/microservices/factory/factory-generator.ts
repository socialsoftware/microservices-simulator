import { Aggregate } from "../../../../language/generated/ast.js";
import type { DtoSchemaRegistry } from "../../../services/dto-schema-service.js";
import { BaseGenerator, GeneratorCapabilitiesFactory } from "../../common/generator-capabilities.js";
import { ContextBuilderFactory } from "../../common/template-context-builder.js";
import { TypeResolver } from "../../common/resolvers/type-resolver.js";

export function generateFactoryCode(aggregate: Aggregate, projectName: string): string {
    const generator = new FactoryGenerator();
    return generator.generateFactoryInterface(aggregate, { projectName });
}


export class FactoryGenerator extends BaseGenerator {
    constructor() {
        super(GeneratorCapabilitiesFactory.createEntityCapabilities());
    }

    generateFactoryInterface(aggregate: Aggregate, options: { projectName: string, dtoSchemaRegistry?: DtoSchemaRegistry }): string {
        this.validateAggregate(aggregate);
        const rootEntity = this.findRootEntity(aggregate);

        const baseContext: any = ContextBuilderFactory
            .forEntity(options.projectName, aggregate)
            .build();

        // Find entity relationships (complex fields that are other entities)
        const entityRelationships = this.findEntityRelationships(rootEntity, aggregate);

        // Separate single entities and collections
        const singleEntityRels = entityRelationships.filter(rel => !rel.isCollection);
        const collectionEntityRels = entityRelationships.filter(rel => rel.isCollection);

        // Build parameter strings
        const singleEntityParams = singleEntityRels.map(rel => `${rel.entityType} ${rel.paramName}`).join(', ');
        const collectionEntityParams = collectionEntityRels.map(rel => `${rel.javaType} ${rel.paramName}`).join(', ');

        const dtoParamName = baseContext.lowerAggregateName || aggregate.name.toLowerCase();

        // Build create method params: aggregateId, single entities, DTO, collections
        const params: string[] = ['Integer aggregateId'];
        if (singleEntityParams) {
            params.push(singleEntityParams);
        }
        // Use regular DTO for factory interface
        params.push(`${rootEntity.name}Dto ${dtoParamName}Dto`);
        if (collectionEntityParams) {
            params.push(collectionEntityParams);
        }

        const createMethodParams = params.join(', ');

        const finalContext = {
            ...baseContext,
            dtoName: `${rootEntity.name}Dto`,
            lowerAggregateName: baseContext.lowerAggregate ?? aggregate.name.toLowerCase(),
            entityRelationships,
            createMethodParams,
            imports: this.generateFactoryImports(`${rootEntity.name}Dto`, options.projectName, aggregate.name, entityRelationships)
        };

        return this.render('entity/factory-interface.hbs', finalContext);
    }

    async generateFactory(aggregate: Aggregate, options: { projectName: string, dtoSchemaRegistry?: DtoSchemaRegistry }): Promise<string> {
        return this.generateFactoryInterface(aggregate, options);
    }

    /**
     * Find entity relationships (both single and collection entity fields) from root entity properties
     */
    private findEntityRelationships(rootEntity: any, aggregate: Aggregate): Array<{ entityType: string; paramName: string; javaType: string; isCollection: boolean }> {
        const relationships: Array<{ entityType: string; paramName: string; javaType: string; isCollection: boolean }> = [];

        if (!rootEntity.properties) {
            return relationships;
        }

        for (const prop of rootEntity.properties) {
            const javaType = TypeResolver.resolveJavaType(prop.type);
            const isCollection = javaType.startsWith('Set<') || javaType.startsWith('List<');

            // Check if this is an entity type (not enum)
            const isEntityType = !this.isEnumType(prop.type) && TypeResolver.isEntityType(javaType);

            if (isEntityType) {
                // Resolve entity type
                const entityRef = (prop.type as any).type?.ref;
                let entityName: string;

                if (isCollection) {
                    // For collections, extract element type
                    const elementType = TypeResolver.getElementType(prop.type);
                    entityName = elementType || javaType.replace(/^(Set|List)<(.+)>$/, '$2');
                } else {
                    entityName = entityRef?.name || javaType;
                }

                // Only include if it's an entity within this aggregate
                const relatedEntity = aggregate.entities?.find((e: any) => e.name === entityName);
                const isEntityInAggregate = !!relatedEntity;

                // Include all entity relationships in the factory signature
                // Note: generateDto flag just means "generate a DTO class", not "exclude from factory"
                if (isEntityInAggregate) {
                    const paramName = prop.name;
                    relationships.push({
                        entityType: entityName,
                        paramName,
                        javaType: isCollection ? javaType : entityName,
                        isCollection
                    });
                }
            }
        }

        return relationships;
    }

    /**
     * Check if a type is an enum
     */
    private isEnumType(type: any): boolean {
        if (type && typeof type === 'object' &&
            type.$type === 'EntityType' &&
            type.type) {
            if (type.type.$refText && type.type.$refText.match(/^[A-Z][a-zA-Z]*Type$/)) {
                return true;
            }
            if (type.type.ref && type.type.ref.$type === 'EnumDefinition') {
                return true;
            }
        }
        return false;
    }

    private generateFactoryImports(
        dtoName: string,
        projectName: string,
        owningAggregate: string,
        entityRelationships: Array<{ entityType: string; paramName: string; javaType: string; isCollection: boolean }>
    ): string {
        const importBuilder = this.capabilities.importBuilder;
        importBuilder.reset();

        // Add DTO import (DTOs are in shared package, so always need import)
        const dtoPackage = this.capabilities.packageBuilder.buildSharedPackage(projectName, 'dtos');
        importBuilder.addCustomImport(`${dtoPackage}.${dtoName}`);

        // Entity relationships are in the same package as the factory, so no imports needed
        // (All entities in the same aggregate are in the same package)

        // Add List/Set imports if collections are used
        const hasCollections = entityRelationships.some(rel => rel.isCollection);
        if (hasCollections) {
            const hasList = entityRelationships.some(rel => rel.isCollection && rel.javaType.startsWith('List<'));
            const hasSet = entityRelationships.some(rel => rel.isCollection && rel.javaType.startsWith('Set<'));

            if (hasList) {
                importBuilder.addCustomImport('import java.util.List;');
            }
            if (hasSet) {
                importBuilder.addCustomImport('import java.util.Set;');
            }
        }

        const imports = importBuilder.formatImports();
        return imports.join('\n');
    }
}