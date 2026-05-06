#!/usr/bin/env python3
"""
generate_changelog.py

Replicates the changelog format of the automatic-releases GitHub Action.
Designed to run inside a GitHub Actions workflow — all required values are
sourced from the standard GHA environment variables automatically.

Required GHA environment variables (set automatically by GitHub Actions):
  GITHUB_TOKEN        - Auth token for API calls          (secrets.GITHUB_TOKEN)
  GITHUB_REPOSITORY   - "owner/repo"                      (github.repository)
  GITHUB_SHA          - Current commit SHA                 (github.sha)
  GITHUB_REF          - Current ref, e.g. refs/tags/v1.2  (github.ref)
  GITHUB_OUTPUT       - Path to the GHA output file        (set by runner)

Usage:
  python generate_changelog.py [PREVIOUS_TAG]

Args:
  PREVIOUS_TAG  - Optional. Tag to compare from. Auto-detected via semver if omitted.
"""

import json
import os
import re
import secrets
import subprocess
import sys
import urllib.error
import urllib.request
from typing import Optional

# ---------------------------------------------------------------------------
# Config — sourced entirely from GHA environment
# ---------------------------------------------------------------------------

def require_env(name: str) -> str:
    val = os.environ.get(name, "")
    if not val:
        die(f"{name} is not set — are you running outside of GitHub Actions?")
    return val

def log(msg: str) -> None:
    print(f"[generate_changelog] {msg}", file=sys.stderr)

def die(msg: str) -> None:
    log(f"ERROR: {msg}")
    sys.exit(1)

PREVIOUS_TAG: str = sys.argv[1] if len(sys.argv) > 1 else ""
GITHUB_TOKEN: str = require_env("GITHUB_TOKEN")
GITHUB_REPOSITORY: str = require_env("GITHUB_REPOSITORY")
GITHUB_SHA: str = os.environ.get("GITHUB_SHA") or subprocess.check_output(
    ["git", "rev-parse", "HEAD"], text=True
).strip()
GITHUB_REF: str = os.environ.get("GITHUB_REF", "")
GITHUB_OUTPUT: str = require_env("GITHUB_OUTPUT")

OWNER, REPO = GITHUB_REPOSITORY.split("/", 1)

log(f"Repo: {OWNER}/{REPO}")
log(f"Current SHA: {GITHUB_SHA}")

# ---------------------------------------------------------------------------
# GitHub API
# ---------------------------------------------------------------------------

def gh_api(endpoint: str) -> list | dict:
    url = f"https://api.github.com{endpoint}"
    req = urllib.request.Request(
        url,
        headers={
            "Authorization": f"token {GITHUB_TOKEN}",
            "Accept": "application/vnd.github.v3+json",
        },
    )
    try:
        with urllib.request.urlopen(req) as resp:
            return json.loads(resp.read())
    except urllib.error.HTTPError as e:
        log(f"API request failed for {url}: {e}")
        return []

# ---------------------------------------------------------------------------
# Conventional commit types (matches ConventionalCommitTypes enum order)
# ---------------------------------------------------------------------------

CC_TYPES: dict[str, str] = {
    "feat":     "Features",
    "fix":      "Bug Fixes",
    "docs":     "Documentation",
    "style":    "Styles",
    "refactor": "Code Refactoring",
    "perf":     "Performance Improvements",
    "test":     "Tests",
    "build":    "Builds",
    "ci":       "Continuous Integration",
    "chore":    "Chores",
    "revert":   "Reverts",
}

CC_HEADER_RE = re.compile(r"^([a-z]+)(\([^)]*\))?(!)?:(.*)$")
BREAKING_BODY_RE = re.compile(r"^BREAKING\s+CHANGES?:\s+", re.MULTILINE)
MERGE_RE = re.compile(r"^Merge pull request #\d+ from ")

# ---------------------------------------------------------------------------
# Git helpers
# ---------------------------------------------------------------------------

def git(*args: str) -> str:
    return subprocess.check_output(["git", *args], text=True).strip()

def get_current_tag() -> str:
    """Extract tag from GITHUB_REF, or fall back to git describe."""
    m = re.match(r"^refs/tags/(.+)$", GITHUB_REF)
    if m:
        tag = m.group(1)
        log(f"Detected tag from GITHUB_REF: {tag}")
        return tag
    try:
        return git("describe", "--exact-match", GITHUB_SHA)
    except subprocess.CalledProcessError:
        return ""

def find_previous_tag(current_tag: str) -> str:
    """Return the nearest semver tag strictly less than current_tag."""
    log(f"Searching for previous semver tag before {current_tag}")
    raw_tags = git("tag", "--list").splitlines()
    semver_re = re.compile(r"^v?\d+\.\d+\.\d+")
    tags = [t for t in raw_tags if semver_re.match(t)]
    if not tags:
        log("No semver tags found")
        return ""

    def parse_semver(tag: str) -> tuple[int, ...]:
        digits = re.sub(r"^v", "", tag)
        parts = re.split(r"[.\-]", digits)
        result = []
        for p in parts[:3]:
            try:
                result.append(int(p))
            except ValueError:
                result.append(0)
        return tuple(result)

    current_ver = parse_semver(current_tag)
    candidates = sorted(
        [t for t in tags if t != current_tag and parse_semver(t) < current_ver],
        key=parse_semver,
        reverse=True,
    )
    return candidates[0] if candidates else ""

def get_commits(base: str, head: str) -> list[tuple[str, str]]:
    """Return list of (sha, subject) tuples."""
    if base:
        log(f"Getting commits between {base} and {head}")
        raw = git("log", "--format=%H %s", f"{base}..{head}")
    else:
        log(f"No previous tag — using full history to {head}")
        raw = git("log", "--format=%H %s", head)
    result = []
    for line in raw.splitlines():
        if not line.strip():
            continue
        sha, _, subject = line.partition(" ")
        result.append((sha, subject))
    return result

# ---------------------------------------------------------------------------
# Conventional commit parsing
# ---------------------------------------------------------------------------

def parse_commit(subject: str) -> tuple[str, str, str]:
    """Return (type, scope, subject). type is empty if not conventional."""
    m = CC_HEADER_RE.match(subject)
    if not m:
        return "", "", ""
    cc_type = m.group(1)
    scope = m.group(2)[1:-1] if m.group(2) else ""  # strip parens
    subj = m.group(4).strip()
    return cc_type, scope, subj

def is_breaking(header: str, body: str) -> bool:
    if re.match(r"^[a-z]+(\([^)]*\))?!:", header):
        return True
    if BREAKING_BODY_RE.search(body or ""):
        return True
    return False

# ---------------------------------------------------------------------------
# Entry formatting (matches getFormattedChangelogEntry)
# ---------------------------------------------------------------------------

def format_entry(
    sha: str,
    header: str,
    author_name: str,
    commit_url: str,
    cc_type: str,
    scope: str,
    subject: str,
    prs: list[dict],
) -> str:
    short = sha[:7]

    pr_parts = [f"[#{pr['number']}]({pr['html_url']})" for pr in prs]
    pr_string = (" " + ",".join(pr_parts)) if pr_parts else ""

    if cc_type:
        scope_str = f"**{scope}**: " if scope else ""
        return f"- {scope_str}{subject}{pr_string} ([{author_name}]({commit_url}))"
    else:
        return f"- {short}: {header} ({author_name}){pr_string}"

# ---------------------------------------------------------------------------
# Changelog assembly (matches generateChangelogFromParsedCommits)
# ---------------------------------------------------------------------------

def generate_changelog(commits: list[tuple[str, str]]) -> str:
    breaking_entries: list[str] = []
    type_entries: dict[str, list[str]] = {k: [] for k in CC_TYPES}
    other_entries: list[str] = []

    for sha, subject in commits:
        if MERGE_RE.match(subject):
            log(f"Skipping merge commit: {sha}")
            continue

        log(f"Processing commit {sha}: {subject}")

        author_name = git("log", "-1", "--format=%an", sha) or "unknown"
        commit_url = f"https://github.com/{OWNER}/{REPO}/commit/{sha}"
        commit_body = git("log", "-1", "--format=%b", sha)

        cc_type, scope, parsed_subject = parse_commit(subject)
        known_type = cc_type if cc_type in CC_TYPES else ""

        breaking = is_breaking(subject, commit_body)

        log(f"Fetching PRs for {sha}")
        prs = gh_api(f"/repos/{OWNER}/{REPO}/commits/{sha}/pulls")
        if not isinstance(prs, list):
            prs = []

        entry = format_entry(sha, subject, author_name, commit_url,
                             known_type, scope, parsed_subject, prs)

        if breaking:
            breaking_entries.append(entry)

        if known_type:
            type_entries[known_type].append(entry)
        else:
            other_entries.append(entry)

    sections: list[str] = []

    if breaking_entries:
        sections.append("## Breaking Changes\n" + "\n".join(breaking_entries))

    for key, label in CC_TYPES.items():
        if type_entries[key]:
            sections.append(f"## {label}\n" + "\n".join(type_entries[key]))

    if other_entries:
        sections.append("## Commits\n" + "\n".join(other_entries))

    return "\n\n".join(sections).strip()

# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------

def main() -> None:
    global PREVIOUS_TAG

    current_tag = get_current_tag()

    if not PREVIOUS_TAG and current_tag:
        PREVIOUS_TAG = find_previous_tag(current_tag)

    log(f"Previous tag: {PREVIOUS_TAG or '<none>'}")

    commits = get_commits(PREVIOUS_TAG, GITHUB_SHA)
    if not commits:
        log("No commits found")
        changelog = ""
    else:
        changelog = generate_changelog(commits)

    # Write to GITHUB_OUTPUT using the multiline heredoc format required by GHA
    # https://docs.github.com/en/actions/writing-workflows/choosing-what-your-workflow-does/passing-information-between-jobs#setting-a-multiline-string
    delimiter = secrets.token_hex(16)
    with open(GITHUB_OUTPUT, "a") as f:
        f.write(f"changelog<<{delimiter}\n{changelog}\n{delimiter}\n")

    log("Changelog written to GITHUB_OUTPUT as 'changelog'")

if __name__ == "__main__":
    main()
