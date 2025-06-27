package com.bluebin.data.repository

import com.bluebin.data.model.TPS
import com.bluebin.data.model.TPSStatus
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TPSRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val tpsCollection = firestore.collection("tps")

    suspend fun createTPS(tps: TPS): Result<String> {
        return try {
            android.util.Log.d("TPSRepository", "Creating new TPS: ${tps.name}")
            val documentRef = tpsCollection.add(tps).await()
            android.util.Log.d("TPSRepository", "TPS created successfully with ID: ${documentRef.id}")
            Result.success(documentRef.id)
        } catch (e: Exception) {
            android.util.Log.e("TPSRepository", "Failed to create TPS: ${tps.name}", e)
            Result.failure(e)
        }
    }

    suspend fun updateTPS(tps: TPS): Result<Unit> {
        return try {
            tpsCollection.document(tps.tpsId).set(tps).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateTPSStatus(tpsId: String, status: TPSStatus): Result<Unit> {
        return try {
            android.util.Log.d("TPSRepository", "Updating TPS status - ID: $tpsId, Status: ${status.name}")
            
            // Check if document exists first
            val docRef = tpsCollection.document(tpsId)
            val docSnapshot = docRef.get().await()
            
            if (!docSnapshot.exists()) {
                android.util.Log.e("TPSRepository", "TPS document does not exist with ID: $tpsId")
                return Result.failure(Exception("TPS document not found with ID: $tpsId"))
            }
            
            android.util.Log.d("TPSRepository", "TPS document found, updating status...")
            
            docRef.update(
                mapOf(
                    "status" to status.name,
                    "lastUpdated" to System.currentTimeMillis()
                )
            ).await()
            
            android.util.Log.d("TPSRepository", "TPS status update completed successfully for ID: $tpsId")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("TPSRepository", "Failed to update TPS status for ID: $tpsId", e)
            Result.failure(e)
        }
    }

    suspend fun assignOfficerToTPS(tpsId: String, officerId: String): Result<Unit> {
        return try {
            tpsCollection.document(tpsId)
                .update("assignedOfficerId", officerId)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTPSById(tpsId: String): Result<TPS?> {
        return try {
            val document = tpsCollection.document(tpsId).get().await()
            val tps = document.toObject(TPS::class.java)?.copy(tpsId = document.id)
            Result.success(tps)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAllTPS(): Result<List<TPS>> {
        return try {
            android.util.Log.d("TPSRepository", "Loading all TPS locations...")
            val snapshot = tpsCollection
                .orderBy("name")
                .get()
                .await()
            
            android.util.Log.d("TPSRepository", "Retrieved ${snapshot.documents.size} TPS documents")
            
            val tpsList = snapshot.documents.mapNotNull { doc ->
                val tps = doc.toObject(TPS::class.java)?.copy(tpsId = doc.id)
                android.util.Log.d("TPSRepository", "TPS loaded - ID: '${doc.id}', Name: '${tps?.name}', Status: ${tps?.status}")
                tps
            }
            
            android.util.Log.d("TPSRepository", "Successfully loaded ${tpsList.size} TPS locations")
            Result.success(tpsList)
        } catch (e: Exception) {
            android.util.Log.e("TPSRepository", "Failed to load TPS locations", e)
            Result.failure(e)
        }
    }

    fun getAllTPSFlow(): Flow<List<TPS>> = callbackFlow {
        val listener = tpsCollection
            .orderBy("name")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val tpsList = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(TPS::class.java)?.copy(tpsId = doc.id)
                } ?: emptyList()
                
                trySend(tpsList)
            }

        awaitClose { listener.remove() }
    }

    fun getTPSByOfficer(officerId: String): Flow<List<TPS>> = callbackFlow {
        val listener = tpsCollection
            .whereEqualTo("assignedOfficerId", officerId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val tpsList = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(TPS::class.java)?.copy(tpsId = doc.id)
                } ?: emptyList()
                
                trySend(tpsList)
            }

        awaitClose { listener.remove() }
    }

    fun getFullTPS(): Flow<List<TPS>> = callbackFlow {
        val listener = tpsCollection
            .whereEqualTo("status", TPSStatus.PENUH.name)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val tpsList = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(TPS::class.java)?.copy(tpsId = doc.id)
                } ?: emptyList()
                
                trySend(tpsList)
            }

        awaitClose { listener.remove() }
    }

    suspend fun deleteTPS(tpsId: String): Result<Unit> {
        return try {
            tpsCollection.document(tpsId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
} 