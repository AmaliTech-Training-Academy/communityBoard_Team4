#!/bin/bash

# Install detect-secrets if not installed
if ! command -v detect-secrets &> /dev/null; then
  echo "Installing detect-secrets..."
  pip install detect-secrets
fi

# Install pre-commit hooks
pre-commit install
pre-commit install --hook-type commit-msg

# Generate secrets baseline if not exists
if [ ! -f .secrets.baseline ]; then
  echo "Generating secrets baseline..."
  detect-secrets scan > .secrets.baseline
fi

# Run pre-commit on all files to test
echo "Testing pre-commit hooks..."
pre-commit run --all-files

echo "✅ Pre-commit hooks installed successfully"
