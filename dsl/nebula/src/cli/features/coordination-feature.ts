import * as fs from "node:fs/promises";
import * as path from "node:path";
import { GenerationOptions } from "../engine/types.js";

export class CoordinationFeature {
    static async generateCoordination(
        aggregate: any,
        paths: any,
        options: GenerationOptions,
        generators: any,
        allAggregates?: any[]
    ): Promise<void> {
        try {
            const coordinationCode = await generators.coordinationGenerator.generateCoordination(aggregate, options, allAggregates);

            const coordinationPath = path.join(paths.javaPath, 'coordination', 'functionalities', `${aggregate.name}Functionalities.java`);
            await fs.mkdir(path.dirname(coordinationPath), { recursive: true });
            await fs.writeFile(coordinationPath, coordinationCode['functionalities'], 'utf-8');
            console.log(`\t- Generated coordination ${aggregate.name}Functionalities`);

            if (coordinationCode['event-processing']) {
                const eventProcessingPath = path.join(paths.javaPath, 'coordination', 'eventProcessing', `${aggregate.name}EventProcessing.java`);
                await fs.mkdir(path.dirname(eventProcessingPath), { recursive: true });
                await fs.writeFile(eventProcessingPath, coordinationCode['event-processing'], 'utf-8');
                console.log(`\t- Generated event processing ${aggregate.name}EventProcessing`);
            }

            try {
                const { SagaFunctionalityGenerator } = await import('../generators/sagas/saga-functionality-generator.js');
                const sagaGen = new SagaFunctionalityGenerator();
                const sagaFiles = sagaGen.generateForAggregate(aggregate, { projectName: options.projectName! });
                const sagaDir = path.join(paths.javaPath, 'sagas', 'coordination', aggregate.name.toLowerCase());
                await fs.mkdir(sagaDir, { recursive: true });
                for (const [fileName, content] of Object.entries(sagaFiles)) {
                    const outPath = path.join(sagaDir, fileName);
                    await fs.writeFile(outPath, content, 'utf-8');
                    console.log(`\t- Generated saga functionality ${fileName}`);
                }
            } catch (err) {
                console.log(`\t- Skipped saga functionality generation: ${err instanceof Error ? err.message : String(err)}`);
            }
        } catch (error) {
            console.error(`\t- Error generating coordination for ${aggregate.name}: ${error instanceof Error ? error.message : String(error)}`);
        }
    }
}
