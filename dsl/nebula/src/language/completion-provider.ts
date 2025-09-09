/**
 * DSL Completion Provider for Nebula
 */

import { CompletionItem, CompletionItemKind, CompletionList, CompletionParams } from 'vscode-languageserver';
import { LangiumCoreServices, URI } from 'langium';

export class NebulaCompletionProvider {
    private services: LangiumCoreServices;

    constructor(services: LangiumCoreServices) {
        this.services = services;
    }

    async provideCompletion(params: CompletionParams): Promise<CompletionList> {
        const document = this.services.shared.workspace.LangiumDocuments.getDocument(URI.parse(params.textDocument.uri));
        if (!document) {
            return { items: [], isIncomplete: false };
        }

        // For now, provide general completions regardless of position
        const items: CompletionItem[] = [];
        items.push(...this.getAggregateCompletions());
        items.push(...this.getEntityCompletions());
        items.push(...this.getGeneralCompletions());

        return { items, isIncomplete: false };
    }

    private getAggregateCompletions(): CompletionItem[] {
        return [
            {
                label: 'entity',
                kind: CompletionItemKind.Keyword,
                detail: 'Define an entity within the aggregate',
                documentation: 'An entity represents a domain object with identity and lifecycle.',
                insertText: 'entity ${1:EntityName} {\n\tisRoot = ${2|true,false|}\n\t\n\t${3:// properties}\n\t\n\t${4:// invariants}\n\t\n\t${5:// business rules}\n\t\n\t${6:// methods}\n}',
                insertTextFormat: 2
            },
            {
                label: 'methods',
                kind: CompletionItemKind.Keyword,
                detail: 'Define aggregate-level methods',
                documentation: 'Methods that operate on the aggregate as a whole.',
                insertText: 'methods {\n\t${1:methodName}(${2:ParamType param}): ${3:ReturnType};\n}',
                insertTextFormat: 2
            },
            {
                label: 'workflows',
                kind: CompletionItemKind.Keyword,
                detail: 'Define workflow patterns',
                documentation: 'Define saga workflows (SAGAS-only mode).',
                insertText: 'workflows {\n\t${1:workflowName} {\n\t\ttype: ${2|saga,compensation|};\n\t\tparameters: [${3:ParamType param}];\n\t\treturnType: ${4:ReturnType};\n\t}\n}',
                insertTextFormat: 2
            }
        ];
    }

    private getEntityCompletions(): CompletionItem[] {
        return [
            {
                label: 'isRoot',
                kind: CompletionItemKind.Property,
                detail: 'Mark entity as root entity',
                documentation: 'Only one entity per aggregate can be marked as root.',
                insertText: 'isRoot = ${1|true,false|}',
                insertTextFormat: 2
            },
            {
                label: 'invariants',
                kind: CompletionItemKind.Keyword,
                detail: 'Define entity invariants',
                documentation: 'Invariants are conditions that must always be true for the entity.',
                insertText: 'invariants {\n\t${1:invariantName}: ${2:condition};\n}',
                insertTextFormat: 2
            },
            {
                label: 'businessRules',
                kind: CompletionItemKind.Keyword,
                detail: 'Define business rules',
                documentation: 'Business rules define conditional logic and validation.',
                insertText: 'businessRules {\n\t${1:ruleName} {\n\t\tconditions: ["${2:condition}"];\n\t\texception: "${3:Error message}";\n\t}\n}',
                insertTextFormat: 2
            },
            {
                label: 'methods',
                kind: CompletionItemKind.Keyword,
                detail: 'Define entity methods',
                documentation: 'Methods that operate on the entity.',
                insertText: 'methods {\n\t${1:methodName}(${2:ParamType param}): ${3:ReturnType};\n}',
                insertTextFormat: 2
            }
        ];
    }

    private getGeneralCompletions(): CompletionItem[] {
        return [
            {
                label: 'aggregate',
                kind: CompletionItemKind.Keyword,
                detail: 'Define an aggregate',
                documentation: 'An aggregate is a cluster of related entities.',
                insertText: 'aggregate ${1:AggregateName} {\n\t${2:// entities, methods, workflows}\n}',
                insertTextFormat: 2
            },
            {
                label: 'entity',
                kind: CompletionItemKind.Keyword,
                detail: 'Define an entity',
                documentation: 'An entity represents a domain object.',
                insertText: 'entity ${1:EntityName} {\n\tisRoot = ${2|true,false|}\n\t\n\t${3:// properties}\n}',
                insertTextFormat: 2
            }
        ];
    }
}
