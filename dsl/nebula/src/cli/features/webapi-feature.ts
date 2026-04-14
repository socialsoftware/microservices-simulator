import chalk from "chalk";
import * as fs from "node:fs/promises";
import * as path from "node:path";
import { GenerationOptions } from "../engine/types.js";
import { AggregatePaths, ProjectPaths } from "../utils/path-builder.js";

export class WebApiFeature {
    static async generateWebApi(
        aggregate: any,
        aggregatePath: string,
        options: GenerationOptions,
        generators: any,
        allAggregates?: any[]
    ): Promise<void> {
        const hasManualEndpoints = aggregate.webApiEndpoints && aggregate.webApiEndpoints.endpoints.length > 0;
        const hasAutoCrud = (aggregate as any).generateCrud;
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

            const paths = new AggregatePaths(aggregatePath, aggregate.name);
            const webApiPath = paths.controller();
            await fs.mkdir(path.dirname(webApiPath), { recursive: true });
            await fs.writeFile(webApiPath, controllerCode, 'utf-8');

            if (requestDtos && typeof requestDtos === 'object') {
                const dtoKeys = Object.keys(requestDtos);
                if (dtoKeys.length > 0) {
                    const dtosDir = paths.requestDtosDir();
                    await fs.mkdir(dtosDir, { recursive: true });

                    for (const [dtoName, dtoContent] of Object.entries(requestDtos)) {
                        if (dtoContent && typeof dtoContent === 'string' && dtoContent.trim()) {
                            await fs.writeFile(paths.requestDto(dtoName), dtoContent, 'utf-8');
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
            const projectPaths = new ProjectPaths(paths.javaPath);

            const behaviourControllerPath = projectPaths.behaviourController();
            await fs.mkdir(path.dirname(behaviourControllerPath), { recursive: true });
            await fs.writeFile(behaviourControllerPath, globalControllersCode['behaviour-controller'], 'utf-8');

            await fs.writeFile(projectPaths.tracesController(), globalControllersCode['traces-controller'], 'utf-8');
        } catch (error) {
            console.error(chalk.red(`[ERROR] Error generating global web API controllers: ${error instanceof Error ? error.message : String(error)}`));
        }
    }
}
