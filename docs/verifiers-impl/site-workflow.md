# Verifier docs site workflow

The verifier knowledge base is served as an on-demand MkDocs Material site scoped to `docs/verifiers-impl/`.

Run commands from the repository root.

## Live preview

Start the live preview with:

```bash
./scripts/verifier-docs serve
```

The preview binds to `0.0.0.0:8000` by default so it can be read privately from another machine over Tailscale, assuming the repository host is already reachable on the Tailnet and local firewall rules allow the port.

To use a different private address or port:

```bash
VERIFIER_DOCS_ADDR=0.0.0.0:8010 ./scripts/verifier-docs serve
```

This workflow is intended for private Tailnet access, not public internet exposure.

## Static build

Build the static site with:

```bash
./scripts/verifier-docs build
```

The generated site is written to `target/verifier-docs-site/`.

Successful command output includes MkDocs build messages ending with `Documentation built in ...`.
