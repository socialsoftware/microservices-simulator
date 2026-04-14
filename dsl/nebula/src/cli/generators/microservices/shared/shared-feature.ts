import { EnumGenerator, EnumDefinition } from "./enum-generator.js";
import { SagaStateGenerator, SagaStateDefinition } from "./saga-state-generator.js";

export interface SharedGenerationOptions {
    projectName: string;
    outputPath: string;
    models?: any[];
    configPath?: string;
}

export class SharedFeature {
    private enumGenerator = new EnumGenerator();
    private sagaStateGenerator = new SagaStateGenerator();

    async generateSharedComponents(options: SharedGenerationOptions): Promise<{ [key: string]: string }> {
        const results: { [key: string]: string } = {};

        const enums = this.getEnumsToGenerate(options);
        const enumResults = this.enumGenerator.generateAllEnums(enums, options);
        Object.assign(results, enumResults);

        const sagaStates = this.getSagaStatesToGenerate(options);
        const sagaStateResults = this.sagaStateGenerator.generateAll(sagaStates, options);
        Object.assign(results, sagaStateResults);

        return results;
    }

    private getSagaStatesToGenerate(options: SharedGenerationOptions): SagaStateDefinition[] {
        const defs: SagaStateDefinition[] = [];
        if (!options.models) return defs;
        for (const model of options.models) {
            const blocks = (model as any).sagaStatesBlocks || [];
            for (const block of blocks) {
                defs.push({
                    name: block.name,
                    values: [...(block.states || [])]
                });
            }
        }
        return defs;
    }

    private getEnumsToGenerate(options: SharedGenerationOptions): EnumDefinition[] {
        const enums: EnumDefinition[] = [];

        
        if (options.models) {
            for (const model of options.models) {
                if (model.sharedEnums) {
                    for (const sharedEnum of model.sharedEnums) {
                        if (sharedEnum.enums) {
                            for (const enumDef of sharedEnum.enums) {
                                enums.push({
                                    name: enumDef.name,
                                    values: enumDef.values.map((v: any) => v.name)
                                });
                            }
                        }
                    }
                }
            }
        }

        return enums;
    }

    static async generateSharedComponentsStatic(options: SharedGenerationOptions): Promise<{ [key: string]: string }> {
        const feature = new SharedFeature();
        return feature.generateSharedComponents(options);
    }

    isEnabled(): boolean {
        return true; 
    }
}
