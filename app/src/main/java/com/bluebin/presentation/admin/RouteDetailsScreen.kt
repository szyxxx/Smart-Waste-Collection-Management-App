package com.bluebin.presentation.admin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.bluebin.data.model.*
import com.bluebin.ui.components.*
import com.bluebin.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteDetailsScreen(
    scheduleId: String,
    onBackClick: () -> Unit,
    viewModel: RouteDetailsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showPhotoDialog by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(scheduleId) {
        viewModel.loadRouteDetails(scheduleId)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Modern Header
        ModernGradientHeader(
            title = "Route Details",
            subtitle = uiState.schedule?.let { "ID: ${it.scheduleId.take(8)}" } ?: "Loading...",
            emoji = "🚛",
            onBackClick = onBackClick,
            actions = {
                ModernIconButton(
                    onClick = { viewModel.refreshRouteDetails() },
                    icon = Icons.Default.Refresh,
                    contentDescription = "Refresh"
                )
            }
        )

        // Schedule info bar
        AnimatedVisibility(visible = uiState.schedule != null) {
            uiState.schedule?.let { schedule ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = uiState.driver?.name ?: "No driver assigned",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = SimpleDateFormat("EEEE, MMM dd, yyyy", Locale.getDefault()).format(schedule.date.toDate()),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        ModernStatusChip(
                            text = schedule.status.name.replace("_", " "),
                            color = when (schedule.status) {
                                ScheduleStatus.COMPLETED -> SuccessColor
                                ScheduleStatus.IN_PROGRESS -> InfoColor
                                ScheduleStatus.ASSIGNED -> WarningColor
                                else -> MaterialTheme.colorScheme.outline
                            }
                        )
                    }
                }
            }
        }

        // Error handling
        uiState.error?.let { error ->
            ModernAlertCard(
                title = "Error Loading Route",
                message = error,
                alertType = AlertType.ERROR,
                action = {
                    IconButton(onClick = { viewModel.clearError() }) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Dismiss",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            )
        }

        // Content
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                ModernLoadingIndicator(message = "Loading route details...")
            }
        } else if (uiState.schedule != null) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Route Summary
                item {
                    ModernRouteSummaryCard(
                        schedule = uiState.schedule!!,
                        driver = uiState.driver,
                        routeSteps = uiState.routeSteps
                    )
                }

                // Route Progress
                item {
                    ModernRouteProgressCard(routeSteps = uiState.routeSteps)
                }

                // Route Steps Section
                item {
                    ModernSectionHeader(
                        title = "Route Details",
                        subtitle = "${uiState.routeSteps.size} TPS locations • ${uiState.routeSteps.count { it.isCompleted }} completed"
                    )
                }

                // Individual route steps
                items(uiState.routeSteps) { step ->
                    ModernRouteStepCard(
                        step = step,
                        onPhotoClick = { photoUrl ->
                            showPhotoDialog = photoUrl
                        }
                    )
                }

                // Route Optimization (if available)
                uiState.schedule?.optimizationData?.let { optimizationData ->
                    item {
                        ModernSectionHeader(
                            title = "Optimized Route",
                            subtitle = "AI-generated route optimization details"
                        )
                    }
                    
                    item {
                        ModernRouteOptimizationCard(optimizationData = optimizationData)
                    }
                }
            }
        }
    }

    // Photo Dialog
    showPhotoDialog?.let { photoUrl ->
        ModernPhotoViewDialog(
            photoUrl = photoUrl,
            onDismiss = { showPhotoDialog = null }
        )
    }
}

@Composable
private fun ModernRouteSummaryCard(
    schedule: Schedule,
    driver: User?,
    routeSteps: List<RouteStep>
) {
    val completedSteps = routeSteps.count { it.isCompleted }
    val totalSteps = routeSteps.size
    val progressPercentage = if (totalSteps > 0) (completedSteps.toFloat() / totalSteps) else 0f

    ModernCard {
        Column(
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Route,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Route Summary",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "${completedSteps}/${totalSteps} stops completed (${progressPercentage.roundToInt()}%)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Progress bar
            ModernProgressBar(
                progress = progressPercentage / 100f,
                label = "Overall Progress",
                color = MaterialTheme.colorScheme.primary
            )

            // Stats grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ModernSummaryStatCard(
                    title = "Total Distance",
                    value = "${schedule.totalDistance.roundToInt()} km",
                    icon = Icons.Default.Straighten,
                    modifier = Modifier.weight(1f)
                )
                ModernSummaryStatCard(
                    title = "Est. Duration",
                    value = "${schedule.estimatedDuration.roundToInt()} min",
                    icon = Icons.Default.AccessTime,
                    modifier = Modifier.weight(1f)
                )
                ModernSummaryStatCard(
                    title = "TPS Stops",
                    value = "$totalSteps",
                    icon = Icons.Default.LocationOn,
                    modifier = Modifier.weight(1f)
                )
            }

            // Driver info if available
            driver?.let { driver ->
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            driver.name.first().uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            driver.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "Assigned Driver",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    ModernStatusChip(
                        text = "ACTIVE",
                        color = SuccessColor
                    )
                }
            }
        }
    }
}

@Composable
private fun ModernRouteProgressCard(routeSteps: List<RouteStep>) {
    val completedSteps = routeSteps.count { it.isCompleted }
    val stepsWithIssues = routeSteps.count { it.hasIssue }
    val stepsWithPhotos = routeSteps.count { !it.proofPhotoUrl.isNullOrEmpty() }

    ModernCard {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Collection Progress",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ModernProgressStatCard(
                    title = "Completed",
                    value = completedSteps.toString(),
                    total = routeSteps.size,
                    color = SuccessColor,
                    icon = Icons.Default.CheckCircle,
                    modifier = Modifier.weight(1f)
                )
                ModernProgressStatCard(
                    title = "With Photos",
                    value = stepsWithPhotos.toString(),
                    total = routeSteps.size,
                    color = InfoColor,
                    icon = Icons.Default.PhotoCamera,
                    modifier = Modifier.weight(1f)
                )
                if (stepsWithIssues > 0) {
                    ModernProgressStatCard(
                        title = "Issues",
                        value = stepsWithIssues.toString(),
                        total = routeSteps.size,
                        color = WarningColor,
                        icon = Icons.Default.Warning,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ModernRouteStepCard(
    step: RouteStep,
    onPhotoClick: (String) -> Unit
) {
    val dateFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    
    ModernCard {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Step header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            if (step.isCompleted) SuccessColor.copy(alpha = 0.12f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
                            RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (step.isCompleted) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                            tint = SuccessColor
                        )
                    } else {
                        Text(
                            step.stepNumber.toString(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "Stop ${step.stepNumber}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        if (step.hasIssue) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = "Issue reported",
                                modifier = Modifier.size(18.dp),
                                tint = WarningColor
                            )
                        }
                    }
                    
                    Text(
                        step.tpsName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        step.tpsAddress,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                ModernStatusChip(
                    text = if (step.isCompleted) "COMPLETED" else "PENDING",
                    color = if (step.isCompleted) SuccessColor else MaterialTheme.colorScheme.outline
                )
            }

            // Completion details
            if (step.isCompleted) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Completion time
                    step.completedAt?.let { completedAt ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Schedule,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "Completed at ${dateFormatter.format(Date(completedAt))}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    
                    // Photo proof
                    if (!step.proofPhotoUrl.isNullOrEmpty()) {
                        ModernPhotoProofCard(
                            photoUrl = step.proofPhotoUrl,
                            onPhotoClick = onPhotoClick
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.PhotoCamera,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "No proof photo available",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    // Notes
                    if (step.notes.isNotEmpty()) {
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Note,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Column {
                                Text(
                                    "Driver Notes:",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    step.notes,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            } else {
                // Pending state
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Awaiting collection",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
            }
        }
    }
}

@Composable
private fun ModernRouteOptimizationCard(optimizationData: OptimizationData) {
    ModernCard {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Route Optimization Details",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ModernOptimizationStatCard(
                    title = "Total Distance",
                    value = "${optimizationData.totalDistanceKm.roundToInt()} km",
                    icon = Icons.Default.Straighten,
                    modifier = Modifier.weight(1f)
                )
                ModernOptimizationStatCard(
                    title = "Est. Duration",
                    value = "${optimizationData.estimatedTotalMinutes.roundToInt()} min",
                    icon = Icons.Default.AccessTime,
                    modifier = Modifier.weight(1f)
                )
                ModernOptimizationStatCard(
                    title = "Segments",
                    value = optimizationData.routeSegments.size.toString(),
                    icon = Icons.Default.Route,
                    modifier = Modifier.weight(1f)
                )
            }
            
            if (optimizationData.routeSegments.isNotEmpty()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                
                Text(
                    "Route Segments",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    optimizationData.routeSegments.forEachIndexed { index, segment ->
                        ModernRouteSegmentItem(
                            segment = segment,
                            segmentNumber = index + 1
                        )
                    }
                }
            }
        }
    }
}

// Helper Composables
@Composable
private fun ModernSummaryStatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Text(
                title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ModernProgressStatCard(
    title: String,
    value: String,
    total: Int,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.12f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = color
            )
            Text(
                "$value/$total",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Text(
                title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ModernOptimizationStatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Text(
                title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ModernRouteSegmentItem(
    segment: SimpleRouteSegment,
    segmentNumber: Int
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                segmentNumber.toString(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        
        Text(
            "${segment.from} → ${segment.to}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        
        Text(
            "${segment.distanceKm.roundToInt()}km",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Text(
            "${segment.estimatedTimeMinutes.roundToInt()}min",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ModernPhotoProofCard(
    photoUrl: String,
    onPhotoClick: (String) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Photo thumbnail
        Card(
            modifier = Modifier
                .size(64.dp)
                .clickable { onPhotoClick(photoUrl) },
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            )
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(photoUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "Proof photo thumbnail",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                placeholder = painterResource(android.R.drawable.ic_menu_gallery),
                error = painterResource(android.R.drawable.ic_menu_report_image)
            )
        }
        
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.PhotoCamera,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = InfoColor
                )
                Text(
                    "Collection Proof",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )
            }
            Text(
                "Tap to view full image",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        TextButton(
            onClick = { onPhotoClick(photoUrl) }
        ) {
            Text(
                "View",
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun ModernPhotoViewDialog(
    photoUrl: String,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        ModernCard(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.PhotoCamera,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "Collection Proof Photo",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Image
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(photoUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Collection proof photo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit,
                        placeholder = painterResource(android.R.drawable.ic_menu_gallery),
                        error = painterResource(android.R.drawable.ic_menu_report_image)
                    )
                }
                
                // Close button
                ModernPrimaryButton(
                    text = "Close",
                    onClick = onDismiss
                )
            }
        }
    }
} 