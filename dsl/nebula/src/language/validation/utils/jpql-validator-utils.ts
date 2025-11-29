export interface JpqlSyntaxError {
    message: string;
}

export class JpqlValidatorUtils {
    private static readonly VALID_KEYWORDS = new Set([
        'SELECT', 'FROM', 'WHERE', 'AND', 'OR', 'NOT', 'IN', 'LIKE', 'BETWEEN',
        'IS', 'NULL', 'ORDER', 'BY', 'GROUP', 'HAVING', 'JOIN', 'INNER', 'LEFT',
        'RIGHT', 'OUTER', 'ON', 'AS', 'DISTINCT', 'COUNT', 'SUM', 'AVG', 'MAX',
        'MIN', 'CASE', 'WHEN', 'THEN', 'ELSE', 'END', 'EXISTS', 'ALL', 'ANY',
        'SOME', 'UNION', 'INTERSECT', 'EXCEPT'
    ]);

    static validateJpqlSyntax(query: string): JpqlSyntaxError[] {
        const errors: JpqlSyntaxError[] = [];

        const queryWithoutStrings = query.replace(/['"](?:[^'"]|'')*['"]/g, '');
        const queryUpper = queryWithoutStrings.toUpperCase();

        let parenCount = 0;
        for (let i = 0; i < queryWithoutStrings.length; i++) {
            if (queryWithoutStrings[i] === '(') parenCount++;
            if (queryWithoutStrings[i] === ')') parenCount--;
            if (parenCount < 0) {
                errors.push({ message: "Unmatched closing parenthesis ')' in query" });
                break;
            }
        }
        if (parenCount > 0) {
            errors.push({ message: "Unmatched opening parenthesis '(' in query" });
        }

        const singleQuotes = (query.match(/'/g) || []).length;
        const doubleQuotes = (query.match(/"/g) || []).length;

        if (singleQuotes % 2 !== 0) {
            errors.push({ message: "Unmatched single quote in query. String literals must be properly quoted." });
        }

        if (doubleQuotes > 0) {
            errors.push({ message: "Double quotes should not be used in JPQL queries. Use single quotes for string literals." });
        }

        const entityNames = new Set<string>();
        const fromPattern = /\bFROM\s+(\w+)(?:\s+(\w+))?/gi;
        let fromMatch;
        while ((fromMatch = fromPattern.exec(queryWithoutStrings)) !== null) {
            entityNames.add(fromMatch[1].toUpperCase());
            if (fromMatch[2]) {
                entityNames.add(fromMatch[2].toUpperCase());
            }
        }

        const keywordPattern = /\b([A-Z][A-Z0-9_]*)\b/g;
        const foundInvalidKeywords = new Set<string>();
        let match;

        while ((match = keywordPattern.exec(queryUpper)) !== null) {
            const word = match[1];
            const matchIndex = match.index;
            const originalWord = queryWithoutStrings.substring(matchIndex, matchIndex + word.length);

            if (this.VALID_KEYWORDS.has(word) || /^\d+$/.test(word) || word.length <= 2) {
                continue;
            }

            if (entityNames.has(word)) {
                continue;
            }

            const beforeChar = queryWithoutStrings[matchIndex - 1];
            if (beforeChar === '.' || beforeChar === ':') {
                continue;
            }

            const afterWord = queryWithoutStrings.substring(matchIndex + word.length, matchIndex + word.length + 1);
            if (afterWord === '.') {
                continue;
            }

            if (word === originalWord.toUpperCase() && word.length >= 3 && !foundInvalidKeywords.has(word)) {
                const similarKeyword = this.findSimilarKeyword(word, Array.from(this.VALID_KEYWORDS));
                if (similarKeyword) {
                    errors.push({
                        message: `Unknown keyword '${originalWord}' in query. Did you mean '${similarKeyword}'?`
                    });
                    foundInvalidKeywords.add(word);
                }
            }
        }

        const selectMatch = queryUpper.match(/\bSELECT\b/i);
        const fromKeywordMatch = queryUpper.match(/\bFROM\b/i);

        if (!selectMatch) {
            errors.push({ message: "Query must contain SELECT clause" });
        }
        if (!fromKeywordMatch) {
            errors.push({ message: "Query must contain FROM clause" });
        }

        if (selectMatch && fromKeywordMatch && selectMatch.index !== undefined && fromKeywordMatch.index !== undefined && selectMatch.index > fromKeywordMatch.index) {
            errors.push({ message: "FROM clause must come after SELECT clause" });
        }

        const whereMatch = queryUpper.match(/\bWHERE\b/i);
        if (whereMatch && fromKeywordMatch && whereMatch.index !== undefined && fromKeywordMatch.index !== undefined && whereMatch.index < fromKeywordMatch.index) {
            errors.push({ message: "WHERE clause must come after FROM clause" });
        }

        return errors;
    }

    private static findSimilarKeyword(word: string, validKeywords: string[]): string | null {
        for (const keyword of validKeywords) {
            const distance = this.levenshteinDistance(word, keyword);
            if (distance === 1 && Math.abs(word.length - keyword.length) <= 1) {
                return keyword;
            }
        }
        return null;
    }

    private static levenshteinDistance(str1: string, str2: string): number {
        const m = str1.length;
        const n = str2.length;
        const dp: number[][] = [];

        for (let i = 0; i <= m; i++) {
            dp[i] = [i];
        }
        for (let j = 0; j <= n; j++) {
            dp[0][j] = j;
        }

        for (let i = 1; i <= m; i++) {
            for (let j = 1; j <= n; j++) {
                if (str1[i - 1] === str2[j - 1]) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    dp[i][j] = Math.min(
                        dp[i - 1][j] + 1,
                        dp[i][j - 1] + 1,
                        dp[i - 1][j - 1] + 1
                    );
                }
            }
        }

        return dp[m][n];
    }

    static extractQueryParameters(query: string): string[] {
        const paramPattern = /:(\w+)/g;
        const params: string[] = [];
        let match;

        while ((match = paramPattern.exec(query)) !== null) {
            const paramName = match[1];
            if (!params.includes(paramName)) {
                params.push(paramName);
            }
        }

        return params;
    }
}

