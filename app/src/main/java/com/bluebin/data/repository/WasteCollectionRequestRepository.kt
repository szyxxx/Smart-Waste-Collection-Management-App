package com.bluebin.data.repository

import com.bluebin.data.model.WasteCollectionRequest
import com.bluebin.data.model.WasteRequestStatus
import com.bluebin.data.model.WasteRequestPriority
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WasteCollectionRequestRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val requestsCollection = firestore.collection("waste_collection_requests")

    suspend fun createRequest(request: WasteCollectionRequest): Result<String> {
        return try {
            val documentRef = requestsCollection.add(request).await()
            Result.success(documentRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateRequest(request: WasteCollectionRequest): Result<Unit> {
        return try {
            requestsCollection.document(request.id).set(request).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateRequestStatus(requestId: String, status: WasteRequestStatus): Result<Unit> {
        return try {
            val updateData = mutableMapOf<String, Any>(
                "status" to status.name,
                "updatedAt" to System.currentTimeMillis()
            )
            
            if (status == WasteRequestStatus.COMPLETED) {
                updateData["completedAt"] = System.currentTimeMillis()
            }
            
            requestsCollection.document(requestId).update(updateData).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun assignDriver(requestId: String, driverId: String, driverName: String): Result<Unit> {
        return try {
            requestsCollection.document(requestId)
                .update(
                    mapOf(
                        "assignedDriverId" to driverId,
                        "assignedDriverName" to driverName,
                        "status" to WasteRequestStatus.IN_PROGRESS.name,
                        "updatedAt" to System.currentTimeMillis()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getAllRequestsFlow(): Flow<List<WasteCollectionRequest>> = callbackFlow {
        val listener = requestsCollection
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val requests = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(WasteCollectionRequest::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                
                trySend(requests)
            }

        awaitClose { listener.remove() }
    }

    fun getRequestsByStatus(status: WasteRequestStatus): Flow<List<WasteCollectionRequest>> = callbackFlow {
        val listener = requestsCollection
            .whereEqualTo("status", status.name)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val requests = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(WasteCollectionRequest::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                
                trySend(requests)
            }

        awaitClose { listener.remove() }
    }

    fun getUrgentRequests(): Flow<List<WasteCollectionRequest>> = callbackFlow {
        val listener = requestsCollection
            .whereEqualTo("priority", WasteRequestPriority.HIGH.name)
            .whereEqualTo("status", WasteRequestStatus.PENDING.name)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val requests = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(WasteCollectionRequest::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                
                trySend(requests)
            }

        awaitClose { listener.remove() }
    }

    suspend fun deleteRequest(requestId: String): Result<Unit> {
        return try {
            requestsCollection.document(requestId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getRequestStats(): WasteRequestStats {
        return try {
            val allRequests = requestsCollection.get().await()
            val requests = allRequests.documents.mapNotNull { doc ->
                doc.toObject(WasteCollectionRequest::class.java)
            }

            WasteRequestStats(
                totalRequests = requests.size,
                pendingRequests = requests.count { it.status == WasteRequestStatus.PENDING },
                inProgressRequests = requests.count { it.status == WasteRequestStatus.IN_PROGRESS },
                completedRequests = requests.count { it.status == WasteRequestStatus.COMPLETED },
                urgentRequests = requests.count { it.priority == WasteRequestPriority.HIGH && it.status == WasteRequestStatus.PENDING }
            )
        } catch (e: Exception) {
            WasteRequestStats()
        }
    }
}

data class WasteRequestStats(
    val totalRequests: Int = 0,
    val pendingRequests: Int = 0,
    val inProgressRequests: Int = 0,
    val completedRequests: Int = 0,
    val urgentRequests: Int = 0
) 