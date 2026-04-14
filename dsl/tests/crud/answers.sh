crud_answers() {
    local base=$1
    local code

    code=$(curl -sS -o $RESP -w '%{http_code}' -X POST "$base/courses/create" \
        -H 'Content-Type: application/json' \
        -d '{"name":"SE","type":"TECNICO","creationDate":"2026-01-01T00:00:00"}')
    [ "$code" = "201" ] || { echo "POST /courses/create => HTTP $code"; return 1; }
    local courseId
    courseId=$(sed -E 's/.*"aggregateId":([0-9]+).*/\1/' $RESP)

    code=$(curl -sS -o $RESP -w '%{http_code}' -X POST "$base/users/create" \
        -H 'Content-Type: application/json' \
        -d '{"name":"Alice","username":"alice","role":"STUDENT","active":true}')
    [ "$code" = "201" ] || { echo "POST /users/create => HTTP $code"; return 1; }
    local userId
    userId=$(sed -E 's/.*"aggregateId":([0-9]+).*/\1/' $RESP)

    code=$(curl -sS -o $RESP -w '%{http_code}' -X POST "$base/executions/create" \
        -H 'Content-Type: application/json' \
        -d "{\"acronym\":\"SE-2026\",\"academicTerm\":\"S1\",\"endDate\":\"2026-12-31T00:00:00\",\"course\":{\"aggregateId\":$courseId},\"users\":[{\"aggregateId\":$userId}]}")
    [ "$code" = "201" ] || { echo "POST /executions/create => HTTP $code"; return 1; }

    grep -q "\"name\":\"SE\"" $RESP || { echo "course projection not enriched (name=SE expected)"; return 1; }
    grep -q "\"username\":\"alice\"" $RESP || { echo "user projection not enriched (username=alice expected)"; return 1; }

    code=$(curl -sS -o $RESP -w '%{http_code}' -X DELETE "$base/courses/$courseId")
    if [ "$code" = "204" ]; then
        echo "DELETE /courses/$courseId succeeded but should be blocked by prevent"
        return 1
    fi
    grep -q "Cannot delete course that has executions" $RESP || {
        echo "prevent message missing (got: $(cat $RESP))"
        return 1
    }

    code=$(curl -sS -o /dev/null -w '%{http_code}' "$base/courses/$courseId")
    [ "$code" = "200" ] || { echo "GET /courses/$courseId => HTTP $code (expected 200)"; return 1; }

    code=$(curl -sS -o /dev/null -w '%{http_code}' -X DELETE "$base/users/$userId")
    [ "$code" = "204" ] || { echo "DELETE /users/$userId => HTTP $code"; return 1; }

    code=$(curl -sS -o /dev/null -w '%{http_code}' "$base/users/$userId")
    [ "$code" = "404" ] || { echo "GET /users/$userId (deleted) => HTTP $code (expected 404)"; return 1; }

    return 0
}
