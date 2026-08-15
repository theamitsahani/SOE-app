const functions = require("firebase-functions");
const admin = require("firebase-admin");

admin.initializeApp();

/**
 * Callable Cloud Function: createEmployeeUser
 * 
 * Securely creates a Firebase Authentication user account and corresponding Firestore
 * user document (role: "EMPLOYEE", status: "ACTIVE") on behalf of an authenticated Admin.
 * 
 * Verifications:
 * 1. Caller is authenticated.
 * 2. Caller has role == "ADMIN" in Firestore `users/{callerUID}`.
 * 3. Duplicate email detection before creation.
 * 4. Password is NOT stored anywhere in Firestore or client; password reset email can be sent.
 */
exports.createEmployeeUser = functions.https.onCall(async (data, context) => {
  // 1. Verify Authentication
  if (!context.auth || !context.auth.uid) {
    throw new functions.https.HttpsError(
      "unauthenticated",
      "The function must be called while authenticated."
    );
  }

  const callerUid = context.auth.uid;
  const db = admin.firestore();

  // 2. Verify Caller is an ADMIN in Firestore
  const callerDoc = await db.collection("users").doc(callerUid).get();
  if (!callerDoc.exists) {
    throw new functions.https.HttpsError(
      "permission-denied",
      "Admin profile not found. Access denied."
    );
  }

  const callerRole = (callerDoc.data().role || "").toUpperCase();
  if (callerRole !== "ADMIN") {
    throw new functions.https.HttpsError(
      "permission-denied",
      "Only Administrators can create employee accounts."
    );
  }

  // 3. Extract & Validate Input Data
  const name = (data.name || "").trim();
  const email = (data.email || "").trim().toLowerCase();
  const mobile = (data.mobile || "").trim();
  const state = (data.state || "Rajasthan").trim();
  const district = (data.district || "").trim();

  if (!name) {
    throw new functions.https.HttpsError("invalid-argument", "Name is required.");
  }
  if (!email || !email.includes("@")) {
    throw new functions.https.HttpsError("invalid-argument", "A valid email address is required.");
  }

  // 4. Check for duplicate email in Firebase Authentication
  try {
    const existingUser = await admin.auth().getUserByEmail(email);
    if (existingUser) {
      throw new functions.https.HttpsError(
        "already-exists",
        "An account with this email already exists."
      );
    }
  } catch (error) {
    // If error is 'auth/user-not-found', we can proceed. If it's already-exists, rethrow it.
    if (error.code === "auth/email-already-exists" || error.code === "already-exists") {
      throw new functions.https.HttpsError(
        "already-exists",
        "An account with this email already exists."
      );
    }
    if (error.code !== "auth/user-not-found") {
      // If error is custom HttpsError from above, rethrow
      if (error instanceof functions.https.HttpsError) {
        throw error;
      }
    }
  }

  // Also check Firestore for any duplicate email record
  const existingFirestoreDocs = await db.collection("users").where("email", "==", email).limit(1).get();
  if (!existingFirestoreDocs.empty) {
    throw new functions.https.HttpsError(
      "already-exists",
      "An account with this email already exists."
    );
  }

  // 5. Create Firebase Authentication user with Admin SDK
  let newUserRecord;
  try {
    newUserRecord = await admin.auth().createUser({
      email: email,
      displayName: name,
      disabled: false
    });
  } catch (authError) {
    if (authError.code === "auth/email-already-exists") {
      throw new functions.https.HttpsError(
        "already-exists",
        "An account with this email already exists."
      );
    }
    throw new functions.https.HttpsError(
      "internal",
      authError.message || "Failed to create Firebase Authentication user."
    );
  }

  const newUid = newUserRecord.uid;

  // 6. Create Firestore Document users/{newEmployeeUID}
  try {
    await db.collection("users").doc(newUid).set({
      userId: newUid,
      name: name,
      email: email,
      mobile: mobile,
      state: state || "Rajasthan",
      district: district,
      role: "EMPLOYEE",
      status: "ACTIVE",
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
      createdBy: callerUid
    });
  } catch (firestoreError) {
    // If Firestore write fails, rollback auth user to prevent orphaned credentials
    try {
      await admin.auth().deleteUser(newUid);
    } catch (delError) {
      console.error("Rollback failed for user:", newUid, delError);
    }
    throw new functions.https.HttpsError(
      "internal",
      "Failed to initialize Firestore user document."
    );
  }

  return {
    success: true,
    userId: newUid,
    name: name,
    email: email,
    role: "EMPLOYEE",
    status: "ACTIVE"
  };
});
