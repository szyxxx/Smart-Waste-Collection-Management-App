package com.bluebin.data.repository

import com.bluebin.data.seeder.DataSeeder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SeederRepository @Inject constructor(
    private val dataSeeder: DataSeeder
) {
    
    /**
     * Seeds the entire database with comprehensive sample data
     * Includes users, TPS locations, schedules, and proofs
     */
    suspend fun seedFullDatabase(): Result<String> {
        return dataSeeder.seedDatabase()
    }
    
    /**
     * Seeds only TPS sample data for quick testing
     */
    suspend fun seedTPSData(): Result<String> {
        return dataSeeder.seedTPSOnly()
    }
    
    /**
     * Clears all data from the database
     * WARNING: This will permanently delete all data!
     */
    suspend fun clearAllData(): Result<String> {
        return dataSeeder.clearAllData()
    }
    
    /**
     * Resets database with fresh sample data
     * Clears existing data and seeds new sample data
     */
    suspend fun resetDatabaseWithSamples(): Result<String> {
        return try {
            // First clear all existing data
            val clearResult = dataSeeder.clearAllData()
            if (clearResult.isFailure) {
                return clearResult
            }
            
            // Then seed with fresh sample data
            val seedResult = dataSeeder.seedDatabase()
            if (seedResult.isFailure) {
                return seedResult
            }
            
            Result.success("Database reset and seeded successfully with fresh sample data")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
} 