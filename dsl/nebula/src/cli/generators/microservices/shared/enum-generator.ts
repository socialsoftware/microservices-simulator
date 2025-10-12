import { getGlobalConfig } from "../../shared/config.js";

export interface EnumDefinition {
    name: string;
    values: string[];
    packagePath?: string;
}

export interface EnumGenerationOptions {
    projectName: string;
    outputPath: string;
}

export class EnumGenerator {

    generateEnumCode(enumDef: EnumDefinition, options: EnumGenerationOptions): string {
        const config = getGlobalConfig();
        const packageName = enumDef.packagePath ||
            config.buildPackageName(options.projectName, 'shared', 'enums');

        const enumValues = enumDef.values.map(value => `    ${value}`).join(',\n');

        return `package ${packageName};

public enum ${enumDef.name} {
${enumValues}
}`;
    }

    generateAllEnums(enums: EnumDefinition[], options: EnumGenerationOptions): { [key: string]: string } {
        const results: { [key: string]: string } = {};

        for (const enumDef of enums) {
            const fileName = `${enumDef.name}.java`;
            const filePath = `shared/enums/${fileName}`;
            results[filePath] = this.generateEnumCode(enumDef, options);
        }

        return results;
    }

    // Helper method to extract enums from entity properties
    static extractEnumsFromEntities(entities: any[]): EnumDefinition[] {
        const enums: EnumDefinition[] = [];

        // This is a placeholder - in a real implementation, we would parse
        // enum definitions from the DSL or detect enum-like string properties

        // Example: if we find properties with specific naming patterns or annotations
        // we could automatically generate enums

        return enums;
    }

    // Method to create common enums that might be needed
    static createCommonEnums(): EnumDefinition[] {
        return [
            {
                name: 'CourseType',
                values: ['TECNICO', 'EXTERNAL']
            },
            {
                name: 'UserRole',
                values: ['STUDENT', 'TEACHER', 'ADMIN']
            },
            {
                name: 'QuestionType',
                values: ['MULTIPLE_CHOICE', 'TRUE_FALSE', 'OPEN_ENDED']
            }
        ];
    }
}
