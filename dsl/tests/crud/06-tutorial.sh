crud_06_tutorial() {
    local base=$1 code id
    code=$(curl -sS -o $RESP -w '%{http_code}' -X POST "$base/members/create" \
        -H 'Content-Type: application/json' \
        -d '{"name":"Alice","email":"alice@example.com","membership":"BASIC"}')
    [ "$code" = "201" ] || { echo "POST /members/create => HTTP $code"; return 1; }
    id=$(sed -E 's/.*"aggregateId":([0-9]+).*/\1/' $RESP)

    code=$(curl -sS -o $RESP -w '%{http_code}' "$base/members")
    [ "$code" = "200" ] || { echo "GET /members => HTTP $code"; return 1; }
    grep -q "\"name\":\"Alice\"" $RESP || { echo "Alice missing in /members"; return 1; }

    code=$(curl -sS -o $RESP -w '%{http_code}' -X PUT "$base/members" \
        -H 'Content-Type: application/json' \
        -d "{\"aggregateId\":$id,\"name\":\"Alice Renamed\",\"email\":\"alice@example.com\",\"membership\":\"PREMIUM\"}")
    [ "$code" = "200" ] || { echo "PUT /members => HTTP $code"; return 1; }
    grep -q "\"name\":\"Alice Renamed\"" $RESP || { echo "PUT did not update name"; return 1; }

    code=$(curl -sS -o /dev/null -w '%{http_code}' -X DELETE "$base/members/$id")
    [ "$code" = "204" ] || { echo "DELETE /members/$id => HTTP $code"; return 1; }

    code=$(curl -sS -o $RESP -w '%{http_code}' "$base/members/$id")
    [ "$code" = "404" ] || { echo "GET /members/$id (deleted) => HTTP $code (expected 404)"; return 1; }
    return 0
}
