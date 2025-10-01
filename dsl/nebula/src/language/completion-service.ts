/**
 * Completion Service for Nebula DSL
 */

import { CompletionItem, CompletionList, CompletionParams } from 'vscode-languageserver';
import { LangiumCoreServices } from 'langium';
import { NebulaCompletionProvider } from './completion-provider.js';

export class NebulaCompletionService {
    private completionProvider: NebulaCompletionProvider;

    constructor(services: LangiumCoreServices) {
        this.completionProvider = new NebulaCompletionProvider(services);
    }

    async provideCompletion(params: CompletionParams): Promise<CompletionList> {
        try {
            return await this.completionProvider.provideCompletion(params);
        } catch (error) {
            console.error('Error providing completion:', error);
            return { items: [], isIncomplete: false };
        }
    }

    async provideCompletionResolve(item: CompletionItem): Promise<CompletionItem> {
        // Enhance completion item with additional information
        if (item.detail && !item.documentation) {
            item.documentation = this.generateDocumentation(item);
        }

        return item;
    }

    private generateDocumentation(item: CompletionItem): string {
        const label = item.label;

        // Generate documentation based on completion type
        if (typeof label === 'string') {
            switch (label) {
                case 'aggregate':
                    return `An aggregate is a cluster of related entities that are treated as a unit for data changes. It defines the consistency boundary for your domain model.

Example:
\`\`\`nebula
aggregate OrderManagement {
    entity Order {
        isRoot = true
        // properties and methods
    }
}
\`\`\``;

                case 'entity':
                    return `An entity represents a domain object with identity and lifecycle. It has properties, invariants, business rules, and methods.

Example:
\`\`\`nebula
entity User {
    isRoot = true
    
    Integer id;
    String name;
    String email;
    
    invariants {
        nameNotEmpty: name.length() > 0;
    }
    
    methods {
        updateEmail(String newEmail): User;
    }
}
\`\`\``;

                case 'String':
                    return `String type for text values. Use for names, descriptions, emails, etc.

Example:
\`\`\`nebula
String name;
String email;
String description;
\`\`\``;

                case 'Integer':
                    return `Integer type for whole numbers. Use for IDs, counts, ages, etc.

Example:
\`\`\`nebula
Integer id;
Integer age;
Integer quantity;
\`\`\``;

                case 'Boolean':
                    return `Boolean type for true/false values. Use for flags, status indicators, etc.

Example:
\`\`\`nebula
Boolean active;
Boolean isPublic;
Boolean completed;
\`\`\``;

                case 'LocalDateTime':
                    return `LocalDateTime type for date and time values. Use for timestamps, creation dates, etc.

Example:
\`\`\`nebula
LocalDateTime createdAt;
LocalDateTime updatedAt;
LocalDateTime lastLoginDate;
\`\`\``;

                case 'List<String>':
                    return `List<String> type for collections of strings. Use for tags, categories, etc.

Example:
\`\`\`nebula
List<String> tags;
List<String> categories;
List<String> permissions;
\`\`\``;

                case 'Set<String>':
                    return `Set<String> type for unique collections of strings. Use when duplicates should not be allowed.

Example:
\`\`\`nebula
Set<String> uniqueTags;
Set<String> permissions;
Set<String> roles;
\`\`\``;

                case 'invariants':
                    return `Invariants define conditions that must always be true for an entity. They are automatically validated.

Example:
\`\`\`nebula
invariants {
    nameNotEmpty: name.length() > 0;
    positiveAge: age > 0;
    validEmail: email.contains("@");
}
\`\`\``;

                case 'businessRules':
                    return `Business rules define conditional logic and validation rules that can be violated.

Example:
\`\`\`nebula
businessRules {
    cannotCancelCompleted {
        conditions: ["status == 'COMPLETED'", "cancelled == true"];
        exception: "Cannot cancel a completed order";
    }
}
\`\`\``;

                case 'methods':
                    return `Methods define business operations on entities and aggregates.

Example:
\`\`\`nebula
methods {
    updateEmail(String newEmail): User;
    calculateTotal(): BigDecimal;
    isActive(): Boolean;
}
\`\`\``;

                case 'workflows':
                    return `Workflows define complex business processes and saga patterns.

Example:
\`\`\`nebula
workflows {
    processOrder {
        type: saga;
        parameters: [OrderData data, User user];
        returnType: Order;
    }
}
\`\`\``;

                default:
                    return item.detail || 'No documentation available.';
            }
        }

        return item.detail || 'No documentation available.';
    }
}

// Completion context helpers
export class CompletionContext {
    static isInAggregate(cstNode: any): boolean {
        // Check if we're inside an aggregate definition
        return this.findAncestor(cstNode, 'Aggregate') !== null;
    }

    static isInEntity(cstNode: any): boolean {
        // Check if we're inside an entity definition
        return this.findAncestor(cstNode, 'Entity') !== null;
    }

    static isInProperty(cstNode: any): boolean {
        // Check if we're inside a property definition
        return this.findAncestor(cstNode, 'Property') !== null;
    }

    static isInMethod(cstNode: any): boolean {
        // Check if we're inside a method definition
        return this.findAncestor(cstNode, 'Method') !== null;
    }

    static isInInvariant(cstNode: any): boolean {
        // Check if we're inside an invariant definition
        return this.findAncestor(cstNode, 'Invariant') !== null;
    }

    static isInBusinessRule(cstNode: any): boolean {
        // Check if we're inside a business rule definition
        return this.findAncestor(cstNode, 'BusinessRule') !== null;
    }

    static isInWorkflow(cstNode: any): boolean {
        // Check if we're inside a workflow definition
        return this.findAncestor(cstNode, 'Workflow') !== null;
    }

    private static findAncestor(node: any, type: string): any {
        let current = node;
        while (current) {
            if (current.$type === type) {
                return current;
            }
            current = current.$container;
        }
        return null;
    }

    static getCompletionContext(cstNode: any): string {
        if (this.isInWorkflow(cstNode)) return 'workflow';
        if (this.isInBusinessRule(cstNode)) return 'businessRule';
        if (this.isInInvariant(cstNode)) return 'invariant';
        if (this.isInMethod(cstNode)) return 'method';
        if (this.isInProperty(cstNode)) return 'property';
        if (this.isInEntity(cstNode)) return 'entity';
        if (this.isInAggregate(cstNode)) return 'aggregate';
        return 'general';
    }
}

// Completion utilities
export class CompletionUtils {
    static createSnippetItem(
        label: string,
        insertText: string,
        detail: string,
        documentation?: string,
        kind: any = 'Snippet'
    ): CompletionItem {
        return {
            label,
            kind: kind as any,
            detail,
            documentation,
            insertText,
            insertTextFormat: 2 // Snippet format
        };
    }

    static createKeywordItem(
        label: string,
        detail: string,
        documentation?: string
    ): CompletionItem {
        return {
            label,
            kind: 'Keyword' as any,
            detail,
            documentation,
            insertText: label
        };
    }

    static createTypeItem(
        label: string,
        detail: string,
        documentation?: string
    ): CompletionItem {
        return {
            label,
            kind: 'TypeParameter' as any,
            detail,
            documentation,
            insertText: label
        };
    }

    static createPropertyItem(
        label: string,
        detail: string,
        documentation?: string
    ): CompletionItem {
        return {
            label,
            kind: 'Property' as any,
            detail,
            documentation,
            insertText: label
        };
    }

    static createMethodItem(
        label: string,
        detail: string,
        documentation?: string
    ): CompletionItem {
        return {
            label,
            kind: 'Method' as any,
            detail,
            documentation,
            insertText: label
        };
    }
}
