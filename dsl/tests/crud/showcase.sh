crud_showcase() {
    local base=$1 code roomId bookingId

    # 1. CRUD baseline: create a User via the standard /users/create endpoint.
    code=$(curl -sS -o $RESP -w '%{http_code}' -X POST "$base/users/create" \
        -H 'Content-Type: application/json' \
        -d '{"username":"alice","email":"alice@example.com","loyaltyPoints":0,"tier":"BRONZE","active":true}')
    [ "$code" = "201" ] || { echo "POST /users/create => HTTP $code"; return 1; }
    local userId
    userId=$(sed -E 's/.*"aggregateId":([0-9]+).*/\1/' $RESP)

    # 2. Custom WebAPIEndpoint for a custom method: POST /users/signup (RequestParam binding).
    code=$(curl -sS -o /dev/null -w '%{http_code}' -X POST "$base/users/signup?username=bob&email=bob@example.com")
    [ "$code" = "200" ] || { echo "POST /users/signup => HTTP $code"; return 1; }

    code=$(curl -sS -o $RESP -w '%{http_code}' "$base/users")
    [ "$code" = "200" ] || { echo "GET /users => HTTP $code"; return 1; }
    grep -q "\"username\":\"bob\"" $RESP || { echo "signup user not in /users list"; return 1; }
    grep -q "\"tier\":\"BRONZE\"" $RESP || { echo "default tier BRONZE not applied by signup"; return 1; }

    # 3. Custom WebAPIEndpoint with PathVariable + RequestParam: award loyalty points.
    code=$(curl -sS -o /dev/null -w '%{http_code}' -X POST "$base/users/$userId/loyalty?points=50")
    [ "$code" = "200" ] || { echo "POST /users/$userId/loyalty => HTTP $code"; return 1; }

    code=$(curl -sS -o $RESP -w '%{http_code}' "$base/users/$userId")
    [ "$code" = "200" ] || { echo "GET /users/$userId => HTTP $code"; return 1; }
    grep -q "\"loyaltyPoints\":50" $RESP || { echo "awardLoyaltyPoints did not set loyaltyPoints to 50"; return 1; }

    # 4. Room state machine: AVAILABLE -> RESERVED -> OCCUPIED -> AVAILABLE.
    code=$(curl -sS -o $RESP -w '%{http_code}' -X POST "$base/rooms/create" \
        -H 'Content-Type: application/json' \
        -d '{"roomNumber":"101","description":"Suite","pricePerNight":250.0,"amenities":[],"status":"AVAILABLE"}')
    [ "$code" = "201" ] || { echo "POST /rooms/create => HTTP $code"; return 1; }
    roomId=$(sed -E 's/.*"aggregateId":([0-9]+).*/\1/' $RESP)

    code=$(curl -sS -o /dev/null -w '%{http_code}' -X POST "$base/rooms/$roomId/reserve")
    [ "$code" = "200" ] || { echo "POST /rooms/$roomId/reserve => HTTP $code"; return 1; }
    code=$(curl -sS -o $RESP -w '%{http_code}' "$base/rooms/$roomId")
    grep -q "\"status\":\"RESERVED\"" $RESP || { echo "reserve did not set status to RESERVED"; return 1; }

    code=$(curl -sS -o /dev/null -w '%{http_code}' -X POST "$base/rooms/$roomId/checkin")
    [ "$code" = "200" ] || { echo "POST /rooms/$roomId/checkin => HTTP $code"; return 1; }
    code=$(curl -sS -o $RESP -w '%{http_code}' "$base/rooms/$roomId")
    grep -q "\"status\":\"OCCUPIED\"" $RESP || { echo "checkIn did not set status to OCCUPIED"; return 1; }

    code=$(curl -sS -o /dev/null -w '%{http_code}' -X POST "$base/rooms/$roomId/checkout")
    [ "$code" = "200" ] || { echo "POST /rooms/$roomId/checkout => HTTP $code"; return 1; }
    code=$(curl -sS -o $RESP -w '%{http_code}' "$base/rooms/$roomId")
    grep -q "\"status\":\"AVAILABLE\"" $RESP || { echo "checkOut did not set status back to AVAILABLE"; return 1; }

    # 5. Booking + projection enrichment + confirmBooking custom endpoint.
    code=$(curl -sS -o $RESP -w '%{http_code}' -X POST "$base/bookings/create" \
        -H 'Content-Type: application/json' \
        -d "{\"user\":{\"aggregateId\":$userId},\"room\":{\"aggregateId\":$roomId},\"checkInDate\":\"2026-04-15\",\"checkOutDate\":\"2026-04-16\",\"numberOfNights\":1,\"totalPrice\":250.0,\"paymentMethod\":\"CREDIT_CARD\",\"confirmed\":false}")
    [ "$code" = "201" ] || { echo "POST /bookings/create => HTTP $code"; return 1; }
    # The booking response embeds nested user.aggregateId and room.aggregateId; take the first match.
    bookingId=$(grep -o '"aggregateId":[0-9]*' $RESP | head -1 | sed 's/.*://')
    grep -q "\"username\":\"alice\"" $RESP || { echo "booking user projection not enriched (expected username=alice)"; return 1; }
    grep -q "\"roomNumber\":\"101\"" $RESP || { echo "booking room projection not enriched (expected roomNumber=101)"; return 1; }
    grep -q "\"confirmed\":false" $RESP || { echo "booking confirmed default not false"; return 1; }
    grep -q "\"paymentMethod\":\"CREDIT_CARD\"" $RESP || { echo "paymentMethod not preserved"; return 1; }

    code=$(curl -sS -o /dev/null -w '%{http_code}' -X POST "$base/bookings/$bookingId/confirm")
    [ "$code" = "200" ] || { echo "POST /bookings/$bookingId/confirm => HTTP $code"; return 1; }

    code=$(curl -sS -o $RESP -w '%{http_code}' "$base/bookings/$bookingId")
    [ "$code" = "200" ] || { echo "GET /bookings/$bookingId => HTTP $code"; return 1; }
    grep -q "\"confirmed\":true" $RESP || { echo "confirmBooking did not flip confirmed to true"; return 1; }

    # 6. @PreventDelete: deleting a User with active bookings must be blocked.
    code=$(curl -sS -o $RESP -w '%{http_code}' -X DELETE "$base/users/$userId")
    if [ "$code" = "204" ]; then
        echo "DELETE /users/$userId succeeded but should be blocked by @PreventDelete"
        return 1
    fi
    grep -q "Cannot delete user that has bookings" $RESP || {
        echo "expected prevent message missing (got: $(cat $RESP))"
        return 1
    }

    return 0
}
