package com.thirtytwo_cereernote.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.thirtytwo_cereernote.ui.component.StatCard
import com.thirtytwo_cereernote.viewmodel.WeeklySummaryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklySummaryScreen(
    onBack: () -> Unit,
    onApplicationClick: (Long) -> Unit = {},
    onAddApplicationClick: () -> Unit = {},
    onStartPracticeClick: () -> Unit = {},
    viewModel: WeeklySummaryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("주간 준비 요약", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // [1] Week Navigation Header
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { viewModel.navigateWeek(-1) }) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Week")
                        }
                        Text(
                            text = uiState.weekLabel,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { viewModel.navigateWeek(1) }) {
                            Icon(Icons.Default.ChevronRight, contentDescription = "Next Week")
                        }
                    }
                }
            }

            // [2] Primary Summary Cards
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatCard(
                            title = "제출한 지원서",
                            value = "${uiState.submittedAppsCount}건",
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "완료한 면접",
                            value = "${uiState.completedInterviewsCount}건",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatCard(
                            title = "작성 완료 복기",
                            value = "${uiState.completedReviewsCount}건",
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "면접 연습 진행",
                            value = "${uiState.completedPracticesCount}회",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // [3] Submitted Applications Details
            item {
                Text("제출 완료 지원서", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            if (uiState.submittedApps.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("해당 주간에 제출된 지원서가 없습니다.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(onClick = onAddApplicationClick, modifier = Modifier.fillMaxWidth()) {
                                Text("새 지원 정보 등록하기")
                            }
                        }
                    }
                }
            } else {
                items(uiState.submittedApps) { app ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onApplicationClick(app.id) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(app.companyName, fontWeight = FontWeight.Bold)
                                Text(app.jobTitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(app.currentStatus.displayName, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            // [4] Next Week Preview
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text("다음 주 예정 사항", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("• 다음 주 제출 마감 공고: ${uiState.upcomingDeadlinesCount}건", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text("• 다음 주 예정 면접 일정: ${uiState.upcomingInterviewsCount}건", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Quick Practice Guide
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("💡 면접 연습으로 다음 주 면접을 준비해보세요", fontWeight = FontWeight.Bold)
                        Text("저장된 질문과 자기소개서를 기반으로 1분 모의 연습을 진행할 수 있습니다.", style = MaterialTheme.typography.bodySmall)
                        Button(onClick = onStartPracticeClick, modifier = Modifier.fillMaxWidth()) {
                            Text("면접 연습 시작하기")
                        }
                    }
                }
            }
        }
    }
}
