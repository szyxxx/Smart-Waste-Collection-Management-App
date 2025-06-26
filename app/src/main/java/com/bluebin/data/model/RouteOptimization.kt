package com.bluebin.data.model

import com.google.gson.annotations.SerializedName
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

// Request models for the route optimization API
data class RouteOptimizationRequest(
    @SerializedName("tps")
    val tps: List<TPSCoordinate>
)

data class TPSCoordinate(
    @SerializedName("name")
    val name: String,
    @SerializedName("lat")
    val lat: Double,
    @SerializedName("lng")
    val lng: Double
)

// Response models from the route optimization API
data class RouteOptimizationResponse(
    @SerializedName("segments")
    @PropertyName("segments")
    val segments: List<RouteSegment> = emptyList(),
    @SerializedName("total_distance_km")
    @PropertyName("totalDistanceKm")
    val totalDistanceKm: Double = 0.0,
    @SerializedName("estimated_total_minutes")
    @PropertyName("estimatedTotalMinutes")
    val estimatedTotalMinutes: Double = 0.0
) {
    // No-argument constructor required for Firestore
    constructor() : this(emptyList(), 0.0, 0.0)
}

data class RouteSegment(
    @SerializedName("from")
    @PropertyName("from")
    val from: String = "",
    @SerializedName("to")
    @PropertyName("to")
    val to: String = "",
    @SerializedName("distance_km")
    @PropertyName("distanceKm")
    val distanceKm: Double = 0.0,
    @SerializedName("estimated_time_minutes")
    @PropertyName("estimatedTimeMinutes")
    val estimatedTimeMinutes: Double = 0.0
) {
    // No-argument constructor required for Firestore
    constructor() : this("", "", 0.0, 0.0)
}

// Models for schedule management with auto-generation
data class OptimizedSchedule(
    @DocumentId
    val scheduleId: String = "",
    @PropertyName("optimizedRoute")
    val optimizedRoute: RouteOptimizationResponse = RouteOptimizationResponse(),
    @PropertyName("tpsLocations")
    val tpsLocations: List<TPS> = emptyList(),
    @PropertyName("estimatedDuration")
    val estimatedDuration: Double = 0.0,
    @PropertyName("totalDistance")
    val totalDistance: Double = 0.0,
    @PropertyName("status")
    val status: OptimizedScheduleStatus = OptimizedScheduleStatus.PENDING_APPROVAL,
    @PropertyName("generatedAt")
    val generatedAt: Long = System.currentTimeMillis(),
    @PropertyName("approvedAt")
    val approvedAt: Long? = null,
    @PropertyName("assignedDriverId")
    val assignedDriverId: String? = null,
    @PropertyName("scheduledDate")
    val scheduledDate: com.google.firebase.Timestamp = com.google.firebase.Timestamp.now(),
    @PropertyName("priority")
    val priority: SchedulePriority = SchedulePriority.NORMAL,
    @PropertyName("isOptimized")
    val isOptimized: Boolean = true
) {
    // No-argument constructor required for Firestore
    constructor() : this(
        scheduleId = "",
        optimizedRoute = RouteOptimizationResponse(),
        tpsLocations = emptyList(),
        estimatedDuration = 0.0,
        totalDistance = 0.0,
        status = OptimizedScheduleStatus.PENDING_APPROVAL,
        generatedAt = System.currentTimeMillis(),
        approvedAt = null,
        assignedDriverId = null,
        scheduledDate = com.google.firebase.Timestamp.now(),
        priority = SchedulePriority.NORMAL,
        isOptimized = true
    )
}

enum class OptimizedScheduleStatus {
    PENDING_APPROVAL,
    APPROVED,
    ASSIGNED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED
}

// Legacy models for backward compatibility
data class TPSStatusRequest(
    @SerializedName("is_full")
    val isFull: Boolean,
    @SerializedName("tps_id")
    val tpsId: String
)

data class OptimizedTPS(
    @SerializedName("tps_id")
    val tpsId: String,
    @SerializedName("lat")
    val lat: Double,
    @SerializedName("lng")
    val lng: Double
) 