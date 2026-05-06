#!/usr/bin/env bash
set -euo pipefail

# =============================================================================
# generate_changelog.sh
#
# Replicates the changelog format of the automatic-releases GitHub Action.
# Designed to run inside a GitHub Actions workflow — all required values are
# sourced from the standard GHA environment variables automatically.
#
# Usage:
#   generate_changelog.sh [PREVIOUS_TAG]
#
# Args:
#   PREVIOUS_TAG  - Optional. Tag to compare from. Auto-detected via semver if omitted.
#
# Required GHA environment variables (set automatically by GitHub Actions):
#   GITHUB_REPOSITORY   - "owner/repo"                      (github.repository)
#   GITHUB_SHA          - Current commit SHA                 (github.sha)
#   GITHUB_REF          - Current ref, e.g. refs/tags/v1.2  (github.ref)
# =============================================================================

# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

short_sha() {
  echo "${1:0:7}"
}

# Print to stderr so it doesn't pollute changelog output
log() { echo "[generate_changelog] $*" >&2; }

require() {
  command -v "$1" &>/dev/null || { log "ERROR: '$1' is required but not found"; exit 1; }
}

require git
require curl
require jq

# ---------------------------------------------------------------------------
# Config — sourced entirely from GHA environment
# ---------------------------------------------------------------------------

PREVIOUS_TAG="${1:-}"

# GITHUB_SHA is the SHA of the commit that triggered the workflow
CURRENT_SHA="${GITHUB_SHA:-$(git rev-parse HEAD)}"

# GITHUB_REPOSITORY is "owner/repo"
if [[ -z "${GITHUB_REPOSITORY:-}" ]]; then
  log "ERROR: GITHUB_REPOSITORY is not set — are you running outside of GitHub Actions?"
  exit 1
fi
OWNER="$(echo "$GITHUB_REPOSITORY" | cut -d/ -f1)"
REPO="$(echo "$GITHUB_REPOSITORY" | cut -d/ -f2)"

log "Repo: ${OWNER}/${REPO}"
log "Current SHA: ${CURRENT_SHA}"

# ---------------------------------------------------------------------------
# GitHub API helper
# ---------------------------------------------------------------------------

gh_api() {
  local endpoint="$1"
  curl -fsSL \
    -H "Accept: application/vnd.github.v3+json" \
    "https://api.github.com${endpoint}"
}

# ---------------------------------------------------------------------------
# Find previous release tag (semver, descending)
# ---------------------------------------------------------------------------

find_previous_tag() {
  local current_tag="$1"
  log "Searching for previous semver tag before ${current_tag}"

  # Collect all semver tags from git
  local tags
  tags="$(git tag --list | grep -E '^v?[0-9]+\.[0-9]+\.[0-9]+' || true)"

  if [[ -z "$tags" ]]; then
    log "No semver tags found"
    echo ""
    return
  fi

  # Sort descending (semver-aware via sort -V), find first tag strictly less than current
  # Strip leading 'v' for comparison, then restore it
  local prev=""
  while IFS= read -r tag; do
    [[ "$tag" == "$current_tag" ]] && continue
    # Check tag < current_tag using sort -V
    local lower
    lower="$(printf '%s\n%s\n' "$current_tag" "$tag" | sort -V | head -1)"
    if [[ "$lower" == "$tag" ]]; then
      prev="$tag"
      break
    fi
  done < <(echo "$tags" | sort -rV)

  echo "$prev"
}

# ---------------------------------------------------------------------------
# Get commits between previous tag and current SHA
# ---------------------------------------------------------------------------

get_commits() {
  local base="$1"
  local head="$2"

  if [[ -z "$base" ]]; then
    log "No previous tag — using full history to ${head}"
    git log --format='%H %s' "${head}"
  else
    log "Getting commits between ${base} and ${head}"
    git log --format='%H %s' "${base}..${head}"
  fi
}

# ---------------------------------------------------------------------------
# Get pull requests associated with a commit SHA (via GitHub API)
# ---------------------------------------------------------------------------

get_prs_for_commit() {
  local sha="$1"
  gh_api "/repos/${OWNER}/${REPO}/commits/${sha}/pulls" \
    2>/dev/null || echo "[]"
}

# ---------------------------------------------------------------------------
# Conventional commit parser
# Parses "type(scope): subject" or plain subject
# ---------------------------------------------------------------------------

# Known types -> section headers (matches ConventionalCommitTypes enum order)
declare -a CC_KEYS=(feat fix docs style refactor perf test build ci chore revert)
declare -A CC_LABELS=(
  [feat]="Features"
  [fix]="Bug Fixes"
  [docs]="Documentation"
  [style]="Styles"
  [refactor]="Code Refactoring"
  [perf]="Performance Improvements"
  [test]="Tests"
  [build]="Builds"
  [ci]="Continuous Integration"
  [chore]="Chores"
  [revert]="Reverts"
)

parse_commit_type() {
  # Returns type if conventional, else empty
  echo "$1" | sed -nE 's/^([a-z]+)(\([^)]*\))?!?:.*/\1/p'
}

parse_commit_scope() {
  echo "$1" | sed -nE 's/^[a-z]+\(([^)]*)\)!?:.*/\1/p'
}

parse_commit_subject() {
  echo "$1" | sed -nE 's/^[a-z]+(\([^)]*\))?!?:[[:space:]]*(.*)/\2/p'
}

is_breaking() {
  # Check for '!' in type(scope)!: or BREAKING CHANGE in body/footer
  local header="$1"
  local body="$2"
  echo "$header" | grep -qE '^[a-z]+(\([^)]*\))?!:' && return 0
  echo "$body"   | grep -qE '^BREAKING[[:space:]]+CHANGES?:[[:space:]]' && return 0
  return 1
}

# ---------------------------------------------------------------------------
# Format a single changelog entry
#
# Matches getFormattedChangelogEntry() in utils.ts:
#
#   If type is set (conventional commit):
#     - **scope**: subject [author](url)  (with optional PR links)
#   Else (plain commit):
#     - sha: header (author)  (with optional PR links)
# ---------------------------------------------------------------------------

format_entry() {
  local sha="$1"
  local header="$2"
  local author_name="$3"
  local commit_url="$4"
  local type="$5"
  local scope="$6"
  local subject="$7"
  local pr_json="$8"   # JSON array of {number, url}

  local short
  short="$(short_sha "$sha")"

  # Build PR string: [#1](url),[#2](url)
  local pr_string=""
  if [[ "$pr_json" != "[]" && -n "$pr_json" ]]; then
    pr_string="$(echo "$pr_json" | jq -r '[.[] | "[#\(.number)](\(.html_url))"] | join(",")')"
    [[ -n "$pr_string" ]] && pr_string=" ${pr_string}"
  fi

  if [[ -n "$type" ]]; then
    # Conventional commit format
    local scope_str=""
    [[ -n "$scope" ]] && scope_str="**${scope}**: "
    echo "- ${scope_str}${subject}${pr_string} ([${author_name}](${commit_url}))"
  else
    # Plain commit format
    echo "- ${short}: ${header} (${author_name})${pr_string}"
  fi
}

# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------

# Determine current tag
# GITHUB_REF is "refs/tags/v1.2.3" on tag push events — extract the tag name directly
CURRENT_TAG=""
if [[ "${GITHUB_REF:-}" =~ ^refs/tags/(.+)$ ]]; then
  CURRENT_TAG="${BASH_REMATCH[1]}"
  log "Detected tag from GITHUB_REF: ${CURRENT_TAG}"
else
  # Fallback: check if HEAD is on an exact tag (e.g. workflow_dispatch on a tag)
  CURRENT_TAG="$(git describe --exact-match "${CURRENT_SHA}" 2>/dev/null || true)"
fi

if [[ -z "$PREVIOUS_TAG" && -n "$CURRENT_TAG" ]]; then
  PREVIOUS_TAG="$(find_previous_tag "$CURRENT_TAG")"
fi

log "Previous tag: ${PREVIOUS_TAG:-<none>}"

# Collect raw commits
mapfile -t RAW_COMMITS < <(get_commits "$PREVIOUS_TAG" "$CURRENT_SHA")

if [[ ${#RAW_COMMITS[@]} -eq 0 ]]; then
  log "No commits found"
  echo ""
  exit 0
fi

# ---------------------------------------------------------------------------
# Temporary storage: parallel arrays indexed by commit position
# We separate commits into buckets: breaking | by type | other
# ---------------------------------------------------------------------------

declare -a BREAKING_ENTRIES=()
declare -A TYPE_ENTRIES=()   # key = cc type, value = newline-separated entries
declare -a OTHER_ENTRIES=()

for raw in "${RAW_COMMITS[@]}"; do
  sha="${raw%% *}"
  message="${raw#* }"

  # Skip merge commits (matches mergePattern: ^Merge pull request #(.*) from (.*)$)
  if echo "$message" | grep -qE '^Merge pull request #[0-9]+ from '; then
    log "Skipping merge commit: ${sha}"
    continue
  fi

  log "Processing commit ${sha}: ${message}"

  # Get commit detail from git for author info
  author_name="$(git log -1 --format='%an' "$sha" 2>/dev/null || echo "unknown")"
  commit_url="https://github.com/${OWNER}/${REPO}/commit/${sha}"

  # Get commit body for breaking change detection
  commit_body="$(git log -1 --format='%b' "$sha" 2>/dev/null || true)"

  # Parse conventional commit
  type="$(parse_commit_type "$message")"
  scope="$(parse_commit_scope "$message")"
  subject="$(parse_commit_subject "$message")"

  # Validate type is a known CC type (else treat as plain)
  known_type=""
  for k in "${CC_KEYS[@]}"; do
    [[ "$type" == "$k" ]] && known_type="$type" && break
  done

  # Detect breaking change
  breaking=false
  if is_breaking "$message" "$commit_body"; then
    breaking=true
  fi

  # Fetch associated PRs
  log "Fetching PRs for ${sha}"
  pr_json="$(get_prs_for_commit "$sha")"

  entry="$(format_entry "$sha" "$message" "$author_name" "$commit_url" \
            "$known_type" "$scope" "$subject" "$pr_json")"

  if [[ "$breaking" == "true" ]]; then
    BREAKING_ENTRIES+=("$entry")
  fi

  if [[ -n "$known_type" ]]; then
    existing="${TYPE_ENTRIES[$known_type]:-}"
    if [[ -n "$existing" ]]; then
      TYPE_ENTRIES[$known_type]="${existing}"$'\n'"${entry}"
    else
      TYPE_ENTRIES[$known_type]="${entry}"
    fi
  else
    OTHER_ENTRIES+=("$entry")
  fi
done

# ---------------------------------------------------------------------------
# Assemble changelog (matches generateChangelogFromParsedCommits order)
# ---------------------------------------------------------------------------

CHANGELOG=""

# Breaking Changes section
if [[ ${#BREAKING_ENTRIES[@]} -gt 0 ]]; then
  CHANGELOG+="## Breaking Changes"$'\n'
  for e in "${BREAKING_ENTRIES[@]}"; do
    CHANGELOG+="${e}"$'\n'
  done
fi

# Conventional commit type sections (in enum declaration order)
for key in "${CC_KEYS[@]}"; do
  block="${TYPE_ENTRIES[$key]:-}"
  [[ -z "$block" ]] && continue

  if [[ -n "$CHANGELOG" ]]; then
    CHANGELOG+=$'\n'
  fi
  CHANGELOG+=$'\n'"## ${CC_LABELS[$key]}"$'\n'
  CHANGELOG+="${block}"$'\n'
done

# Commits section (plain / unrecognised type)
if [[ ${#OTHER_ENTRIES[@]} -gt 0 ]]; then
  if [[ -n "$CHANGELOG" ]]; then
    CHANGELOG+=$'\n'
  fi
  CHANGELOG+=$'\n'"## Commits"$'\n'
  for e in "${OTHER_ENTRIES[@]}"; do
    CHANGELOG+="${e}"$'\n'
  done
fi

# Trim leading/trailing whitespace (matches .trim() at end of TS function)
CHANGELOG="$(echo "$CHANGELOG" | sed -e 's/^[[:space:]]*//' -e 's/[[:space:]]*$//')"

# Write to GITHUB_OUTPUT using the multiline format required by GHA
# https://docs.github.com/en/actions/writing-workflows/choosing-what-your-workflow-does/passing-information-between-jobs#setting-a-multiline-string
DELIMITER="$(openssl rand -hex 16)"
{
  echo "changelog<<${DELIMITER}"
  echo "$CHANGELOG"
  echo "${DELIMITER}"
} >> "${GITHUB_OUTPUT}"

log "Changelog written to GITHUB_OUTPUT as 'changelog'"
