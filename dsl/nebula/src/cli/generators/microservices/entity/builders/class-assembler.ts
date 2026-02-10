import { EntityExt } from "../../../../types/ast-extensions.js";
import { getGlobalConfig } from "../../../common/config.js";

/**
 * Type for entity class structure metadata
 */
export interface ClassStructure {
    aggregateName: string;
    extendsClause: string;
    abstractModifier: string;
    packageName: string;
}

/**
 * Type for entity components that will be assembled into a Java class
 */
export interface EntityComponents {
    fields: string;
    defaultConstructor: string;
    dtoConstructor: string;
    projectionDtoConstructor: string;
    copyConstructor: string;
    gettersSetters: string;
    backRefGetterSetter: string;
    eventSubscriptions: string;
    interInvariantMethods: string;
    invariants: string;
    buildDtoMethod: string;
}

/**
 * Handles assembly of entity components into final Java class code.
 *
 * This builder takes the various generated components (fields, constructors,
 * methods, etc.) and assembles them into a complete Java class with proper
 * structure, package declaration, and inheritance hierarchy.
 *
 * Responsibilities:
 * - Build class structure metadata (package, extends clause, modifiers)
 * - Assemble components into final Java class string
 * - Apply proper formatting and organization
 */
export class ClassAssembler {
    /**
     * Builds the class structure metadata for an entity
     */
    buildClassStructure(entity: EntityExt, projectName: string, isRootEntity: boolean): ClassStructure {
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

    /**
     * Assembles all entity components into final Java class code
     */
    assembleJavaCode(classStructure: ClassStructure, components: EntityComponents, entityName: string): string {
        return `package ${classStructure.packageName};

IMPORTS_PLACEHOLDER

@Entity
public ${classStructure.abstractModifier}class ${entityName}${classStructure.extendsClause} {
${components.fields}
${components.defaultConstructor}
${components.dtoConstructor}
${components.projectionDtoConstructor}
${components.copyConstructor}
${components.gettersSetters}
${components.backRefGetterSetter}
${components.eventSubscriptions}
${components.interInvariantMethods}
${components.invariants}
${components.buildDtoMethod}
}`;
    }
}
