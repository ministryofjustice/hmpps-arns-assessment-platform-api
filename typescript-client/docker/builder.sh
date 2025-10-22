#!/usr/bin/env bash

CACHE_DIR="${CACHE_DIR:-./.cache}"
SPEC_JSON="${SPEC_JSON:-$CACHE_DIR/spec.json}"
SPEC_SORT="${SPEC_SORT:-$CACHE_DIR/spec.sorted.json}"
HASH_FILE="${HASH_FILE:-$CACHE_DIR/spec.sha256}"
OUT_DIR="${OUT_DIR:-./src}"
CLIENT_DIR="$OUT_DIR/client"

mkdir -p ${CACHE_DIR}
echo "📥 Fetching OpenAPI spec from $SPEC_URL"
curl -sSL "$SPEC_URL" -o ${SPEC_JSON}
jq -S . ${SPEC_JSON} > ${SPEC_SORT}

NEW_HASH=$(sha256sum ${SPEC_SORT} | cut -d' ' -f1)
OLD_HASH=$(cat ${HASH_FILE} 2>/dev/null || true)

if [ "$NEW_HASH" = "$OLD_HASH" ]; then
  echo '✅ Spec unchanged, skipping codegen'
  echo "✅ The output is in ${HOST_DIR:+$HOST_DIR/}$CLIENT_DIR"
  exit 0
fi

echo "⬆️ Spec updated! Old hash: $OLD_HASH"
echo "🔄 New hash: $NEW_HASH"
echo "$NEW_HASH" > ${HASH_FILE}
echo "⚙️ Generating client..."

mkdir -p ${CLIENT_DIR}
rm -rf ${CLIENT_DIR}/*

npx --yes @hey-api/openapi-ts -i ${SPEC_JSON} -o ${CLIENT_DIR}

echo "✅ Done! The output is in ${HOST_DIR:+$HOST_DIR/}$CLIENT_DIR"
