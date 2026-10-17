const assert = require("assert");
const { decide, recipientOf, buildPayload, MAX_AGE_MS } = require("./friend_request_push");

const now = Date.UTC(2026, 9, 15, 12, 0, 0);
const ts = (ms) => ({ toMillis: () => ms });
const fresh = { uid1: "a", uid2: "b", requestedBy: "a", status: "pending", createdAt: ts(now - 60 * 1000) };

// a new pending request is announced to the OTHER person
assert.strictEqual(decide(fresh, now), "notify");
assert.strictEqual(recipientOf(fresh), "b");
assert.strictEqual(recipientOf({ ...fresh, requestedBy: "b" }), "a");

// already announced, accepted or not pending: nothing to do
assert.strictEqual(decide({ ...fresh, notifiedRequest: true }, now), "skip");
assert.strictEqual(decide({ ...fresh, status: "accepted" }, now), "skip");
assert.strictEqual(decide(null, now), "skip");

// old requests (from before this feature) and incomplete documents are marked without a push
assert.strictEqual(decide({ ...fresh, createdAt: ts(now - MAX_AGE_MS - 1) }, now), "mark");
assert.strictEqual(decide({ ...fresh, createdAt: ts(now - MAX_AGE_MS) }, now), "notify");
assert.strictEqual(decide({ ...fresh, createdAt: undefined }, now), "mark");
assert.strictEqual(decide({ ...fresh, requestedBy: "zzz" }, now), "mark");
assert.strictEqual(decide({ ...fresh, uid2: undefined }, now), "mark");

// the payload: shape the app expects, never the keys the app would take for a message or a challenge
const payload = buildPayload("  Zunaid   Hossain ", "a");
assert.deepStrictEqual(payload, {
  type: "friend_request", fromUid: "a", title: "New friend request",
  message: "Zunaid Hossain wants to be your friend on KardIQ.", route: "friends",
});
assert.ok(!("pairId" in payload) && !("challengeId" in payload));
assert.strictEqual(buildPayload("", "a").message, "Someone wants to be your friend on KardIQ.");
assert.strictEqual(buildPayload("x".repeat(200), "a").message.length, 60 + " wants to be your friend on KardIQ.".length);
console.log("friend_request_push: all checks passed");
