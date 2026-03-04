import chalk from "chalk";
import * as fs from "node:fs/promises";
import * as path from "node:path";
import { GenerationOptions } from "../engine/types.js";

export class SagaFeature {
    static async generateCausalEntities(
        aggregate: any,
        aggregatePath: string,
        options: GenerationOptions,
        generators: any
    ): Promise<void> {
        try {
            const causalCode = await generators.causalEntityGenerator.generateCausal(aggregate, options);
            const causalDir = path.join(aggregatePath, 'aggregate', 'causal');
            await fs.mkdir(causalDir, { recursive: true });
            const causalEntityPath = path.join(causalDir, `Causal${aggregate.name}.java`);
            const causalFactoryPath = path.join(causalDir, `Causal${aggregate.name}Factory.java`);
            await fs.writeFile(causalEntityPath, causalCode['entity'], 'utf-8');
            await fs.writeFile(causalFactoryPath, causalCode['factory'], 'utf-8');
        } catch (error) {
            console.error(chalk.red(`[ERROR] Error generating causal entities for ${aggregate.name}: ${error instanceof Error ? error.message : String(error)}`));
        }
    }

    static async generateSaga(
        aggregate: any,
        aggregatePath: string,
        options: GenerationOptions,
        generators: any,
        allAggregates?: any[]
    ): Promise<void> {
        try {
            const sagaCode = await generators.sagaGenerator.generateSaga(aggregate, options);

            const sagaAggregatesPath = path.join(aggregatePath, 'aggregate', 'sagas', `Saga${aggregate.name}.java`);
            await fs.mkdir(path.dirname(sagaAggregatesPath), { recursive: true });
            await fs.writeFile(sagaAggregatesPath, sagaCode['aggregates'], 'utf-8');

            const sagaDtosPath = path.join(aggregatePath, 'aggregate', 'sagas', 'dtos', `Saga${aggregate.name}Dto.java`);
            await fs.mkdir(path.dirname(sagaDtosPath), { recursive: true });
            await fs.writeFile(sagaDtosPath, sagaCode['dtos'], 'utf-8');

            const sagaStatesPath = path.join(aggregatePath, 'aggregate', 'sagas', 'states', `${aggregate.name}SagaState.java`);
            await fs.mkdir(path.dirname(sagaStatesPath), { recursive: true });
            await fs.writeFile(sagaStatesPath, sagaCode['states'], 'utf-8');

            const sagaFactoriesPath = path.join(aggregatePath, 'aggregate', 'sagas', 'factories', `Sagas${aggregate.name}Factory.java`);
            await fs.mkdir(path.dirname(sagaFactoriesPath), { recursive: true });
            await fs.writeFile(sagaFactoriesPath, sagaCode['factories'], 'utf-8');

            const sagaRepositoriesPath = path.join(aggregatePath, 'aggregate', 'sagas', 'repositories', `${aggregate.name}CustomRepositorySagas.java`);
            await fs.mkdir(path.dirname(sagaRepositoriesPath), { recursive: true });
            await fs.writeFile(sagaRepositoriesPath, sagaCode['repositories'], 'utf-8');

            const sagaFunctionalityCode = generators.sagaFunctionalityGenerator.generateForAggregate(aggregate, options, allAggregates);
            for (const [fileName, content] of Object.entries(sagaFunctionalityCode)) {
                const sagaFunctionalityPath = path.join(aggregatePath, 'coordination', 'sagas', fileName);
                await fs.mkdir(path.dirname(sagaFunctionalityPath), { recursive: true });
                await fs.writeFile(sagaFunctionalityPath, String(content), 'utf-8');
            }
        } catch (error) {
            console.error(chalk.red(`[ERROR] Error generating saga for ${aggregate.name}: ${error instanceof Error ? error.message : String(error)}`));
        }
    }
}
