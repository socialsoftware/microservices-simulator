import { Aggregate } from "../../../../language/generated/ast.js";
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

    generateFactoryInterface(aggregate: Aggregate, options: { projectName: string, allSharedDtos?: any[] }): string {
        this.validateAggregate(aggregate);
        const rootEntity = this.findRootEntity(aggregate);

        const baseContext: any = ContextBuilderFactory
            .forEntity(options.projectName, aggregate)
            .build();

        const finalContext = {
            ...baseContext,
            dtoName: `${rootEntity.name}Dto`,
            lowerAggregateName: baseContext.lowerAggregate ?? aggregate.name.toLowerCase(),
            imports: this.generateFactoryImports(`${rootEntity.name}Dto`, options.projectName, options.allSharedDtos)
        };

        return this.render('entity/factory-interface.hbs', finalContext);
    }

    async generateFactory(aggregate: Aggregate, options: { projectName: string, allSharedDtos?: any[] }): Promise<string> {
        return this.generateFactoryInterface(aggregate, options);
    }


    private generateFactoryImports(dtoName: string, projectName: string, allSharedDtos?: any[]): string {
        // Use capabilities for import building
        const importBuilder = this.capabilities.importBuilder;
        importBuilder.reset();

        // Check if this is a shared DTO using dynamic detection
        if (this.isSharedDto(dtoName, allSharedDtos)) {
            const sharedPackage = this.capabilities.packageBuilder.buildSharedPackage(projectName, 'dtos');
            importBuilder.addCustomImport(`${sharedPackage}.${dtoName}`);
        }

        const imports = importBuilder.formatImports();
        return imports.join('\n');
    }

    private isSharedDto(dtoName: string, allSharedDtos?: any[]): boolean {
        // Dynamic detection: check if DTO exists in allSharedDtos
        if (allSharedDtos) {
            return allSharedDtos.some((dto: any) => dto.name === dtoName);
        }
        return false;
    }
}