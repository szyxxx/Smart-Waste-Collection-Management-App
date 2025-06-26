package com.bluebin.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.GeoPoint

data class TPS(
    @DocumentId
    val tpsId: String = "",
    val name: String = "",
    val location: GeoPoint = GeoPoint(0.0, 0.0),
    val status: TPSStatus = TPSStatus.TIDAK_PENUH,
    val assignedOfficerId: String? = null,
    val lastUpdated: Long = System.currentTimeMillis(),
    val address: String = ""
)

enum class TPSStatus {
    PENUH, TIDAK_PENUH
} 