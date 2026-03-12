#!/usr/bin/env python3
"""Parse k6 summary JSON and print threshold results as a Markdown table row per threshold.

Usage: python3 parse-thresholds.py k6-summary.json
"""
import json
import sys

if len(sys.argv) < 2:
    sys.exit(1)

try:
    with open(sys.argv[1]) as f:
        data = json.load(f)
except (OSError, json.JSONDecodeError) as e:
    print(f"Could not read {sys.argv[1]}: {e}", file=sys.stderr)
    sys.exit(1)

thresholds = data.get("thresholds", {})
if not thresholds:
    print("_No threshold data found._")
    sys.exit(0)

for name, result in thresholds.items():
    ok = result.get("ok", True)
    status = "✅ pass" if ok else "❌ fail"
    print(f"| `{name}` | {status} |")
