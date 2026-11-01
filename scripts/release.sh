#!/usr/bin/env bash
# Usage: ./scripts/release.sh 1.1.0   (or 1.1.0-beta.1)
# Creates an annotated tag vX.Y.Z on the current commit and pushes it, which triggers the Release workflow.
set -euo pipefail

VERSION="${1:-}"
[[ "$VERSION" =~ ^[0-9]+\.[0-9]+\.[0-9]+(-[0-9A-Za-z.]+)?$ ]] || { echo "Usage: $0 MAJOR.MINOR.PATCH[-suffix]"; exit 1; }
TAG="v$VERSION"

git diff --quiet && git diff --cached --quiet || { echo "Working tree not clean. Commit first."; exit 1; }
git rev-parse "$TAG" >/dev/null 2>&1 && { echo "Tag $TAG already exists."; exit 1; }

BRANCH=$(git rev-parse --abbrev-ref HEAD)
echo "Releasing $TAG from $BRANCH ..."
git push origin "$BRANCH"
git tag -a "$TAG" -m "Black Call $TAG"
git push origin "$TAG"
echo "Done. Watch: https://github.com/$(git remote get-url origin | sed -E 's#.*github.com[:/]##; s#\.git$##')/actions"
