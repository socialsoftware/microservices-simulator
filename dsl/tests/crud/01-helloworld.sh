crud_01_helloworld() {
    local base=$1 code id
    code=$(curl -sS -o $RESP -w '%{http_code}' -X POST "$base/tasks/create" \
        -H 'Content-Type: application/json' \
        -d '{"title":"Buy milk","description":"At the store","done":false}')
    [ "$code" = "201" ] || { echo "POST /tasks/create => HTTP $code"; return 1; }
    id=$(sed -E 's/.*"aggregateId":([0-9]+).*/\1/' $RESP)

    code=$(curl -sS -o $RESP -w '%{http_code}' "$base/tasks/$id")
    [ "$code" = "200" ] || { echo "GET /tasks/$id => HTTP $code"; return 1; }
    grep -q "\"title\":\"Buy milk\"" $RESP || { echo "task title not preserved"; return 1; }
    return 0
}
