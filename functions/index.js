
const {setGlobalOptions} = require("firebase-functions");
const {onRequest} = require("firebase-functions/https");
const logger = require("firebase-functions/logger");
setGlobalOptions({ maxInstances: 10 });



const functions = require("firebase-functions");
const admin = require("firebase-admin");
admin.initializeApp();

function euclideanDistanceSquared(emb1, emb2) {
  let distance = 0.0;
  for (let i = 0; i < emb1.length; i++) {
    const diff = emb1[i] - emb2[i];
    distance += diff * diff;
  }
  return distance;
}


exports.findUserByFace = functions.https.onCall(async (data, context) => {
  // Dữ liệu embedding gửi từ ứng dụng Android
  const newEmbedding = data.embedding;
  if (!newEmbedding) {
    throw new functions.https.HttpsError("invalid-argument", "Thiếu embedding.");
  }

  const db = admin.firestore();
  const usersSnapshot = await db.collection("users").get();

  let minDistance = Infinity;
  let bestMatchUser = null;

  usersSnapshot.forEach((doc) => {
    const userData = doc.data();
    const storedEmbedding = userData.faceEmbedding;

    if (storedEmbedding && storedEmbedding.length > 0) {
      const distance = euclideanDistanceSquared(newEmbedding, storedEmbedding);
      if (distance < minDistance) {
        minDistance = distance;
        bestMatchUser = userData;
      }
    }
  });

  const RECOGNITION_THRESHOLD = 1.0;

  if (minDistance < RECOGNITION_THRESHOLD) {
    console.log(`Tìm thấy: ${bestMatchUser.displayName} với khoảng cách ${minDistance}`);
    // Trả về vai trò
    return { role: bestMatchUser.role };
  } else {
    console.log(`Không tìm thấy. Khoảng cách nhỏ nhất: ${minDistance}`);
    // Ném lỗi 'not-found'
    throw new functions.https.HttpsError("not-found", "Không nhận dạng được khuôn mặt.");
  }
});