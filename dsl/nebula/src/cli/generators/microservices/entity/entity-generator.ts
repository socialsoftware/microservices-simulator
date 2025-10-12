import { Entity } from "../../../../language/generated/ast.js";
import { getGlobalConfig } from "../../shared/config.js";
import { EntityGenerationOptions } from "./entity-generator/types.js";
import { generateFields } from "./entity-generator/field-generator.js";
import { generateDefaultConstructor, generateEntityDtoConstructor, generateCopyConstructor } from "./entity-generator/constructor-generator.js";
import { generateGettersSetters, generateBackReferenceGetterSetter } from "./entity-generator/method-generator.js";
import { generateInvariants } from "./entity-generator/invariant-generator.js";
import { scanCodeForImports } from "./entity-generator/import-detector.js";

// ============================================================================
// MAIN ENTITY GENERATION ORCHESTRATION
// ============================================================================

export function generateEntityCode(entity: Entity, projectName: string, options?: EntityGenerationOptions): string {
    const opts = options || { projectName };
    const isRootEntity = entity.isRoot || false;

    // Generate all components using modular functions
    const { code: fields } = generateFields(entity.properties, entity, isRootEntity, projectName);
    const { code: defaultConstructor } = generateDefaultConstructor(entity.name);
    const { code: dtoConstructor } = generateEntityDtoConstructor(entity, projectName, opts.allSharedDtos, opts.dtoMappings);
    const { code: copyConstructor } = generateCopyConstructor(entity);
    const { code: gettersSetters } = generateGettersSetters(entity.properties, entity, projectName, opts.allEntities);

    // Generate getter/setter for back-reference field in non-root entities
    const backRefGetterSetter = (!isRootEntity && entity.$container)
        ? generateBackReferenceGetterSetter(entity.$container.name)
        : '';

    // Only generate invariants for root entities
    const { code: invariants } = isRootEntity ? generateInvariants(entity) : { code: '' };

    // Generate custom constructors and methods (simplified for now)
    const constructors = generateConstructors(entity);
    const methods = generateMethods(entity);

    // Build the complete Java class
    const aggregateName = entity.$container?.name || 'unknown';
    const extendsClause = isRootEntity
        ? (aggregateName !== entity.name ? ` extends ${aggregateName}` : ' extends Aggregate')
        : '';
    const abstractModifier = isRootEntity ? 'abstract ' : '';
    const config = getGlobalConfig();
    const packageName = config.buildPackageName(
        projectName,
        'microservices',
        aggregateName.toLowerCase(),
        'aggregate'
    );

    // Generate the complete Java code first
    const javaCode = `package ${packageName};

IMPORTS_PLACEHOLDER

@Entity
public ${abstractModifier}class ${entity.name}${extendsClause} {
${fields}
${defaultConstructor}
${dtoConstructor}
${copyConstructor}
${constructors}
${gettersSetters}
${backRefGetterSetter}
${methods}
${invariants}
}`;

    // Scan the generated code for imports and replace placeholder
    const detectedImports = scanCodeForImports(javaCode, projectName, isRootEntity, aggregateName, entity.name);
    const importsString = detectedImports.join('\n');

    return javaCode.replace('IMPORTS_PLACEHOLDER', importsString);
}

// ============================================================================
// PLACEHOLDER FUNCTIONS (TO BE IMPLEMENTED)
// ============================================================================

// Placeholder functions for custom constructors and methods
function generateConstructors(entity: Entity): string {
    return '';
}

function generateMethods(entity: Entity): string {
    return '';
}

// ============================================================================
// DTO GENERATION (LEGACY - TO BE REFACTORED LATER)
// ============================================================================

export function generateDtoCode(entity: Entity, projectName: string): string {
    const config = getGlobalConfig();
    const packageName = config.buildPackageName(
        projectName,
        'microservices',
        entity.$container?.name?.toLowerCase() || 'unknown',
        'aggregate'
    );
    const fields = generateDtoFields(entity);
    const constructor = generateDtoConstructor(entity);
    const gettersSetters = generateDtoGettersSetters(entity.properties);

    return `package ${packageName};

import java.io.Serializable;

public class ${entity.name}Dto implements Serializable {
${fields}
${constructor}
${gettersSetters}
}`;
}

function generateDtoFields(entity: Entity): string {
    const simpleFields = entity.properties.map((property: any) => {
        // Simplified DTO field generation
        return `\tprivate String ${property.name};`;
    }).join('\n');

    return simpleFields;
}

function generateDtoConstructor(entity: Entity): string {
    return `\n\tpublic ${entity.name}Dto() {}\n`;
}

function generateDtoGettersSetters(properties: any[]): string {
    return properties.map((prop: any) => {
        const capName = prop.name.charAt(0).toUpperCase() + prop.name.slice(1);
        return `\n\tpublic String get${capName}() { return ${prop.name}; }\n\tpublic void set${capName}(String ${prop.name}) { this.${prop.name} = ${prop.name}; }`;
    }).join('\n');
}

// ============================================================================
// MAIN ENTITY GENERATOR CLASS
// ============================================================================

export class EntityGenerator {
    async generateEntity(entity: Entity, options: EntityGenerationOptions): Promise<string> {
        return generateEntityCode(entity, options.projectName, options);
    }
}