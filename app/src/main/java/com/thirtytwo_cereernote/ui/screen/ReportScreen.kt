package com.thirtytwo_cereernote.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.thirtytwo_cereernote.ui.component.StatCard
import com.thirtytwo_cereernote.util.StatPeriod
import com.thirtytwo_cereernote.viewmodel.ReportViewModel

@Composable
fun ReportScreen(viewModel: ReportViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val stats = uiState.stats

    var showInfoDialog by remember { mutableStateOf(false) }

    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            title = { Text("지원 통계 집계 기준", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("• 관심/지원 예정 항목은 실제 지원 건수에서 제외됩니다.", style = MaterialTheme.typography.bodySmall)
                    Text("• 서류 합격률 = (서류 합격 건수) / (서류 결과가 확정된 건수)", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    Text("  * 결과 대기 중인 항목은 합격률 분모에서 제외됩니다.", style = MaterialTheme.typography.bodySmall)
                    Text("• 면접 합격률 = (면접 합격 건수) / (면접 결과가 확정된 건수)", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    Text("• 제출 대비 최종 합격률 = (최종 합격 건수) / (실제 제출 완료 건수)", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    Text("• 결과가 확인된 건수가 0건이면 0%로 표시하지 않고 '-'로 안내합니다.", style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(onClick = { showInfoDialog = false }) { Text("확인") }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "지원 성과 리포트",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { showInfoDialog = true }) {
                        Icon(Icons.Default.Info, contentDescription = "집계 기준 안내", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            // [1] Period & Category Filters (Smooth Rounded Pill Chips)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatPeriod.entries.filter { it != StatPeriod.CUSTOM }.forEach { period ->
                            SmoothFilterChip(
                                selected = uiState.currentFilter.period == period,
                                onClick = { viewModel.setPeriod(period) },
                                label = period.displayName
                            )
                        }
                    }

                    if (stats.availableJobs.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SmoothFilterChip(
                                selected = uiState.currentFilter.jobFilter == null,
                                onClick = { viewModel.setJobFilter(null) },
                                label = "전체 직무"
                            )
                            stats.availableJobs.forEach { job ->
                                SmoothFilterChip(
                                    selected = uiState.currentFilter.jobFilter == job,
                                    onClick = { viewModel.setJobFilter(job) },
                                    label = job
                                )
                            }
                        }
                    }

                    if (stats.availableDocVersions.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SmoothFilterChip(
                                selected = uiState.currentFilter.docVersionFilter == null,
                                onClick = { viewModel.setDocVersionFilter(null) },
                                label = "전체 서류"
                            )
                            stats.availableDocVersions.forEach { ver ->
                                SmoothFilterChip(
                                    selected = uiState.currentFilter.docVersionFilter == ver,
                                    onClick = { viewModel.setDocVersionFilter(ver) },
                                    label = ver
                                )
                            }
                        }
                    }
                }
            }

            // Small Sample Warning Box
            stats.smallSampleNotice?.let { notice ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "⚠️ $notice",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }

            // [2] Primary KPI Cards
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val docVal = if (stats.docFinishedCount > 0 && stats.docPassRate != null) {
                            "%.1f%%".format(stats.docPassRate)
                        } else {
                            "—"
                        }
                        val docSub = if (stats.docFinishedCount > 0) {
                            "${stats.docPassedCount}건 합격 · 대기 ${stats.docPendingCount}건"
                        } else {
                            "결과 대기 ${stats.docPendingCount}건"
                        }
                        StatCard(
                            title = "서류 합격률",
                            value = docVal,
                            subtitle = docSub,
                            modifier = Modifier.weight(1f)
                        )

                        val intVal = if (stats.interviewFinishedCount > 0 && stats.interviewPassRate != null) {
                            "%.1f%%".format(stats.interviewPassRate)
                        } else {
                            "—"
                        }
                        val intSub = if (stats.interviewFinishedCount > 0) {
                            "${stats.interviewPassedCount}건 합격 · 대기 ${stats.interviewPendingCount}건"
                        } else {
                            "결과 대기 ${stats.interviewPendingCount}건"
                        }
                        StatCard(
                            title = "면접 합격률",
                            value = intVal,
                            subtitle = intSub,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val finalVal = if (stats.actualAppliedCount > 0 && stats.finalPassRateVsSubmitted != null) {
                            "%.1f%%".format(stats.finalPassRateVsSubmitted)
                        } else {
                            "—"
                        }
                        val finalSub = if (stats.actualAppliedCount > 0) {
                            "${stats.finalPassedCount}건 최종합격"
                        } else {
                            "제출 기록 없음"
                        }
                        StatCard(
                            title = "최종 합격률",
                            value = finalVal,
                            subtitle = finalSub,
                            modifier = Modifier.weight(1f)
                        )

                        val appliedSub = if (stats.finalPendingCount > 0) {
                            "결과 대기 ${stats.finalPendingCount}건"
                        } else {
                            "전체 완료"
                        }
                        StatCard(
                            title = "실제 제출",
                            value = "${stats.actualAppliedCount}건",
                            subtitle = appliedSub,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // [3] Insights Section
            item {
                Text(text = "커리어 인사이트", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            if (stats.insights.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = "지원서를 작성하면 데이터 기반 리포트가 생성됩니다.",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else {
                items(stats.insights) { insight ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Lightbulb, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(text = insight, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            // [4] Status Breakdown
            item {
                Text(text = "지원 상태별 분포", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            if (uiState.statusBreakdown.isEmpty()) {
                item { Text("등록된 지원서가 없습니다.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline) }
            } else {
                uiState.statusBreakdown.forEach { (status, count) ->
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 1.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(status.displayName, fontWeight = FontWeight.Medium)
                                Text("${count}건", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SmoothFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    val containerColor = if (selected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    }
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
    } else {
        Color.Transparent
    }

    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = CircleShape,
        color = containerColor,
        contentColor = contentColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = contentColor
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}
