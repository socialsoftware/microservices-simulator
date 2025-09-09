import * as fs from "node:fs/promises";
import * as path from "node:path";
import { GenerationOptions } from "../core/types.js";

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
        } catch (error) {
            console.error(`\t- Error generating coordination for ${aggregate.name}: ${error instanceof Error ? error.message : String(error)}`);
        }
    }
}
