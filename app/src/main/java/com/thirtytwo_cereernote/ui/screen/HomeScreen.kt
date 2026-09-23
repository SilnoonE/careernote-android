package com.thirtytwo_cereernote.ui.screen

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.thirtytwo_cereernote.R
import com.thirtytwo_cereernote.data.model.Application
import com.thirtytwo_cereernote.data.model.CareerTip
import com.thirtytwo_cereernote.data.model.Interview
import com.thirtytwo_cereernote.ui.component.StatCard
import com.thirtytwo_cereernote.util.CommonUtils
import com.thirtytwo_cereernote.viewmodel.*
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    onApplicationClick: (Long) -> Unit = {},
    onTipClick: (String) -> Unit = {},
    onAddApplicationClick: () -> Unit = {},
    onWeeklySummaryClick: () -> Unit = {},
    onStartPracticeClick: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Notification Permission Request for Android 13+
    var showPermissionSettingGuide by remember { mutableStateOf(false) }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val permissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (!isGranted) {
                showPermissionSettingGuide = true
            }
        }

        LaunchedEffect(Unit) {
            val status = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            )
            if (status != PackageManager.PERMISSION_GRANTED) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    if (showPermissionSettingGuide) {
        AlertDialog(
            onDismissRequest = { showPermissionSettingGuide = false },
            title = { Text("알림 권한 안내", fontWeight = FontWeight.Bold) },
            text = { Text("면접 및 지원 마감 알림을 받으시려면 앱 설정에서 알림 권한을 허용해주세요.", style = MaterialTheme.typography.bodyMedium) },
            confirmButton = {
                TextButton(onClick = {
                    showPermissionSettingGuide = false
                    try {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }) { Text("설정으로 이동") }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionSettingGuide = false }) { Text("나중에") }
            }
        )
    }

    // Lifecycle resume listener
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Active Dialog States
    var selectedSnapshotApp by remember { mutableStateOf<Application?>(null) }
    var selectedPrepInterview by remember { mutableStateOf<Interview?>(null) }
    var selectedReviewInterview by remember { mutableStateOf<Interview?>(null) }

    var isDialogSaving by remember { mutableStateOf(false) }
    var dialogSaveError by remember { mutableStateOf<String?>(null) }

    // Preparation Memo Dialog
    if (selectedPrepInterview != null) {
        val interview = selectedPrepInterview!!
        var memoText by remember { mutableStateOf(interview.preparations.ifBlank { interview.memo }) }

        AlertDialog(
            onDismissRequest = { if (!isDialogSaving) selectedPrepInterview = null },
            title = { Text("면접 준비 메모", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${interview.stage} 준비 사항을 자유롭게 기록하세요.", style = MaterialTheme.typography.bodySmall)
                    dialogSaveError?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                    OutlinedTextField(
                        value = memoText,
                        onValueChange = { memoText = it; dialogSaveError = null },
                        modifier = Modifier.fillMaxWidth().height(140.dp),
                        placeholder = { Text("예: 자기소개 1분, 예상 질문 3가지, 지원 동기 체크") },
                        enabled = !isDialogSaving
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = !isDialogSaving,
                    onClick = {
                        isDialogSaving = true
                        scope.launch {
                            try {
                                val success = viewModel.savePreparationMemo(interview, memoText)
                                isDialogSaving = false
                                if (success) {
                                    Toast.makeText(context, "메모가 저장되었습니다.", Toast.LENGTH_SHORT).show()
                                    selectedPrepInterview = null
                                } else {
                                    dialogSaveError = "저장하지 못했습니다. 다시 시도해 주세요."
                                }
                            } catch (e: Exception) {
                                isDialogSaving = false
                                dialogSaveError = "저장 오류가 발생했습니다."
                            }
                        }
                    }
                ) { Text(if (isDialogSaving) "저장 중..." else "저장") }
            },
            dismissButton = {
                TextButton(
                    enabled = !isDialogSaving,
                    onClick = { selectedPrepInterview = null; dialogSaveError = null }
                ) { Text("취소") }
            }
        )
    }

    // Interview Review Dialog
    if (selectedReviewInterview != null) {
        val interview = selectedReviewInterview!!
        var strengths by remember { mutableStateOf(interview.strengths) }
        var weaknesses by remember { mutableStateOf(interview.weaknesses) }
        var betterAnswer by remember { mutableStateOf(interview.review) }
        var nextPrep by remember { mutableStateOf(interview.nextPreparations) }

        AlertDialog(
            onDismissRequest = { if (!isDialogSaving) selectedReviewInterview = null },
            title = { Text("면접 복기 작성", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    dialogSaveError?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                    OutlinedTextField(
                        value = strengths,
                        onValueChange = { strengths = it; dialogSaveError = null },
                        label = { Text("잘한 점") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isDialogSaving
                    )
                    OutlinedTextField(
                        value = weaknesses,
                        onValueChange = { weaknesses = it; dialogSaveError = null },
                        label = { Text("어려웠던 질문 / 아쉬운 점") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isDialogSaving
                    )
                    OutlinedTextField(
                        value = betterAnswer,
                        onValueChange = { betterAnswer = it; dialogSaveError = null },
                        label = { Text("보완할 답변") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isDialogSaving
                    )
                    OutlinedTextField(
                        value = nextPrep,
                        onValueChange = { nextPrep = it; dialogSaveError = null },
                        label = { Text("다음 면접 준비사항") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isDialogSaving
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = !isDialogSaving,
                    onClick = {
                        isDialogSaving = true
                        scope.launch {
                            try {
                                val success = viewModel.saveInterviewReview(interview, strengths, weaknesses, betterAnswer, nextPrep)
                                isDialogSaving = false
                                if (success) {
                                    Toast.makeText(context, "복기가 저장되었습니다.", Toast.LENGTH_SHORT).show()
                                    selectedReviewInterview = null
                                } else {
                                    dialogSaveError = "저장하지 못했습니다. 다시 시도해 주세요."
                                }
                            } catch (e: Exception) {
                                isDialogSaving = false
                                dialogSaveError = "저장 오류가 발생했습니다."
                            }
                        }
                    }
                ) { Text(if (isDialogSaving) "저장 중..." else "복기 완료") }
            },
            dismissButton = {
                TextButton(
                    enabled = !isDialogSaving,
                    onClick = { selectedReviewInterview = null; dialogSaveError = null }
                ) { Text("취소") }
            }
        )
    }

    // Full Snapshot Viewer Dialog
    if (selectedSnapshotApp != null) {
        val app = selectedSnapshotApp!!
        AlertDialog(
            onDismissRequest = { selectedSnapshotApp = null },
            title = { Text("제출 당시 서류 내용", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("💡 제출 시점 스냅샷은 원본 문서 변경에 영향을 받지 않습니다.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    HorizontalDivider()
                    Text("• 회사명: ${app.companyName}", fontWeight = FontWeight.Bold)
                    Text("• 직무: ${app.jobTitle}")

                    if (app.resumeSnapshot != null) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("📄 [이력서 스냅샷]", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                                ParseAndRenderSnapshotText(app.resumeSnapshot)
                            }
                        }
                    }

                    if (app.coverLetterSnapshot != null) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("📝 [자기소개서 스냅샷]", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                                ParseAndRenderSnapshotText(app.coverLetterSnapshot)
                            }
                        }
                    }

                    if (app.portfolioSnapshot != null) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("🔗 [포트폴리오 스냅샷]", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                                ParseAndRenderSnapshotText(app.portfolioSnapshot)
                            }
                        }
                    }

                    if (app.attachedPdfPath != null) {
                        Button(
                            onClick = { CommonUtils.openFile(context, app.attachedPdfPath) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("첨부 PDF 파일 열기 (${app.attachedPdfName ?: "파일"})")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val appId = app.id
                    selectedSnapshotApp = null
                    onApplicationClick(appId)
                }) { Text("지원 상세 보기") }
            },
            dismissButton = {
                TextButton(onClick = { selectedSnapshotApp = null }) { Text("닫기") }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // [1] Hero Section
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                            )
                        ),
                        shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)
                    )
                    .padding(horizontal = 24.dp, vertical = 28.dp)
            ) {
                Column {
                    Text(
                        text = "오늘 행동할 커리어 일정을 확인해보세요",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "면접 준비, 마감 서류 체크, 면접 복기를 바로 진행할 수 있습니다.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                    )
                }
            }
        }

        // [New Quick Action Cards: Weekly Summary & Practice]
        item {
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(onClick = onWeeklySummaryClick),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Icon(Icons.Default.DateRange, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("이번 주 준비 현황", fontWeight = FontWeight.Bold)
                            Text("주간 요약 보고서", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(onClick = onStartPracticeClick),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Icon(Icons.Default.RecordVoiceOver, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("내 기록 면접 연습", fontWeight = FontWeight.Bold)
                            Text("1분 질문 실전 연습", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        // [2] Section A: 오늘의 면접
        if (uiState.todayInterviews.isNotEmpty()) {
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    Text(
                        text = "오늘의 면접 일정",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    uiState.todayInterviews.forEach { item ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = item.application.companyName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    AssistChip(
                                        onClick = {},
                                        label = { Text(item.interview.stage) },
                                        colors = AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                                    )
                                }
                                Text(
                                    text = "${item.application.jobTitle} • ${if (item.interview.isOnline) "온라인" else item.interview.location.ifBlank { "장소 미정" }}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = { selectedSnapshotApp = item.application },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("제출 서류", style = MaterialTheme.typography.labelMedium)
                                    }
                                    Button(
                                        onClick = { selectedPrepInterview = item.interview },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(if (item.hasPrepMemo) "메모 보기" else "준비 메모", style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // [3] Section B: 제출이 필요한 마감
        item {
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Text(
                    text = "제출이 필요한 마감",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.height(12.dp))

                if (uiState.unsubmittedDeadlines.isEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onAddApplicationClick),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.AutoMirrored.Filled.EventNote, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(16.dp))
                            Text("마감 서류가 모두 제출되었습니다! 새로운 지원을 등록해보세요.", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(end = 4.dp)
                    ) {
                        items(uiState.unsubmittedDeadlines) { item ->
                            val badgeColor = when (item.category) {
                                DeadlineCategory.TODAY -> MaterialTheme.colorScheme.error
                                DeadlineCategory.TOMORROW -> MaterialTheme.colorScheme.tertiary
                                DeadlineCategory.UPCOMING -> MaterialTheme.colorScheme.primary
                                DeadlineCategory.OVERDUE -> MaterialTheme.colorScheme.outline
                            }
                            Card(
                                modifier = Modifier
                                    .width(220.dp)
                                    .clickable { onApplicationClick(item.application.id) },
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Surface(color = badgeColor, shape = RoundedCornerShape(6.dp)) {
                                        Text(
                                            text = item.dDayText,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(item.application.companyName, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(item.application.jobTitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(item.formattedDeadline, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                }
                            }
                        }
                    }
                }
            }
        }

        // [4] Section C: 면접 복기
        if (uiState.pendingReviews.isNotEmpty()) {
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    Text(
                        text = "면접 복기 작성 필요",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    uiState.pendingReviews.forEach { item ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("${item.application.companyName} • ${item.interview.stage}", fontWeight = FontWeight.Bold)
                                if (item.needsCompletionConfirm) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("면접이 끝났나요? 완료 후 복기를 작성해보세요.", style = MaterialTheme.typography.bodySmall)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = {
                                            scope.launch { viewModel.confirmInterviewCompleted(item.interview) }
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) { Text("네, 면접을 완료했습니다") }
                                } else if (item.needsReview) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = { selectedReviewInterview = item.interview },
                                        modifier = Modifier.fillMaxWidth()
                                    ) { Text("면접 복기 작성하기") }
                                }
                            }
                        }
                    }
                }
            }
        }

        // [5] Section D: 제출 전 서류 확인
        if (uiState.docCheckItems.isNotEmpty()) {
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    Text(
                        text = "제출 전 필요 서류 체크",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    uiState.docCheckItems.forEach { docItem ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(docItem.application.companyName, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val reqs = docItem.requiredTypes
                                    val rChecked = reqs.contains("RESUME")
                                    val cChecked = reqs.contains("COVER_LETTER")
                                    val pChecked = reqs.contains("PORTFOLIO")

                                    FilterChip(
                                        selected = rChecked,
                                        onClick = {
                                            val newTypes = if (rChecked) reqs - "RESUME" else reqs + "RESUME"
                                            scope.launch { viewModel.updateRequiredDocTypes(docItem.application.id, newTypes.joinToString(",")) }
                                        },
                                        label = { Text("이력서 ${if (docItem.resumeState == DocState.CONNECTED) "✓" else ""}") }
                                    )
                                    FilterChip(
                                        selected = cChecked,
                                        onClick = {
                                            val newTypes = if (cChecked) reqs - "COVER_LETTER" else reqs + "COVER_LETTER"
                                            scope.launch { viewModel.updateRequiredDocTypes(docItem.application.id, newTypes.joinToString(",")) }
                                        },
                                        label = { Text("자소서 ${if (docItem.coverLetterState == DocState.CONNECTED) "✓" else ""}") }
                                    )
                                    FilterChip(
                                        selected = pChecked,
                                        onClick = {
                                            val newTypes = if (pChecked) reqs - "PORTFOLIO" else reqs + "PORTFOLIO"
                                            scope.launch { viewModel.updateRequiredDocTypes(docItem.application.id, newTypes.joinToString(",")) }
                                        },
                                        label = { Text("포트폴리오 ${if (docItem.portfolioState == DocState.CONNECTED) "✓" else ""}") }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // [6] Floating Summary Stats
        item {
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Text("전체 누적 지원 현황", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        title = stringResource(id = R.string.home_kpi_total_apps),
                        value = uiState.totalApplications.toString(),
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = stringResource(id = R.string.home_kpi_ongoing),
                        value = uiState.ongoingApplications.toString(),
                        modifier = Modifier.weight(1f)
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
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = stringResource(id = R.string.home_kpi_final_passed),
                        value = uiState.finalPassed.toString(),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // [7] Daily Tip
        uiState.randomTip?.let { tip ->
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    Text(
                        text = "오늘의 커리어 꿀팁",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    HomeTipCard(tip, onClick = { onTipClick(tip.category.name) })
                }
            }
        }
    }
}

@Composable
fun ParseAndRenderSnapshotText(json: String) {
    val text = remember(json) {
        try {
            val jsonElement = kotlinx.serialization.json.Json.parseToJsonElement(json)
            val obj = jsonElement as? kotlinx.serialization.json.JsonObject ?: return@remember json
            val sb = StringBuilder()
            obj["title"]?.let { sb.append("제목: ${(it as kotlinx.serialization.json.JsonPrimitive).content}\n") }
            obj["version"]?.let { sb.append("버전: ${(it as kotlinx.serialization.json.JsonPrimitive).content}\n") }
            obj["url"]?.let { sb.append("URL: ${(it as kotlinx.serialization.json.JsonPrimitive).content}\n") }
            obj["memo"]?.let { sb.append("메모: ${(it as kotlinx.serialization.json.JsonPrimitive).content}\n") }
            obj["questions"]?.let { q ->
                val qArr = q as kotlinx.serialization.json.JsonArray
                qArr.forEachIndexed { idx, item ->
                    val qObj = item as kotlinx.serialization.json.JsonObject
                    val question = (qObj["question"] as? kotlinx.serialization.json.JsonPrimitive)?.content ?: ""
                    val answer = (qObj["answer"] as? kotlinx.serialization.json.JsonPrimitive)?.content ?: ""
                    sb.append("\nQ${idx + 1}. $question\n")
                    sb.append("A. $answer\n")
                }
            }
            sb.toString().trim()
        } catch (_: Exception) {
            json
        }
    }
    Text(text, style = MaterialTheme.typography.bodySmall)
}

@Composable
fun HomeTipCard(tip: CareerTip, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f))
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primary,
                shape = CircleShape,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.TipsAndUpdates,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tip.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = tip.body.take(60).replace("\n", " ") + "...",
                    style = MaterialTheme.typography.bodySmall,
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
