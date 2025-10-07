import { Aggregate } from "../../../../language/generated/ast.js";
import { OrchestrationBase } from "../../base/orchestration-base.js";

export function generateFactoryCode(aggregate: Aggregate, projectName: string): string {
    const generator = new FactoryGenerator();
    return generator.generateFactoryInterface(aggregate, { projectName });
}


export class FactoryGenerator extends OrchestrationBase {
    generateFactoryInterface(aggregate: Aggregate, options: { projectName: string, allSharedDtos?: any[] }): string {
        const rootEntity = aggregate.entities.find((e: any) => e.isRoot);
        if (!rootEntity) {
            throw new Error(`No root entity found in aggregate ${aggregate.name}`);
        }

        const context = this.buildFactoryContext(aggregate, rootEntity, options);
        const template = this.loadTemplate('entity/factory-interface.hbs');
        return this.renderTemplate(template, context);
    }

    async generateFactory(aggregate: Aggregate, options: { projectName: string, allSharedDtos?: any[] }): Promise<string> {
        return this.generateFactoryInterface(aggregate, options);
    }

    private buildFactoryContext(aggregate: Aggregate, rootEntity: any, options: { projectName: string, allSharedDtos?: any[] }): any {
        const aggregateName = aggregate.name;
        const capitalizedAggregate = this.capitalize(aggregateName);
        const lowerAggregateName = aggregateName.toLowerCase();
        const dtoName = `${rootEntity.name}Dto`;

        // Check if DTO is shared and add import if needed
        const imports = this.generateFactoryImports(dtoName, options.projectName, options.allSharedDtos);

        return {
            packageName: `${this.getBasePackage()}.${options.projectName.toLowerCase()}.microservices.${lowerAggregateName}.aggregate`,
            aggregateName: capitalizedAggregate,
            lowerAggregateName,
            dtoName,
            imports
        };
    }

    private generateFactoryImports(dtoName: string, projectName: string, allSharedDtos?: any[]): string {
        // Check if this is a shared DTO using dynamic detection
        if (this.isSharedDto(dtoName, allSharedDtos)) {
            return `import ${this.getBasePackage()}.${projectName.toLowerCase()}.shared.dtos.${dtoName};`;
        }
        return '';
    }

    private isSharedDto(dtoName: string, allSharedDtos?: any[]): boolean {
        // Dynamic detection: check if DTO exists in allSharedDtos
        if (allSharedDtos) {
            return allSharedDtos.some((dto: any) => dto.name === dtoName);
        }
        return false;
    }
}