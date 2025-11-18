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

        const finalContext = {
            ...baseContext,
            dtoName: `${rootEntity.name}Dto`,
            lowerAggregateName: baseContext.lowerAggregate ?? aggregate.name.toLowerCase(),
            imports: this.generateFactoryImports(`${rootEntity.name}Dto`, options.projectName, options.dtoSchemaRegistry, aggregate.name)
        };

        return this.render('entity/factory-interface.hbs', finalContext);
    }

    async generateFactory(aggregate: Aggregate, options: { projectName: string, dtoSchemaRegistry?: DtoSchemaRegistry }): Promise<string> {
        return this.generateFactoryInterface(aggregate, options);
    }


    private generateFactoryImports(dtoName: string, projectName: string, _dtoSchemaRegistry: DtoSchemaRegistry | undefined, _owningAggregate: string): string {
        const importBuilder = this.capabilities.importBuilder;
        importBuilder.reset();

        const dtoPackage = this.capabilities.packageBuilder.buildSharedPackage(projectName, 'dtos');
        importBuilder.addCustomImport(`${dtoPackage}.${dtoName}`);

        const imports = importBuilder.formatImports();
        return imports.join('\n');
    }
}