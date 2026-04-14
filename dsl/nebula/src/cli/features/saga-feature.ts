import chalk from "chalk";
import * as fs from "node:fs/promises";
import * as path from "node:path";
import { GenerationOptions } from "../engine/types.js";
import { AggregatePaths } from "../utils/path-builder.js";

export class SagaFeature {
    static async generateSaga(
        aggregate: any,
        aggregatePath: string,
        options: GenerationOptions,
        generators: any,
        allAggregates?: any[]
    ): Promise<void> {
        try {
            const sagaCode = await generators.sagaGenerator.generateSaga(aggregate, options);
            const paths = new AggregatePaths(aggregatePath, aggregate.name);

            const writes: Array<[string, string]> = [
                [paths.sagaAggregate(),    sagaCode['aggregates']],
                [paths.sagaDto(),          sagaCode['dtos']],
                [paths.sagaState(),        sagaCode['states']],
                [paths.sagaFactory(),      sagaCode['factories']],
                [paths.sagaRepository(),   sagaCode['repositories']],
            ];

            for (const [filePath, content] of writes) {
                await fs.mkdir(path.dirname(filePath), { recursive: true });
                await fs.writeFile(filePath, content, 'utf-8');
            }

            const sagaFunctionalityCode = generators.sagaFunctionalityGenerator.generateForAggregate(aggregate, options, allAggregates);
            for (const [fileName, content] of Object.entries(sagaFunctionalityCode)) {
                const sagaFunctionalityPath = paths.sagaFunctionality(fileName);
                await fs.mkdir(path.dirname(sagaFunctionalityPath), { recursive: true });
                await fs.writeFile(sagaFunctionalityPath, String(content), 'utf-8');
            }
        } catch (error) {
            console.error(chalk.red(`[ERROR] Error generating saga for ${aggregate.name}: ${error instanceof Error ? error.message : String(error)}`));
        }
    }
}
