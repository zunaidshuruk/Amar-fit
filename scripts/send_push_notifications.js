// KardIQ push notification poller.
// Runs on a schedule via GitHub Actions (see .github/workflows/push-notifications.yml).
// Uses a Firebase service account (free on any plan) to read/write Firestore directly
// and send FCM pushes (also free, unlimited) -- no Cloud Functions, no Blaze plan needed.

const admin = require("firebase-admin");

const serviceAccount = JSON.parse(process.env.FIREBASE_SERVICE_ACCOUNT_KEY);
admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
});

const db = admin.firestore();
const messaging = admin.messaging();

async function getProfileForPush(uid) {
  const snap = await db.collection("public_profiles").doc(uid).get();
  if (!snap.exists) return { name: "A friend", token: null };
  const data = snap.data();
  return { name: data.name || "A friend", token: data.fcmToken || null };
}

async function sendDataMessage(token, data) {
  if (!token) return;
  try {
    await messaging.send({ token, data, android: { priority: "high" } });
    console.log(`  -> sent (${data.type}) to token ...${token.slice(-8)}`);
  } catch (err) {
    console.warn(`  -> FCM send failed for token ...${token.slice(-8)}: ${err.message}`);
  }
}

async function processNewMessages() {
  const snap = await db
    .collectionGroup("messages")
    .where("notified", "==", false)
    .get();

  console.log(`Found ${snap.size} unnotified message(s).`);

  for (const doc of snap.docs) {
    const message = doc.data();
    const pairId = doc.ref.parent.parent.id; // dm_threads/{pairId}/messages/{messageId}
    const senderUid = message.senderUid;
    const text = message.text || "";

    const [uidA, uidB] = pairId.split("_");
    if (!uidA || !uidB) {
      console.warn(`  Malformed pairId, skipping: ${pairId}`);
      await doc.ref.update({ notified: true });
      continue;
    }
    const recipientUid = senderUid === uidA ? uidB : uidA;

    const [sender, recipient] = await Promise.all([
      getProfileForPush(senderUid),
      getProfileForPush(recipientUid),
    ]);

    await sendDataMessage(recipient.token, {
      type: "direct_message",
      pairId,
      senderName: sender.name,
      text: text.length > 200 ? text.slice(0, 200) + "..." : text,
    });

    await doc.ref.update({ notified: true });
  }
}

async function processNewChallenges() {
  const snap = await db
    .collection("challenges")
    .where("status", "==", "active")
    .where("notifiedCreation", "==", false)
    .get();

  console.log(`Found ${snap.size} new challenge(s) to announce.`);

  for (const doc of snap.docs) {
    const challenge = doc.data();
    const { uid1, uid2, createdBy } = challenge;
    if (!uid1 || !uid2 || !createdBy) {
      await doc.ref.update({ notifiedCreation: true });
      continue;
    }
    const recipientUid = createdBy === uid1 ? uid2 : uid1;
    const [creator, recipient] = await Promise.all([
      getProfileForPush(createdBy),
      getProfileForPush(recipientUid),
    ]);

    await sendDataMessage(recipient.token, {
      type: "challenge",
      challengeId: doc.id,
      title: "New Challenge ⚔️",
      message: `${creator.name} challenged you to a 7-day steps race!`,
    });

    await doc.ref.update({ notifiedCreation: true });
  }
}

async function processCompletedChallenges() {
  const snap = await db
    .collection("challenges")
    .where("status", "==", "completed")
    .where("notifiedCompletion", "==", false)
    .get();

  console.log(`Found ${snap.size} completed challenge(s) to announce.`);

  for (const doc of snap.docs) {
    const challenge = doc.data();
    const { uid1, uid2, winnerUid } = challenge;
    if (!uid1 || !uid2) {
      await doc.ref.update({ notifiedCompletion: true });
      continue;
    }

    const [p1, p2] = await Promise.all([getProfileForPush(uid1), getProfileForPush(uid2)]);

    if (winnerUid === null || winnerUid === undefined) {
      await Promise.all([
        sendDataMessage(p1.token, {
          type: "challenge",
          challengeId: doc.id,
          title: "Challenge Over",
          message: `Your 7-day challenge with ${p2.name} ended in a tie!`,
        }),
        sendDataMessage(p2.token, {
          type: "challenge",
          challengeId: doc.id,
          title: "Challenge Over",
          message: `Your 7-day challenge with ${p1.name} ended in a tie!`,
        }),
      ]);
    } else {
      const winner = winnerUid === uid1 ? p1 : p2;
      const loser = winnerUid === uid1 ? p2 : p1;

      await Promise.all([
        sendDataMessage(winner.token, {
          type: "challenge",
          challengeId: doc.id,
          title: "Challenge Victory! 🏆",
          message: `You won the 7-day challenge against ${loser.name}!`,
        }),
        sendDataMessage(loser.token, {
          type: "challenge",
          challengeId: doc.id,
          title: "Challenge Complete",
          message: `${winner.name} won your 7-day challenge. Rematch?`,
        }),
      ]);
    }

    await doc.ref.update({ notifiedCompletion: true });
  }
}

async function main() {
  console.log("KardIQ push poller starting run:", new Date().toISOString());
  await processNewMessages();
  await processNewChallenges();
  await processCompletedChallenges();
  console.log("Run complete.");
}

main()
  .then(() => process.exit(0))
  .catch((err) => {
    console.error("Poller failed:", err);
    process.exit(1);
  });
