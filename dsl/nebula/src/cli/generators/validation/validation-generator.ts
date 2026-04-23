import { Aggregate } from "../../../language/generated/ast.js";
import { ValidationGenerationOptions } from "./validation-types.js";
import { InvariantGenerator } from "./invariant-generator.js";
import { AnnotationGenerator } from "./annotation-generator.js";
import { CustomValidatorGenerator } from "./custom-validator-generator.js";

export { ValidationGenerationOptions } from "./validation-types.js";

export class ValidationGenerator {
    private invariantGenerator = new InvariantGenerator();
    private annotationGenerator = new AnnotationGenerator();
    private customValidatorGenerator = new CustomValidatorGenerator();

    async generateValidation(aggregate: Aggregate, options: ValidationGenerationOptions): Promise<{ [key: string]: string }> {
        const rootEntity = aggregate.entities.find((e: any) => e.isRoot);
        if (!rootEntity) {
            throw new Error(`No root entity found in aggregate ${aggregate.name}`);
        }

        const results: { [key: string]: string } = {};

        results['invariants'] = await this.invariantGenerator.generateInvariants(aggregate, rootEntity, options);

        results['annotations'] = await this.annotationGenerator.generateValidationAnnotations(aggregate, rootEntity, options);

        const validators = await this.customValidatorGenerator.generateCustomValidators(aggregate, rootEntity, options);
        Object.assign(results, validators);

        return results;
    }
}
