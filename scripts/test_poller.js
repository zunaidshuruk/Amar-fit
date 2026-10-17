// Runs the whole poller (send_push_notifications.js) against a fake in-memory Firestore and a fake Firebase Admin, with no network.
// It checks that messages, challenges AND friend requests are all announced once, and that a failure in the friend-request step cannot stop the others.
const assert = require("assert");
const Module = require("module");
const path = require("path");

const now = Date.now();
const ts = (ms) => ({ toMillis: () => ms });

function makeDb(state) {
  const docRef = (collection, id, extra = {}) => ({
    id,
    ref: undefined,
    async get() {
      const data = (state[collection] || {})[id];
      return { exists: data !== undefined, data: () => data };
    },
    async update(patch) { Object.assign(state[collection][id], patch); },
    ...extra,
  });
  const queryOver = (collection, filters, isGroup) => ({
    where(field, op, value) { return queryOver(collection, [...filters, [field, op, value]], isGroup); },
    async get() {
      const docs = [];
      for (const [id, data] of Object.entries(state[collection] || {})) {
        if (filters.every(([f, , v]) => data[f] === v)) {
          const doc = { id, data: () => data, ref: { update: async (patch) => { Object.assign(state[collection][id], patch); }, parent: { parent: { id: data.__pairId } } } };
          docs.push(doc);
        }
      }
      return { size: docs.length, docs };
    },
  });
  return {
    collection(name) {
      return { doc: (id) => docRef(name, id), where: (f, o, v) => queryOver(name, [[f, o, v]], false) };
    },
    collectionGroup(name) { return queryOver(name, [], true); },
  };
}

function run(state, { failFriendships = false } = {}) {
  const sent = [];
  const db = makeDb(state);
  if (failFriendships) {
    const original = db.collection.bind(db);
    db.collection = (name) => {
      if (name === "friendships") throw new Error("simulated Firestore failure");
      return original(name);
    };
  }
  const fakeAdmin = {
    initializeApp() {}, credential: { cert: () => ({}) },
    firestore: () => db,
    messaging: () => ({ send: async (m) => { sent.push(m); } }),
  };
  const originalLoad = Module._load;
  Module._load = function (request, ...rest) {
    if (request === "firebase-admin") return fakeAdmin;
    return originalLoad.call(this, request, ...rest);
  };
  process.env.FIREBASE_SERVICE_ACCOUNT_KEY = "{}";
  const originalExit = process.exit;
  const scriptPath = path.join(__dirname, "send_push_notifications.js");
  delete require.cache[require.resolve(scriptPath)];
  return new Promise((resolve) => {
    process.exit = (code) => { process.exit = originalExit; Module._load = originalLoad; resolve({ code, sent }); };
    require(scriptPath);
  });
}

function baseState() {
  return {
    public_profiles: { a: { name: "Amina" }, b: { name: "Bilal" }, c: { name: "Chitra" } },
    users: { a: { fcmToken: "token-a" }, b: { fcmToken: "token-b" }, c: { fcmToken: "token-c" } },
    messages: {
      m1: { __pairId: "a_b", senderUid: "a", text: "Hello", notified: false },
    },
    challenges: {
      ch1: { uid1: "a", uid2: "b", createdBy: "a", status: "active", notifiedCreation: false, notifiedCompletion: false },
    },
    friendships: {
      a_b: { uid1: "a", uid2: "b", requestedBy: "a", status: "pending", createdAt: ts(now - 1000) },            // new: announce to b
      a_c: { uid1: "a", uid2: "c", requestedBy: "c", status: "pending", createdAt: ts(now - 30 * 86400000) },    // old: mark only
      b_c: { uid1: "b", uid2: "c", requestedBy: "b", status: "accepted", createdAt: ts(now - 1000) },           // accepted: nothing
    },
  };
}

(async () => {
  // 1. everything is announced once
  let state = baseState();
  let result = await run(state);
  assert.strictEqual(result.code, 0);
  const types = result.sent.map((m) => m.data.type).sort();
  assert.deepStrictEqual(types, ["challenge", "direct_message", "friend_request"]);
  const request = result.sent.find((m) => m.data.type === "friend_request");
  assert.strictEqual(request.token, "token-b");
  assert.strictEqual(request.data.message, "Amina wants to be your friend on KardIQ.");
  assert.strictEqual(request.data.route, "friends");
  assert.strictEqual(request.android.priority, "high");
  assert.strictEqual(state.friendships.a_b.notifiedRequest, true);
  assert.strictEqual(state.friendships.a_c.notifiedRequest, true, "the old request is marked");
  assert.strictEqual(state.friendships.b_c.notifiedRequest, undefined, "an accepted friendship is left alone");

  // 2. a second run sends nothing new
  result = await run(state);
  assert.strictEqual(result.sent.length, 0, "nothing is announced twice");

  // 3. a request whose recipient has no token is marked and nothing crashes
  state = baseState();
  delete state.users.b;
  delete state.public_profiles.b;
  result = await run(state);
  assert.strictEqual(result.code, 0);
  assert.strictEqual(state.friendships.a_b.notifiedRequest, true);

  // 4. if the friend-request step fails, messages and challenges were already sent and the run still finishes
  state = baseState();
  result = await run(state, { failFriendships: true });
  assert.strictEqual(result.code, 0, "the run still completes");
  assert.deepStrictEqual(result.sent.map((m) => m.data.type).sort(), ["challenge", "direct_message"]);

  console.log("poller: all checks passed");
})().catch((err) => { console.error(err); process.exit(1); });
