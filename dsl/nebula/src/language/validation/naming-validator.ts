import type { ValidationAcceptor } from "langium";

export class NamingValidator {
    private readonly reservedWords = new Set([
        'class', 'interface', 'enum', 'package', 'import', 'public', 'private', 'protected',
        'static', 'final', 'abstract', 'extends', 'implements', 'new', 'this', 'super',
        'if', 'else', 'for', 'while', 'do', 'switch', 'case', 'default', 'break', 'continue',
        'return', 'try', 'catch', 'finally', 'throw', 'throws', 'void', 'int', 'long',
        'float', 'double', 'boolean', 'char', 'byte', 'short', 'String', 'Object',
        'List', 'Set', 'Map', 'Collection', 'ArrayList', 'HashMap', 'HashSet'
    ]);

    private readonly javaNamingPattern = /^[a-zA-Z_$][a-zA-Z0-9_$]*$/;

    validateName(name: string, type: string, node: any, accept: ValidationAcceptor): void {
        if (!name || name.trim() === '') {
            accept("error", `${type} name cannot be empty`, {
                node: node,
                property: "name",
            });
            return;
        }

        if (this.reservedWords.has(name.toLowerCase())) {
            accept("error", `'${name}' is a reserved word and cannot be used as ${type} name`, {
                node: node,
                property: "name",
            });
        }

        if (!this.javaNamingPattern.test(name)) {
            accept("error", `Invalid ${type} name '${name}'. Must start with letter or underscore and contain only letters, digits, underscores, and dollar signs`, {
                node: node,
                property: "name",
            });
        }

        if (type === 'entity' || type === 'aggregate') {
            if (name[0] !== name[0].toUpperCase()) {
                accept("warning", `${type} name should start with uppercase letter`, {
                    node: node,
                    property: "name",
                });
            }
        } else if (type === 'property' || type === 'method') {
            if (name[0] !== name[0].toLowerCase()) {
                accept("warning", `${type} name should start with lowercase letter`, {
                    node: node,
                    property: "name",
                });
            }
        }
    }
}

