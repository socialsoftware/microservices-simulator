import chalk from "chalk";
import * as fs from "node:fs/promises";
import * as path from "node:path";
import { GenerationOptions } from "../engine/types.js";

export class CoordinationFeature {
    static async generateCoordination(
        aggregate: any,
        aggregatePath: string,
        options: GenerationOptions,
        generators: any,
        allAggregates?: any[]
    ): Promise<void> {
        try {
            const coordinationCode = await generators.coordinationGenerator.generateCoordination(aggregate, options, allAggregates);

            const coordinationPath = path.join(aggregatePath, 'coordination', 'functionalities', `${aggregate.name}Functionalities.java`);
            await fs.mkdir(path.dirname(coordinationPath), { recursive: true });
            await fs.writeFile(coordinationPath, coordinationCode['functionalities'], 'utf-8');

            if (coordinationCode['event-processing']) {
                const eventProcessingPath = path.join(aggregatePath, 'coordination', 'eventProcessing', `${aggregate.name}EventProcessing.java`);
                await fs.mkdir(path.dirname(eventProcessingPath), { recursive: true });
                await fs.writeFile(eventProcessingPath, coordinationCode['event-processing'], 'utf-8');
            }
        } catch (error) {
            console.error(chalk.red(`[ERROR] Error generating coordination for ${aggregate.name}: ${error instanceof Error ? error.message : String(error)}`));
        }
    }
}
