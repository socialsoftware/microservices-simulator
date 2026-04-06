import type { LanguageClientOptions, ServerOptions } from 'vscode-languageclient/node.js';
import type * as vscode from 'vscode';
import * as path from 'node:path';
import { LanguageClient, TransportKind } from 'vscode-languageclient/node.js';

let client: LanguageClient;


export function activate(context: vscode.ExtensionContext): void {
    client = startLanguageClient(context);
}


export function deactivate(): Thenable<void> | undefined {
    if (client) {
        return client.stop();
    }
    return undefined;
}

function startLanguageClient(context: vscode.ExtensionContext): LanguageClient {
    const serverModule = context.asAbsolutePath(path.join('out', 'language', 'main.cjs'));
    
    
    
    const debugOptions = { execArgv: ['--nolazy', `--inspect${process.env.DEBUG_BREAK ? '-brk' : ''}=${process.env.DEBUG_SOCKET || '6009'}`] };

    
    
    const serverOptions: ServerOptions = {
        run: { module: serverModule, transport: TransportKind.ipc },
        debug: { module: serverModule, transport: TransportKind.ipc, options: debugOptions }
    };

    
    const clientOptions: LanguageClientOptions = {
        documentSelector: [{ scheme: 'file', language: 'nebula' }]
    };

    
    const client = new LanguageClient(
        'nebula',
        'Nebula',
        serverOptions,
        clientOptions
    );

    
    client.start();
    return client;
}
