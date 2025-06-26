package com.bluebin.data.seeder

import com.bluebin.data.model.*
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataSeeder @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    
    /**
     * Seeds the database with sample data for testing
     * Call this method only once or when you want to reset all data
     */
    suspend fun seedDatabase(): Result<String> {
        return try {
            // Clear existing data first (optional)
            // clearAllData()
            
            // Seed in order due to dependencies
            val userIds = seedUsers()
            val tpsIds = seedTPS(userIds)
            val scheduleIds = seedSchedules(userIds, tpsIds)
            seedProofs(userIds, tpsIds, scheduleIds)
            
            Result.success("Database seeded successfully with sample data")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Seeds sample users for all roles
     */
    private suspend fun seedUsers(): Map<String, String> {
        val userIds = mutableMapOf<String, String>()
        
        // Admin users
        val adminUsers = listOf(
            User(
                uid = "admin_001",
                name = "Admin Utama",
                email = "admin@bluebin.com", 
                role = UserRole.ADMIN,
                approved = true,
                createdAt = System.currentTimeMillis() - 86400000L * 30 // 30 days ago
            ),
            User(
                uid = "admin_002", 
                name = "Admin Kedua",
                email = "admin2@bluebin.com",
                role = UserRole.ADMIN,
                approved = true,
                createdAt = System.currentTimeMillis() - 86400000L * 20
            )
        )
        
        // TPS Officers
        val tpsOfficers = listOf(
            User(
                uid = "tps_001",
                name = "Budi Santoso",
                email = "budi.tps@bluebin.com",
                role = UserRole.TPS_OFFICER,
                approved = true,
                createdAt = System.currentTimeMillis() - 86400000L * 25
            ),
            User(
                uid = "tps_002",
                name = "Sari Indah",
                email = "sari.tps@bluebin.com", 
                role = UserRole.TPS_OFFICER,
                approved = true,
                createdAt = System.currentTimeMillis() - 86400000L * 22
            ),
            User(
                uid = "tps_003",
                name = "Ahmad Wijaya",
                email = "ahmad.tps@bluebin.com",
                role = UserRole.TPS_OFFICER,
                approved = true,
                createdAt = System.currentTimeMillis() - 86400000L * 18
            ),
            User(
                uid = "tps_004",
                name = "Maya Putri",
                email = "maya.tps@bluebin.com",
                role = UserRole.TPS_OFFICER,
                approved = false, // Pending approval
                createdAt = System.currentTimeMillis() - 86400000L * 5
            )
        )
        
        // Drivers
        val drivers = listOf(
            User(
                uid = "driver_001",
                name = "Joko Suprianto",
                email = "joko.driver@bluebin.com",
                role = UserRole.DRIVER,
                approved = true,
                createdAt = System.currentTimeMillis() - 86400000L * 20
            ),
            User(
                uid = "driver_002", 
                name = "Andi Kurniawan",
                email = "andi.driver@bluebin.com",
                role = UserRole.DRIVER,
                approved = true,
                createdAt = System.currentTimeMillis() - 86400000L * 15
            ),
            User(
                uid = "driver_003",
                name = "Rudi Hartono",
                email = "rudi.driver@bluebin.com",
                role = UserRole.DRIVER,
                approved = true,
                createdAt = System.currentTimeMillis() - 86400000L * 12
            ),
            User(
                uid = "driver_004",
                name = "Dedi Setiawan",
                email = "dedi.driver@bluebin.com",
                role = UserRole.DRIVER,
                approved = false, // Pending approval
                createdAt = System.currentTimeMillis() - 86400000L * 3
            )
        )
        
        // Insert all users
        val allUsers = adminUsers + tpsOfficers + drivers
        for (user in allUsers) {
            firestore.collection("users")
                .document(user.uid)
                .set(user)
                .await()
            userIds[user.role.name.lowercase() + "_" + user.name.replace(" ", "_").lowercase()] = user.uid
        }
        
        return userIds
    }
    
    /**
     * Seeds sample TPS locations
     */
    private suspend fun seedTPS(userIds: Map<String, String>): List<String> {
        val tpsData = listOf(
            TPS(
                tpsId = "", // Will be auto-generated
                name = "TPS Kebon Jeruk",
                location = GeoPoint(-6.2088, 106.8456), // Jakarta coordinates
                status = TPSStatus.TIDAK_PENUH,
                assignedOfficerId = "tps_001",
                lastUpdated = System.currentTimeMillis() - 86400000L * 2,
                address = "Jl. Raya Kebon Jeruk No. 123, Jakarta Barat"
            ),
            TPS(
                tpsId = "",
                name = "TPS Menteng", 
                location = GeoPoint(-6.1944, 106.8229),
                status = TPSStatus.PENUH,
                assignedOfficerId = "tps_002",
                lastUpdated = System.currentTimeMillis() - 86400000L * 1,
                address = "Jl. Menteng Raya No. 45, Jakarta Pusat"
            ),
            TPS(
                tpsId = "",
                name = "TPS Kemang",
                location = GeoPoint(-6.2615, 106.8106),
                status = TPSStatus.TIDAK_PENUH,
                assignedOfficerId = "tps_003",
                lastUpdated = System.currentTimeMillis() - 86400000L * 3,
                address = "Jl. Kemang Raya No. 67, Jakarta Selatan"
            ),
            TPS(
                tpsId = "",
                name = "TPS Kelapa Gading",
                location = GeoPoint(-6.1487, 106.8998),
                status = TPSStatus.PENUH,
                assignedOfficerId = "tps_001",
                lastUpdated = System.currentTimeMillis() - 86400000L * 1,
                address = "Jl. Kelapa Gading Boulevard No. 88, Jakarta Utara"
            ),
            TPS(
                tpsId = "",
                name = "TPS Cibubur",
                location = GeoPoint(-6.3451, 106.8974),
                status = TPSStatus.TIDAK_PENUH,
                assignedOfficerId = "tps_002",
                lastUpdated = System.currentTimeMillis() - 86400000L * 4,
                address = "Jl. Cibubur Raya No. 32, Depok"
            ),
            TPS(
                tpsId = "",
                name = "TPS Tangerang Kota",
                location = GeoPoint(-6.1783, 106.6319),
                status = TPSStatus.TIDAK_PENUH,
                assignedOfficerId = "tps_003",
                lastUpdated = System.currentTimeMillis() - 86400000L * 5,
                address = "Jl. Daan Mogot No. 156, Tangerang"
            )
        )
        
        val tpsIds = mutableListOf<String>()
        for (tps in tpsData) {
            val docRef = firestore.collection("tps")
                .add(tps)
                .await()
            tpsIds.add(docRef.id)
        }
        
        return tpsIds
    }
    
    /**
     * Seeds sample schedules for drivers
     */
    private suspend fun seedSchedules(userIds: Map<String, String>, tpsIds: List<String>): List<String> {
        val now = System.currentTimeMillis()
        val oneDayMs = 86400000L
        
        val schedules = listOf(
            Schedule(
                scheduleId = "",
                date = Timestamp(java.util.Date(now + oneDayMs)), // Tomorrow
                driverId = "driver_001",
                tpsRoute = tpsIds.take(3), // First 3 TPS
                status = ScheduleStatus.PENDING,
                createdAt = now - oneDayMs * 2,
                completedAt = null
            ),
            Schedule(
                scheduleId = "",
                date = Timestamp(java.util.Date(now)),  // Today
                driverId = "driver_002",
                tpsRoute = tpsIds.drop(2).take(3), // TPS 3-5
                status = ScheduleStatus.IN_PROGRESS,
                createdAt = now - oneDayMs * 1,
                completedAt = null
            ),
            Schedule(
                scheduleId = "",
                date = Timestamp(java.util.Date(now - oneDayMs)), // Yesterday
                driverId = "driver_003",
                tpsRoute = tpsIds.takeLast(2), // Last 2 TPS
                status = ScheduleStatus.COMPLETED,
                createdAt = now - oneDayMs * 3,
                completedAt = now - oneDayMs / 2 // 12 hours ago
            ),
            Schedule(
                scheduleId = "",
                date = Timestamp(java.util.Date(now + oneDayMs * 2)), // Day after tomorrow
                driverId = "driver_001",
                tpsRoute = listOf(tpsIds[0], tpsIds[2], tpsIds[4]), // Alternate TPS
                status = ScheduleStatus.PENDING,
                createdAt = now - oneDayMs,
                completedAt = null
            ),
            Schedule(
                scheduleId = "",
                date = Timestamp(java.util.Date(now - oneDayMs * 2)), // 2 days ago  
                driverId = "driver_002",
                tpsRoute = tpsIds.take(4),
                status = ScheduleStatus.COMPLETED,
                createdAt = now - oneDayMs * 4,
                completedAt = now - oneDayMs * 2 + 3600000L // 2 days ago + 1 hour
            )
        )
        
        val scheduleIds = mutableListOf<String>()
        for (schedule in schedules) {
            val docRef = firestore.collection("schedules")
                .add(schedule)
                .await()
            scheduleIds.add(docRef.id)
        }
        
        return scheduleIds
    }
    
    /**
     * Seeds sample proof records for completed pickups
     */
    private suspend fun seedProofs(userIds: Map<String, String>, tpsIds: List<String>, scheduleIds: List<String>) {
        val now = System.currentTimeMillis()
        val oneHourMs = 3600000L
        
        val proofs = listOf(
            Proof(
                proofId = "",
                driverId = "driver_003",
                tpsId = tpsIds[0],
                scheduleId = scheduleIds[2], // Completed schedule
                photoUrl = "https://firebasestorage.googleapis.com/sample_proof_1.jpg",
                timestamp = Timestamp(java.util.Date(now - 86400000L + oneHourMs)), // Yesterday + 1 hour
                verified = true
            ),
            Proof(
                proofId = "",
                driverId = "driver_003", 
                tpsId = tpsIds[1],
                scheduleId = scheduleIds[2],
                photoUrl = "https://firebasestorage.googleapis.com/sample_proof_2.jpg",
                timestamp = Timestamp(java.util.Date(now - 86400000L + oneHourMs * 2)),
                verified = true
            ),
            Proof(
                proofId = "",
                driverId = "driver_002",
                tpsId = tpsIds[2],
                scheduleId = scheduleIds[4], // Another completed schedule
                photoUrl = "https://firebasestorage.googleapis.com/sample_proof_3.jpg", 
                timestamp = Timestamp(java.util.Date(now - 86400000L * 2 + oneHourMs)),
                verified = false // Pending verification
            ),
            Proof(
                proofId = "",
                driverId = "driver_002",
                tpsId = tpsIds[3], 
                scheduleId = scheduleIds[4],
                photoUrl = "https://firebasestorage.googleapis.com/sample_proof_4.jpg",
                timestamp = Timestamp(java.util.Date(now - 86400000L * 2 + oneHourMs * 2)),
                verified = true
            )
        )
        
        for (proof in proofs) {
            firestore.collection("proofs")
                .add(proof)
                .await()
        }
    }
    
    /**
     * Clears all existing data from the database
     * Use with caution!
     */
    suspend fun clearAllData(): Result<String> {
        return try {
            val collections = listOf("users", "tps", "schedules", "proofs")
            
            for (collectionName in collections) {
                val documents = firestore.collection(collectionName).get().await()
                for (document in documents.documents) {
                    document.reference.delete().await()
                }
            }
            
            Result.success("All data cleared successfully")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Seeds only sample TPS data (useful for testing TPS operations)
     */
    suspend fun seedTPSOnly(): Result<String> {
        return try {
            val basicTPS = listOf(
                TPS(
                    name = "TPS Test 1",
                    location = GeoPoint(-6.2088, 106.8456),
                    status = TPSStatus.TIDAK_PENUH,
                    address = "Test Address 1"
                ),
                TPS(
                    name = "TPS Test 2",
                    location = GeoPoint(-6.1944, 106.8229),
                    status = TPSStatus.PENUH,
                    address = "Test Address 2"
                )
            )
            
            for (tps in basicTPS) {
                firestore.collection("tps").add(tps).await()
            }
            
            Result.success("TPS test data seeded successfully")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
} 