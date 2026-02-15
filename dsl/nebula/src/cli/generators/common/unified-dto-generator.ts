


import { Entity, Property } from "../../../language/generated/ast.js";
import { UnifiedTypeResolver } from "./unified-type-resolver.js";
import { ImportManager, ImportManagerFactory } from "../../utils/import-manager.js";
import { ErrorHandler, ErrorUtils, ErrorSeverity } from "../../utils/error-handler.js";
import { StringUtils } from '../../utils/string-utils.js';



export enum DtoType {
    ENTITY_DTO = 'entity',           
    REQUEST_DTO = 'request',         
    RESPONSE_DTO = 'response',       
    CREATE_REQUEST = 'create-request', 
    UPDATE_REQUEST = 'update-request', 
    SHARED_DTO = 'shared'            
}



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



export interface DtoField {
    name: string;
    type: string;
    annotations?: string[];
    getter?: string;
    setter?: string;
    isRequired?: boolean;
}



export interface GeneratedDto {
    className: string;
    packageName: string;
    imports: string[];
    fields: DtoField[];
    constructors: string[];
    methods: string[];
    annotations: string[];
}



export class UnifiedDtoGenerator {
    private importManager: ImportManager;

    constructor(projectName: string) {
        this.importManager = ImportManagerFactory.createForMicroservice(projectName);
    }

    

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

    

    generateDtoPair(entity: Entity, baseOptions: Omit<DtoGenerationOptions, 'dtoType'>): {
        request: GeneratedDto;
        response: GeneratedDto;
    } {
        const requestOptions: DtoGenerationOptions = {
            ...baseOptions,
            dtoType: DtoType.REQUEST_DTO,
            excludeFields: ['id'] 
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

    

    generateCrudDtos(entity: Entity, baseOptions: Omit<DtoGenerationOptions, 'dtoType'>): {
        create: GeneratedDto;
        response: GeneratedDto;
    } {
        return {
            create: this.generateDto(entity, { ...baseOptions, dtoType: DtoType.CREATE_REQUEST, excludeFields: ['id'] }),
            response: this.generateDto(entity, { ...baseOptions, dtoType: DtoType.RESPONSE_DTO, includeAggregateFields: true })
        };
    }

    

    private generateDtoInternal(entity: Entity, options: DtoGenerationOptions): GeneratedDto {
        
        this.importManager.clear();

        
        const className = this.buildDtoClassName(entity, options);
        const packageName = this.buildDtoPackageName(entity, options);
        const fields = this.buildDtoFields(entity, options);
        const constructors = this.buildDtoConstructors(entity, className, fields, options);
        const methods = this.buildDtoMethods(fields, options);
        const annotations = this.buildDtoAnnotations(options);

        
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

    

    private buildDtoClassName(entity: Entity, options: DtoGenerationOptions): string {
        const entityName = entity.name;

        switch (options.dtoType) {
            case DtoType.CREATE_REQUEST:
                return `Create${entityName}RequestDto`;
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
                
                return `pt.ulisboa.tecnico.socialsoftware.ms.${options.projectName.toLowerCase()}.coordination.webapi.requestDtos`;
            case DtoType.SHARED_DTO:
                
                return `pt.ulisboa.tecnico.socialsoftware.ms.${options.projectName.toLowerCase()}.shared.dtos`;
            case DtoType.ENTITY_DTO:
            default:
                
                return `pt.ulisboa.tecnico.socialsoftware.ms.${options.projectName.toLowerCase()}.microservices.${aggregateName.toLowerCase()}.aggregate`;
        }
    }

    

    private buildDtoFields(entity: Entity, options: DtoGenerationOptions): DtoField[] {
        const fields: DtoField[] = [];

        
        if (entity.isRoot && options.includeAggregateFields) {
            fields.push({
                name: 'aggregateId',
                type: 'Integer',
                annotations: [],
                getter: 'getAggregateId',
                setter: 'setAggregateId'
            });
        }

        
        if (entity.properties) {
            for (const property of entity.properties) {
                
                if (options.excludeFields?.includes(property.name)) {
                    continue;
                }

                const field = this.buildDtoFieldFromProperty(property, options);
                fields.push(field);
            }
        }

        
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

        
        if (options.customFields) {
            fields.push(...options.customFields);
        }

        return fields;
    }

    

    private buildDtoFieldFromProperty(property: Property, options: DtoGenerationOptions): DtoField {
        const propertyName = property.name;
        const javaType = UnifiedTypeResolver.resolveForDto(property.type);
        const capitalizedName = StringUtils.capitalize(propertyName);

        const annotations: string[] = [];

        
        if (options.includeValidationAnnotations) {
            if (javaType === 'String') {
                annotations.push('@NotBlank');
            } else if (!javaType.includes('Optional')) {
                annotations.push('@NotNull');
            }
        }

        
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

    

    private buildDtoConstructors(entity: Entity, className: string, fields: DtoField[], options: DtoGenerationOptions): string[] {
        const constructors: string[] = [];

        
        constructors.push(`public ${className}() {}`);

        
        if (options.dtoType === DtoType.ENTITY_DTO || options.dtoType === DtoType.SHARED_DTO) {
            constructors.push(this.buildEntityConstructor(entity, className, fields));
        }

        return constructors;
    }

    

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

    

    private buildDtoMethods(fields: DtoField[], options: DtoGenerationOptions): string[] {
        const methods: string[] = [];

        for (const field of fields) {
            
            const getterAnnotations = field.annotations?.length ? field.annotations.join('\n\t') + '\n\t' : '';
            methods.push(`${getterAnnotations}public ${field.type} ${field.getter}() {
\t\treturn ${field.name};
\t}`);

            
            methods.push(`public void ${field.setter}(${field.type} ${field.name}) {
\t\tthis.${field.name} = ${field.name};
\t}`);
        }

        return methods;
    }

    

    private buildDtoAnnotations(options: DtoGenerationOptions): string[] {
        const annotations: string[] = [];

        if (options.includeValidationAnnotations) {
            
        }

        return annotations;
    }

    

    private addRequiredImports(fields: DtoField[], options: DtoGenerationOptions): void {
        
        if (options.includeSerializable !== false) {
            this.importManager.addJavaImport('io.Serializable');
        }

        
        if (options.includeValidationAnnotations) {
            const hasValidation = fields.some(f => f.annotations?.some(a => a.startsWith('@')));
            if (hasValidation) {
                this.importManager.addJakartaImport('validation.constraints.*');
            }
        }

        
        fields.forEach(field => {
            this.addImportForType(field.type);
        });

        
        const hasAggregateState = fields.some(f => f.type === 'AggregateState');
        if (hasAggregateState) {
            this.importManager.addBaseFrameworkImport('ms.domain.aggregate.Aggregate.AggregateState');
        }
    }

    

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



export class DtoRenderer {
    

    static renderToJavaCode(dto: GeneratedDto): string {
        const parts: string[] = [];

        
        parts.push(`package ${dto.packageName};`);
        parts.push('');

        
        if (dto.imports.length > 0) {
            parts.push(...dto.imports);
            parts.push('');
        }

        
        if (dto.annotations.length > 0) {
            parts.push(...dto.annotations);
        }

        
        const implementsClause = dto.imports.some(imp => imp.includes('Serializable')) ? ' implements Serializable' : '';
        parts.push(`public class ${dto.className}${implementsClause} {`);

        
        if (dto.fields.length > 0) {
            dto.fields.forEach(field => {
                if (field.annotations && field.annotations.length > 0) {
                    parts.push(`\t${field.annotations.join('\n\t')}`);
                }
                parts.push(`\tprivate ${field.type} ${field.name};`);
            });
            parts.push('');
        }

        
        if (dto.constructors.length > 0) {
            dto.constructors.forEach(constructor => {
                parts.push(`\t${constructor}`);
                parts.push('');
            });
        }

        
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
