import { Entity } from "../../../../language/generated/ast.js";
import { getGlobalConfig } from "../../common/config.js";
import { EntityGenerationOptions } from "./types.js";
import { generateFields } from "./fields.js";
import { generateDefaultConstructor, generateEntityDtoConstructor, generateCopyConstructor } from "./constructors.js";
import { generateGettersSetters, generateBackReferenceGetterSetter } from "./methods.js";
import { generateInvariants } from "./invariants.js";
import { ImportManager, ImportManagerFactory } from "../../../utils/import-manager.js";
import { ErrorHandler, ErrorUtils, ErrorSeverity } from "../../../utils/error-handler.js";

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
        return this.finalizeWithImports(javaCode, projectName, isRootEntity, classStructure.aggregateName, entity.name);
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

    private finalizeWithImports(javaCode: string, projectName: string, isRootEntity: boolean, aggregateName: string, entityName: string): string {
        // Use improved import scanning with the import manager
        this.importManager.clear();

        // Scan the generated code for imports and add them to the manager
        this.importManager.resolveAndAddImports(javaCode);

        // Add any additional imports that might be needed
        this.addRequiredImports(isRootEntity, aggregateName);

        // Get formatted imports
        const formattedImports = this.importManager.formatImports();
        const importsString = formattedImports.join('\n');

        return javaCode.replace('IMPORTS_PLACEHOLDER', importsString);
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