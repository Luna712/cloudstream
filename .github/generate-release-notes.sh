#!/usr/bin/env bash
set -euo pipefail

# -----------------------------
# CONFIG
# -----------------------------
REPO="${GITHUB_REPOSITORY}"
GH_TOKEN="${GH_TOKEN:-}"

if [ -z "$GH_TOKEN" ]; then
  echo "GH_TOKEN is required"
  exit 1
fi

# -----------------------------
# FIND RANGE
# -----------------------------
LAST_TAG=$(git describe --tags --abbrev=0 2>/dev/null || echo "")

if [ -z "$LAST_TAG" ]; then
  COMMITS=$(git rev-list HEAD)
else
  COMMITS=$(git rev-list "${LAST_TAG}..HEAD")
fi

declare -g -A GROUPS 2>/dev/null || true
GROUPS=()

# -----------------------------
# PROCESS COMMITS
# -----------------------------
for sha in $COMMITS; do
  shortsha="${sha:0:7}"

  msg=$(git show -s --format=%s "$sha")
  body=$(git show -s --format=%b "$sha")
  author=$(git show -s --format=%an "$sha")

  type=$(echo "$msg" | cut -d':' -f1 | tr '[:upper:]' '[:lower:]')

  # -----------------------------
  # PR LOOKUP (GitHub API)
  # -----------------------------
  pr_json=$(gh api "repos/${REPO}/commits/${sha}/pulls" \
    -H "Accept: application/vnd.github.groot-preview+json" \
    --silent 2>/dev/null || true)

  prs=$(echo "$pr_json" | jq -r '.[].number' 2>/dev/null | paste -sd "," -)

  if [ -n "$prs" ]; then
    prs=" #${prs}"
  else
    prs=""
  fi

  # -----------------------------
  # FORMAT LINE
  # -----------------------------
  line="\`${shortsha}\`: ${msg} (${author})${prs}"

  # -----------------------------
  # BREAKING CHANGE FLAG
  # -----------------------------
  if echo "$msg$body" | grep -q "BREAKING CHANGE"; then
    line="${line} **BREAKING**"
  fi

  # -----------------------------
  # SPECIAL CASE: chore(locales)
  # -----------------------------
  if [[ "$msg" == chore\(locales\)* ]]; then
    GROUPS["Chores"]+="**locales**: ${line}"$'\n'
    continue
  fi

  # -----------------------------
  # GROUPING (conventional commits)
  # -----------------------------
  case "$type" in
    feat*)
      GROUPS["Features"]+="${line}"$'\n'
      ;;
    fix*)
      GROUPS["Bug Fixes"]+="${line}"$'\n'
      ;;
    chore*)
      GROUPS["Chores"]+="${line}"$'\n'
      ;;
    *)
      GROUPS["Other"]+="${line}"$'\n'
      ;;
  esac
done

# -----------------------------
# OUTPUT FOR GITHUB ACTIONS
# -----------------------------
{
  echo "body<<EOF"

  for section in "Features" "Bug Fixes" "Chores" "Other"; do
    if [ -n "${GROUPS[$section]:-}" ]; then
      echo "## ${section}"
      echo -e "${GROUPS[$section]}"
    fi
  done

  echo ""
  echo "EOF"
} >> "$GITHUB_OUTPUT"
