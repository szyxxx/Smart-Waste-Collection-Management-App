package com.bluebin.data.repository

import com.bluebin.data.model.User
import com.bluebin.data.model.UserRole
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    private val usersCollection = firestore.collection("users")

    suspend fun createUser(user: User): Result<Unit> {
        return try {
            usersCollection.document(user.uid).set(user).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCurrentUser(): Result<User?> {
        return try {
            val currentUser = auth.currentUser
            if (currentUser != null) {
                val document = usersCollection.document(currentUser.uid).get().await()
                val user = document.toObject(User::class.java)
                Result.success(user)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserById(uid: String): Result<User?> {
        return try {
            val document = usersCollection.document(uid).get().await()
            val user = document.toObject(User::class.java)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getUsersByRole(role: UserRole): Flow<List<User>> = callbackFlow {
        val listener = usersCollection
            .whereEqualTo("role", role.name)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val users = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(User::class.java)
                } ?: emptyList()
                
                trySend(users)
            }

        awaitClose { listener.remove() }
    }

    fun getPendingUsers(): Flow<List<User>> = callbackFlow {
        val listener = usersCollection
            .whereEqualTo("approved", false)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val users = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(User::class.java)
                } ?: emptyList()
                
                trySend(users)
            }

        awaitClose { listener.remove() }
    }

    suspend fun approveUser(uid: String): Result<Unit> {
        return try {
            usersCollection.document(uid)
                .update("approved", true)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun rejectUser(uid: String): Result<Unit> {
        return try {
            usersCollection.document(uid).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteUser(uid: String): Result<Unit> {
        return try {
            usersCollection.document(uid).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAllUsers(): Result<List<User>> {
        return try {
            // First try with ordering by createdAt
            val snapshot = try {
                usersCollection
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .get()
                    .await()
            } catch (orderException: Exception) {
                // If ordering fails (e.g., index not available), get all documents without ordering
                android.util.Log.w("UserRepository", "Failed to order by createdAt, fetching without ordering", orderException)
                usersCollection.get().await()
            }
            
            val users = snapshot.documents.mapNotNull { doc ->
                try {
                    // Try automatic parsing first
                    val user = doc.toObject(User::class.java)
                    if (user != null) {
                        // Ensure the user has the uid field set from document ID
                        return@mapNotNull user.copy(uid = doc.id)
                    }
                    
                    // If automatic parsing fails, try manual parsing
                    val data = doc.data
                    if (data != null) {
                        val roleString = data["role"] as? String ?: "TPS_OFFICER"
                        val role = try {
                            UserRole.valueOf(roleString)
                        } catch (e: Exception) {
                            UserRole.TPS_OFFICER
                        }
                        
                        return@mapNotNull User(
                            uid = doc.id,
                            name = data["name"] as? String ?: "",
                            email = data["email"] as? String ?: "",
                            role = role,
                            approved = data["approved"] as? Boolean ?: false,
                            createdAt = (data["createdAt"] as? Long) ?: System.currentTimeMillis()
                        )
                    }
                    
                    null
                } catch (parseException: Exception) {
                    android.util.Log.w("UserRepository", "Failed to parse user document ${doc.id}", parseException)
                    // Try manual parsing as fallback
                    try {
                        val data = doc.data
                        if (data != null) {
                            val roleString = data["role"] as? String ?: "TPS_OFFICER"
                            val role = try {
                                UserRole.valueOf(roleString)
                            } catch (e: Exception) {
                                UserRole.TPS_OFFICER
                            }
                            
                            User(
                                uid = doc.id,
                                name = data["name"] as? String ?: "",
                                email = data["email"] as? String ?: "",
                                role = role,
                                approved = data["approved"] as? Boolean ?: false,
                                createdAt = (data["createdAt"] as? Long) ?: System.currentTimeMillis()
                            )
                        } else null
                    } catch (fallbackException: Exception) {
                        android.util.Log.e("UserRepository", "Complete failure to parse user document ${doc.id}", fallbackException)
                        null
                    }
                }
            }
            
            android.util.Log.d("UserRepository", "Successfully loaded ${users.size} users")
            Result.success(users)
        } catch (e: Exception) {
            android.util.Log.e("UserRepository", "Failed to load users", e)
            Result.failure(e)
        }
    }

    suspend fun updateUser(user: User): Result<Unit> {
        return try {
            usersCollection.document(user.uid).set(user).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signOut() {
        auth.signOut()
    }
} 