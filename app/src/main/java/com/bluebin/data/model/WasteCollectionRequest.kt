package com.bluebin.data.model

data class WasteCollectionRequest(
    val id: String = "",
    val location: String = "",
    val requesterName: String = "",
    val requesterContact: String = "",
    val wasteType: String = "",
    val estimatedVolume: Double = 0.0,
    val priority: WasteRequestPriority = WasteRequestPriority.MEDIUM,
    val status: WasteRequestStatus = WasteRequestStatus.PENDING,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val assignedDriverId: String? = null,
    val assignedDriverName: String? = null,
    val scheduledDate: Long? = null,
    val completedAt: Long? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val photos: List<String> = emptyList()
)

enum class WasteRequestStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED
}

enum class WasteRequestPriority {
    LOW,
    MEDIUM,
    HIGH
} 