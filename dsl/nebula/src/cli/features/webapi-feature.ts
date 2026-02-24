import chalk from "chalk";
import * as fs from "node:fs/promises";
import * as path from "node:path";
import { GenerationOptions } from "../engine/types.js";

export class WebApiFeature {
    static async generateWebApi(
        aggregate: any,
        aggregatePath: string,
        options: GenerationOptions,
        generators: any,
        allAggregates?: any[]
    ): Promise<void> {
        const hasManualEndpoints = aggregate.webApiEndpoints && aggregate.webApiEndpoints.endpoints.length > 0;
        const hasAutoCrud = aggregate.generateCrud;
        const hasEndpoints = hasManualEndpoints || hasAutoCrud;

        try {
            let controllerCode: string;
            let requestDtos: Record<string, string> | undefined;

            if (hasEndpoints) {
                const webApiCode = await generators.webApiGenerator.generateWebApi(aggregate, options, allAggregates);
                controllerCode = webApiCode['controller'] as string;
                requestDtos = webApiCode['request-dtos'] as Record<string, string>;
            } else {
                controllerCode = await generators.webApiGenerator.generateEmptyController(aggregate, options);
            }

            const webApiPath = path.join(aggregatePath, 'coordination', 'webapi', `${aggregate.name}Controller.java`);
            await fs.mkdir(path.dirname(webApiPath), { recursive: true });
            await fs.writeFile(webApiPath, controllerCode, 'utf-8');

            if (requestDtos && typeof requestDtos === 'object') {
                const dtoKeys = Object.keys(requestDtos);
                if (dtoKeys.length > 0) {
                    const dtosDir = path.join(aggregatePath, 'coordination', 'webapi', 'requestDtos');
                    await fs.mkdir(dtosDir, { recursive: true });

                    for (const [dtoName, dtoContent] of Object.entries(requestDtos)) {
                        if (dtoContent && typeof dtoContent === 'string' && dtoContent.trim()) {
                            const dtoPath = path.join(dtosDir, `${dtoName}.java`);
                            await fs.writeFile(dtoPath, dtoContent, 'utf-8');
                        }
                    }
                }
            }
        } catch (error) {
            console.error(chalk.red(`[ERROR] Error generating web API for ${aggregate.name}: ${error instanceof Error ? error.message : String(error)}`));
        }
    }

    static async generateGlobalWebApi(paths: any, options: GenerationOptions, generators: any): Promise<void> {
        try {
            const globalControllersCode = await generators.webApiGenerator.generateGlobalControllers(options);

            const behaviourControllerPath = path.join(paths.javaPath, 'coordination', 'webapi', 'BehaviourController.java');
            await fs.mkdir(path.dirname(behaviourControllerPath), { recursive: true });
            await fs.writeFile(behaviourControllerPath, globalControllersCode['behaviour-controller'], 'utf-8');

            const tracesControllerPath = path.join(paths.javaPath, 'coordination', 'webapi', 'TracesController.java');
            await fs.writeFile(tracesControllerPath, globalControllersCode['traces-controller'], 'utf-8');
        } catch (error) {
            console.error(chalk.red(`[ERROR] Error generating global web API controllers: ${error instanceof Error ? error.message : String(error)}`));
        }
    }
}
