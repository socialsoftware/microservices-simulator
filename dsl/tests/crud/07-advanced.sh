crud_07_advanced() {
    local base=$1 code customerId

    code=$(curl -sS -o $RESP -w '%{http_code}' -X POST "$base/customers/create" \
        -H 'Content-Type: application/json' \
        -d '{"name":"Bob","email":"bob@example.com","active":true}')
    [ "$code" = "201" ] || { echo "POST /customers/create => HTTP $code"; return 1; }
    customerId=$(sed -E 's/.*"aggregateId":([0-9]+).*/\1/' $RESP)

    code=$(curl -sS -o $RESP -w '%{http_code}' "$base/customers/$customerId")
    [ "$code" = "200" ] || { echo "GET /customers/$customerId => HTTP $code"; return 1; }

    code=$(curl -sS -o /dev/null -w '%{http_code}' -X DELETE "$base/customers/$customerId")
    [ "$code" = "204" ] || { echo "DELETE /customers/$customerId => HTTP $code"; return 1; }
    code=$(curl -sS -o /dev/null -w '%{http_code}' "$base/customers/$customerId")
    [ "$code" = "404" ] || { echo "GET /customers/$customerId (deleted) => HTTP $code (expected 404)"; return 1; }
    return 0
}
