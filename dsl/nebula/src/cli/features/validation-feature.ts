import { GenerationOptions } from "../engine/types.js";
import { FileWriter } from '../utils/file-writer.js';
import { ErrorHandler, ErrorUtils, ErrorSeverity } from '../utils/error-handler.js';
import * as path from 'node:path';

export class ValidationFeature {

    static async generateValidation(
        aggregate: any,
        paths: any,
        options: GenerationOptions,
        generators: any
    ): Promise<void> {
        if (!generators?.validationGenerator?.generateValidation) {
            return;
        }

        try {
            const validationCode = await generators.validationGenerator.generateValidation(aggregate, {
                projectName: options.projectName
            });

            if (validationCode['invariants']) {
                const invariantsPath = path.join(
                    paths.javaPath,
                    'coordination', 'validation',
                    `${aggregate.name}Invariants.java`
                );
                await FileWriter.writeGeneratedFile(
                    invariantsPath,
                    validationCode['invariants'],
                    `validation ${aggregate.name}Invariants`
                );
            }

            if (validationCode['annotations']) {
                const annotationsPath = path.join(
                    paths.javaPath,
                    'coordination', 'validation',
                    `${aggregate.name}ValidationAnnotations.java`
                );
                await FileWriter.writeGeneratedFile(
                    annotationsPath,
                    validationCode['annotations'],
                    `validation annotations ${aggregate.name}ValidationAnnotations`
                );
            }

            for (const [key, content] of Object.entries(validationCode)) {
                if (key !== 'invariants' && key !== 'annotations' && typeof content === 'string') {
                    const validatorPath = path.join(paths.javaPath, 'coordination', 'validation', key);
                    await FileWriter.writeGeneratedFile(
                        validatorPath,
                        content,
                        `validator ${key}`
                    );
                }
            }
        } catch (error) {
            ErrorHandler.handle(
                error instanceof Error ? error : new Error(String(error)),
                ErrorUtils.aggregateContext(
                    'generate validation',
                    aggregate.name,
                    'validation-feature'
                ),
                ErrorSeverity.ERROR,
                false
            );
        }
    }
}
