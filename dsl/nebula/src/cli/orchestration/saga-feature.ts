import * as fs from "node:fs/promises";
import * as path from "node:path";
import { GenerationOptions } from "../core/types.js";

export class SagaFeature {
    static async generateCausalEntities(
        aggregate: any,
        paths: any,
        options: GenerationOptions,
        generators: any
    ): Promise<void> {
        try {
            const causalCode = await generators.causalEntityGenerator.generateCausal(aggregate, options);
            const causalDir = path.join(paths.javaPath, 'sagas', 'aggregates', 'causal');
            await fs.mkdir(causalDir, { recursive: true });
            const causalEntityPath = path.join(causalDir, `Causal${aggregate.name}.java`);
            const causalFactoryPath = path.join(causalDir, `Causal${aggregate.name}Factory.java`);
            await fs.writeFile(causalEntityPath, causalCode['entity'], 'utf-8');
            await fs.writeFile(causalFactoryPath, causalCode['factory'], 'utf-8');
            console.log(`\t- Generated causal entity Causal${aggregate.name}`);
            console.log(`\t- Generated causal factory Causal${aggregate.name}Factory`);
        } catch (error) {
            console.error(`\t- Error generating causal entities for ${aggregate.name}: ${error instanceof Error ? error.message : String(error)}`);
        }
    }

    static async generateSaga(
        aggregate: any,
        paths: any,
        options: GenerationOptions,
        generators: any
    ): Promise<void> {
        try {
            const sagaCode = await generators.sagaGenerator.generateSaga(aggregate, options);

            const sagaAggregatesPath = path.join(paths.javaPath, 'sagas', 'aggregates', `Saga${aggregate.name}.java`);
            await fs.mkdir(path.dirname(sagaAggregatesPath), { recursive: true });
            await fs.writeFile(sagaAggregatesPath, sagaCode['aggregates'], 'utf-8');
            console.log(`\t- Generated saga aggregate Saga${aggregate.name}`);

            const sagaDtosPath = path.join(paths.javaPath, 'sagas', 'aggregates', 'dtos', `Saga${aggregate.name}Dto.java`);
            await fs.mkdir(path.dirname(sagaDtosPath), { recursive: true });
            await fs.writeFile(sagaDtosPath, sagaCode['dtos'], 'utf-8');
            console.log(`\t- Generated saga DTO Saga${aggregate.name}Dto`);

            const sagaStatesPath = path.join(paths.javaPath, 'sagas', 'aggregates', 'states', `${aggregate.name}SagaState.java`);
            await fs.mkdir(path.dirname(sagaStatesPath), { recursive: true });
            await fs.writeFile(sagaStatesPath, sagaCode['states'], 'utf-8');
            console.log(`\t- Generated saga state ${aggregate.name}SagaState`);

            const sagaFactoriesPath = path.join(paths.javaPath, 'sagas', 'aggregates', 'factories', `Sagas${aggregate.name}Factory.java`);
            await fs.mkdir(path.dirname(sagaFactoriesPath), { recursive: true });
            await fs.writeFile(sagaFactoriesPath, sagaCode['factories'], 'utf-8');
            console.log(`\t- Generated saga factory Sagas${aggregate.name}Factory`);

            const sagaRepositoriesPath = path.join(paths.javaPath, 'sagas', 'aggregates', 'repositories', `${aggregate.name}CustomRepositorySagas.java`);
            await fs.mkdir(path.dirname(sagaRepositoriesPath), { recursive: true });
            await fs.writeFile(sagaRepositoriesPath, sagaCode['repositories'], 'utf-8');
            console.log(`\t- Generated saga repository ${aggregate.name}CustomRepositorySagas`);

            const sagaCoordinationPath = path.join(paths.javaPath, 'sagas', 'coordination', `${aggregate.name.toLowerCase()}`, `${aggregate.name}SagaCoordination.java`);
            await fs.mkdir(path.dirname(sagaCoordinationPath), { recursive: true });
            await fs.writeFile(sagaCoordinationPath, sagaCode['coordination'], 'utf-8');
            console.log(`\t- Generated saga coordination ${aggregate.name}SagaCoordination`);

            if (sagaCode['workflows']) {
                const workflowsPath = path.join(paths.javaPath, 'sagas', 'coordination', `${aggregate.name.toLowerCase()}`, `${aggregate.name}Workflows.java`);
                await fs.mkdir(path.dirname(workflowsPath), { recursive: true });
                await fs.writeFile(workflowsPath, sagaCode['workflows'], 'utf-8');
                console.log(`\t- Generated saga workflows ${aggregate.name}Workflows`);
            }

            const sagaFunctionalityCode = await generators.sagaFunctionalityGenerator.generateSagaFunctionality(aggregate, options);
            const sagaFunctionalityPath = path.join(paths.javaPath, 'sagas', 'coordination', `${aggregate.name.toLowerCase()}`, `${aggregate.name}SagaFunctionality.java`);
            await fs.mkdir(path.dirname(sagaFunctionalityPath), { recursive: true });
            await fs.writeFile(sagaFunctionalityPath, sagaFunctionalityCode['saga-functionality'], 'utf-8');
            console.log(`\t- Generated saga functionality ${aggregate.name}SagaFunctionality`);
        } catch (error) {
            console.error(`\t- Error generating saga for ${aggregate.name}: ${error instanceof Error ? error.message : String(error)}`);
        }
    }
}
