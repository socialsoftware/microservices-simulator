import * as fs from "node:fs/promises";
import * as path from "node:path";
import { GenerationOptions } from "../engine/types.js";

export class ValidationFeature {
    static async generateValidation(
        aggregate: any,
        paths: any,
        options: GenerationOptions,
        generators: any
    ): Promise<void> {
        try {
            const validationCode = await generators.validationGenerator.generateValidation(aggregate, options);

            const validationPath = path.join(paths.javaPath, 'coordination', 'validation', `${aggregate.name}Invariants.java`);
            await fs.mkdir(path.dirname(validationPath), { recursive: true });
            await fs.writeFile(validationPath, validationCode['invariants'], 'utf-8');
            console.log(`\t- Generated validation ${aggregate.name}Invariants`);

            if (validationCode['annotations']) {
                const annotationsPath = path.join(paths.javaPath, 'coordination', 'validation', `${aggregate.name}ValidationAnnotations.java`);
                await fs.writeFile(annotationsPath, validationCode['annotations'], 'utf-8');
                console.log(`\t- Generated validation annotations ${aggregate.name}ValidationAnnotations`);
            }

            for (const [key, content] of Object.entries(validationCode)) {
                if (key !== 'invariants' && key !== 'annotations' && typeof content === 'string') {
                    const validatorPath = path.join(paths.javaPath, 'coordination', 'validation', key);
                    await fs.mkdir(path.dirname(validatorPath), { recursive: true });
                    await fs.writeFile(validatorPath, content, 'utf-8');
                    console.log(`\t- Generated validator ${key}`);
                }
            }
        } catch (error) {
            console.error(`\t- Error generating validation for ${aggregate.name}: ${error instanceof Error ? error.message : String(error)}`);
        }
    }

}
