package com.bluebin.data.repository

import android.util.Log
import com.bluebin.data.api.RouteOptimizationApi
import com.bluebin.data.model.*
import com.bluebin.presentation.driver.DriverLocation
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScheduleRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val routeOptimizationApi: RouteOptimizationApi
) {
    private val schedulesCollection = firestore.collection("schedules")
    private val driverLocationsCollection = firestore.collection("driver_locations")
    // Note: We'll phase out optimized_schedules collection in favor of unified schedules

    companion object {
        private const val TAG = "ScheduleRepository"
    }

    // UNIFIED APPROACH: All schedules in one collection
    
    suspend fun getAllSchedules(): List<Schedule> {
        return try {
            val snapshot = schedulesCollection.get().await()
            snapshot.toObjects(Schedule::class.java).sortedByDescending { 
                it.generatedAt ?: it.createdAt 
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading schedules", e)
            emptyList()
        }
    }

    suspend fun getSchedulesByType(type: ScheduleGenerationType): List<Schedule> {
        return try {
            val snapshot = schedulesCollection
                .whereEqualTo("generationType", type.name)
                .get()
                .await()
            snapshot.toObjects(Schedule::class.java).sortedByDescending { 
                it.generatedAt ?: it.createdAt 
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading schedules by type", e)
            emptyList()
        }
    }

    suspend fun getPendingApprovalSchedules(): List<Schedule> {
        return try {
            Log.d(TAG, "Loading pending approval schedules")
            val snapshot = schedulesCollection
                .whereEqualTo("status", ScheduleStatus.PENDING_APPROVAL.name)
                .get()
                .await()
            
            val schedules = snapshot.toObjects(Schedule::class.java).sortedByDescending { 
                it.generatedAt ?: it.createdAt 
            }
            
            Log.d(TAG, "Loaded ${schedules.size} pending approval schedules")
            schedules
        } catch (e: Exception) {
            Log.e(TAG, "Error loading pending approval schedules", e)
            emptyList()
        }
    }

    suspend fun getOptimizedSchedules(): List<Schedule> {
        return try {
            Log.d(TAG, "Loading optimized schedules")
            val snapshot = schedulesCollection
                .whereEqualTo("generationType", ScheduleGenerationType.AI_GENERATED.name)
                .get()
                .await()
            
            val schedules = snapshot.toObjects(Schedule::class.java).sortedByDescending { 
                it.generatedAt ?: it.createdAt 
            }
            
            Log.d(TAG, "Loaded ${schedules.size} optimized schedules")
            schedules
        } catch (e: Exception) {
            Log.e(TAG, "Error loading optimized schedules", e)
            emptyList()
        }
    }

    suspend fun getSchedulesByStatus(status: ScheduleStatus): List<Schedule> {
        return try {
            val snapshot = schedulesCollection
                .whereEqualTo("status", status.name)
                .get()
                .await()
            snapshot.toObjects(Schedule::class.java).sortedByDescending { it.createdAt }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading schedules by status", e)
            emptyList()
        }
    }

    suspend fun getSchedulesByDriver(driverId: String): List<Schedule> {
        return try {
            val snapshot = schedulesCollection
                .whereEqualTo("driverId", driverId)
                .get()
                .await()
            snapshot.toObjects(Schedule::class.java).sortedByDescending { it.createdAt }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading schedules by driver", e)
            emptyList()
        }
    }

    suspend fun getScheduleById(scheduleId: String): Schedule? {
        return try {
            val document = schedulesCollection.document(scheduleId).get().await()
            document.toObject(Schedule::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Error loading schedule by ID", e)
            null
        }
    }

    suspend fun updateScheduleStatus(scheduleId: String, status: ScheduleStatus): Boolean {
        return try {
            val updateData = mutableMapOf<String, Any>(
                "status" to status.name
            )
            
            if (status == ScheduleStatus.COMPLETED) {
                updateData["completedAt"] = System.currentTimeMillis()
            }
            
            if (status == ScheduleStatus.IN_PROGRESS) {
                updateData["startedAt"] = System.currentTimeMillis()
            }
            
            schedulesCollection.document(scheduleId).update(updateData).await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error updating schedule status", e)
            false
        }
    }

    suspend fun updateScheduleStopCompletion(
        scheduleId: String, 
        stopCompletion: RouteStopCompletion
    ): Boolean {
        return try {
            Log.d(TAG, "Updating schedule stop completion for schedule: $scheduleId, TPS: ${stopCompletion.tpsId}")
            
            // Get current schedule
            val currentSchedule = getScheduleById(scheduleId)
            if (currentSchedule == null) {
                Log.e(TAG, "Schedule not found: $scheduleId")
                return false
            }
            
            // Update or add the completion data
            val updatedCompletionData = currentSchedule.routeCompletionData.toMutableList()
            val existingIndex = updatedCompletionData.indexOfFirst { it.tpsId == stopCompletion.tpsId }
            
            if (existingIndex >= 0) {
                updatedCompletionData[existingIndex] = stopCompletion
            } else {
                updatedCompletionData.add(stopCompletion)
            }
            
            // Update the schedule in Firestore
            val updateData = mapOf(
                "routeCompletionData" to updatedCompletionData.map { completion ->
                    mapOf(
                        "tpsId" to completion.tpsId,
                        "completedAt" to completion.completedAt,
                        "proofPhotoUrl" to completion.proofPhotoUrl,
                        "notes" to completion.notes,
                        "hasIssue" to completion.hasIssue,
                        "driverLocation" to completion.driverLocation
                    )
                }
            )
            
            schedulesCollection.document(scheduleId).update(updateData).await()
            Log.d(TAG, "Successfully updated schedule stop completion")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error updating schedule stop completion", e)
            false
        }
    }

    suspend fun deleteSchedule(scheduleId: String): Boolean {
        return try {
            schedulesCollection.document(scheduleId).delete().await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting schedule", e)
            false
        }
    }

    suspend fun createSchedule(schedule: Schedule): Boolean {
        return try {
            schedulesCollection
                .add(schedule)
                .await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error creating schedule", e)
            false
        }
    }

    // LOCATION TRACKING METHODS
    
    suspend fun updateDriverLocation(location: DriverLocation): Boolean {
        return try {
            Log.d(TAG, "Updating driver location for ${location.driverId}")
            
            val locationData = mapOf(
                "driverId" to location.driverId,
                "scheduleId" to location.scheduleId,
                "latitude" to location.latitude,
                "longitude" to location.longitude,
                "timestamp" to location.timestamp,
                "speed" to location.speed,
                "heading" to location.heading
            )
            
            // Use driver ID as document ID for easy retrieval
            driverLocationsCollection
                .document(location.driverId)
                .set(locationData)
                .await()
            
            Log.d(TAG, "Successfully updated driver location")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error updating driver location", e)
            false
        }
    }

    suspend fun getDriverLocation(driverId: String): DriverLocation? {
        return try {
            val document = driverLocationsCollection.document(driverId).get().await()
            if (document.exists()) {
                val data = document.data!!
                DriverLocation(
                    driverId = data["driverId"] as String,
                    scheduleId = data["scheduleId"] as String,
                    latitude = data["latitude"] as Double,
                    longitude = data["longitude"] as Double,
                    timestamp = data["timestamp"] as Long,
                    speed = data["speed"] as? Double ?: 0.0,
                    heading = data["heading"] as? Double ?: 0.0
                )
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting driver location", e)
            null
        }
    }

    suspend fun getAllActiveDriverLocations(): List<DriverLocation> {
        return try {
            val snapshot = driverLocationsCollection.get().await()
            val locations = mutableListOf<DriverLocation>()
            
            for (document in snapshot.documents) {
                document.data?.let { data ->
                    val location = DriverLocation(
                        driverId = data["driverId"] as String,
                        scheduleId = data["scheduleId"] as String,
                        latitude = data["latitude"] as Double,
                        longitude = data["longitude"] as Double,
                        timestamp = data["timestamp"] as Long,
                        speed = data["speed"] as? Double ?: 0.0,
                        heading = data["heading"] as? Double ?: 0.0
                    )
                    
                    // Only include recent locations (within last 5 minutes)
                    val fiveMinutesAgo = System.currentTimeMillis() - (5 * 60 * 1000)
                    if (location.timestamp > fiveMinutesAgo) {
                        locations.add(location)
                    }
                }
            }
            
            Log.d(TAG, "Retrieved ${locations.size} active driver locations")
            locations
        } catch (e: Exception) {
            Log.e(TAG, "Error getting active driver locations", e)
            emptyList()
        }
    }

    suspend fun clearDriverLocation(driverId: String): Boolean {
        return try {
            driverLocationsCollection.document(driverId).delete().await()
            Log.d(TAG, "Cleared location for driver: $driverId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing driver location", e)
            false
        }
    }

    suspend fun generateAndSaveOptimizedSchedule(fullTPSLocations: List<TPS>): Result<String> {
        return try {
            Log.d(TAG, "Generating optimized schedule for ${fullTPSLocations.size} TPS locations")
            
            // Convert TPS locations to API format
            val tpsCoordinates = fullTPSLocations.map { tps ->
                TPSCoordinate(
                    name = tps.name,
                    lat = tps.location.latitude,
                    lng = tps.location.longitude
                )
            }
            
            val request = RouteOptimizationRequest(tps = tpsCoordinates)
            val response = routeOptimizationApi.optimizeRoute(request)
            
            if (response.isSuccessful && response.body() != null) {
                val optimizationResult = response.body()!!
                
                // Create unified schedule with optimization data
                val optimizedSchedule = Schedule(
                    date = Timestamp.now(),
                    driverId = "", // Will be assigned later
                    tpsRoute = fullTPSLocations.map { it.tpsId }, // TPS IDs in order
                    status = ScheduleStatus.PENDING_APPROVAL,
                    isOptimized = true,
                    generationType = ScheduleGenerationType.AI_GENERATED,
                    generatedAt = System.currentTimeMillis(),
                    priority = determinePriority(fullTPSLocations.size),
                    estimatedDuration = optimizationResult.estimatedTotalMinutes,
                    totalDistance = optimizationResult.totalDistanceKm,
                    optimizationData = OptimizationData(
                        totalDistanceKm = optimizationResult.totalDistanceKm,
                        estimatedTotalMinutes = optimizationResult.estimatedTotalMinutes,
                        routeSegments = optimizationResult.segments.map { segment ->
                            SimpleRouteSegment(
                                from = segment.from,
                                to = segment.to,
                                distanceKm = segment.distanceKm,
                                estimatedTimeMinutes = segment.estimatedTimeMinutes
                            )
                        },
                        optimizedTpsOrder = fullTPSLocations.map { it.tpsId }
                    )
                )
                
                val documentRef = schedulesCollection.add(optimizedSchedule).await()
                Log.d(TAG, "Successfully saved optimized schedule with ID: ${documentRef.id}")
                Result.success(documentRef.id)
            } else {
                val errorMsg = "Failed to optimize route: ${response.message()}"
                Log.e(TAG, errorMsg)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error generating optimized schedule", e)
            Result.failure(e)
        }
    }

    suspend fun approveSchedule(scheduleId: String): Result<Unit> {
        return try {
            Log.d(TAG, "Approving schedule: $scheduleId")
            
            schedulesCollection
                .document(scheduleId)
                .update(
                    mapOf(
                        "status" to ScheduleStatus.APPROVED.name,
                        "approvedAt" to System.currentTimeMillis()
                    )
                )
                .await()
            
            Log.d(TAG, "Successfully approved schedule: $scheduleId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error approving schedule $scheduleId", e)
            Result.failure(e)
        }
    }

    suspend fun assignDriverToSchedule(scheduleId: String, driverId: String): Result<Unit> {
        return try {
            Log.d(TAG, "Assigning driver $driverId to schedule: $scheduleId")
            
            schedulesCollection
                .document(scheduleId)
                .update(
                    mapOf(
                        "status" to ScheduleStatus.ASSIGNED.name,
                        "driverId" to driverId
                    )
                )
                .await()
            
            Log.d(TAG, "Successfully assigned driver to schedule: $scheduleId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error assigning driver to schedule $scheduleId", e)
            Result.failure(e)
        }
    }

    suspend fun assignDriverWithDate(
        scheduleId: String, 
        driverId: String, 
        assignedDate: Timestamp,
        isRecurring: Boolean = false
    ): Result<Unit> {
        return try {
            Log.d(TAG, "Assigning driver $driverId to schedule $scheduleId for date $assignedDate")
            
            val updateData = mutableMapOf<String, Any>(
                "driverId" to driverId,
                "status" to ScheduleStatus.ASSIGNED.name,
                "assignedDate" to assignedDate,
                "isRecurring" to isRecurring
            )
            
            if (isRecurring) {
                updateData["recurrenceType"] = RecurrenceType.WEEKLY.name
                // Calculate next occurrence (7 days later)
                val nextWeek = Timestamp(assignedDate.seconds + (7 * 24 * 60 * 60), assignedDate.nanoseconds)
                updateData["nextOccurrence"] = nextWeek
            } else {
                updateData["recurrenceType"] = RecurrenceType.NONE.name
                updateData["nextOccurrence"] = com.google.firebase.firestore.FieldValue.delete()
            }
            
            schedulesCollection.document(scheduleId).update(updateData).await()
            Log.d(TAG, "Successfully assigned driver with date and recurrence settings")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error assigning driver with date to schedule", e)
            Result.failure(e)
        }
    }

    suspend fun canDriverStartSchedule(scheduleId: String): Boolean {
        return try {
            val schedule = getScheduleById(scheduleId)
            if (schedule?.assignedDate == null) return true // No date restriction
            
            val assignedDate = schedule.assignedDate.toDate()
            val today = java.util.Date()
            
            // Remove time component for date comparison
            val assignedCalendar = java.util.Calendar.getInstance().apply { time = assignedDate }
            val todayCalendar = java.util.Calendar.getInstance().apply { time = today }
            
            assignedCalendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
            assignedCalendar.set(java.util.Calendar.MINUTE, 0)
            assignedCalendar.set(java.util.Calendar.SECOND, 0)
            assignedCalendar.set(java.util.Calendar.MILLISECOND, 0)
            
            todayCalendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
            todayCalendar.set(java.util.Calendar.MINUTE, 0)
            todayCalendar.set(java.util.Calendar.SECOND, 0)
            todayCalendar.set(java.util.Calendar.MILLISECOND, 0)
            
            // Can start if today is on or after the assigned date
            !todayCalendar.time.before(assignedCalendar.time)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking if driver can start schedule", e)
            true // Allow start if there's an error
        }
    }

    suspend fun getScheduleStats(): ScheduleStats {
        return try {
            Log.d(TAG, "Loading schedule stats")
            
            val allSchedules = getAllSchedules()
            
            val today = System.currentTimeMillis()
            val todayStart = today - (today % (24 * 60 * 60 * 1000))
            val todayEnd = todayStart + (24 * 60 * 60 * 1000)

            // Regular schedules (manual + converted)
            val regularSchedules = allSchedules.filter { 
                it.generationType == ScheduleGenerationType.MANUAL || it.status == ScheduleStatus.ASSIGNED 
            }
            
            // Optimized schedules  
            val optimizedSchedules = allSchedules.filter { 
                it.generationType == ScheduleGenerationType.AI_GENERATED 
            }

            // Regular schedules stats
            val totalSchedules = regularSchedules.size
            val pendingSchedules = regularSchedules.count { it.status == ScheduleStatus.PENDING }
            val activeSchedules = regularSchedules.count { it.status == ScheduleStatus.IN_PROGRESS }
            val completedSchedules = regularSchedules.count { it.status == ScheduleStatus.COMPLETED }
            val cancelledSchedules = regularSchedules.count { it.status == ScheduleStatus.CANCELLED }
            
            val completedToday = regularSchedules.count { schedule ->
                schedule.status == ScheduleStatus.COMPLETED &&
                schedule.completedAt != null &&
                schedule.completedAt!! >= todayStart &&
                schedule.completedAt!! < todayEnd
            }

            // Optimized schedules stats
            val totalOptimizedSchedules = optimizedSchedules.size
            val pendingOptimizedSchedules = optimizedSchedules.count { 
                it.status == ScheduleStatus.PENDING_APPROVAL 
            }
            val approvedOptimizedSchedules = optimizedSchedules.count { 
                it.status == ScheduleStatus.APPROVED 
            }
            val assignedOptimizedSchedules = optimizedSchedules.count { 
                it.status == ScheduleStatus.ASSIGNED 
            }
            val completedOptimizedSchedules = optimizedSchedules.count { 
                it.status == ScheduleStatus.COMPLETED 
            }
            
            val optimizedSchedulesToday = optimizedSchedules.count { schedule ->
                schedule.generatedAt != null && 
                schedule.generatedAt!! >= todayStart && 
                schedule.generatedAt!! < todayEnd
            }

            // Calculate total route distances for efficiency metrics
            val totalOptimizedDistance = optimizedSchedules.sumOf { it.totalDistance }
            val averageOptimizedDistance = if (optimizedSchedules.isNotEmpty()) {
                totalOptimizedDistance / optimizedSchedules.size
            } else 0.0

            // Calculate average TPS per route
            val averageTpsPerRoute = if (optimizedSchedules.isNotEmpty()) {
                optimizedSchedules.sumOf { it.tpsRoute.size } / optimizedSchedules.size
            } else 0

            val stats = ScheduleStats(
                totalSchedules = totalSchedules,
                pendingSchedules = pendingSchedules,
                activeSchedules = activeSchedules,
                completedSchedules = completedSchedules,
                cancelledSchedules = cancelledSchedules,
                completedToday = completedToday,
                totalOptimizedSchedules = totalOptimizedSchedules,
                pendingOptimizedSchedules = pendingOptimizedSchedules,
                approvedOptimizedSchedules = approvedOptimizedSchedules,
                assignedOptimizedSchedules = assignedOptimizedSchedules,
                completedOptimizedSchedules = completedOptimizedSchedules,
                optimizedSchedulesToday = optimizedSchedulesToday,
                totalOptimizedDistance = totalOptimizedDistance,
                averageOptimizedDistance = averageOptimizedDistance,
                averageTpsPerRoute = averageTpsPerRoute
            )
            
            Log.d(TAG, "Schedule stats: $stats")
            stats
        } catch (e: Exception) {
            Log.e(TAG, "Error loading schedule stats", e)
            ScheduleStats()
        }
    }

    private fun determinePriority(tpsCount: Int): SchedulePriority {
        return when {
            tpsCount >= 8 -> SchedulePriority.URGENT
            tpsCount >= 5 -> SchedulePriority.HIGH
            tpsCount >= 3 -> SchedulePriority.NORMAL
            else -> SchedulePriority.LOW
        }
    }

    // Migration helper - can be called once to migrate old optimized_schedules
    suspend fun migrateOptimizedSchedulesToUnifiedCollection(): Result<Int> {
        return try {
            Log.d(TAG, "Starting migration of optimized_schedules to unified schedules collection")
            
            val oldOptimizedCollection = firestore.collection("optimized_schedules")
            val snapshot = oldOptimizedCollection.get().await()
            
            var migratedCount = 0
            
            snapshot.documents.forEach { doc ->
                try {
                    val data = doc.data ?: return@forEach
                    
                    // Create unified schedule from old optimized schedule data
                    val unifiedSchedule = Schedule(
                        date = (data["scheduledDate"] as? Timestamp) ?: Timestamp.now(),
                        driverId = data["assignedDriverId"] as? String ?: "",
                        tpsRoute = extractTpsIdsFromData(data),
                        status = mapOldStatusToNewStatus(data["status"] as? String),
                        createdAt = data["generatedAt"] as? Long ?: System.currentTimeMillis(),
                        isOptimized = true,
                        generationType = ScheduleGenerationType.AI_GENERATED,
                        generatedAt = data["generatedAt"] as? Long,
                        approvedAt = data["approvedAt"] as? Long,
                        priority = mapPriorityFromString(data["priority"] as? String),
                        estimatedDuration = (data["estimatedDuration"] as? Number)?.toDouble() ?: 0.0,
                        totalDistance = (data["totalDistance"] as? Number)?.toDouble() ?: 0.0,
                        optimizationData = extractOptimizationData(data)
                    )
                    
                    // Save to unified collection
                    schedulesCollection.add(unifiedSchedule).await()
                    migratedCount++
                    
                    Log.d(TAG, "Migrated schedule ${doc.id} to unified collection")
                    
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to migrate schedule ${doc.id}", e)
                }
            }
            
            Log.d(TAG, "Migration completed. Migrated $migratedCount schedules")
            Result.success(migratedCount)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during migration", e)
            Result.failure(e)
        }
    }

    private fun extractTpsIdsFromData(data: Map<String, Any>): List<String> {
        return try {
            @Suppress("UNCHECKED_CAST")
            val tpsLocations = data["tpsLocations"] as? List<Map<String, Any>> ?: emptyList()
            tpsLocations.mapNotNull { it["tpsId"] as? String }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun mapOldStatusToNewStatus(oldStatus: String?): ScheduleStatus {
        return when (oldStatus) {
            "PENDING_APPROVAL" -> ScheduleStatus.PENDING_APPROVAL
            "APPROVED" -> ScheduleStatus.APPROVED
            "ASSIGNED" -> ScheduleStatus.ASSIGNED
            "COMPLETED" -> ScheduleStatus.COMPLETED
            else -> ScheduleStatus.PENDING_APPROVAL
        }
    }

    private fun mapPriorityFromString(priorityString: String?): SchedulePriority {
        return try {
            SchedulePriority.valueOf(priorityString ?: "NORMAL")
        } catch (e: Exception) {
            SchedulePriority.NORMAL
        }
    }

    private fun extractOptimizationData(data: Map<String, Any>): OptimizationData? {
        return try {
            @Suppress("UNCHECKED_CAST")
            val routeData = data["optimizedRoute"] as? Map<String, Any>
            if (routeData != null) {
                OptimizationData(
                    totalDistanceKm = (routeData["totalDistanceKm"] as? Number)?.toDouble() ?: 0.0,
                    estimatedTotalMinutes = (routeData["estimatedTotalMinutes"] as? Number)?.toDouble() ?: 0.0,
                    routeSegments = emptyList(), // Could extract if needed
                    optimizedTpsOrder = extractTpsIdsFromData(data)
                )
            } else null
        } catch (e: Exception) {
            null
        }
    }
}

data class ScheduleStats(
    // Regular schedules
    val totalSchedules: Int = 0,
    val pendingSchedules: Int = 0,
    val activeSchedules: Int = 0,
    val completedSchedules: Int = 0,
    val cancelledSchedules: Int = 0,
    val completedToday: Int = 0,
    
    // Optimized schedules
    val totalOptimizedSchedules: Int = 0,
    val pendingOptimizedSchedules: Int = 0,
    val approvedOptimizedSchedules: Int = 0,
    val assignedOptimizedSchedules: Int = 0,
    val completedOptimizedSchedules: Int = 0,
    val optimizedSchedulesToday: Int = 0,
    
    // Efficiency metrics
    val totalOptimizedDistance: Double = 0.0,
    val averageOptimizedDistance: Double = 0.0,
    val averageTpsPerRoute: Int = 0
) 