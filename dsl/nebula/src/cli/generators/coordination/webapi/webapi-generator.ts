import { AggregateExt, TypeGuards } from "../../../types/ast-extensions.js";
import { WebApiGenerationOptions } from "./webapi-types.js";
import { ControllerGenerator } from "./controller-generator.js";
import { WebApiDtoGenerator } from "./dto-generator.js";

export { WebApiGenerationOptions } from "./webapi-types.js";

export class WebApiGenerator {
    private controllerGenerator = new ControllerGenerator();
    private dtoGenerator = new WebApiDtoGenerator();

    async generateWebApi(aggregate: AggregateExt, options: WebApiGenerationOptions, allAggregates?: AggregateExt[]): Promise<{ [key: string]: string | Record<string, string> }> {
        const rootEntity = aggregate.entities.find((e: any) => TypeGuards.isRootEntity(e));
        if (!rootEntity) {
            throw new Error(`No root entity found in aggregate ${aggregate.name}`);
        }

        const results: { [key: string]: string | Record<string, string> } = {};

        results['controller'] = await this.controllerGenerator.generateController(aggregate, rootEntity, options, allAggregates);
        results['request-dtos'] = await this.dtoGenerator.generateRequestDtos(aggregate, rootEntity, options, allAggregates);
        results['response-dtos'] = await this.dtoGenerator.generateResponseDtos(aggregate, rootEntity, options);

        return results;
    }

    async generateEmptyController(aggregate: AggregateExt, options: WebApiGenerationOptions): Promise<string> {
        const rootEntity = aggregate.entities.find((e: any) => TypeGuards.isRootEntity(e));
        if (!rootEntity) {
            throw new Error(`No root entity found in aggregate ${aggregate.name}`);
        }

        return await this.controllerGenerator.generateEmptyController(aggregate, options);
    }

    async generateGlobalControllers(options: { projectName: string; basePackage?: string }): Promise<{ [key: string]: string }> {
        const projectName = options.projectName.toLowerCase();
        const basePackage = options.basePackage || 'pt.ulisboa.tecnico.socialsoftware';
        const packageName = `${basePackage}.${projectName}.coordination.webapi`;

        const behaviourController = `package ${packageName};

import java.io.File;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ${basePackage}.ms.utils.BehaviourService;

@RestController
public class BehaviourController {
    private static final String mavenBaseDir = System.getProperty("maven.basedir", new File(".").getAbsolutePath());

    @Autowired
    private BehaviourService behaviourService;

    @PostMapping("/behaviour/load")
    public String load(@RequestParam String dir) {
        System.out.println("Behaviour load started");
        behaviourService.LoadDir(mavenBaseDir, dir);
        System.out.println("Provided dir: " + dir);
        return "OK";
    }

    @GetMapping(value = "/behaviour/clean")
    public String clean() {
        System.out.println("Report clean started");
        behaviourService.cleanReportFile();
        return "OK";
    }
}
`;

        const tracesController = `package ${packageName};

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import ${basePackage}.ms.utils.TraceService;

@RestController
public class TracesController {
    @Autowired
    private TraceService traceService;

    @GetMapping("/traces/start")
    public String start() {
        System.out.println("Root span started");
        traceService.startRootSpan();
        return "OK";
    }

    @GetMapping(value = "/traces/end")
    public String stop() {
        System.out.println("Stop root span");
        traceService.endRootSpan();
        return "OK";
    }

    @GetMapping(value = "/traces/flush")
    public String flush() {
        System.out.println("Flush root span");
        traceService.spanFlush();
        return "OK";
    }
}
`;

        return {
            'behaviour-controller': behaviourController,
            'traces-controller': tracesController
        };
    }

}
