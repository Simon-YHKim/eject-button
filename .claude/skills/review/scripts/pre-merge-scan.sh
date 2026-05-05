#!/usr/bin/env bash
# pre-merge-scan.sh — Detect dead code, unused imports, orphaned files before merging.
#
# Usage: bash .claude/skills/review/scripts/pre-merge-scan.sh [src-dir]
# Default src-dir: src
#
# Best-effort: skips checks if tooling is missing.

set -uo pipefail

SRC_DIR="${1:-src}"
LOG_PREFIX="[pre-merge-scan]"
log() { echo "$LOG_PREFIX $*" >&2; }

if [ ! -d "$SRC_DIR" ]; then
  log "ERROR: directory $SRC_DIR not found"
  exit 1
fi

issues=0

log "=== Dead code (TS unused exports) ==="
if command -v npx >/dev/null 2>&1 && [ -f "tsconfig.json" ]; then
  out=$(npx -y ts-unused-exports tsconfig.json 2>&1 || true)
  if echo "$out" | grep -qE "^[0-9]+ modules"; then
    echo "$out" | tail -30 >&2
    issues=$((issues + 1))
  else
    log "✅ No unused TS exports"
  fi
else
  log "SKIP: npx or tsconfig.json missing"
fi

log "=== Unused imports (TS via tsc --noUnusedLocals) ==="
if command -v npx >/dev/null 2>&1 && [ -f "tsconfig.json" ]; then
  out=$(npx -y tsc --noEmit --noUnusedLocals --noUnusedParameters 2>&1 || true)
  flags=$(echo "$out" | grep -c "is declared but" || true)
  if [ "$flags" -gt 0 ]; then
    echo "$out" | head -20 >&2
    log "❌ $flags unused locals/params"
    issues=$((issues + 1))
  else
    log "✅ No unused locals"
  fi
else
  log "SKIP: tsc not available"
fi

log "=== Commented code blocks (>3 consecutive lines) ==="
commented=$(grep -rn -A 3 -B 0 -E "^\s*//" "$SRC_DIR" --include="*.ts" --include="*.tsx" --include="*.js" 2>/dev/null \
  | awk '/--/{count=0; next} /^[^-]/{count++} count >= 4 {print; exit}' || true)
if [ -n "$commented" ]; then
  log "❌ Commented code blocks found (review and remove)"
  issues=$((issues + 1))
else
  log "✅ No long commented blocks"
fi

log "=== Python unused imports (if applicable) ==="
if command -v python3 >/dev/null 2>&1 && find "$SRC_DIR" -name "*.py" -print -quit 2>/dev/null | grep -q .; then
  if command -v autoflake >/dev/null 2>&1; then
    out=$(autoflake --check --remove-all-unused-imports -r "$SRC_DIR" 2>&1 || true)
    if echo "$out" | grep -q "would be reformatted\|unused"; then
      echo "$out" | head -10 >&2
      issues=$((issues + 1))
    else
      log "✅ No unused Python imports"
    fi
  else
    log "SKIP: autoflake not installed (pip install autoflake)"
  fi
fi

log ""
if [ $issues -gt 0 ]; then
  log "❌ Pre-merge scan found $issues issue(s) — clean up before merge"
  exit 1
fi
log "✅ Pre-merge scan PASS — ready to merge"
exit 0
