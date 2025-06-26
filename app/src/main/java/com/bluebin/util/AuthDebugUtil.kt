package com.bluebin.util

import android.util.Log
import com.bluebin.presentation.auth.AuthUiState
import com.bluebin.data.model.User

object AuthDebugUtil {
    private const val TAG = "AuthDebug"
    private var isDebugEnabled = true // Set to false in production
    
    fun logAuthState(authState: AuthUiState, currentUser: User?, context: String = "") {
        if (!isDebugEnabled) return
        
        val prefix = if (context.isNotEmpty()) "[$context] " else ""
        Log.d(TAG, "${prefix}Auth State:")
        Log.d(TAG, "  - isLoading: ${authState.isLoading}")
        Log.d(TAG, "  - isAuthenticated: ${authState.isAuthenticated}")
        Log.d(TAG, "  - user: ${authState.user?.name} (${authState.user?.role})")
        Log.d(TAG, "  - currentUser: ${currentUser?.name} (${currentUser?.role})")
        Log.d(TAG, "  - approved: ${authState.user?.approved}")
        Log.d(TAG, "  - error: ${authState.error}")
        Log.d(TAG, "  - uid match: ${authState.user?.uid == currentUser?.uid}")
    }
    
    fun logNavigation(from: String, to: String, reason: String = "") {
        if (!isDebugEnabled) return
        
        val reasonText = if (reason.isNotEmpty()) " - $reason" else ""
        Log.i(TAG, "Navigation: $from → $to$reasonText")
    }
    
    fun logUserLoad(uid: String, success: Boolean, user: User? = null) {
        if (!isDebugEnabled) return
        
        if (success) {
            Log.d(TAG, "User loaded successfully: $uid → ${user?.name} (${user?.role}, approved: ${user?.approved})")
        } else {
            Log.e(TAG, "Failed to load user: $uid")
        }
    }
    
    fun logFirebaseAuthChange(firebaseUser: com.google.firebase.auth.FirebaseUser?) {
        if (!isDebugEnabled) return
        
        if (firebaseUser != null) {
            Log.d(TAG, "Firebase user: ${firebaseUser.uid} (${firebaseUser.email})")
        } else {
            Log.d(TAG, "Firebase user: null")
        }
    }
    
    fun setDebugEnabled(enabled: Boolean) {
        isDebugEnabled = enabled
    }
} 