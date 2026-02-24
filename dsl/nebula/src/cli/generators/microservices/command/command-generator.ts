import { Aggregate, Entity } from "../../../../language/generated/ast.js";
import { getGlobalConfig } from "../../common/config.js";
import { StringUtils } from "../../../utils/string-utils.js";
import { CollectionMetadataBuilder, CollectionMetadata } from "../../common/utils/collection-metadata-builder.js";

interface CommandInfo {
    className: string;
    params: Array<{ type: string; name: string }>;
    aggregateIdParam: string;
    extraImports?: string[];
}

export class CommandGenerator {
    generate(aggregate: Aggregate, options: { projectName: string; basePackage?: string }): Record<string, string> {
        const results: Record<string, string> = {};
        const basePackage = options.basePackage || getGlobalConfig().getBasePackage();
        const projectName = options.projectName.toLowerCase();
        const aggregateName = aggregate.name;
        const capitalizedAggregate = StringUtils.capitalize(aggregateName);
        const lowerAggregate = aggregateName.toLowerCase();
        const packageName = `${basePackage}.${projectName}.command.${lowerAggregate}`;

        const rootEntity = aggregate.entities.find((e: any) => e.isRoot);
        const rootEntityName = rootEntity ? rootEntity.name : aggregateName;
        const dtoType = `${rootEntityName}Dto`;
        const createRequestDtoType = `Create${capitalizedAggregate}RequestDto`;

        const commands = this.buildCommandList(aggregate, capitalizedAggregate, lowerAggregate, dtoType, createRequestDtoType, rootEntity as Entity);

        for (const cmd of commands) {
            const imports = this.buildImports(cmd, basePackage, projectName, lowerAggregate, capitalizedAggregate, rootEntityName);
            const code = this.generateCommandClass(packageName, imports, cmd);
            results[`${cmd.className}.java`] = code;
        }

        return results;
    }

    private buildCommandList(aggregate: Aggregate, capitalizedAggregate: string, lowerAggregate: string, dtoType: string, createRequestDtoType: string, rootEntity: Entity): CommandInfo[] {
        const commands: CommandInfo[] = [];

        if (aggregate.generateCrud) {
            commands.push({
                className: `Create${capitalizedAggregate}Command`,
                params: [{ type: createRequestDtoType, name: 'createRequest' }],
                aggregateIdParam: 'null'
            });
            commands.push({
                className: `Get${capitalizedAggregate}ByIdCommand`,
                params: [],
                aggregateIdParam: 'aggregateId'
            });
            commands.push({
                className: `GetAll${capitalizedAggregate}sCommand`,
                params: [],
                aggregateIdParam: 'null'
            });
            commands.push({
                className: `Update${capitalizedAggregate}Command`,
                params: [{ type: dtoType, name: `${lowerAggregate}Dto` }],
                aggregateIdParam: 'null'
            });
            commands.push({
                className: `Delete${capitalizedAggregate}Command`,
                params: [],
                aggregateIdParam: 'aggregateId'
            });
        }

        if (rootEntity) {
            const collections = CollectionMetadataBuilder.extractCollections(aggregate, rootEntity);
            for (const col of collections) {
                this.buildCollectionCommands(commands, capitalizedAggregate, lowerAggregate, col);
            }
        }

        return commands;
    }

    private buildCollectionCommands(commands: CommandInfo[], capitalizedAggregate: string, lowerAggregate: string, col: CollectionMetadata): void {
        const elementDtoType = col.elementDtoType;
        const lowerSingular = col.singularName;
        const capitalSingular = col.capitalizedSingular;
        const capitalCollection = col.capitalizedCollection;
        const identifierField = col.identifierField;

        // Add{Aggregate}{Element}Command - params: aggregateId, identifierField, elementDto
        commands.push({
            className: `Add${capitalizedAggregate}${capitalSingular}Command`,
            params: [
                { type: 'Integer', name: `${lowerAggregate}Id` },
                { type: 'Integer', name: identifierField },
                { type: elementDtoType, name: `${lowerSingular}Dto` }
            ],
            aggregateIdParam: 'null'
        });

        commands.push({
            className: `Add${capitalizedAggregate}${capitalCollection}Command`,
            params: [
                { type: 'Integer', name: `${lowerAggregate}Id` },
                { type: `List<${elementDtoType}>`, name: `${lowerSingular}Dtos` }
            ],
            aggregateIdParam: 'null',
            extraImports: ['import java.util.List;']
        });

        commands.push({
            className: `Get${capitalizedAggregate}${capitalSingular}Command`,
            params: [
                { type: 'Integer', name: `${lowerAggregate}Id` },
                { type: 'Integer', name: identifierField }
            ],
            aggregateIdParam: 'null'
        });

        commands.push({
            className: `Update${capitalizedAggregate}${capitalSingular}Command`,
            params: [
                { type: 'Integer', name: `${lowerAggregate}Id` },
                { type: 'Integer', name: identifierField },
                { type: elementDtoType, name: `${lowerSingular}Dto` }
            ],
            aggregateIdParam: 'null'
        });

        commands.push({
            className: `Remove${capitalizedAggregate}${capitalSingular}Command`,
            params: [
                { type: 'Integer', name: `${lowerAggregate}Id` },
                { type: 'Integer', name: identifierField }
            ],
            aggregateIdParam: 'null'
        });
    }

    private buildImports(cmd: CommandInfo, basePackage: string, projectName: string, lowerAggregate: string, capitalizedAggregate: string, rootEntityName: string): string[] {
        const imports: string[] = [];
        imports.push(`import ${basePackage}.ms.coordination.unitOfWork.UnitOfWork;`);
        imports.push(`import ${basePackage}.ms.coordination.workflow.Command;`);

        const dtoTypes = new Set<string>();
        for (const p of cmd.params) {
            const baseType = p.type.startsWith('List<') ? p.type.slice(5, -1) : p.type;
            if (baseType.endsWith('Dto') || baseType.endsWith('RequestDto')) {
                dtoTypes.add(baseType);
            }
        }

        for (const dt of dtoTypes) {
            if (dt.startsWith('Create') && dt.endsWith('RequestDto')) {
                imports.push(`import ${basePackage}.${projectName}.microservices.${lowerAggregate}.coordination.webapi.requestDtos.${dt};`);
            } else {
                imports.push(`import ${basePackage}.${projectName}.shared.dtos.${dt};`);
            }
        }

        if (cmd.extraImports) {
            for (const extra of cmd.extraImports) {
                if (!imports.includes(extra)) {
                    imports.push(extra);
                }
            }
        }

        return imports;
    }

    private generateCommandClass(packageName: string, imports: string[], cmd: CommandInfo): string {
        const allParams = cmd.params;
        const constructorParams = [
            'UnitOfWork unitOfWork',
            'String serviceName',
            ...(cmd.aggregateIdParam !== 'null' ? ['Integer aggregateId'] : []),
            ...allParams.map(p => `${p.type} ${p.name}`)
        ];

        const superArgs = cmd.aggregateIdParam !== 'null'
            ? 'unitOfWork, serviceName, aggregateId'
            : 'unitOfWork, serviceName, null';

        const fieldDeclarations = allParams.map(p =>
            `    private final ${p.type} ${p.name};`
        ).join('\n');

        const fieldAssignments = allParams.map(p =>
            `        this.${p.name} = ${p.name};`
        ).join('\n');

        const getters = allParams.map(p => {
            const capitalizedName = StringUtils.capitalize(p.name);
            return `    public ${p.type} get${capitalizedName}() { return ${p.name}; }`;
        }).join('\n');

        return `package ${packageName};

${imports.join('\n')}

public class ${cmd.className} extends Command {
${fieldDeclarations}

    public ${cmd.className}(${constructorParams.join(', ')}) {
        super(${superArgs});
${fieldAssignments}
    }

${getters}
}
`;
    }
}
