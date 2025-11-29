const functions = require("firebase-functions");
const admin = require("firebase-admin");

admin.initializeApp();

const firestore = admin.firestore();

// --- HÀM TIỆN ÍCH ---

/**
 * Tính bình phương khoảng cách Euclid giữa hai vector.
 * @param {number[]} emb1 Vector embedding thứ nhất.
 * @param {number[]} emb2 Vector embedding thứ hai.
 * @return {number} Bình phương khoảng cách.
 */
function euclideanDistanceSquared(emb1, emb2) {
  if (!emb1 || !emb2 || emb1.length !== emb2.length) {
    return Infinity;
  }
  let distance = 0.0;
  for (let i = 0; i < emb1.length; i++) {
    const diff = emb1[i] - emb2[i];
    distance += diff * diff;
  }
  return distance;
}

/**
 * Tính toán vector embedding trung bình từ một danh sách các vector.
 * @param {number[][]} embeddingsList Danh sách các vector embedding.
 * @return {number[]|null} Vector trung bình hoặc null nếu không hợp lệ.
 */
function averageEmbeddings(embeddingsList) {
  if (!embeddingsList || embeddingsList.length === 0) {
    return null;
  }
  const dimension = embeddingsList[0].length;
  const average = new Array(dimension).fill(0);

  for (const embeddings of embeddingsList) {
    for (let i = 0; i < dimension; i++) {
      average[i] += embeddings[i];
    }
  }

  for (let i = 0; i < dimension; i++) {
    average[i] /= embeddingsList.length;
  }
  return average;
}


// --- CLOUD FUNCTIONS ---

/**
 * Cloud Function để đăng ký khuôn mặt từ 3 ảnh.
 * Nhận một mảng 3 vector, tính trung bình và lưu vào Firestore.
 */
exports.registerFaceEmbeddings = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError(
        "unauthenticated",
        "Yêu cầu cần được xác thực.",
    );
  }

  const userId = context.auth.uid;
  const embeddingsList = data.embeddings;

  if (!Array.isArray(embeddingsList) || embeddingsList.length !== 3) {
    throw new functions.https.HttpsError(
        "invalid-argument",
        "Dữ liệu không hợp lệ. Cần một mảng chứa 3 vector embeddings.",
    );
  }

  const averageEmbedding = averageEmbeddings(embeddingsList);

  if (!averageEmbedding) {
    throw new functions.https.HttpsError(
        "internal",
        "Không thể tính toán vector khuôn mặt trung bình.",
    );
  }

  try {
    const userRef = firestore.collection("users").doc(userId);
    await userRef.update({
      faceEmbedding: averageEmbedding,
      faceRegisteredAt: admin.firestore.FieldValue.serverTimestamp(),
    });

    return {success: true, message: "Đăng ký khuôn mặt thành công!"};
  } catch (error) {
    console.error("Lỗi khi lưu face-embedding:", error);
    throw new functions.https.HttpsError(
        "internal",
        "Lỗi khi cập nhật dữ liệu người dùng.",
    );
  }
});


/**
 * Cloud Function để nhận dạng và điểm danh.
 */
exports.recordAttendance = functions.https.onCall(async (data, context) => {
  const newEmbedding = data.embedding;
  const attendanceType = data.type; // "check-in" or "check-out"
  const kioskId = data.kioskId || "UNKNOWN_KIOSK";

  if (!newEmbedding || !attendanceType) {
    throw new functions.https.HttpsError(
        "invalid-argument",
        "Dữ liệu không hợp lệ.",
    );
  }

  const RECOGNITION_THRESHOLD = 1.0; // Ngưỡng nhận dạng
  const usersSnapshot = await firestore.collection("users")
      .where("faceEmbedding", "!=", null).get();

  if (usersSnapshot.empty) {
    throw new functions.https.HttpsError(
        "failed-precondition",
        "Không có người dùng nào đã đăng ký khuôn mặt.",
    );
  }

  let minDistance = Infinity;
  let bestMatch = null;

  usersSnapshot.forEach((doc) => {
    const userData = doc.data();
    const storedEmbedding = userData.faceEmbedding;
    const distance = euclideanDistanceSquared(newEmbedding, storedEmbedding);
    if (distance < minDistance) {
      minDistance = distance;
      bestMatch = {id: doc.id, ...userData};
    }
  });

  const finalConfidence = Math.sqrt(minDistance);

  if (bestMatch && finalConfidence < RECOGNITION_THRESHOLD) {
    const logRef = firestore.collection("attendance_logs").doc();
    await logRef.set({
      userId: bestMatch.id,
      employeeId: bestMatch.employeeId,
      fullName: bestMatch.fullName,
      timestamp: admin.firestore.FieldValue.serverTimestamp(),
      type: attendanceType,
      matchConfidence: finalConfidence,
      kioskDeviceId: kioskId,
    });
    return {
      success: true,
      message: `Chào mừng ${bestMatch.fullName}!`,
      name: bestMatch.fullName,
    };
  } else {
    throw new functions.https.HttpsError(
        "not-found",
        "Không nhận dạng được khuôn mặt.",
        {confidence: finalConfidence},
    );
  }
});