package com.thirtytwo_cereernote.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.thirtytwo_cereernote.R
import com.thirtytwo_cereernote.ui.component.StatCard
import com.thirtytwo_cereernote.viewmodel.ReportViewModel

@Composable
fun ReportScreen(viewModel: ReportViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(text = "지원 통계", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCard(
                        title = "서류 합격률", 
                        value = (uiState.docPassRate?.let { "%.1f%%".format(it) } ?: "-") + 
                                "\n(${uiState.docPassedCount}/${uiState.docFinishedCount}건)", 
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "면접 합격률", 
                        value = (uiState.interviewPassRate?.let { "%.1f%%".format(it) } ?: "-") + 
                                "\n(${uiState.interviewPassedCount}/${uiState.interviewFinishedCount}건)", 
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCard(
                        title = "최종 합격률", 
                        value = (uiState.finalPassRate?.let { "%.1f%%".format(it) } ?: "-") + 
                                "\n(${uiState.finalPassedCount}/${uiState.actualAppliedCount}건)",
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(title = "관심 기업", value = "${uiState.favoriteCount}곳", modifier = Modifier.weight(1f))
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("💡 지표 산정 기준 안내", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        Text("• 서류 합격률: 서류 결과 완료 건 중 서류를 합격한 비율 (관심/예정 제외)", style = MaterialTheme.typography.labelSmall)
                        Text("• 면접 합격률: 면접 결과 완료 건 중 면접을 합격한 비율", style = MaterialTheme.typography.labelSmall)
                        Text("• 최종 합격률: 실제 전체 지원서 제출 완료 건 중 최종 합격에 도달한 비율", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "커리어 인사이트", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }

            if (uiState.insights.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Text(
                            text = "데이터를 입력하면 맞춤형 분석 리포트가 생성됩니다.",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else {
                items(uiState.insights) { insight ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Icon(Icons.Default.Lightbulb, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(text = insight, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "지원 현황 상세", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }

            if (uiState.statusBreakdown.isEmpty()) {
                item { Text("데이터가 없습니다.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline) }
            }

            uiState.statusBreakdown.forEach { (status, count) ->
                item {
                    ListItem(
                        headlineContent = { Text(status.displayName, fontWeight = FontWeight.Medium) },
                        trailingContent = { Text("${count}건", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) }
                    )
                }
            }
        }
    }
}
