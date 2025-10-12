export enum LogLevel {
    DEBUG = 0,
    INFO = 1,
    WARN = 2,
    ERROR = 3,
    SILENT = 4
}

export interface LogEntry {
    level: LogLevel;
    message: string;
    timestamp: Date;
    context?: any;
}

export class Logger {
    private level: LogLevel;
    private entries: LogEntry[] = [];
    private maxEntries: number;

    constructor(level: LogLevel = LogLevel.INFO, maxEntries: number = 1000) {
        this.level = level;
        this.maxEntries = maxEntries;
    }

    setLevel(level: LogLevel): void {
        this.level = level;
    }

    getLevel(): LogLevel {
        return this.level;
    }

    debug(message: string, context?: any): void {
        this.log(LogLevel.DEBUG, message, context);
    }

    info(message: string, context?: any): void {
        this.log(LogLevel.INFO, message, context);
    }

    warn(message: string, context?: any): void {
        this.log(LogLevel.WARN, message, context);
    }

    error(message: string, context?: any): void {
        this.log(LogLevel.ERROR, message, context);
    }

    private log(level: LogLevel, message: string, context?: any): void {
        if (level < this.level) {
            return;
        }

        const entry: LogEntry = {
            level,
            message,
            timestamp: new Date(),
            context
        };

        this.entries.push(entry);

        if (this.entries.length > this.maxEntries) {
            this.entries = this.entries.slice(-this.maxEntries);
        }

        this.outputToConsole(entry);
    }

    private outputToConsole(entry: LogEntry): void {
        const timestamp = entry.timestamp.toISOString();
        const levelName = LogLevel[entry.level];
        const prefix = `[${timestamp}] ${levelName}:`;

        switch (entry.level) {
            case LogLevel.DEBUG:
                console.debug(`🔍 ${prefix}`, entry.message, entry.context || '');
                break;
            case LogLevel.INFO:
                console.info(`ℹ️  ${prefix}`, entry.message, entry.context || '');
                break;
            case LogLevel.WARN:
                console.warn(`⚠️  ${prefix}`, entry.message, entry.context || '');
                break;
            case LogLevel.ERROR:
                console.error(`❌ ${prefix}`, entry.message, entry.context || '');
                break;
        }
    }

    getEntries(): LogEntry[] {
        return [...this.entries];
    }

    getEntriesForLevel(level: LogLevel): LogEntry[] {
        return this.entries.filter(entry => entry.level === level);
    }

    clear(): void {
        this.entries = [];
    }

    getSummary(): { [key: string]: number } {
        const summary: { [key: string]: number } = {};

        for (const level of Object.values(LogLevel)) {
            if (typeof level === 'number') {
                const levelName = LogLevel[level];
                summary[levelName] = this.getEntriesForLevel(level).length;
            }
        }

        return summary;
    }

    child(contextPrefix: string): Logger {
        const childLogger = new Logger(this.level, this.maxEntries);

        const originalLog = childLogger.log.bind(childLogger);
        childLogger.log = (level: LogLevel, message: string, context?: any) => {
            originalLog(level, `[${contextPrefix}] ${message}`, context);
        };

        return childLogger;
    }
}

let globalLogger: Logger | null = null;

export function getGlobalLogger(): Logger {
    if (!globalLogger) {
        globalLogger = new Logger();
    }
    return globalLogger;
}

export function initializeLogger(level: LogLevel = LogLevel.INFO): Logger {
    globalLogger = new Logger(level);
    return globalLogger;
}

export function debug(message: string, context?: any): void {
    getGlobalLogger().debug(message, context);
}

export function info(message: string, context?: any): void {
    getGlobalLogger().info(message, context);
}

export function warn(message: string, context?: any): void {
    getGlobalLogger().warn(message, context);
}

export function error(message: string, context?: any): void {
    getGlobalLogger().error(message, context);
}
