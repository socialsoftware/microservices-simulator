export interface JpqlAst {
    select: SelectClause;
    from: FromClause;
    where?: WhereClause;
}

export interface SelectClause {
    items: SelectItem[];
}

export interface SelectItem {
    type: 'property' | 'aggregate';
    path: PropertyPath;
}

export interface FromClause {
    entity: string;
    alias?: string;
}

export interface WhereClause {
    condition: Condition;
}

export interface Condition {
    type: 'and' | 'or' | 'comparison' | 'grouped';
    left?: Condition;
    right?: Condition;
    comparison?: Comparison;
}

export interface Comparison {
    left: PropertyPath;
    operator: string;
    right: PropertyPath | Parameter | Literal;
}

export interface PropertyPath {
    alias?: string;
    properties: string[];
}

export interface Parameter {
    type: 'parameter';
    name: string;
}

export interface Literal {
    type: 'string' | 'number' | 'null';
    value: string | number;
}

export interface ParseError {
    message: string;
    position?: number;
}

export interface ParseResult {
    isValid: boolean;
    errors: ParseError[];
    ast?: JpqlAst;
    aliases: Map<string, string>;
}

export class JpqlParser {
    private tokens: string[] = [];
    private current = 0;
    private query: string = '';

    parse(query: string): ParseResult {
        this.query = query.trim();
        if ((this.query.startsWith('"') && this.query.endsWith('"')) ||
            (this.query.startsWith("'") && this.query.endsWith("'"))) {
            this.query = this.query.slice(1, -1);
        }

        this.tokens = this.tokenize(this.query);
        this.current = 0;

        const errors: ParseError[] = [];
        const aliases = new Map<string, string>();

        try {
            const ast = this.parseQuery(aliases);
            return {
                isValid: true,
                errors: [],
                ast,
                aliases
            };
        } catch (error: any) {
            errors.push({
                message: error.message || 'Parse error',
                position: this.current
            });
            return {
                isValid: false,
                errors,
                aliases
            };
        }
    }

    private tokenize(query: string): string[] {
        const tokens: string[] = [];
        let i = 0;
        let current = '';

        while (i < query.length) {
            const char = query[i];

            if (/\s/.test(char)) {
                if (current) {
                    tokens.push(current);
                    current = '';
                }
                i++;
                continue;
            }

            if (char === "'") {
                if (current) {
                    tokens.push(current);
                    current = '';
                }
                let str = char;
                i++;
                while (i < query.length && query[i] !== "'") {
                    if (query[i] === '\\' && i + 1 < query.length) {
                        str += query[i] + query[i + 1];
                        i += 2;
                    } else {
                        str += query[i];
                        i++;
                    }
                }
                if (i < query.length) {
                    str += query[i];
                    i++;
                }
                tokens.push(str);
                continue;
            }

            if (['(', ')', '.', ',', ';'].includes(char)) {
                if (current) {
                    tokens.push(current);
                    current = '';
                }
                tokens.push(char);
                i++;
                continue;
            }

            if (['=', '!', '<', '>'].includes(char)) {
                if (current) {
                    tokens.push(current);
                    current = '';
                }
                if (i + 1 < query.length) {
                    const twoChar = char + query[i + 1];
                    if (['==', '!=', '<=', '>='].includes(twoChar)) {
                        tokens.push(twoChar);
                        i += 2;
                        continue;
                    }
                }
                tokens.push(char);
                i++;
                continue;
            }

            if (char === ':') {
                if (current) {
                    tokens.push(current);
                    current = '';
                }
                let param = ':';
                i++;
                while (i < query.length && /[a-zA-Z0-9_]/.test(query[i])) {
                    param += query[i];
                    i++;
                }
                tokens.push(param);
                continue;
            }

            current += char;
            i++;
        }

        if (current) {
            tokens.push(current);
        }

        return tokens;
    }

    private parseQuery(aliases: Map<string, string>): JpqlAst {
        const select = this.parseSelect();
        const from = this.parseFrom(aliases);
        const where = this.peek() && this.match('WHERE') ? this.parseWhere() : undefined;

        if (this.current < this.tokens.length) {
            throw new Error(`Unexpected token: ${this.peek()}`);
        }

        return { select, from, where };
    }

    private parseSelect(): SelectClause {
        if (!this.match('SELECT')) {
            throw new Error('Expected SELECT clause');
        }

        const items: SelectItem[] = [];
        do {
            if (items.length > 0) {
                this.consume(',');
            }
            items.push(this.parseSelectItem());
        } while (this.check(','));

        return { items };
    }

    private parseSelectItem(): SelectItem {
        const aggregateFunctions = ['COUNT', 'SUM', 'AVG', 'MAX', 'MIN'];
        const token = this.peek()?.toUpperCase();

        if (token && aggregateFunctions.includes(token)) {
            this.advance();
            this.consume('(');
            const path = this.parsePropertyPath();
            this.consume(')');
            return { type: 'aggregate', path };
        }

        return { type: 'property', path: this.parsePropertyPath() };
    }

    private parseFrom(aliases: Map<string, string>): FromClause {
        if (!this.match('FROM')) {
            throw new Error('Expected FROM clause');
        }

        const entity = this.consumeIdentifier();
        const alias = this.checkIdentifier() ? this.consumeIdentifier() : undefined;

        if (alias) {
            aliases.set(alias, entity);
        } else {
            const defaultAlias = entity.charAt(0).toLowerCase();
            aliases.set(defaultAlias, entity);
        }

        return { entity, alias };
    }

    private parseWhere(): WhereClause {
        return { condition: this.parseCondition() };
    }

    private parseCondition(): Condition {
        let condition = this.parseComparisonOrGrouped();

        while (this.check('AND') || this.check('OR')) {
            const operator = this.advance()!.toLowerCase() as 'and' | 'or';
            const right = this.parseComparisonOrGrouped();
            condition = {
                type: operator,
                left: condition,
                right
            };
        }

        return condition;
    }

    private parseComparisonOrGrouped(): Condition {
        if (this.match('(')) {
            const condition = this.parseCondition();
            this.consume(')');
            return { type: 'grouped', comparison: undefined, left: condition };
        }

        return { type: 'comparison', comparison: this.parseComparison() };
    }

    private parseComparison(): Comparison {
        const left = this.parsePropertyPath();
        const operator = this.parseOperator();

        let right: PropertyPath | Parameter | Literal;
        if (operator === 'IS NULL' || operator === 'IS NOT NULL') {
            right = { type: 'null', value: 'null' };
        } else if (operator === 'IN' || operator === 'NOT IN') {
            this.consume('(');
            const nextToken = this.peek()?.toUpperCase();
            if (nextToken === 'SELECT') {
                let parenCount = 1;
                while (parenCount > 0 && this.current < this.tokens.length) {
                    const token = this.advance();
                    if (token === '(') parenCount++;
                    if (token === ')') parenCount--;
                }
                right = { type: 'null', value: 'subquery' } as any;
            } else {
                const values: (PropertyPath | Parameter | Literal)[] = [];
                if (!this.check(')')) {
                    do {
                        if (values.length > 0) {
                            this.consume(',');
                        }
                        values.push(this.parseValue());
                    } while (this.check(','));
                }
                this.consume(')');
                right = values[0] || { type: 'null', value: 'null' };
            }
        } else {
            right = this.parseValue();
        }

        return { left, operator, right };
    }

    private parseOperator(): string {
        if (this.match('=')) return '=';
        if (this.match('==')) return '==';
        if (this.match('!=')) return '!=';
        if (this.match('<=')) return '<=';
        if (this.match('>=')) return '>=';
        if (this.match('<')) return '<';
        if (this.match('>')) return '>';
        if (this.match('LIKE')) return 'LIKE';
        if (this.match('NOT')) {
            if (this.match('IN')) {
                return 'NOT IN';
            }
            if (this.match('LIKE')) {
                return 'NOT LIKE';
            }
            if (this.match('NULL')) {
                return 'NOT NULL';
            }
            throw new Error(`Unexpected token after NOT: ${this.peek()}`);
        }
        if (this.match('IN')) return 'IN';
        if (this.match('IS')) {
            const not = this.match('NOT');
            this.consume('NULL');
            return not ? 'IS NOT NULL' : 'IS NULL';
        }
        throw new Error(`Unexpected operator: ${this.peek()}`);
    }

    private parseValue(): PropertyPath | Parameter | Literal {
        const token = this.peek();

        if (token && token.startsWith(':')) {
            return this.parseParameter();
        }

        if (this.checkStringLiteral()) {
            return this.parseStringLiteral();
        }

        if (this.checkNumberLiteral()) {
            return this.parseNumberLiteral();
        }

        if (this.match('NULL')) {
            return { type: 'null', value: 'null' };
        }

        return this.parsePropertyPath();
    }

    private parsePropertyPath(): PropertyPath {
        const first = this.consumeIdentifier();
        let alias: string | undefined;
        let properties: string[];

        if (this.match('.')) {
            alias = first;
            properties = [this.consumeIdentifier()];
        } else {
            properties = [first];
        }

        while (this.match('.')) {
            properties.push(this.consumeIdentifier());
        }

        return { alias, properties };
    }

    private parseParameter(): Parameter {
        const token = this.advance()!;
        if (!token.startsWith(':')) {
            throw new Error('Expected parameter');
        }
        return { type: 'parameter', name: token.substring(1) };
    }

    private parseStringLiteral(): Literal {
        const token = this.advance()!;
        const value = token.slice(1, -1).replace(/''/g, "'");
        return { type: 'string', value };
    }

    private parseNumberLiteral(): Literal {
        const token = this.advance()!;
        const value = token.includes('.') ? parseFloat(token) : parseInt(token, 10);
        return { type: 'number', value };
    }

    private peek(): string | undefined {
        return this.tokens[this.current];
    }

    private advance(): string | undefined {
        if (this.current < this.tokens.length) {
            return this.tokens[this.current++];
        }
        return undefined;
    }

    private match(...tokens: string[]): boolean {
        const current = this.peek()?.toUpperCase();
        for (const token of tokens) {
            if (current === token.toUpperCase()) {
                this.advance();
                return true;
            }
        }
        return false;
    }

    private check(token: string): boolean {
        return this.peek()?.toUpperCase() === token.toUpperCase();
    }

    private checkIdentifier(): boolean {
        const token = this.peek();
        return token !== undefined && /^[a-zA-Z_][a-zA-Z0-9_]*$/.test(token);
    }

    private checkStringLiteral(): boolean {
        const token = this.peek();
        return token !== undefined && token.startsWith("'") && token.endsWith("'");
    }

    private checkNumberLiteral(): boolean {
        const token = this.peek();
        return token !== undefined && /^[0-9]+(\.[0-9]+)?$/.test(token);
    }

    private consume(token: string): string {
        if (this.match(token)) {
            return token;
        }
        throw new Error(`Expected '${token}', got '${this.peek()}'`);
    }

    private consumeIdentifier(): string {
        if (this.checkIdentifier()) {
            return this.advance()!;
        }
        throw new Error(`Expected identifier, got '${this.peek()}'`);
    }
}

