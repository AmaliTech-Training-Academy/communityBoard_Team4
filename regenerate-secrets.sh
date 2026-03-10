#!/bin/bash

# Regenerate secrets baseline with current detect-secrets version
rm .secrets.baseline
detect-secrets scan > .secrets.baseline

echo "✅ Secrets baseline regenerated"
