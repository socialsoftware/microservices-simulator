crud_03_businessrules() {
    local base=$1 code

    code=$(curl -sS -o $RESP -w '%{http_code}' -X POST "$base/products/create" \
        -H 'Content-Type: application/json' \
        -d '{"name":"","sku":"SKU-1","price":9.99,"stockQuantity":5,"active":true}')
    [ "$code" != "201" ] || { echo "blank name should fail invariant but got HTTP $code"; return 1; }

    code=$(curl -sS -o $RESP -w '%{http_code}' -X POST "$base/products/create" \
        -H 'Content-Type: application/json' \
        -d '{"name":"Widget","sku":"SKU-1","price":-1.0,"stockQuantity":5,"active":true}')
    [ "$code" != "201" ] || { echo "negative price should fail invariant but got HTTP $code"; return 1; }

    code=$(curl -sS -o $RESP -w '%{http_code}' -X POST "$base/products/create" \
        -H 'Content-Type: application/json' \
        -d '{"name":"Widget","sku":"SKU-1","price":9.99,"stockQuantity":5,"active":true}')
    [ "$code" = "201" ] || { echo "POST /products/create (happy) => HTTP $code"; return 1; }
    grep -q "\"name\":\"Widget\"" $RESP || { echo "product name missing"; return 1; }
    return 0
}
