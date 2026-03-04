import { EntityExt } from "../../../../types/ast-extensions.js";
import { getGlobalConfig } from "../../../common/config.js";



export interface ClassStructure {
    aggregateName: string;
    extendsClause: string;
    abstractModifier: string;
    packageName: string;
}



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



export class ClassAssembler {
    

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
