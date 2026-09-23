package com.thirtytwo_cereernote.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.thirtytwo_cereernote.viewmodel.PracticeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PracticeScreen(
    onBack: () -> Unit,
    viewModel: PracticeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("내 기록 기반 면접 연습", fontWeight = FontWeight.Bold) },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .padding(16.dp)
        ) {
            when {
                // 1. Session Active (In Practice Flashcard Step)
                uiState.isSessionActive && uiState.currentQuestionResult != null -> {
                    val currentRes = uiState.currentQuestionResult!!
                    val idx = uiState.currentQuestionIndex
                    val total = uiState.totalQuestionsCount

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Progress
                        LinearProgressIndicator(
                            progress = { (idx.toFloat() + 1f) / total.coerceAtLeast(1) },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text(
                            text = "문항 ${idx + 1} / $total",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        // Question Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                AssistChip(
                                    onClick = {},
                                    label = { Text(currentRes.sourceLabel) }
                                )
                                Text(
                                    text = currentRes.questionText,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Practice Answer Input
                        OutlinedTextField(
                            value = uiState.practiceAnswerText,
                            onValueChange = { viewModel.updatePracticeAnswerText(it) },
                            label = { Text("나의 연습 답변 작성 (선택)") },
                            modifier = Modifier.fillMaxWidth().height(140.dp),
                            placeholder = { Text("생각한 핵심 키워드나 두괄식 답변을 간단히 적어보세요.") }
                        )

                        // Toggle Original Answer
                        if (currentRes.originalAnswer.isNotBlank()) {
                            OutlinedButton(
                                onClick = { viewModel.toggleOriginalAnswer() },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = if (uiState.isOriginalAnswerVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(if (uiState.isOriginalAnswerVisible) "내 기존 답변 숨기기" else "내 기존 작성 답변 보기")
                            }

                            if (uiState.isOriginalAnswerVisible) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text("📝 내 기존 작성 답변:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(currentRes.originalAnswer, style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text("답변 자기 평가 선택:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.submitQuestionEvaluation("GOOD") },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF34D399))
                            ) {
                                Text("잘 답함", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = { viewModel.submitQuestionEvaluation("NEEDS_WORK") },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFB923C))
                            ) {
                                Text("보완 필요", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = { viewModel.submitQuestionEvaluation("RETRY") },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF87171))
                            ) {
                                Text("다시 연습", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // 2. Session Completed Summary Screen
                uiState.isCompleted && uiState.sessionResults.isNotEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("연습 세션 완료!", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                }
                                Text("총 ${uiState.totalQuestionsCount}문항 중 보완 필요/다시 연습 ${uiState.needsWorkCount}문항입니다.", style = MaterialTheme.typography.bodyMedium)
                            }
                        }

                        Text("이번 세션 연습 결과", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                        uiState.sessionResults.forEach { res ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(res.sourceLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                        val evalText = when (res.selfEvaluation) {
                                            "GOOD" -> "잘 답함"
                                            "NEEDS_WORK" -> "보완 필요"
                                            else -> "다시 연습"
                                        }
                                        Text(evalText, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                    }
                                    Text(res.questionText, fontWeight = FontWeight.Bold)
                                    if (res.practiceAnswer.isNotBlank()) {
                                        Text("연습 답변: ${res.practiceAnswer}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (uiState.needsWorkCount > 0) {
                            Button(
                                onClick = { viewModel.retryNeedsWork() },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("보완 필요 문항만 다시 연습하기")
                            }
                        }

                        OutlinedButton(
                            onClick = { viewModel.startNewSession() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("새 연습 세션 시작하기")
                        }

                        TextButton(
                            onClick = { viewModel.finishSession() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("연습 마치기")
                        }
                    }
                }

                // 3. Main Practice Selection Screen (No active session)
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text("🎯 내 기록 기반 모의 면접", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text(
                                        "등록한 면접 질문과 자기소개서 문항 중 3개를 무작위로 추출하여 짧게 실전 연습을 진행합니다.",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text("• 연습 가능 질문 수: ${uiState.availableCandidatesCount}개", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)

                                    uiState.errorMessage?.let { err ->
                                        Text(err, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    Button(
                                        onClick = { viewModel.startNewSession() },
                                        modifier = Modifier.fillMaxWidth(),
                                        enabled = uiState.availableCandidatesCount > 0
                                    ) {
                                        Text("3문항 연습 시작하기")
                                    }
                                }
                            }
                        }

                        item {
                            Text("과거 연습 기록", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }

                        if (uiState.pastSessions.isEmpty()) {
                            item {
                                Text("저장된 과거 연습 기록이 없습니다.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                            }
                        } else {
                            items(uiState.pastSessions) { session ->
                                val dateStr = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(session.startedAt)
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("연습 세션 ($dateStr)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                            Text(if (session.isCompleted) "완료됨" else "진행 중", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                        }
                                        IconButton(onClick = { viewModel.deleteSession(session) }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
