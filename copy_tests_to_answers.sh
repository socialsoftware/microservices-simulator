#!/bin/bash

# Script to copy test structure from quizzes to answers
# This will replace the existing answers test directory with quizzes test structure

set -e  # Exit on any error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check if we're in the right directory
if [[ ! -f "docker-compose.yml" ]] || [[ ! -d "applications" ]]; then
    print_error "This script must be run from the microservices-simulator root directory"
    exit 1
fi

# Define paths
QUIZZES_TEST_DIR="applications/quizzes/src/test"
ANSWERS_TEST_DIR="applications/answers/src/test"
BACKUP_DIR="applications/answers/src/test_backup_$(date +%Y%m%d_%H%M%S)"

print_status "Starting test directory copy from quizzes to answers..."

# Check if source directory exists
if [[ ! -d "$QUIZZES_TEST_DIR" ]]; then
    print_error "Source directory $QUIZZES_TEST_DIR does not exist"
    exit 1
fi

# Check if target directory exists
if [[ ! -d "$ANSWERS_TEST_DIR" ]]; then
    print_error "Target directory $ANSWERS_TEST_DIR does not exist"
    exit 1
fi

# Create backup of existing answers test directory
print_status "Creating backup of existing answers test directory..."
if cp -r "$ANSWERS_TEST_DIR" "$BACKUP_DIR"; then
    print_success "Backup created at: $BACKUP_DIR"
else
    print_error "Failed to create backup"
    exit 1
fi

# Remove existing test directory content (but keep the directory)
print_status "Removing existing test directory content..."
if rm -rf "${ANSWERS_TEST_DIR}"/*; then
    print_success "Existing test content removed"
else
    print_error "Failed to remove existing test content"
    exit 1
fi

# Copy quizzes test structure to answers
print_status "Copying test structure from quizzes to answers..."
if cp -r "${QUIZZES_TEST_DIR}"/* "$ANSWERS_TEST_DIR/"; then
    print_success "Test structure copied successfully"
else
    print_error "Failed to copy test structure"
    print_warning "Restoring backup..."
    rm -rf "$ANSWERS_TEST_DIR"
    mv "$BACKUP_DIR" "$ANSWERS_TEST_DIR"
    exit 1
fi

# Update package names in copied files (quizzes -> answers)
print_status "Updating package names from 'quizzes' to 'answers'..."

# Find all .groovy files and update package declarations
find "$ANSWERS_TEST_DIR" -name "*.groovy" -type f -exec sed -i.bak 's/package pt\.ulisboa\.tecnico\.socialsoftware\.quizzes/package pt.ulisboa.tecnico.socialsoftware.answers/g' {} \;

# Find all .groovy files and update import statements
find "$ANSWERS_TEST_DIR" -name "*.groovy" -type f -exec sed -i.bak 's/import pt\.ulisboa\.tecnico\.socialsoftware\.quizzes/import pt.ulisboa.tecnico.socialsoftware.answers/g' {} \;

# Remove backup files created by sed
find "$ANSWERS_TEST_DIR" -name "*.bak" -type f -delete

print_success "Package names updated"

# Update directory structure (rename quizzes folder to answers)
if [[ -d "$ANSWERS_TEST_DIR/groovy/pt/ulisboa/tecnico/socialsoftware/quizzes" ]]; then
    print_status "Renaming directory structure from 'quizzes' to 'answers'..."
    
    QUIZZES_DIR="$ANSWERS_TEST_DIR/groovy/pt/ulisboa/tecnico/socialsoftware/quizzes"
    ANSWERS_DIR="$ANSWERS_TEST_DIR/groovy/pt/ulisboa/tecnico/socialsoftware/answers"
    
    if mv "$QUIZZES_DIR" "$ANSWERS_DIR"; then
        print_success "Directory structure renamed"
    else
        print_error "Failed to rename directory structure"
        exit 1
    fi
fi

# Show summary
print_status "Summary of changes:"
echo "  ✓ Backup created: $BACKUP_DIR"
echo "  ✓ Test structure copied from quizzes to answers"
echo "  ✓ Package names updated (quizzes → answers)"
echo "  ✓ Directory structure renamed"

print_success "Test copy operation completed successfully!"
print_warning "Note: You may need to manually update test content to match answers domain logic"

# Show what was copied
print_status "Copied test structure:"
tree "$ANSWERS_TEST_DIR" -I "*.class" 2>/dev/null || find "$ANSWERS_TEST_DIR" -type f -name "*.groovy" | head -10

echo
print_status "To run the copied tests:"
echo "  docker compose up build-simulator"
echo "  docker compose up test-answers-sagas"
echo
print_warning "Remember to add test-answers-sagas service to docker-compose.yml if not already present"
