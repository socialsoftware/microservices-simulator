crud_02_typesenums() {
    local base=$1 code id
    code=$(curl -sS -o $RESP -w '%{http_code}' -X POST "$base/contacts/create" \
        -H 'Content-Type: application/json' \
        -d '{"firstName":"Ada","lastName":"Lovelace","email":"ada@example.com","category":"WORK","createdAt":"2026-01-01T00:00:00","favorite":true,"callCount":0}')
    [ "$code" = "201" ] || { echo "POST /contacts/create => HTTP $code"; return 1; }
    id=$(sed -E 's/.*"aggregateId":([0-9]+).*/\1/' $RESP)

    code=$(curl -sS -o $RESP -w '%{http_code}' "$base/contacts/$id")
    [ "$code" = "200" ] || { echo "GET /contacts/$id => HTTP $code"; return 1; }
    grep -q "\"category\":\"WORK\"" $RESP || { echo "enum category not preserved"; return 1; }
    grep -q "\"favorite\":true" $RESP || { echo "favorite flag not preserved"; return 1; }
    return 0
}
