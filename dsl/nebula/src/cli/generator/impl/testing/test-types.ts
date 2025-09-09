import type { Aggregate } from "../../../../language/generated/ast.js";
import { BaseGenerationOptions } from "../../base/base-generator.js";

export interface TestGenerationOptions {
    aggregate: Aggregate;
    projectName: string;
    outputDirectory: string;
    features?: string[];
}

export interface TestContext {
    aggregate: Aggregate;
    projectName: string;
    capitalizedProjectName: string;
    aggregateName: string;
    rootEntity: any;
    entityName: string;
    testPath: string;
    features: string[];
}

export interface TestGenerationContext extends BaseGenerationOptions {
    testGenerationOptions: TestGenerationOptions;
}
