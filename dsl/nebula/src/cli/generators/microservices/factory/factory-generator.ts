import { Aggregate } from "../../../../language/generated/ast.js";
import type { DtoSchemaRegistry } from "../../../services/dto-schema-service.js";
import { BaseGenerator, GeneratorCapabilitiesFactory } from "../../common/generator-capabilities.js";
import { ContextBuilderFactory } from "../../common/template-context-builder.js";

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

        const dtoParamName = baseContext.lowerAggregateName || aggregate.name.toLowerCase();

        // SIMPLIFIED: Factory interface just takes (aggregateId, dto)
        // The aggregate constructor handles DTO-to-entity conversion internally
        const createMethodParams = `Integer aggregateId, ${rootEntity.name}Dto ${dtoParamName}Dto`;

        const finalContext = {
            ...baseContext,
            dtoName: `${rootEntity.name}Dto`,
            lowerAggregateName: baseContext.lowerAggregate ?? aggregate.name.toLowerCase(),
            entityRelationships: [], // No longer needed in factory interface
            createMethodParams,
            imports: this.generateFactoryImportsSimplified(`${rootEntity.name}Dto`, options.projectName)
        };

        return this.render('entity/factory-interface.hbs', finalContext);
    }

    async generateFactory(aggregate: Aggregate, options: { projectName: string, dtoSchemaRegistry?: DtoSchemaRegistry }): Promise<string> {
        return this.generateFactoryInterface(aggregate, options);
    }

    /**
     * SIMPLIFIED: Only need DTO import for factory interface
     */
    private generateFactoryImportsSimplified(dtoName: string, projectName: string): string {
        const importBuilder = this.capabilities.importBuilder;
        importBuilder.reset();

        // Add DTO import (DTOs are in shared package)
        const dtoPackage = this.capabilities.packageBuilder.buildSharedPackage(projectName, 'dtos');
        importBuilder.addCustomImport(`${dtoPackage}.${dtoName}`);

        const imports = importBuilder.formatImports();
        return imports.join('\n');
    }
}