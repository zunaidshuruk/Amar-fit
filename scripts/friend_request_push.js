// Decisions for the friend-request push (kept apart from Firestore so they can be tested without a database).
// A friendship document is { uid1, uid2, requestedBy, status, createdAt, notifiedRequest? }.

const MAX_AGE_MS = 7 * 24 * 60 * 60 * 1000;

function createdMillis(friendship) {
  const c = friendship && friendship.createdAt;
  if (c && typeof c.toMillis === "function") return c.toMillis();
  if (typeof c === "number") return c;
  return 0;
}

// "skip"   = nothing to do (not pending, or already notified)
// "mark"   = mark it as handled WITHOUT sending (an old request from before this feature, or an incomplete document), so nobody gets stale pushes
// "notify" = send the push, then mark it
function decide(friendship, nowMs) {
  if (!friendship || friendship.status !== "pending" || friendship.notifiedRequest === true) return "skip";
  const { uid1, uid2, requestedBy } = friendship;
  if (!uid1 || !uid2 || !requestedBy || (requestedBy !== uid1 && requestedBy !== uid2)) return "mark";
  const created = createdMillis(friendship);
  if (!created || nowMs - created > MAX_AGE_MS) return "mark";
  return "notify";
}

function recipientOf(friendship) {
  return friendship.requestedBy === friendship.uid1 ? friendship.uid2 : friendship.uid1;
}

// The data payload must NOT contain "pairId" or "challengeId": the app would take it for a direct message or a challenge.
function buildPayload(requesterName, requestedBy) {
  const name = String(requesterName || "Someone").replace(/\s+/g, " ").trim().slice(0, 60) || "Someone";
  return {
    type: "friend_request",
    fromUid: requestedBy,
    title: "New friend request",
    message: `${name} wants to be your friend on KardIQ.`,
    route: "friends",
  };
}

module.exports = { decide, recipientOf, buildPayload, MAX_AGE_MS };
