import { Entity } from "../../../../language/generated/ast.js";
import { getGlobalConfig } from "../../common/config.js";
import { EntityGenerationOptions } from "./types.js";
import { generateFields } from "./fields.js";
import { generateDefaultConstructor, generateEntityDtoConstructor, generateCopyConstructor } from "./constructors.js";
import { generateGettersSetters, generateBackReferenceGetterSetter } from "./methods.js";
import { generateInvariants } from "./invariants.js";
import { ImportManager, ImportManagerFactory } from "../../../utils/import-manager.js";
import { ErrorHandler, ErrorUtils, ErrorSeverity } from "../../../utils/error-handler.js";
import { TypeResolver } from "../../common/resolvers/type-resolver.js";

// ============================================================================
// ENTITY GENERATION ORCHESTRATION
// ============================================================================

/**
 * Entity generation orchestrator that coordinates all entity generation components
 */
export class EntityOrchestrator {
    private importManager: ImportManager;

    constructor(projectName: string) {
        this.importManager = ImportManagerFactory.createForMicroservice(projectName);
    }

    /**
     * Generate complete entity code with all components
     */
    generateEntityCode(entity: Entity, projectName: string, options?: EntityGenerationOptions): string {
        return ErrorHandler.wrap(
            () => this.generateEntityCodeInternal(entity, projectName, options),
            ErrorUtils.entityContext(
                'generate entity code',
                entity.$container?.name || 'unknown',
                entity.name,
                'entity-orchestrator',
                { isRoot: entity.isRoot, projectName }
            ),
            ErrorSeverity.FATAL
        ) || '';
    }

    private generateEntityCodeInternal(entity: Entity, projectName: string, options?: EntityGenerationOptions): string {
        const opts = options || { projectName };
        const isRootEntity = entity.isRoot || false;

        // Generate all components using modular functions
        const components = this.generateEntityComponents(entity, projectName, opts, isRootEntity);

        // Build the complete Java class
        const classStructure = this.buildClassStructure(entity, projectName, isRootEntity);

        // Generate the complete Java code
        const javaCode = this.assembleJavaCode(classStructure, components, entity.name);

        // Use improved import management
        return this.finalizeWithImports(javaCode, projectName, isRootEntity, classStructure.aggregateName, entity.name, entity);
    }

    private generateEntityComponents(entity: Entity, projectName: string, opts: EntityGenerationOptions, isRootEntity: boolean) {
        return {
            fields: generateFields(entity.properties, entity, isRootEntity, projectName).code,
            defaultConstructor: generateDefaultConstructor(entity.name).code,
            dtoConstructor: generateEntityDtoConstructor(entity, projectName, opts.allSharedDtos, opts.dtoMappings).code,
            copyConstructor: generateCopyConstructor(entity).code,
            gettersSetters: generateGettersSetters(entity.properties, entity, projectName, opts.allEntities).code,
            backRefGetterSetter: (!isRootEntity && entity.$container)
                ? generateBackReferenceGetterSetter(entity.$container.name)
                : '',
            invariants: isRootEntity ? generateInvariants(entity).code : ''
        };
    }

    private buildClassStructure(entity: Entity, projectName: string, isRootEntity: boolean) {
        const aggregateName = entity.$container?.name || 'unknown';
        const config = getGlobalConfig();

        return {
            aggregateName,
            extendsClause: isRootEntity
                ? (aggregateName !== entity.name ? ` extends ${aggregateName}` : ' extends Aggregate')
                : '',
            abstractModifier: isRootEntity ? 'abstract ' : '',
            packageName: config.buildPackageName(
                projectName,
                'microservices',
                aggregateName.toLowerCase(),
                'aggregate'
            )
        };
    }

    private assembleJavaCode(classStructure: any, components: any, entityName: string): string {
        return `package ${classStructure.packageName};

IMPORTS_PLACEHOLDER

@Entity
public ${classStructure.abstractModifier}class ${entityName}${classStructure.extendsClause} {
${components.fields}
${components.defaultConstructor}
${components.dtoConstructor}
${components.copyConstructor}
${components.gettersSetters}
${components.backRefGetterSetter}
${components.invariants}
}`;
    }

    private finalizeWithImports(javaCode: string, projectName: string, isRootEntity: boolean, aggregateName: string, entityName: string, entity: Entity): string {
        // Use the legacy import scanning which works better for entity code
        const detectedImports = this.scanCodeForImports(javaCode, projectName, isRootEntity, aggregateName, entityName, entity);

        // Combine with any additional imports
        this.importManager.clear();
        detectedImports.forEach(imp => {
            this.importManager.addCustomImport(imp);
        });

        // Add any additional imports that might be needed
        this.addRequiredImports(isRootEntity, aggregateName);

        // Get formatted imports
        const formattedImports = this.importManager.formatImports();
        const importsString = formattedImports.join('\n');

        return javaCode.replace('IMPORTS_PLACEHOLDER', importsString);
    }

    /**
     * Scan code for required imports (copied from imports.ts for better integration)
     */
    private scanCodeForImports(javaCode: string, projectName: string, isRoot: boolean, aggregateName?: string, entityName?: string, entity?: Entity): string[] {
        const imports: string[] = [];

        // Basic JPA imports
        if (javaCode.includes('@Entity')) imports.push('import jakarta.persistence.Entity;');
        if (javaCode.includes('@Id')) imports.push('import jakarta.persistence.Id;');
        if (javaCode.includes('@GeneratedValue')) imports.push('import jakarta.persistence.GeneratedValue;');
        if (javaCode.includes('@OneToOne')) imports.push('import jakarta.persistence.OneToOne;');
        if (javaCode.includes('@OneToMany')) imports.push('import jakarta.persistence.OneToMany;');
        if (javaCode.includes('CascadeType')) imports.push('import jakarta.persistence.CascadeType;');
        if (javaCode.includes('FetchType')) imports.push('import jakarta.persistence.FetchType;');
        if (javaCode.includes('@Enumerated')) imports.push('import jakarta.persistence.Enumerated;');
        if (javaCode.includes('EnumType')) imports.push('import jakarta.persistence.EnumType;');

        // Java utility imports
        if (javaCode.includes('LocalDateTime')) imports.push('import java.time.LocalDateTime;');
        if (javaCode.includes('BigDecimal')) imports.push('import java.math.BigDecimal;');
        if (javaCode.includes('Set<') || javaCode.includes('HashSet')) {
            imports.push('import java.util.Set;');
            imports.push('import java.util.HashSet;');
        }
        if (javaCode.includes('List<') || javaCode.includes('ArrayList')) {
            imports.push('import java.util.List;');
            imports.push('import java.util.ArrayList;');
        }
        if (javaCode.includes('Collectors')) imports.push('import java.util.stream.Collectors;');

        // Get config once for all dynamic imports
        const config = getGlobalConfig();

        // Domain-specific imports
        if (javaCode.includes('AggregateState')) {
            const aggregateStateImport = `import ${config.getBasePackage()}.ms.domain.aggregate.Aggregate.AggregateState;`;
            imports.push(aggregateStateImport);
        }

        // Aggregate imports
        if (isRoot) {
            const aggregateImport = `import ${config.getBasePackage()}.ms.domain.aggregate.Aggregate;`;
            imports.push(aggregateImport);
        }

        // Scan for DTO imports (classes ending with 'Dto')
        const dtoPattern = /\b([A-Z][a-zA-Z]*Dto)\b/g;
        let dtoMatch;
        while ((dtoMatch = dtoPattern.exec(javaCode)) !== null) {
            const dtoType = dtoMatch[1];
            const dtoImport = `import ${config.buildPackageName(projectName, 'shared', 'dtos')}.${dtoType};`;
            imports.push(dtoImport);
        }

        // Scan for other entity imports (classes that are likely entities)
        const entityPattern = /\b([A-Z][a-zA-Z]*(?:User|Course|Question|Quiz|Topic|Tournament|Answer|Execution|Option))\b/g;
        let entityMatch;
        const excludedEntityNames = ['String', 'Integer', 'Long', 'Double', 'Float', 'Boolean', 'LocalDateTime', 'BigDecimal'];

        while ((entityMatch = entityPattern.exec(javaCode)) !== null) {
            const entityType = entityMatch[1];
            if (!excludedEntityNames.includes(entityType) && !entityType.endsWith('Dto')) {
                // Check if it's likely another entity in the same aggregate
                if (entityType !== entityName && entityType !== aggregateName) {
                    const entityImport = `import ${config.buildPackageName(projectName, 'microservices', aggregateName?.toLowerCase() || 'unknown', 'aggregate')}.${entityType};`;
                    imports.push(entityImport);
                }
            }
        }

        if (entity) {
            const enumTypes = new Set<string>();

            for (const prop of entity.properties) {
                if (prop.type && typeof prop.type === 'object' && prop.type.$type === 'EntityType' && prop.type.type) {
                    const ref = prop.type.type.ref;
                    if (ref && typeof ref === 'object' && '$type' in ref && (ref as any).$type === 'EnumDefinition') {
                        const enumName = (ref as any).name;
                        if (enumName) {
                            enumTypes.add(enumName);
                        }
                    } else if (prop.type.type.$refText) {
                        const refText = prop.type.type.$refText;
                        const javaType = TypeResolver.resolveJavaType(prop.type);
                        if (!TypeResolver.isPrimitiveType(javaType) &&
                            !TypeResolver.isEntityType(javaType) &&
                            !javaType.startsWith('List<') &&
                            !javaType.startsWith('Set<')) {
                            enumTypes.add(refText);
                        }
                    }
                }
            }

            const excludedEnums = ['EnumType', 'CascadeType', 'FetchType', 'AggregateState'];
            for (const enumType of enumTypes) {
                if (!excludedEnums.includes(enumType)) {
                    const enumImport = `import ${config.buildPackageName(projectName, 'shared', 'enums')}.${enumType};`;
                    if (!imports.includes(enumImport)) {
                        imports.push(enumImport);
                    }
                }
            }
        }

        const enumPattern = /\b([A-Z][a-zA-Z]*(?:Type|Role|State))\b/g;
        let enumMatch;
        const excludedEnums = ['EnumType', 'CascadeType', 'FetchType', 'AggregateState', 'LocalDateTime'];
        const foundEnums = new Set<string>();

        while ((enumMatch = enumPattern.exec(javaCode)) !== null) {
            const enumType = enumMatch[1];
            if (!excludedEnums.includes(enumType) &&
                !enumType.endsWith('Dto') &&
                !enumType.includes('List') &&
                !enumType.includes('Set')) {
                foundEnums.add(enumType);
            }
        }

        for (const enumType of foundEnums) {
            const enumImport = `import ${config.buildPackageName(projectName, 'shared', 'enums')}.${enumType};`;
            if (!imports.includes(enumImport)) {
                imports.push(enumImport);
            }
        }

        return imports.flat(); // Flatten in case any push had multiple imports
    }

    private addRequiredImports(isRootEntity: boolean, aggregateName: string): void {
        // Add JPA imports
        this.importManager.addJakartaImport('persistence.Entity');

        if (isRootEntity) {
            // Add base aggregate import
            this.importManager.addBaseFrameworkImport('ms.domain.aggregate.Aggregate');
        }
    }
}

// ============================================================================
// NOTE: DTO generation is handled by shared/dto-generator.ts
// Custom constructors and methods are not currently supported in the DSL
// ============================================================================

// ============================================================================
// BACKWARD COMPATIBILITY AND FACADE
// ============================================================================

/**
 * Backward compatibility function for existing code
 */
export function generateEntityCode(entity: Entity, projectName: string, options?: EntityGenerationOptions): string {
    const orchestrator = new EntityOrchestrator(projectName);
    return orchestrator.generateEntityCode(entity, projectName, options);
}

/**
 * Entity generator facade that uses the new orchestrator
 */
export class EntityGenerator {
    private orchestrator: EntityOrchestrator;

    constructor(projectName: string = 'project') {
        this.orchestrator = new EntityOrchestrator(projectName);
    }

    /**
     * Main entry point for entity generation
     */
    async generateEntity(entity: Entity, options: EntityGenerationOptions): Promise<string> {
        return this.orchestrator.generateEntityCode(entity, options.projectName, options);
    }
}