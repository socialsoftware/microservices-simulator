import * as fs from "node:fs/promises";
import * as path from "node:path";
import { GenerationOptions } from "../core/types.js";

export class WebApiFeature {
    static async generateWebApi(
        aggregate: any,
        paths: any,
        options: GenerationOptions,
        generators: any
    ): Promise<void> {
        try {
            const webApiCode = await generators.webApiGenerator.generateWebApi(aggregate, options);
            const webApiPath = path.join(paths.javaPath, 'coordination', 'webapi', `${aggregate.name}Controller.java`);
            await fs.mkdir(path.dirname(webApiPath), { recursive: true });
            await fs.writeFile(webApiPath, webApiCode['controller'], 'utf-8');
            console.log(`\t- Generated web API ${aggregate.name}Controller`);
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

            const utilityServicesCode = await generators.webApiGenerator.generateUtilityServices(options);

            const behaviourServicePath = path.join(paths.javaPath, '..', '..', '..', 'main', 'java', 'pt', 'ulisboa', 'tecnico', 'socialsoftware', 'ms', 'BehaviourService.java');
            await fs.mkdir(path.dirname(behaviourServicePath), { recursive: true });
            await fs.writeFile(behaviourServicePath, utilityServicesCode['behaviour-service'], 'utf-8');
            console.log(`\t- Generated utility service BehaviourService`);

            const tracesServicePath = path.join(paths.javaPath, '..', '..', '..', 'main', 'java', 'pt', 'ulisboa', 'tecnico', 'socialsoftware', 'ms', 'TracesService.java');
            await fs.writeFile(tracesServicePath, utilityServicesCode['traces-service'], 'utf-8');
            console.log(`\t- Generated utility service TracesService`);
        } catch (error) {
            console.error(`\t- Error generating global web API controllers: ${error instanceof Error ? error.message : String(error)}`);
        }
    }
}
