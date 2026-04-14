crud_05_eventdriven() {
    local base=$1
    local code

    code=$(curl -sS -o $RESP -w '%{http_code}' -X POST "$base/authors/create" \
        -H 'Content-Type: application/json' \
        -d '{"name":"Ada","bio":"Pioneer"}')
    [ "$code" = "201" ] || { echo "POST /authors/create => HTTP $code"; return 1; }
    local authorId
    authorId=$(sed -E 's/.*"aggregateId":([0-9]+).*/\1/' $RESP)

    code=$(curl -sS -o $RESP -w '%{http_code}' -X POST "$base/posts/create" \
        -H 'Content-Type: application/json' \
        -d "{\"title\":\"First\",\"content\":\"Hello world\",\"publishedAt\":\"2026-01-01T00:00:00\",\"author\":{\"aggregateId\":$authorId}}")
    [ "$code" = "201" ] || { echo "POST /posts/create => HTTP $code"; return 1; }
    grep -q "\"name\":\"Ada\"" $RESP || { echo "post author projection not enriched (name=Ada expected)"; return 1; }

    code=$(curl -sS -o $RESP -w '%{http_code}' "$base/posts")
    [ "$code" = "200" ] || { echo "GET /posts => HTTP $code"; return 1; }
    grep -q "\"title\":\"First\"" $RESP || { echo "post not in /posts list"; return 1; }

    code=$(curl -sS -o /dev/null -w '%{http_code}' -X DELETE "$base/authors/$authorId")
    [ "$code" = "204" ] || { echo "DELETE /authors/$authorId => HTTP $code"; return 1; }

    code=$(curl -sS -o /dev/null -w '%{http_code}' "$base/authors/$authorId")
    [ "$code" = "404" ] || { echo "GET /authors/$authorId (deleted) => HTTP $code (expected 404)"; return 1; }

    return 0
}
