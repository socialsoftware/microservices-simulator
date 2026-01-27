/**
 * Unified DTO Generation System
 * 
 * This module consolidates all DTO generation logic from WebApiDtoGenerator,
 * DtoGenerator, and SharedDtoGenerator into a single, comprehensive system
 * that handles all DTO generation scenarios.
 */

import { Entity, Property } from "../../../language/generated/ast.js";
import { UnifiedTypeResolver } from "./unified-type-resolver.js";
import { ImportManager, ImportManagerFactory } from "../../utils/import-manager.js";
import { ErrorHandler, ErrorUtils, ErrorSeverity } from "../../utils/error-handler.js";

/**
 * DTO generation types
 */
export enum DtoType {
    ENTITY_DTO = 'entity',           // Standard entity DTO
    REQUEST_DTO = 'request',         // WebAPI request DTO
    RESPONSE_DTO = 'response',       // WebAPI response DTO
    CREATE_REQUEST = 'create-request', // Create operation DTO
    UPDATE_REQUEST = 'update-request', // Update operation DTO
    SHARED_DTO = 'shared'            // Shared DTO across microservices
}

/**
 * DTO generation options
 */
export interface DtoGenerationOptions {
    projectName: string;
    dtoType: DtoType;
    includeAggregateFields?: boolean;
    includeValidationAnnotations?: boolean;
    includeSerializable?: boolean;
    excludeFields?: string[];
    customFields?: DtoField[];
    packageOverride?: string;
}

/**
 * DTO field definition
 */
export interface DtoField {
    name: string;
    type: string;
    annotations?: string[];
    getter?: string;
    setter?: string;
    isRequired?: boolean;
}

/**
 * Generated DTO structure
 */
export interface GeneratedDto {
    className: string;
    packageName: string;
    imports: string[];
    fields: DtoField[];
    constructors: string[];
    methods: string[];
    annotations: string[];
}

/**
 * Unified DTO generator that handles all DTO generation scenarios
 */
export class UnifiedDtoGenerator {
    private importManager: ImportManager;

    constructor(projectName: string) {
        this.importManager = ImportManagerFactory.createForMicroservice(projectName);
    }

    /**
     * Generate DTO for an entity
     */
    generateDto(entity: Entity, options: DtoGenerationOptions): GeneratedDto {
        return ErrorHandler.wrap(
            () => this.generateDtoInternal(entity, options),
            ErrorUtils.entityContext(
                'generate DTO',
                entity.$container?.name || 'unknown',
                entity.name,
                'unified-dto-generator',
                { dtoType: options.dtoType }
            ),
            ErrorSeverity.FATAL
        ) || this.createEmptyDto(entity.name, options);
    }

    /**
     * Generate multiple DTOs (request/response pairs)
     */
    generateDtoPair(entity: Entity, baseOptions: Omit<DtoGenerationOptions, 'dtoType'>): {
        request: GeneratedDto;
        response: GeneratedDto;
    } {
        const requestOptions: DtoGenerationOptions = {
            ...baseOptions,
            dtoType: DtoType.REQUEST_DTO,
            excludeFields: ['id'] // Typically exclude ID from request DTOs
        };

        const responseOptions: DtoGenerationOptions = {
            ...baseOptions,
            dtoType: DtoType.RESPONSE_DTO,
            includeAggregateFields: true
        };

        return {
            request: this.generateDto(entity, requestOptions),
            response: this.generateDto(entity, responseOptions)
        };
    }

    /**
     * Generate CRUD DTOs (create, update, response)
     */
    generateCrudDtos(entity: Entity, baseOptions: Omit<DtoGenerationOptions, 'dtoType'>): {
        create: GeneratedDto;
        update: GeneratedDto;
        response: GeneratedDto;
    } {
        return {
            create: this.generateDto(entity, { ...baseOptions, dtoType: DtoType.CREATE_REQUEST, excludeFields: ['id'] }),
            update: this.generateDto(entity, { ...baseOptions, dtoType: DtoType.UPDATE_REQUEST }),
            response: this.generateDto(entity, { ...baseOptions, dtoType: DtoType.RESPONSE_DTO, includeAggregateFields: true })
        };
    }

    /**
     * Internal DTO generation logic
     */
    private generateDtoInternal(entity: Entity, options: DtoGenerationOptions): GeneratedDto {
        // Reset import manager for this DTO
        this.importManager.clear();

        // Generate DTO components
        const className = this.buildDtoClassName(entity, options);
        const packageName = this.buildDtoPackageName(entity, options);
        const fields = this.buildDtoFields(entity, options);
        const constructors = this.buildDtoConstructors(entity, className, fields, options);
        const methods = this.buildDtoMethods(fields, options);
        const annotations = this.buildDtoAnnotations(options);

        // Add required imports based on fields and options
        this.addRequiredImports(fields, options);

        return {
            className,
            packageName,
            imports: this.importManager.formatImports(),
            fields,
            constructors,
            methods,
            annotations
        };
    }

    /**
     * Build DTO class name based on type and entity
     */
    private buildDtoClassName(entity: Entity, options: DtoGenerationOptions): string {
        const entityName = entity.name;

        switch (options.dtoType) {
            case DtoType.CREATE_REQUEST:
                return `Create${entityName}RequestDto`;
            case DtoType.UPDATE_REQUEST:
                return `Update${entityName}RequestDto`;
            case DtoType.REQUEST_DTO:
                return `${entityName}RequestDto`;
            case DtoType.RESPONSE_DTO:
                return `${entityName}ResponseDto`;
            case DtoType.SHARED_DTO:
                return `${entityName}Dto`;
            case DtoType.ENTITY_DTO:
            default:
                return `${entityName}Dto`;
        }
    }

    /**
     * Build DTO package name
     */
    private buildDtoPackageName(entity: Entity, options: DtoGenerationOptions): string {
        if (options.packageOverride) {
            return options.packageOverride;
        }

        const aggregateName = entity.$container?.name || entity.name;

        switch (options.dtoType) {
            case DtoType.REQUEST_DTO:
            case DtoType.RESPONSE_DTO:
            case DtoType.CREATE_REQUEST:
            case DtoType.UPDATE_REQUEST:
                // WebAPI request DTOs go in coordination requestDtos package
                return `pt.ulisboa.tecnico.socialsoftware.ms.${options.projectName.toLowerCase()}.coordination.webapi.requestDtos`;
            case DtoType.SHARED_DTO:
                // Shared DTOs go in shared package
                return `pt.ulisboa.tecnico.socialsoftware.ms.${options.projectName.toLowerCase()}.shared.dtos`;
            case DtoType.ENTITY_DTO:
            default:
                // Entity DTOs go in microservice aggregate package
                return `pt.ulisboa.tecnico.socialsoftware.ms.${options.projectName.toLowerCase()}.microservices.${aggregateName.toLowerCase()}.aggregate`;
        }
    }

    /**
     * Build DTO fields from entity properties
     */
    private buildDtoFields(entity: Entity, options: DtoGenerationOptions): DtoField[] {
        const fields: DtoField[] = [];

        // Add aggregate fields for root entities (if requested)
        if (entity.isRoot && options.includeAggregateFields) {
            fields.push({
                name: 'aggregateId',
                type: 'Integer',
                annotations: [],
                getter: 'getAggregateId',
                setter: 'setAggregateId'
            });
        }

        // Add entity properties
        if (entity.properties) {
            for (const property of entity.properties) {
                // Skip excluded fields
                if (options.excludeFields?.includes(property.name)) {
                    continue;
                }

                const field = this.buildDtoFieldFromProperty(property, options);
                fields.push(field);
            }
        }

        // Add aggregate metadata for root entities
        if (entity.isRoot && options.includeAggregateFields) {
            fields.push(
                {
                    name: 'version',
                    type: 'Integer',
                    annotations: [],
                    getter: 'getVersion',
                    setter: 'setVersion'
                },
                {
                    name: 'state',
                    type: 'AggregateState',
                    annotations: [],
                    getter: 'getState',
                    setter: 'setState'
                }
            );
        }

        // Add custom fields
        if (options.customFields) {
            fields.push(...options.customFields);
        }

        return fields;
    }

    /**
     * Build DTO field from entity property
     */
    private buildDtoFieldFromProperty(property: Property, options: DtoGenerationOptions): DtoField {
        const propertyName = property.name;
        const javaType = UnifiedTypeResolver.resolveForDto(property.type);
        const capitalizedName = this.capitalize(propertyName);

        const annotations: string[] = [];

        // Add validation annotations if requested
        if (options.includeValidationAnnotations) {
            if (javaType === 'String') {
                annotations.push('@NotBlank');
            } else if (!javaType.includes('Optional')) {
                annotations.push('@NotNull');
            }
        }

        // Handle boolean getters
        const getter = (javaType === 'Boolean' || javaType === 'boolean')
            ? `is${capitalizedName}`
            : `get${capitalizedName}`;

        return {
            name: propertyName,
            type: javaType,
            annotations,
            getter,
            setter: `set${capitalizedName}`,
            isRequired: !javaType.includes('Optional')
        };
    }

    /**
     * Build DTO constructors
     */
    private buildDtoConstructors(entity: Entity, className: string, fields: DtoField[], options: DtoGenerationOptions): string[] {
        const constructors: string[] = [];

        // Default constructor
        constructors.push(`public ${className}() {}`);

        // Entity constructor (for entity DTOs)
        if (options.dtoType === DtoType.ENTITY_DTO || options.dtoType === DtoType.SHARED_DTO) {
            constructors.push(this.buildEntityConstructor(entity, className, fields));
        }

        return constructors;
    }

    /**
     * Build constructor that takes entity as parameter
     */
    private buildEntityConstructor(entity: Entity, className: string, fields: DtoField[]): string {
        const entityName = entity.name;
        const entityVar = entityName.toLowerCase();

        const lines: string[] = [];
        lines.push(`public ${className}(${entityName} ${entityVar}) {`);

        for (const field of fields) {
            if (field.name === 'aggregateId') {
                lines.push(`\tthis.aggregateId = ${entityVar}.getAggregateId();`);
            } else if (field.name === 'version') {
                lines.push(`\tthis.version = ${entityVar}.getVersion();`);
            } else if (field.name === 'state') {
                lines.push(`\tthis.state = ${entityVar}.getState();`);
            } else {
                // Handle enum fields (convert to string)
                if (field.name.endsWith('Type')) {
                    lines.push(`\tthis.${field.name} = ${entityVar}.${field.getter}() != null ? ${entityVar}.${field.getter}().toString() : null;`);
                } else {
                    lines.push(`\tthis.${field.name} = ${entityVar}.${field.getter}();`);
                }
            }
        }

        lines.push('}');
        return lines.join('\n');
    }

    /**
     * Build DTO getter/setter methods
     */
    private buildDtoMethods(fields: DtoField[], options: DtoGenerationOptions): string[] {
        const methods: string[] = [];

        for (const field of fields) {
            // Getter method
            const getterAnnotations = field.annotations?.length ? field.annotations.join('\n\t') + '\n\t' : '';
            methods.push(`${getterAnnotations}public ${field.type} ${field.getter}() {
\t\treturn ${field.name};
\t}`);

            // Setter method
            methods.push(`public void ${field.setter}(${field.type} ${field.name}) {
\t\tthis.${field.name} = ${field.name};
\t}`);
        }

        return methods;
    }

    /**
     * Build DTO class annotations
     */
    private buildDtoAnnotations(options: DtoGenerationOptions): string[] {
        const annotations: string[] = [];

        if (options.includeValidationAnnotations) {
            // Add validation annotations at class level if needed
        }

        return annotations;
    }

    /**
     * Add required imports based on fields and options
     */
    private addRequiredImports(fields: DtoField[], options: DtoGenerationOptions): void {
        // Always add Serializable for DTOs
        if (options.includeSerializable !== false) {
            this.importManager.addJavaImport('io.Serializable');
        }

        // Add validation imports if needed
        if (options.includeValidationAnnotations) {
            const hasValidation = fields.some(f => f.annotations?.some(a => a.startsWith('@')));
            if (hasValidation) {
                this.importManager.addJakartaImport('validation.constraints.*');
            }
        }

        // Add imports based on field types
        fields.forEach(field => {
            this.addImportForType(field.type);
        });

        // Add aggregate state import if needed
        const hasAggregateState = fields.some(f => f.type === 'AggregateState');
        if (hasAggregateState) {
            this.importManager.addBaseFrameworkImport('ms.domain.aggregate.Aggregate.AggregateState');
        }
    }

    /**
     * Add import for a specific type
     */
    private addImportForType(type: string): void {
        if (type === 'LocalDateTime') {
            this.importManager.addJavaTimeImport('LocalDateTime');
        } else if (type === 'BigDecimal') {
            this.importManager.addJavaImport('math.BigDecimal');
        } else if (type.startsWith('Set<')) {
            this.importManager.addJavaUtilImport('Set');
        } else if (type.startsWith('List<')) {
            this.importManager.addJavaUtilImport('List');
        } else if (type.startsWith('Optional<')) {
            this.importManager.addJavaUtilImport('Optional');
        }
    }

    /**
     * Create empty DTO (fallback for errors)
     */
    private createEmptyDto(entityName: string, options: DtoGenerationOptions): GeneratedDto {
        return {
            className: this.buildDtoClassName({ name: entityName } as Entity, options),
            packageName: this.buildDtoPackageName({ name: entityName, $container: { name: entityName } } as Entity, options),
            imports: [],
            fields: [],
            constructors: [`public ${this.buildDtoClassName({ name: entityName } as Entity, options)}() {}`],
            methods: [],
            annotations: []
        };
    }

    /**
     * Utility methods
     */
    private capitalize(str: string): string {
        if (!str) return '';
        return str.charAt(0).toUpperCase() + str.slice(1);
    }

    /**
     * Static factory methods for common DTO types
     */
    static forEntity(entity: Entity, projectName: string): GeneratedDto {
        const generator = new UnifiedDtoGenerator(projectName);
        return generator.generateDto(entity, {
            projectName,
            dtoType: DtoType.ENTITY_DTO,
            includeAggregateFields: entity.isRoot,
            includeSerializable: true
        });
    }

    static forWebApiRequest(entity: Entity, projectName: string): GeneratedDto {
        const generator = new UnifiedDtoGenerator(projectName);
        return generator.generateDto(entity, {
            projectName,
            dtoType: DtoType.REQUEST_DTO,
            includeValidationAnnotations: true,
            excludeFields: ['id', 'version', 'state']
        });
    }

    static forWebApiResponse(entity: Entity, projectName: string): GeneratedDto {
        const generator = new UnifiedDtoGenerator(projectName);
        return generator.generateDto(entity, {
            projectName,
            dtoType: DtoType.RESPONSE_DTO,
            includeAggregateFields: entity.isRoot
        });
    }

    static forShared(entity: Entity, projectName: string): GeneratedDto {
        const generator = new UnifiedDtoGenerator(projectName);
        return generator.generateDto(entity, {
            projectName,
            dtoType: DtoType.SHARED_DTO,
            includeAggregateFields: entity.isRoot,
            includeSerializable: true
        });
    }
}

/**
 * DTO rendering utility that converts GeneratedDto to Java code
 */
export class DtoRenderer {
    /**
     * Render a GeneratedDto to Java code string
     */
    static renderToJavaCode(dto: GeneratedDto): string {
        const parts: string[] = [];

        // Package declaration
        parts.push(`package ${dto.packageName};`);
        parts.push('');

        // Imports
        if (dto.imports.length > 0) {
            parts.push(...dto.imports);
            parts.push('');
        }

        // Class annotations
        if (dto.annotations.length > 0) {
            parts.push(...dto.annotations);
        }

        // Class declaration
        const implementsClause = dto.imports.some(imp => imp.includes('Serializable')) ? ' implements Serializable' : '';
        parts.push(`public class ${dto.className}${implementsClause} {`);

        // Fields
        if (dto.fields.length > 0) {
            dto.fields.forEach(field => {
                if (field.annotations && field.annotations.length > 0) {
                    parts.push(`\t${field.annotations.join('\n\t')}`);
                }
                parts.push(`\tprivate ${field.type} ${field.name};`);
            });
            parts.push('');
        }

        // Constructors
        if (dto.constructors.length > 0) {
            dto.constructors.forEach(constructor => {
                parts.push(`\t${constructor}`);
                parts.push('');
            });
        }

        // Methods
        if (dto.methods.length > 0) {
            dto.methods.forEach(method => {
                parts.push(`\t${method.replace(/\n/g, '\n\t')}`);
                parts.push('');
            });
        }

        parts.push('}');

        return parts.join('\n');
    }
}
