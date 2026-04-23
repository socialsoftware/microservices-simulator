import { startLanguageServer } from 'langium/lsp';
import { NodeFileSystem } from 'langium/node';
import { createConnection, ProposedFeatures } from 'vscode-languageserver/node.js';
import { createNebulaServices } from './nebula-module.js';


const connection = createConnection(ProposedFeatures.all);


const { shared } = createNebulaServices({ connection, ...NodeFileSystem });


startLanguageServer(shared);
