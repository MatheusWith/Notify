#!/bin/bash
# Smoke tests for Notify Docker deployment
# Usage: ./deploy/smoke-test.sh [base_url]
# Default: http://localhost:80

set -euo pipefail

BASE="${1:-http://localhost:80}"
API="$BASE/api/v1"
PASS=0
FAIL=0
SLUG="admin-announcements"

green() { echo -e "\033[32m✓ PASS\033[0m $1"; PASS=$((PASS + 1)); }
red()   { echo -e "\033[31m✗ FAIL\033[0m $1"; FAIL=$((FAIL + 1)); }

echo "========================================"
echo "  Notify Smoke Tests (base: $BASE)"
echo "========================================"

# --- F1: Default Server Block (444) ---
echo -e "\n--- F1: Default Server Block ---"
curl -s -o /dev/null -w "%{http_code}" -H "Host: bogus.notify.local" "http://127.0.0.1:80/" > /dev/null 2>&1 && rc=0 || rc=$?
[ "$rc" -eq 52 ] && green "Unknown Host → 444 (curl exit 52)" || red "Default server did not return 444 (rc=$rc)"

# --- F2: SPA Root ---
echo -e "\n--- F2: SPA Root ---"
code=$(curl -s -o /dev/null -w "%{http_code}" "$BASE/")
[ "$code" = "200" ] && green "GET / → 200" || red "GET / → $code (expected 200)"
title=$(curl -s "$BASE/" | grep -o '<title>[^<]*</title>' | head -1)
[ -n "$title" ] && green "SPA has <title>: $title" || red "SPA missing <title>"

# --- F3: SPA Deep Link ---
echo -e "\n--- F3: SPA Routing ---"
code=$(curl -s -o /dev/null -w "%{http_code}" "$BASE/sender/newsletters/test/campaigns")
[ "$code" = "200" ] && green "SPA deep link → 200" || red "SPA deep link → $code (expected 200)"

# --- F4: API Health ---
echo -e "\n--- F4: API Health ---"
code=$(curl -s -o /dev/null -w "%{http_code}" "$API/health")
[ "$code" = "200" ] && green "GET /api/v1/health → 200" || red "GET /api/v1/health → $code (expected 200)"

# --- Get token ---
echo -e "\n--- Authentication: Login ---"
resp=$(curl -s -X POST "$API/auth/login" -H "Content-Type: application/json" \
  -d '{"email":"admin@notify.com","password":"Admin@123"}')
TOKEN=$(echo "$resp" | python3 -c "import sys,json; print(json.load(sys.stdin).get('accessToken',''))" 2>/dev/null || echo "")
[ -n "$TOKEN" ] && green "JWT token obtained" || red "No accessToken received"

# --- F5: Authentication ---
echo -e "\n--- F5: Authentication ---"
# Bad password
sleep 1
bad_code=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$API/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@notify.com","password":"wrong"}')
[ "$bad_code" = "401" ] && green "Invalid credentials → 401" || red "Invalid credentials → $bad_code (expected 401)"

# --- F6: Authorization ---
echo -e "\n--- F6: Authorization ---"
auth_code=$(curl -s -o /dev/null -w "%{http_code}" "$API/users/me" -H "Authorization: Bearer $TOKEN")
[ "$auth_code" = "200" ] && green "GET /users/me (auth) → 200" || red "GET /users/me (auth) → $auth_code (expected 200)"
noauth_code=$(curl -s -o /dev/null -w "%{http_code}" "$API/users/me")
[ "$noauth_code" = "401" ] && green "GET /users/me (no auth) → 401" || red "GET /users/me (no auth) → $noauth_code (expected 401)"

# --- F7: Newsletter ---
echo -e "\n--- F7: Newsletter ---"
slug200=$(curl -s -o /dev/null -w "%{http_code}" "$API/newsletter/$SLUG" -H "Authorization: Bearer $TOKEN")
[ "$slug200" = "200" ] && green "GET /newsletter/$SLUG → 200" || red "GET /newsletter/$SLUG → $slug200 (expected 200)"
slug404=$(curl -s -o /dev/null -w "%{http_code}" "$API/newsletter/bogus" -H "Authorization: Bearer $TOKEN")
[ "$slug404" = "404" ] && green "GET /newsletter/bogus → 404" || red "GET /newsletter/bogus → $slug404 (expected 404)"

# --- F8: Subscribers ---
echo -e "\n--- F8: Subscribers ---"
sub_code=$(curl -s -o /dev/null -w "%{http_code}" "$API/newsletter/$SLUG/subscribers" -H "Authorization: Bearer $TOKEN")
[ "$sub_code" = "200" ] && green "GET subscribers (owner) → 200" || red "GET subscribers (owner) → $sub_code (expected 200)"
sub_unauth=$(curl -s -o /dev/null -w "%{http_code}" "$API/newsletter/$SLUG/subscribers")
[ "$sub_unauth" = "401" ] && green "GET subscribers (no auth) → 401" || red "GET subscribers (no auth) → $sub_unauth (expected 401)"

# --- F9: Campaign CRUD ---
echo -e "\n--- F9: Campaign CRUD ---"
# Create
camp_resp=$(curl -s -X POST "$API/newsletter/$SLUG/campaigns" \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"subject":"Smoke Campaign","content":"Test body from smoke test"}')
camp_code=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$API/newsletter/$SLUG/campaigns" \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"subject":"Smoke Campaign","content":"Test body from smoke test"}')
CID=$(echo "$camp_resp" | python3 -c "import sys,json; print(json.load(sys.stdin).get('id',''))" 2>/dev/null)
[ "$camp_code" = "201" ] && green "POST /campaigns → 201" || red "POST /campaigns → $camp_code (expected 201)"
[ -n "$CID" ] && green "Campaign ID: ${CID:0:8}..." || red "No campaign ID in response"

# List
list_code=$(curl -s -o /dev/null -w "%{http_code}" "$API/newsletter/$SLUG/campaigns" -H "Authorization: Bearer $TOKEN")
[ "$list_code" = "200" ] && green "GET /campaigns → 200" || red "GET /campaigns → $list_code (expected 200)"

# Get single
get_code=$(curl -s -o /dev/null -w "%{http_code}" "$API/newsletter/$SLUG/campaigns/$CID" -H "Authorization: Bearer $TOKEN")
[ "$get_code" = "200" ] && green "GET /campaigns/$CID → 200" || red "GET /campaigns/$CID → $get_code (expected 200)"

# Update
put_code=$(curl -s -o /dev/null -w "%{http_code}" -X PUT "$API/newsletter/$SLUG/campaigns/$CID" \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"subject":"Updated Campaign","content":"Updated body"}')
[ "$put_code" = "200" ] && green "PUT /campaigns/$CID → 200" || red "PUT /campaigns/$CID → $put_code (expected 200)"

# Publish
pub_code=$(curl -s -o /dev/null -w "%{http_code}" -X PATCH "$API/newsletter/$SLUG/campaigns/$CID/status" \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"status":"PUBLISHED"}')
[ "$pub_code" = "200" ] && green "PATCH /campaigns/$CID/status (PUBLISHED) → 200" || red "Publish → $pub_code (expected 200)"

# Delete published (should fail)
del_code=$(curl -s -o /dev/null -w "%{http_code}" -X DELETE "$API/newsletter/$SLUG/campaigns/$CID" \
  -H "Authorization: Bearer $TOKEN")
[ "$del_code" = "409" ] && green "DELETE published campaign → 409" || red "DELETE → $del_code (expected 409)"

# --- F10: Static Assets ---
echo -e "\n--- F10: Static Assets ---"
js_file=$(curl -s "$BASE/" | grep -o 'src="[^"]*\.js"' | head -1 | sed 's/src="//;s/"//')
[ -n "$js_file" ] && green "Found JS file: $js_file" || red "Could not determine JS filename"
cache=$(curl -sI "$BASE/$js_file" 2>/dev/null | grep -ci "max-age=31536000" || true)
[ "$cache" -gt 0 ] && green "Static assets cached (1 year, immutable)" || red "Static assets missing long cache headers"

# --- F11: Security Headers ---
echo -e "\n--- F11: Security Headers ---"
hdr=$(curl -sI "$API/health" 2>/dev/null)
xct=$(echo "$hdr" | grep -ci "X-Content-Type-Options: nosniff" || true)
xfo=$(echo "$hdr" | grep -ci "X-Frame-Options: DENY" || true)
[ "$xct" -gt 0 ] && green "X-Content-Type-Options: nosniff" || red "Missing X-Content-Type-Options"
[ "$xfo" -gt 0 ] && green "X-Frame-Options: DENY" || red "Missing X-Frame-Options"

# --- F12: Rate Limiting ---
echo -e "\n--- F12: Rate Limiting ---"
limited=0
for i in $(seq 1 30); do
  rc=$(curl -s -o /dev/null -w "%{http_code}" "$API/health")
  if [ "$rc" = "429" ] || [ "$rc" = "503" ]; then limited=1; break; fi
done
[ "$limited" -eq 1 ] && green "Rate limiting active (triggered within 30 requests)" || red "Rate limit never triggered"

echo ""
echo "========================================"
echo "  Results: $PASS passed, $FAIL failed"
echo "========================================"

[ "$FAIL" -eq 0 ] && exit 0 || exit 1
