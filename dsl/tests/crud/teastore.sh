crud_teastore() {
    local base=$1 code id
    code=$(curl -sS -o $RESP -w '%{http_code}' -X POST "$base/users/create" \
        -H 'Content-Type: application/json' \
        -d '{"userName":"alice","password":"secret","realName":"Alice","email":"alice@example.com"}')
    [ "$code" = "201" ] || { echo "POST /users/create => HTTP $code"; return 1; }
    id=$(sed -E 's/.*"aggregateId":([0-9]+).*/\1/' $RESP)

    code=$(curl -sS -o $RESP -w '%{http_code}' "$base/users")
    [ "$code" = "200" ] || { echo "GET /users => HTTP $code"; return 1; }
    grep -q "\"userName\":\"alice\"" $RESP || { echo "user not in list"; return 1; }

    code=$(curl -sS -o /dev/null -w '%{http_code}' -X DELETE "$base/users/$id")
    [ "$code" = "204" ] || { echo "DELETE /users/$id => HTTP $code"; return 1; }
    code=$(curl -sS -o /dev/null -w '%{http_code}' "$base/users/$id")
    [ "$code" = "404" ] || { echo "GET /users/$id (deleted) => HTTP $code (expected 404)"; return 1; }
    return 0
}
