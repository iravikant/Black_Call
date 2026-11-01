#!/usr/bin/env bash
# One-time GitHub setup using the GitHub CLI (https://cli.github.com, run `gh auth login` first).
# Usage: ./scripts/setup-github.sh OWNER/REPO [path/to/release.jks]
set -euo pipefail
REPO="${1:?Usage: $0 OWNER/REPO [keystore.jks]}"
KS="${2:-}"

DESC="🛡️ On-device Android call blocker. Stops spam & unwanted calls before the phone rings. No internet permission. Kotlin + Jetpack Compose."

# Create the repo if it doesn't exist yet, otherwise just update it.
if ! gh repo view "$REPO" >/dev/null 2>&1; then
  gh repo create "$REPO" --public --description "$DESC" --source=. --remote=origin
fi

gh repo edit "$REPO" \
  --description "$DESC" \
  --enable-issues \
  --add-topic android --add-topic kotlin --add-topic jetpack-compose --add-topic material3 \
  --add-topic call-blocker --add-topic spam-blocker --add-topic call-screening \
  --add-topic privacy --add-topic offline --add-topic room-database

# Replace OWNER/REPO placeholders in the README badges/links
sed -i.bak "s#OWNER/REPO#$REPO#g" README.md && rm -f README.md.bak

if [ -n "$KS" ]; then
  read -rp  "Key alias: " ALIAS
  read -rsp "Keystore password: " KSPASS; echo
  read -rsp "Key password (Enter = same): " KPASS; echo
  KPASS="${KPASS:-$KSPASS}"
  base64 "$KS" | tr -d '\r\n' | gh secret set KEYSTORE_BASE64 -R "$REPO"
  printf '%s' "$KSPASS" | gh secret set KEYSTORE_PASSWORD -R "$REPO"
  printf '%s' "$ALIAS"  | gh secret set KEY_ALIAS         -R "$REPO"
  printf '%s' "$KPASS"  | gh secret set KEY_PASSWORD      -R "$REPO"
  echo "Signing secrets set."
fi
echo "Done. Commit the README change, push, then run ./scripts/release.sh 1.0.0"
