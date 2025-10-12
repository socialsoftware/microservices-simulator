export type TemplateGenerateOptions = {
    destination?: string;
    name?: string;
    validate?: boolean;
};

export type GenerationOptions = {
    projectName: string;
    outputPath: string;
    consistencyModels?: string[];
    // Shared metadata extracted from all DSL models before generation
    allSharedDtos?: any[];
    dtoMappings?: any[];
    allModels?: any[];
};

export type ProjectPaths = {
    projectPath: string;
    javaPath: string;
    javaSrcPath: string;
    packagePath: string;
};

export const DEFAULT_OUTPUT_DIR = "../../applications";
export const JAVA_SRC_PATH = ['src', 'main', 'java'];

export type { Aggregate, Entity, Method, Parameter, Workflow } from "../../language/generated/ast.js";

export interface GeneratorRegistry {
    entityGenerator: any;
    dtoGenerator: any;
    serviceGenerator: any;
    factoryGenerator: any;
    repositoryGenerator: any;
    repositoryInterfaceGenerator: any;
    eventGenerator: any;
    coordinationGenerator: any;
    webApiGenerator: any;
    validationGenerator: any;
    integrationGenerator: any;
    sagaGenerator: any;
    sagaFunctionalityGenerator: any;
    exceptionGenerator: any;
    eventHandlerGenerator: any;
    causalEntityGenerator: any;
    configurationGenerator: any;
    serviceDefinitionGenerator: any;
}
