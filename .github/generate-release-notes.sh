#!/usr/bin/env bash
set -euo pipefail

REPO="${GITHUB_REPOSITORY}"

LAST_TAG=$(git describe --tags --abbrev=0 2>/dev/null || echo "")

if [ -z "$LAST_TAG" ]; then
  COMMITS=$(git rev-list HEAD)
else
  COMMITS=$(git rev-list "${LAST_TAG}..HEAD")
fi

FEATURES=""
FIXES=""
CHORES=""
COMMITS_SECTION=""

echo "$COMMITS" | while read -r sha; do
  [ -z "$sha" ] && continue

  shortsha="${sha:0:7}"

  msg=$(git show -s --format=%s "$sha")
  body=$(git show -s --format=%b "$sha")
  author=$(git show -s --format=%an "$sha")

  type=$(echo "$msg" | cut -d':' -f1 | tr '[:upper:]' '[:lower:]')

  pr_json=$(gh api "repos/${REPO}/commits/${sha}/pulls" \
    --header "Accept: application/vnd.github+json" \
    --silent 2>/dev/null || true)

  prs=$(echo "$pr_json" | jq -r '.[].number' 2>/dev/null | paste -sd "," -)

  if [ -n "${prs:-}" ]; then
    prs=" #${prs}"
  else
    prs=""
  fi

  line="\`${shortsha}\`: ${msg} (${author})${prs}"

  if echo "$msg$body" | grep -q "BREAKING CHANGE"; then
    line="${line} **BREAKING**"
  fi

  if [[ "$msg" == chore\(locales\)* ]]; then
    CHORES+="**locales**: ${line}"$'\n'
    continue
  fi

  case "$type" in
    feat*) FEATURES+="${line}"$'\n' ;;
    fix*) FIXES+="${line}"$'\n' ;;
    chore*) CHORES+="${line}"$'\n' ;;
    *) COMMITS_SECTION+="${line}"$'\n' ;;
  esac
done

{
  echo "body<<EOF"

  [ -n "$FEATURES" ] && echo "## Features" && echo -e "$FEATURES"
  [ -n "$FIXES" ] && echo "## Bug Fixes" && echo -e "$FIXES"
  [ -n "$CHORES" ] && echo "## Chores" && echo -e "$CHORES"
  [ -n "$COMMITS_SECTION" ] && echo "## Commits" && echo -e "$COMMITS_SECTION"

  echo ""
  echo "EOF"
} >> "$GITHUB_OUTPUT"
