import { getGlobalConfig } from "../../common/config.js";

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

    
    static extractEnumsFromEntities(entities: any[]): EnumDefinition[] {
        const enums: EnumDefinition[] = [];

        
        

        
        

        return enums;
    }

    
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
