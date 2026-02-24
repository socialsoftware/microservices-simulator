import { Aggregate } from "../../../../language/generated/ast.js";
import { getGlobalConfig } from "../../common/config.js";
import { StringUtils } from "../../../utils/string-utils.js";

interface HandlerCase {
    commandClass: string;
    handlerMethod: string;
    serviceMethod: string;
    serviceArgs: string;
    hasResult: boolean;
}

export class CommandHandlerGenerator {
    generate(aggregate: Aggregate, options: { projectName: string; basePackage?: string }): Record<string, string> {
        const results: Record<string, string> = {};
        const basePackage = options.basePackage || getGlobalConfig().getBasePackage();
        const projectName = options.projectName.toLowerCase();
        const aggregateName = aggregate.name;
        const capitalizedAggregate = StringUtils.capitalize(aggregateName);
        const lowerAggregate = aggregateName.toLowerCase();

        const commandHandlerPackage = `${basePackage}.${projectName}.microservices.${lowerAggregate}.commandHandler`;
        const commandPackage = `${basePackage}.${projectName}.command.${lowerAggregate}`;
        const servicePackage = `${basePackage}.${projectName}.microservices.${lowerAggregate}.service`;

        const cases = this.buildCases(aggregate, capitalizedAggregate, lowerAggregate);

        results[`${capitalizedAggregate}CommandHandler.java`] = this.generateCommandHandler(
            commandHandlerPackage, commandPackage, servicePackage,
            capitalizedAggregate, lowerAggregate, cases, basePackage
        );

        results[`${capitalizedAggregate}StreamCommandHandler.java`] = this.generateStreamCommandHandler(
            commandHandlerPackage, capitalizedAggregate, lowerAggregate, basePackage
        );

        return results;
    }

    private buildCases(aggregate: Aggregate, capitalizedAggregate: string, lowerAggregate: string): HandlerCase[] {
        const cases: HandlerCase[] = [];

        if (aggregate.generateCrud) {
            cases.push({
                commandClass: `Create${capitalizedAggregate}Command`,
                handlerMethod: `handleCreate${capitalizedAggregate}`,
                serviceMethod: `create${capitalizedAggregate}`,
                serviceArgs: 'cmd.getCreateRequest(), cmd.getUnitOfWork()',
                hasResult: true
            });
            cases.push({
                commandClass: `Get${capitalizedAggregate}ByIdCommand`,
                handlerMethod: `handleGet${capitalizedAggregate}ById`,
                serviceMethod: `get${capitalizedAggregate}ById`,
                serviceArgs: 'cmd.getRootAggregateId(), cmd.getUnitOfWork()',
                hasResult: true
            });
            cases.push({
                commandClass: `GetAll${capitalizedAggregate}sCommand`,
                handlerMethod: `handleGetAll${capitalizedAggregate}s`,
                serviceMethod: `getAll${capitalizedAggregate}s`,
                serviceArgs: 'cmd.getUnitOfWork()',
                hasResult: true
            });
            cases.push({
                commandClass: `Update${capitalizedAggregate}Command`,
                handlerMethod: `handleUpdate${capitalizedAggregate}`,
                serviceMethod: `update${capitalizedAggregate}`,
                serviceArgs: `cmd.get${capitalizedAggregate}Dto(), cmd.getUnitOfWork()`,
                hasResult: true
            });
            cases.push({
                commandClass: `Delete${capitalizedAggregate}Command`,
                handlerMethod: `handleDelete${capitalizedAggregate}`,
                serviceMethod: `delete${capitalizedAggregate}`,
                serviceArgs: 'cmd.getRootAggregateId(), cmd.getUnitOfWork()',
                hasResult: false
            });
        }

        return cases;
    }

    private generateCommandHandler(
        packageName: string,
        commandPackage: string,
        servicePackage: string,
        capitalizedAggregate: string,
        lowerAggregate: string,
        cases: HandlerCase[],
        basePackage: string
    ): string {
        const className = `${capitalizedAggregate}CommandHandler`;

        const switchCases = cases.map(c =>
            `            case ${c.commandClass} cmd -> ${c.handlerMethod}(cmd);`
        ).join('\n');

        const handlerMethods = cases.map(c => {
            const returnStmt = c.hasResult
                ? `return ${lowerAggregate}Service.${c.serviceMethod}(${c.serviceArgs});`
                : `${lowerAggregate}Service.${c.serviceMethod}(${c.serviceArgs});\n            return null;`;
            return `    private Object ${c.handlerMethod}(${c.commandClass} cmd) {
        logger.info("${c.handlerMethod}");
        try {
            ${returnStmt}
        } catch (Exception e) {
            logger.severe("Failed: " + e.getMessage());
            return e;
        }
    }`;
        }).join('\n\n');

        return `package ${packageName};

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ${basePackage}.ms.coordination.workflow.Command;
import ${basePackage}.ms.coordination.workflow.CommandHandler;
import ${commandPackage}.*;
import ${servicePackage}.${capitalizedAggregate}Service;

import java.util.logging.Logger;

@Component
public class ${className} extends CommandHandler {
    private static final Logger logger = Logger.getLogger(${className}.class.getName());

    @Autowired
    private ${capitalizedAggregate}Service ${lowerAggregate}Service;

    @Override
    protected String getAggregateTypeName() {
        return "${capitalizedAggregate}";
    }

    @Override
    protected Object handleDomainCommand(Command command) {
        return switch (command) {
${switchCases}
            default -> {
                logger.warning("Unknown command type: " + command.getClass().getName());
                yield null;
            }
        };
    }

${handlerMethods}
}
`;
    }

    private generateStreamCommandHandler(
        packageName: string,
        capitalizedAggregate: string,
        lowerAggregate: string,
        basePackage: string
    ): string {
        const className = `${capitalizedAggregate}StreamCommandHandler`;
        const commandHandlerClass = `${capitalizedAggregate}CommandHandler`;
        const commandHandlerField = `${lowerAggregate}CommandHandler`;

        return `package ${packageName};

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;
import ${basePackage}.ms.coordination.workflow.Command;
import ${basePackage}.ms.coordination.workflow.stream.MessagingObjectMapperProvider;
import ${basePackage}.ms.coordination.workflow.stream.StreamCommandHandler;

import java.util.function.Consumer;

@Component
@Profile("stream")
public class ${className} extends StreamCommandHandler {

    private final ${commandHandlerClass} ${commandHandlerField};

    @Autowired
    public ${className}(StreamBridge streamBridge,
            ${commandHandlerClass} ${commandHandlerField},
            MessagingObjectMapperProvider mapperProvider) {
        super(streamBridge, mapperProvider);
        this.${commandHandlerField} = ${commandHandlerField};
    }

    @Override
    protected String getAggregateTypeName() {
        return "${capitalizedAggregate}";
    }

    @Override
    protected Object handleDomainCommand(Command command) {
        return ${commandHandlerField}.handleDomainCommand(command);
    }

    @Bean
    public Consumer<Message<?>> ${lowerAggregate}ServiceCommandChannel() {
        return this::handleCommandMessage;
    }
}
`;
    }
}
