package com.bluebin.data.api

import com.bluebin.data.model.RouteOptimizationRequest
import com.bluebin.data.model.RouteOptimizationResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface RouteOptimizationApi {
    @POST("optimize")
    suspend fun optimizeRoute(
        @Body request: RouteOptimizationRequest
    ): Response<RouteOptimizationResponse>
} 