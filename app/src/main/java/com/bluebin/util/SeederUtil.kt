package com.bluebin.util

import com.bluebin.data.seeder.DataSeeder
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Utility class for quick database seeding operations
 * Can be used from anywhere in the application for development purposes
 */
object SeederUtil {
    
    /**
     * Quick seed function that can be called from anywhere
     * Usage: SeederUtil.quickSeed() 
     */
    fun quickSeed(
        onSuccess: (String) -> Unit = { println("Seeding successful: $it") },
        onFailure: (Throwable) -> Unit = { println("Seeding failed: ${it.message}") }
    ) {
        val firestore = FirebaseFirestore.getInstance()
        val seeder = DataSeeder(firestore)
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = seeder.seedDatabase()
                result.fold(
                    onSuccess = onSuccess,
                    onFailure = onFailure
                )
            } catch (e: Exception) {
                onFailure(e)
            }
        }
    }
    
    /**
     * Quick TPS-only seed function
     */
    fun quickSeedTPS(
        onSuccess: (String) -> Unit = { println("TPS seeding successful: $it") },
        onFailure: (Throwable) -> Unit = { println("TPS seeding failed: ${it.message}") }
    ) {
        val firestore = FirebaseFirestore.getInstance()
        val seeder = DataSeeder(firestore)
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = seeder.seedTPSOnly()
                result.fold(
                    onSuccess = onSuccess,
                    onFailure = onFailure
                )
            } catch (e: Exception) {
                onFailure(e)
            }
        }
    }
    
    /**
     * Quick clear all data function
     */
    fun quickClear(
        onSuccess: (String) -> Unit = { println("Clear successful: $it") },
        onFailure: (Throwable) -> Unit = { println("Clear failed: ${it.message}") }
    ) {
        val firestore = FirebaseFirestore.getInstance()
        val seeder = DataSeeder(firestore)
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = seeder.clearAllData()
                result.fold(
                    onSuccess = onSuccess,
                    onFailure = onFailure
                )
            } catch (e: Exception) {
                onFailure(e)
            }
        }
    }
} 