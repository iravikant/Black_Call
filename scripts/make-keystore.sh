#!/usr/bin/env bash
# Creates a release keystore and prints the base64 value for the KEYSTORE_BASE64 secret.
set -euo pipefail
OUT="${1:-black-call-release.jks}"
ALIAS="${2:-blackcall}"
[ -e "$OUT" ] && { echo "$OUT already exists, refusing to overwrite."; exit 1; }
keytool -genkeypair -v -keystore "$OUT" -alias "$ALIAS" -keyalg RSA -keysize 4096 -validity 36500
echo
echo "Keystore: $OUT   Alias: $ALIAS"
echo "BACK THIS FILE UP SOMEWHERE SAFE. It is git-ignored; never commit it."
echo
echo "KEYSTORE_BASE64 value:"
base64 -w0 "$OUT" 2>/dev/null || base64 "$OUT"
echo
