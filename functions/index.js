const functions = require("firebase-functions");
const admin = require("firebase-admin");

admin.initializeApp();

/**
 * Callable Cloud Function: createEmployeeUser
 * 
 * Securely creates a Firebase Authentication user account and corresponding Firestore
 * user document (role: "EMPLOYEE", status: "ACTIVE") on behalf of an authenticated Admin.
 * 
 * Security & Data Rules:
 * 1. Caller must be authenticated with Firebase Auth.
 * 2. Caller must have role == "ADMIN" in Firestore `users/{callerUID}`.
 * 3. Checks for duplicate email in Firebase Auth and Firestore.
 * 4. Passwords are NEVER stored in Firestore or database logs.
 * 5. Uses Firebase Admin SDK to create the user account in Firebase Auth.
 * 6. Admin session is completely unaffected and remains logged in.
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
  let callerRole = "";
  try {
    const callerDoc = await db.collection("users").doc(callerUid).get();
    if (callerDoc.exists) {
      callerRole = (callerDoc.data().role || "").toUpperCase();
    }
  } catch (err) {
    console.error("Error reading caller profile:", err);
  }

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
  const rawPassword = (data.password || "").trim();

  if (!name) {
    throw new functions.https.HttpsError("invalid-argument", "Please enter the officer's full name.");
  }
  if (!email || !email.includes("@")) {
    throw new functions.https.HttpsError("invalid-argument", "Please enter a valid email address.");
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
    if (error.code === "auth/email-already-exists" || error.code === "already-exists") {
      throw new functions.https.HttpsError(
        "already-exists",
        "An account with this email already exists."
      );
    }
    if (error.code !== "auth/user-not-found" && !(error instanceof functions.https.HttpsError)) {
      console.warn("getUserByEmail check:", error.message);
    }
  }

  // Check Firestore for duplicate email
  try {
    const existingFirestoreDocs = await db.collection("users").where("email", "==", email).limit(1).get();
    if (!existingFirestoreDocs.empty) {
      throw new functions.https.HttpsError(
        "already-exists",
        "An account with this email already exists."
      );
    }
  } catch (error) {
    if (error instanceof functions.https.HttpsError) {
      throw error;
    }
  }

  // 5. Create Firebase Authentication user with Admin SDK
  const userCreatePayload = {
    email: email,
    displayName: name,
    disabled: false
  };

  // If password provided and >= 6 characters, set initial password in Auth ONLY (never in Firestore)
  if (rawPassword && rawPassword.length >= 6) {
    userCreatePayload.password = rawPassword;
  }

  let newUserRecord;
  try {
    newUserRecord = await admin.auth().createUser(userCreatePayload);
  } catch (authError) {
    console.error("Firebase Admin createUser failed:", authError);
    if (authError.code === "auth/email-already-exists" || authError.message?.includes("already exists")) {
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
    console.error("Firestore document write failed, rolling back auth account:", firestoreError);
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
