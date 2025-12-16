import * as fs from "node:fs/promises";
import * as path from "node:path";
import { GenerationOptions } from "../engine/types.js";

export class WebApiFeature {
    static async generateWebApi(
        aggregate: any,
        paths: any,
        options: GenerationOptions,
        generators: any
    ): Promise<void> {
        const hasManualEndpoints = aggregate.webApiEndpoints && aggregate.webApiEndpoints.endpoints.length > 0;
        const hasAutoCrud = aggregate.webApiEndpoints?.autoCrud;
        const hasEndpoints = hasManualEndpoints || hasAutoCrud;

        try {
            let controllerCode: string;

            if (hasEndpoints) {
                const webApiCode = await generators.webApiGenerator.generateWebApi(aggregate, options);
                controllerCode = webApiCode['controller'];
            } else {
                // Generate empty controller
                controllerCode = await generators.webApiGenerator.generateEmptyController(aggregate, options);
            }

            const webApiPath = path.join(paths.javaPath, 'coordination', 'webapi', `${aggregate.name}Controller.java`);
            await fs.mkdir(path.dirname(webApiPath), { recursive: true });
            await fs.writeFile(webApiPath, controllerCode, 'utf-8');

            if (hasEndpoints) {
                const endpointCount = hasAutoCrud ? 4 : aggregate.webApiEndpoints.endpoints.length;
                const crudNote = hasAutoCrud ? ' (CRUD auto-generated)' : '';
                console.log(`\t- Generated web API ${aggregate.name}Controller (using Functionalities, ${endpointCount} endpoints${crudNote})`);
            } else {
                console.log(`\t- Generated empty ${aggregate.name}Controller (no WebAPIEndpoints defined)`);
            }
        } catch (error) {
            console.error(`\t- Error generating web API for ${aggregate.name}: ${error instanceof Error ? error.message : String(error)}`);
        }
    }

    static async generateGlobalWebApi(paths: any, options: GenerationOptions, generators: any): Promise<void> {
        try {
            const globalControllersCode = await generators.webApiGenerator.generateGlobalControllers(options);

            const behaviourControllerPath = path.join(paths.javaPath, 'coordination', 'webapi', 'BehaviourController.java');
            await fs.mkdir(path.dirname(behaviourControllerPath), { recursive: true });
            await fs.writeFile(behaviourControllerPath, globalControllersCode['behaviour-controller'], 'utf-8');
            console.log(`\t- Generated global web API BehaviourController`);

            const tracesControllerPath = path.join(paths.javaPath, 'coordination', 'webapi', 'TracesController.java');
            await fs.writeFile(tracesControllerPath, globalControllersCode['traces-controller'], 'utf-8');
            console.log(`\t- Generated global web API TracesController`);
        } catch (error) {
            console.error(`\t- Error generating global web API controllers: ${error instanceof Error ? error.message : String(error)}`);
        }
    }
}
