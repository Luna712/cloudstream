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

while read -r sha; do
  [ -z "$sha" ] && continue

  shortsha="${sha:0:7}"

  msg=$(git show -s --format=%s "$sha")
  body=$(git show -s --format=%b "$sha")
  author=$(git show -s --format=%an "$sha")

  type=$(echo "$msg" | cut -d':' -f1 | tr '[:upper:]' '[:lower:]')

  scope=$(echo "$msg" | sed -n 's/^[a-z]*(\([^)]*\)).*/\1/p')
  title=$(echo "$msg" | sed 's/^[a-z]*(\([^)]*\)):\s*//')

  prs=$(gh api \
    -H "Accept: application/vnd.github+json" \
    "/repos/${REPO}/commits/${sha}/pulls" \
    --jq '.[].number' 2>/dev/null | paste -sd "," -)

  has_pr=false
  if [ -n "${prs:-}" ]; then
    has_pr=true
    identity=" #${prs}"
  else
    identity=""
  fi

  if $has_pr; then
    prefix=""
  else
    prefix="\`${shortsha}\`: "
  fi

  if [[ -n "$scope" ]]; then
    scope="**${scope}**: "
  else
    scope=""
  fi

  line="${prefix}${scope}${msg}${identity}"

  if echo "$msg$body" | grep -q "BREAKING CHANGE"; then
    line="${line} **BREAKING**"
  fi

  case "$type" in
    feat*) FEATURES+="${line}"$'\n' ;;
    fix*) FIXES+="${line}"$'\n' ;;
    chore*) CHORES+="${line}"$'\n' ;;
    *) COMMITS_SECTION+="${line}"$'\n' ;;
  esac

done <<< "$COMMITS"

{
  echo "body<<EOF"

  [ -n "$FEATURES" ] && echo "## Features" && echo -e "$FEATURES"
  [ -n "$FIXES" ] && echo "## Bug Fixes" && echo -e "$FIXES"
  [ -n "$CHORES" ] && echo "## Chores" && echo -e "$CHORES"
  [ -n "$COMMITS_SECTION" ] && echo "## Commits" && echo -e "$COMMITS_SECTION"

  echo ""
  echo "EOF"
} >> "$GITHUB_OUTPUT"
