import { GenerationOptions } from "../engine/types.js";
import { FeatureBase, FeatureResult } from "./feature-base.js";

export class ValidationFeature extends FeatureBase {
    constructor() {
        super('validation');
    }

    /**
     * Generate validation files for an aggregate
     */
    async generate(
        aggregate: any,
        paths: any,
        options: GenerationOptions,
        generators: any
    ): Promise<FeatureResult> {
        return this.executeFeatureGeneration(aggregate.name, async () => {
            if (!this.hasGenerator(generators, 'validationGenerator')) {
                return this.createErrorResult(['Validation generator not available']);
            }

            const validationCode = await this.safeGeneratorCall(
                generators.validationGenerator,
                [aggregate, options],
                'validationGenerator'
            );

            let filesGenerated = 0;
            const warnings: string[] = [];

            // Create validation directory
            const validationDir = this.buildPackagePath(paths.javaPath, 'coordination', 'validation');
            await this.createDirectory(validationDir, 'validation directory');

            // Generate main invariants file
            if (validationCode['invariants']) {
                const invariantsPath = this.buildJavaFilePath(
                    paths.javaPath,
                    ['coordination', 'validation'],
                    `${aggregate.name}Invariants`
                );
                await this.writeGeneratedFile(
                    invariantsPath,
                    validationCode['invariants'],
                    `validation ${aggregate.name}Invariants`
                );
                filesGenerated++;
            } else {
                warnings.push('No invariants generated');
            }

            // Generate validation annotations if available
            if (validationCode['annotations']) {
                const annotationsPath = this.buildJavaFilePath(
                    paths.javaPath,
                    ['coordination', 'validation'],
                    `${aggregate.name}ValidationAnnotations`
                );
                await this.writeGeneratedFile(
                    annotationsPath,
                    validationCode['annotations'],
                    `validation annotations ${aggregate.name}ValidationAnnotations`
                );
                filesGenerated++;
            }

            // Generate additional validators
            for (const [key, content] of Object.entries(validationCode)) {
                if (key !== 'invariants' && key !== 'annotations' && typeof content === 'string') {
                    const validatorPath = this.buildPackagePath(paths.javaPath, 'coordination', 'validation', key);
                    await this.writeGeneratedFile(
                        validatorPath,
                        content,
                        `validator ${key}`
                    );
                    filesGenerated++;
                }
            }

            return this.createSuccessResult(filesGenerated, warnings);
        });
    }

    /**
     * Static method for backward compatibility
     */
    static async generateValidation(
        aggregate: any,
        paths: any,
        options: GenerationOptions,
        generators: any
    ): Promise<void> {
        const feature = new ValidationFeature();
        const result = await feature.generate(aggregate, paths, options, generators);

        if (!result.success) {
            throw new Error(`Validation generation failed: ${result.errors.join(', ')}`);
        }
    }
}
