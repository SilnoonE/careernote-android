package com.thirtytwo_cereernote.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.thirtytwo_cereernote.R
import com.thirtytwo_cereernote.data.model.Application
import com.thirtytwo_cereernote.data.model.CareerTip
import com.thirtytwo_cereernote.ui.component.StatCard
import com.thirtytwo_cereernote.viewmodel.HomeViewModel
import java.util.concurrent.TimeUnit

@Composable
fun HomeScreen(
    onApplicationClick: (Long) -> Unit = {},
    onTipClick: (String) -> Unit = {},
    onAddApplicationClick: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    
    // Notification Permission for Android 13+
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (!isGranted) {
                // Handle denial
            }
        }
        
        LaunchedEffect(Unit) {
            val status = androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            )
            if (status != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // D-Day Refresh on Resume
    var lastResumeTime by remember { mutableStateOf(System.currentTimeMillis()) }
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                lastResumeTime = System.currentTimeMillis()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // ... hero section ...
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                            )
                        ),
                        shape = RoundedCornerShape(bottomStart = 40.dp, bottomEnd = 40.dp)
                    )
                    .padding(horizontal = 24.dp, vertical = 40.dp)
            ) {
                Column {
                    Text(
                        text = stringResource(id = R.string.home_greeting),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "오늘의 커리어 성장을 위해\n새로운 기록을 남겨볼까요?",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.9f),
                        lineHeight = 24.sp
                    )
                }
            }
        }

        // Floating Stats Section
        item {
            Column(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .offset(y = (-30).dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        title = stringResource(id = R.string.home_kpi_total_apps),
                        value = uiState.totalApplications.toString(),
                        modifier = Modifier.weight(1f),
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                    StatCard(
                        title = stringResource(id = R.string.home_kpi_ongoing),
                        value = uiState.ongoingApplications.toString(),
                        modifier = Modifier.weight(1f),
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        title = stringResource(id = R.string.home_kpi_doc_passed),
                        value = uiState.documentPassed.toString(),
                        modifier = Modifier.weight(1f),
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                    StatCard(
                        title = stringResource(id = R.string.home_kpi_final_passed),
                        value = uiState.finalPassed.toString(),
                        modifier = Modifier.weight(1f),
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                }
            }
        }

        // D-Day Section
        item {
            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                Text(
                    text = "마감 임박 D-Day",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                if (uiState.upcomingDeadlines.isEmpty()) {
                    // Empty State for D-Day
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onAddApplicationClick),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.EventNote,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = "지원 일정을 등록하고 마감일을 관리해보세요!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(end = 4.dp)
                    ) {
                        items(uiState.upcomingDeadlines) { app ->
                            key(app.id, lastResumeTime) {
                                HomeDeadlineCard(app, onClick = { onApplicationClick(app.id) })
                            }
                        }
                    }
                }
            }
        }

        // Daily Tip Section
        uiState.randomTip?.let { tip ->
            item {
                Column(modifier = Modifier.padding(top = 32.dp, start = 24.dp, end = 24.dp)) {
                    Text(
                        text = "오늘의 커리어 꿀팁",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    HomeTipCard(tip, onClick = { onTipClick(tip.category.name) })
                }
            }
        }
    }
}

@Composable
fun HomeDeadlineCard(app: Application, onClick: () -> Unit) {
    val dDayTextInfo = app.deadlineDate?.let { date ->
        val targetDate = date.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate()
        val today = java.time.LocalDate.now()
        val diff = java.time.temporal.ChronoUnit.DAYS.between(today, targetDate)
        
        when {
            diff < 0 -> "마감" to Color.Gray
            diff == 0L -> "D-Day" to Color(0xFFFF5252)
            diff in 1..3 -> "D-$diff" to Color(0xFFFFA000)
            else -> "D-$diff" to Color(0xFF2196F3)
        }
    } ?: ("상시" to Color(0xFF2196F3))
    
    val dDayText = dDayTextInfo.first
    val dDayColor = dDayTextInfo.second

    Card(
        modifier = Modifier
            .width(200.dp)
            .height(120.dp)
            .padding(bottom = 4.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                color = dDayColor,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = dDayText,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            Column {
                Text(
                    text = app.companyName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = app.jobTitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun HomeTipCard(tip: CareerTip, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp) // Space for shadow
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f))
                .padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primary,
                shape = CircleShape,
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.TipsAndUpdates,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(20.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tip.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = tip.body.split("\n").firstOrNull() ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}
