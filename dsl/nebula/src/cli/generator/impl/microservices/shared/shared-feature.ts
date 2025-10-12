import { EnumGenerator, EnumDefinition } from "./enum-generator.js";

export interface SharedGenerationOptions {
    projectName: string;
    outputPath: string;
    features?: string[];
    models?: any[];
    configPath?: string;
}

export class SharedFeature {
    private enumGenerator = new EnumGenerator();

    async generateSharedComponents(options: SharedGenerationOptions): Promise<{ [key: string]: string }> {
        const results: { [key: string]: string } = {};

        // Generate enums
        const enums = this.getEnumsToGenerate(options);
        const enumResults = this.enumGenerator.generateAllEnums(enums, options);
        Object.assign(results, enumResults);

        return results;
    }

    private getEnumsToGenerate(options: SharedGenerationOptions): EnumDefinition[] {
        const enums: EnumDefinition[] = [];

        // Extract enums from DSL models (from SharedEnums)
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

        // Fallback to common enums if no DSL enums found
        if (enums.length === 0) {
            return EnumGenerator.createCommonEnums();
        }

        return enums;
    }

    static async generateSharedComponentsStatic(options: SharedGenerationOptions): Promise<{ [key: string]: string }> {
        const feature = new SharedFeature();
        return feature.generateSharedComponents(options);
    }

    isEnabled(features: string[]): boolean {
        return features.includes('shared') || features.includes('enums');
    }
}
