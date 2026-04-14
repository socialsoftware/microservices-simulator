crud_04_crossrefs() {
    local base=$1
    local code

    code=$(curl -sS -o $RESP -w '%{http_code}' -X POST "$base/teachers/create" \
        -H 'Content-Type: application/json' \
        -d '{"name":"Dr Smith","email":"smith@example.com","department":"CS"}')
    [ "$code" = "201" ] || { echo "POST /teachers/create => HTTP $code"; return 1; }
    local teacherId
    teacherId=$(sed -E 's/.*"aggregateId":([0-9]+).*/\1/' $RESP)

    code=$(curl -sS -o $RESP -w '%{http_code}' -X POST "$base/courses/create" \
        -H 'Content-Type: application/json' \
        -d "{\"title\":\"Algorithms\",\"description\":\"Intro\",\"maxStudents\":50,\"teacher\":{\"aggregateId\":$teacherId}}")
    [ "$code" = "201" ] || { echo "POST /courses/create => HTTP $code"; return 1; }

    grep -q "\"name\":\"Dr Smith\"" $RESP || { echo "course teacher projection not enriched (name expected)"; return 1; }
    grep -q "\"email\":\"smith@example.com\"" $RESP || { echo "course teacher projection not enriched (email expected)"; return 1; }

    code=$(curl -sS -o $RESP -w '%{http_code}' -X DELETE "$base/teachers/$teacherId")
    if [ "$code" = "204" ]; then
        echo "DELETE /teachers/$teacherId succeeded but should be blocked by prevent"
        return 1
    fi
    grep -q "Cannot delete teacher with active courses" $RESP || {
        echo "prevent message missing (got: $(cat $RESP))"
        return 1
    }

    code=$(curl -sS -o /dev/null -w '%{http_code}' "$base/teachers/$teacherId")
    [ "$code" = "200" ] || { echo "GET /teachers/$teacherId => HTTP $code (expected 200)"; return 1; }

    return 0
}
