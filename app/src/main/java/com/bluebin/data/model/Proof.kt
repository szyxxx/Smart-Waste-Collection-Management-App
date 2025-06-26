package com.bluebin.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class Proof(
    @DocumentId
    val proofId: String = "",
    val driverId: String = "",
    val tpsId: String = "",
    val scheduleId: String = "",
    val photoUrl: String = "",
    val timestamp: Timestamp = Timestamp.now(),
    val verified: Boolean = false
) 