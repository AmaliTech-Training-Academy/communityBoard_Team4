#!/bin/bash

# Update secrets baseline with current findings
detect-secrets scan --baseline .secrets.baseline

echo "✅ Secrets baseline updated"
echo "Run: detect-secrets audit .secrets.baseline"
echo "Then mark false positives (press 'n' for each)"
