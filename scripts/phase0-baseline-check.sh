#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

PARSE_ONLY="${1:-}"
GRADLE_EXIT=0

if [ "$PARSE_ONLY" != "--parse-only" ]; then
  echo "[phase0-baseline] Running test baseline check..."
  set +e
  ./gradlew test --no-daemon
  GRADLE_EXIT=$?
  set -e
else
  echo "[phase0-baseline] Parse-only mode: skipping test execution."
fi

RESULT_DIR="build/test-results/test"

if [ ! -d "$RESULT_DIR" ]; then
  echo "[phase0-baseline] No test result directory found: $RESULT_DIR"
  exit 1
fi

echo
echo "[phase0-baseline] Per-suite summary (only failing suites):"
FAILED_SUITES=0

for f in "$RESULT_DIR"/TEST-*.xml; do
  [ -f "$f" ] || continue

  tests="$(sed -n 's/.*tests="\([0-9][0-9]*\)".*/\1/p' "$f" | head -n1)"
  failures="$(sed -n 's/.*failures="\([0-9][0-9]*\)".*/\1/p' "$f" | head -n1)"
  errors="$(sed -n 's/.*errors="\([0-9][0-9]*\)".*/\1/p' "$f" | head -n1)"

  tests="${tests:-0}"
  failures="${failures:-0}"
  errors="${errors:-0}"

  if [ "$failures" -gt 0 ] || [ "$errors" -gt 0 ]; then
    suite="$(basename "$f" .xml | sed 's/^TEST-//')"
    echo "- $suite: tests=$tests, failures=$failures, errors=$errors"
    FAILED_SUITES=$((FAILED_SUITES + 1))
  fi
done

echo
echo "[phase0-baseline] Failed testcases:"
for f in "$RESULT_DIR"/TEST-*.xml; do
  [ -f "$f" ] || continue
  suite="$(basename "$f" .xml | sed 's/^TEST-//')"
  awk -v suite="$suite" '
    /<testcase / {
      current = ""
      if (match($0, / name="[^"]+"/)) {
        current = substr($0, RSTART + 7, RLENGTH - 8)
      }
    }
    /<failure / || /<error / {
      if (current != "") {
        print "- " suite " :: " current
      }
    }
  ' "$f"
done

echo
if [ "$PARSE_ONLY" = "--parse-only" ]; then
  echo "[phase0-baseline] PARSE-ONLY: test execution skipped."
elif [ "$GRADLE_EXIT" -eq 0 ]; then
  echo "[phase0-baseline] PASS: all tests passed."
else
  echo "[phase0-baseline] FAIL: test command exited with code $GRADLE_EXIT"
fi

echo "[phase0-baseline] Failing suites: $FAILED_SUITES"
echo "[phase0-baseline] HTML report: build/reports/tests/test/index.html"

exit "$GRADLE_EXIT"
