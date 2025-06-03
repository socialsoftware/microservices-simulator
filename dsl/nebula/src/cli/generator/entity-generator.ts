import { Entity } from "../../language/generated/ast.js";
import { generateImports } from "./import-generator.js";
import { generateFields, generateConstructor, generateCopyConstructor, generateGettersSetters } from "./field-generator.js";
import { generateInvariants } from "./invariant-generator.js";
import { resolveJavaType } from "./utils.js";

// Define a type for tracking import requirements
export type ImportRequirements = {
    usesPersistence?: boolean;
    usesLocalDateTime?: boolean;
    usesBigDecimal?: boolean;
    usesSet?: boolean;
    usesList?: boolean;
    usesAggregate?: boolean;
    usesStreams?: boolean;
    customImports?: Set<string>;
};

/**
 * Generates Java code for an entity class.
 * 
 * @param entity The entity to generate code for
 * @param projectName The project name
 * @returns The generated Java code as a string
 */
export function generateEntityCode(entity: Entity, projectName: string): string {
    // Initialize import requirements object
    const importReqs: ImportRequirements = {
        usesPersistence: true, // All entities use persistence
        usesLocalDateTime: false,
        usesBigDecimal: false,
        usesSet: false,
        usesList: false,
        usesAggregate: entity.isRoot, // Root entities always use Aggregate
        usesStreams: false,
        customImports: new Set<string>()
    };

    const { code: fields, imports: fieldImports } = generateFields(entity.properties, entity);
    Object.assign(importReqs, fieldImports);

    const { code: constructor } = generateConstructor(entity.name, entity.properties);

    const { code: copyConstructor, imports: copyImports } = generateCopyConstructor(entity);
    Object.assign(importReqs, copyImports);

    const { code: gettersSetters } = generateGettersSetters(entity.properties);

    const { code: invariants, imports: invariantImports } = entity.isRoot ?
        generateInvariants(entity) : { code: '', imports: {} };

    if (entity.isRoot && invariantImports) {
        Object.assign(importReqs, invariantImports);
    }

    const imports = generateImports(importReqs, projectName, entity.isRoot);

    const extendsClause = entity.isRoot ? ' extends Aggregate' : '';
    const abstractModifier = entity.isRoot ? 'abstract ' : '';
    const packageName = `pt.ulisboa.tecnico.socialsoftware.${projectName.toLowerCase()}.microservices.${entity.$container.name.toLowerCase()}.aggregate`;

    return `package ${packageName};

${imports}

@Entity
public ${abstractModifier}class ${entity.name}${extendsClause} {
${fields} 
${constructor}
${copyConstructor}
${gettersSetters}
${invariants}
}`;
}

export function generateDtoCode(entity: Entity, projectName: string): string {
    const packageName = `pt.ulisboa.tecnico.socialsoftware.${projectName.toLowerCase()}.microservices.${entity.$container.name.toLowerCase()}.aggregate`;
    const fields = generateDtoFields(entity);
    const constructor = generateDtoConstructor(entity);
    const gettersSetters = generateGettersSetters(entity.properties);

    // Collect DTO specific imports
    const importReqs: ImportRequirements = {
        usesLocalDateTime: entity.properties.some(p => resolveJavaType(p.type) === 'LocalDateTime'),
        usesSet: entity.properties.some(p => resolveJavaType(p.type).startsWith('Set<')),
        customImports: new Set(['import java.io.Serializable;'])
    };

    return `package ${packageName};

import java.io.Serializable;
${importReqs.usesLocalDateTime ? 'import java.time.LocalDateTime;' : ''}
${importReqs.usesSet ? 'import java.util.Set;\nimport java.util.HashSet;' : ''}

public class ${entity.name}Dto implements Serializable {
${fields}
${constructor}
${gettersSetters}
}`;
}

/**
 * Generates fields for a DTO class.
 */
function generateDtoFields(entity: Entity): string {
    const simpleFields = entity.properties.map(property => {
        const javaType = resolveJavaType(property.type);
        return `\tprivate ${javaType} ${property.name};`;
    }).join('\n');

    return simpleFields;
}

/**
 * Generates a constructor for a DTO class.
 */
function generateDtoConstructor(entity: Entity): string {
    const params = entity.properties
        .map(p => {
            return `${resolveJavaType(p.type)} ${p.name}`;
        })
        .join(', ');
    const assignments = entity.properties
        .map(p => `\t\tthis.${p.name} = ${p.name};`)
        .join('\n');

    return `
\tpublic ${entity.name}Dto() {}

\tpublic ${entity.name}Dto(${params}) {
${assignments}
\t}`;
} 