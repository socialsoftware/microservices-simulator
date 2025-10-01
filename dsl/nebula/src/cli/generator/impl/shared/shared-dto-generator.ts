import { OrchestrationBase } from "../../base/orchestration-base.js";
import { getGlobalConfig } from "../../base/config.js";
import { DtoDefinition, DtoField } from "../../../../language/generated/ast.js";

export interface SharedDtoGenerationOptions {
    projectName: string;
    architecture?: string;
    features?: string[];
}

export class SharedDtoGenerator extends OrchestrationBase {

    /**
     * Generates a shared DTO from DSL definition
     */
    async generateSharedDtoFromDefinition(
        dtoDefinition: DtoDefinition,
        options: SharedDtoGenerationOptions
    ): Promise<string> {
        const config = getGlobalConfig();
        const packageName = config.buildPackageName(options.projectName, 'shared', 'dtos');

        const context = {
            packageName,
            dtoName: dtoDefinition.name,
            isGeneric: !!dtoDefinition.genericParams,
            genericParams: dtoDefinition.genericParams?.params || [],
            fields: dtoDefinition.fields.map((field: DtoField) => ({
                type: this.resolveDtoFieldType(field),
                name: field.name,
                capitalizedName: this.capitalize(field.name)
            }))
        };

        return this.generateSharedDtoCode(context);
    }

    /**
     * Legacy method for backward compatibility
     */
    async generateSharedDto(
        dtoName: string,
        fields: any[],
        options: SharedDtoGenerationOptions
    ): Promise<string> {
        const config = getGlobalConfig();
        const packageName = config.buildPackageName(options.projectName, 'shared', 'dtos');

        const context = {
            packageName,
            dtoName,
            isGeneric: false,
            genericParams: [],
            fields: fields.map(field => ({
                type: this.resolveJavaType(field.type),
                name: field.name,
                capitalizedName: this.capitalize(field.name)
            }))
        };

        return this.generateSharedDtoCode(context);
    }

    private resolveDtoFieldType(field: DtoField): string {
        const fieldType = field.type;

        if (!fieldType) return 'Object';

        // Handle different field types
        if (fieldType.$type === 'PrimitiveType') {
            return this.resolveJavaType(fieldType);
        }

        if (fieldType.$type === 'DtoReference') {
            return fieldType.dto?.ref?.name || fieldType.dto?.$refText || 'UnknownDto';
        }

        if (fieldType.$type === 'ListType' && fieldType.elementType) {
            const elementType = this.extractElementTypeName(fieldType.elementType);
            return `List<${elementType}>`;
        }

        if (fieldType.$type === 'SetType' && fieldType.elementType) {
            const elementType = this.extractElementTypeName(fieldType.elementType);
            return `Set<${elementType}>`;
        }

        if (fieldType.$type === 'GenericType') {
            return fieldType.name;
        }

        return this.resolveJavaType(fieldType);
    }

    private extractElementTypeName(elementType: any): string {
        if (typeof elementType === 'string') {
            return elementType;
        }
        if (elementType && typeof elementType === 'object') {
            if (elementType.$type === 'PrimitiveType' && elementType.name) {
                return elementType.name;
            }
            if (elementType.$type === 'DtoReference') {
                return elementType.dto?.ref?.name || elementType.dto?.$refText || 'UnknownDto';
            }
            if (elementType.$refText) {
                return elementType.$refText;
            }
            if (elementType.ref && elementType.ref.name) {
                return elementType.ref.name;
            }
            if (elementType.name) {
                return elementType.name;
            }
        }
        return 'Object';
    }

    private generateSharedDtoCode(context: any): string {
        const { packageName, dtoName, fields, isGeneric, genericParams } = context;

        const genericDeclaration = isGeneric ? `<${genericParams.join(', ')}>` : '';

        return `package ${packageName};

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public class ${dtoName}${genericDeclaration} implements Serializable {
    
${fields.map((field: any) => `    private ${field.type} ${field.name};`).join('\n')}
    
    public ${dtoName}() {
        // Default constructor
    }
    
${fields.map((field: any) => `    public ${field.type} get${field.capitalizedName}() {
        return ${field.name};
    }
    
    public void set${field.capitalizedName}(${field.type} ${field.name}) {
        this.${field.name} = ${field.name};
    }`).join('\n\n')}
}`;
    }

    /**
     * Determines if a DTO should be in the shared module
     * Common DTOs like UserDto, CourseDto that are referenced across aggregates
     */
    static isSharedDto(dtoName: string): boolean {
        const sharedDtos = [
            'UserDto',
            'CourseDto',
            'CourseExecutionDto',
            'QuestionDto',
            'QuizDto',
            'TopicDto',
            'TournamentDto',
            'QuizAnswerDto'
        ];

        return sharedDtos.includes(dtoName);
    }

    /**
     * Gets the import path for a DTO (either shared or aggregate-specific)
     */
    static getDtoImportPath(dtoName: string, options: SharedDtoGenerationOptions): string {
        const config = getGlobalConfig();

        if (this.isSharedDto(dtoName)) {
            return `${config.buildPackageName(options.projectName, 'shared', 'dtos')}.${dtoName}`;
        }

        // For aggregate-specific DTOs, determine the aggregate from the DTO name
        const aggregateName = this.getAggregateFromDtoName(dtoName);
        return `${config.buildPackageName(options.projectName, 'microservices', aggregateName.toLowerCase(), 'aggregate')}.${dtoName}`;
    }

    private static getAggregateFromDtoName(dtoName: string): string {
        // Remove 'Dto' suffix and return the base name
        // This is a simplified approach - in practice, you might need a mapping
        const baseName = dtoName.replace('Dto', '');

        // Map common patterns
        const aggregateMap: { [key: string]: string } = {
            'CourseExecution': 'CourseExecution',
            'User': 'User',
            'Course': 'Course',
            'Question': 'Question',
            'Quiz': 'Quiz',
            'Topic': 'Topic',
            'Tournament': 'Tournament',
            'QuizAnswer': 'QuizAnswer'
        };

        return aggregateMap[baseName] || baseName;
    }
}
