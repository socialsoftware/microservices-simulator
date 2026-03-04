#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"

PACKAGE_JSON="package.json"
CURRENT_VERSION=$(node -p "require('./$PACKAGE_JSON').version")

echo "Current version: $CURRENT_VERSION"
echo ""

# Parse current version
IFS='.' read -r MAJOR MINOR PATCH <<< "$CURRENT_VERSION"

# Determine new version
if [ $# -ge 1 ]; then
    case "$1" in
        major)
            MAJOR=$((MAJOR + 1))
            MINOR=0
            PATCH=0
            ;;
        minor)
            MINOR=$((MINOR + 1))
            PATCH=0
            ;;
        patch)
            PATCH=$((PATCH + 1))
            ;;
        *)
            # Treat as explicit version
            NEW_VERSION="$1"
            ;;
    esac
fi

NEW_VERSION="${NEW_VERSION:-$MAJOR.$MINOR.$PATCH}"

echo "New version: $NEW_VERSION"
echo ""

# Update version in package.json
node -e "
const fs = require('fs');
const pkg = JSON.parse(fs.readFileSync('$PACKAGE_JSON', 'utf8'));
pkg.version = '$NEW_VERSION';
fs.writeFileSync('$PACKAGE_JSON', JSON.stringify(pkg, null, 2) + '\n');
"
echo "Updated $PACKAGE_JSON"

# Build
echo ""
echo "Building..."
npm run langium:generate
npm run build

# Package extension
echo ""
echo "Packaging extension..."
EXTENSIONS_DIR="../extensions"
mkdir -p "$EXTENSIONS_DIR"
OUTPUT_FILE="$EXTENSIONS_DIR/nebula-extension-${NEW_VERSION}.vsix"
npx @vscode/vsce package --no-dependencies -o "$OUTPUT_FILE"

echo ""
echo "Extension packaged: dsl/extensions/nebula-extension-${NEW_VERSION}.vsix"
echo ""
echo "To install:"
echo "  code --install-extension dsl/extensions/nebula-extension-${NEW_VERSION}.vsix"
