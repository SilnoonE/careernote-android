package com.thirtytwo_cereernote.ui.screen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.thirtytwo_cereernote.R
import com.thirtytwo_cereernote.data.model.*
import com.thirtytwo_cereernote.util.CommonUtils
import com.thirtytwo_cereernote.viewmodel.ApplicationsViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApplicationDetailScreen(
    applicationId: Long,
    onBack: () -> Unit,
    viewModel: ApplicationsViewModel = hiltViewModel()
) {
    val scope = rememberCoroutineScope()
    var application by remember { mutableStateOf<Application?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    val interviews by viewModel.getInterviews(applicationId).collectAsState(initial = emptyList())

    val coverLetters by viewModel.allCoverLetters.collectAsState()
    val resumes by viewModel.allResumes.collectAsState()
    val portfolios by viewModel.allPortfolios.collectAsState()

    val apps by viewModel.applications.collectAsState()
    LaunchedEffect(apps, applicationId) {
        isLoading = apps.isEmpty() && viewModel.isInitialEmpty.value.not()
        application = apps.find { it.id == applicationId }
        if (apps.isNotEmpty() && application == null) {
            isLoading = false
        }
    }

    var showStatusDialog by remember { mutableStateOf(false) }
    var showEditInfoDialog by remember { mutableStateOf(false) }
    var showAddInterviewDialog by remember { mutableStateOf(false) }
    var showLinkDocDialog by remember { mutableStateOf(false) }
    var showSnapshotDialog by remember { mutableStateOf<Pair<String, String>?>(null) }

    val context = LocalContext.current
    val dateFormat = remember { java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()) }

    val pdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            scope.launch { viewModel.attachPdf(applicationId, it, "제출된 서류.pdf") }
        }
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val currentApp = application
    if (currentApp == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.msg_no_record_found))
        }
        return
    }

    if (showSnapshotDialog != null) {
        AlertDialog(
            onDismissRequest = { showSnapshotDialog = null },
            title = { Text(showSnapshotDialog?.first ?: "스냅샷") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(showSnapshotDialog?.second ?: "")
                }
            },
            confirmButton = {
                TextButton(onClick = { showSnapshotDialog = null }) { Text("닫기") }
            }
        )
    }

    if (showLinkDocDialog) {
        AlertDialog(
            onDismissRequest = { showLinkDocDialog = false },
            title = { Text("서류 연결 및 스냅샷") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text("자기소개서 선택", style = MaterialTheme.typography.labelMedium)
                    coverLetters.forEach { cl ->
                        ListItem(
                            headlineContent = { Text(cl.title) },
                            modifier = Modifier.clickable {
                                scope.launch { viewModel.linkCoverLetter(currentApp, cl) }
                                showLinkDocDialog = false
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("이력서 선택", style = MaterialTheme.typography.labelMedium)
                    resumes.forEach { rs ->
                        ListItem(
                            headlineContent = { Text(rs.title) },
                            modifier = Modifier.clickable {
                                scope.launch { viewModel.linkResume(currentApp, rs) }
                                showLinkDocDialog = false
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("포트폴리오 선택", style = MaterialTheme.typography.labelMedium)
                    portfolios.forEach { pt ->
                        ListItem(
                            headlineContent = { Text(pt.title) },
                            modifier = Modifier.clickable {
                                scope.launch { viewModel.linkPortfolio(currentApp, pt) }
                                showLinkDocDialog = false
                            }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLinkDocDialog = false }) { Text("취소") }
            }
        )
    }

    if (showStatusDialog) {
        AlertDialog(
            onDismissRequest = { showStatusDialog = false },
            title = { Text("상태 변경") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    ApplicationStatus.entries.forEach { status ->
                        TextButton(
                            onClick = {
                                scope.launch { viewModel.updateStatus(applicationId, status, "상태 전환") }
                                showStatusDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = status.displayName, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showStatusDialog = false }) { Text("취소") }
            }
        )
    }

    if (showEditInfoDialog) {
        var editCompany by remember { mutableStateOf(currentApp.companyName) }
        var editJob by remember { mutableStateOf(currentApp.jobTitle) }
        var editChannel by remember { mutableStateOf(currentApp.channel) }
        var editMemo by remember { mutableStateOf(currentApp.memo) }
        var editUrl by remember { mutableStateOf(currentApp.noticeUrl) }

        AlertDialog(
            onDismissRequest = { showEditInfoDialog = false },
            title = { Text("일반 정보 수정") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(value = editCompany, onValueChange = { editCompany = it }, label = { Text(stringResource(R.string.label_company_name)) })
                    OutlinedTextField(value = editJob, onValueChange = { editJob = it }, label = { Text(stringResource(R.string.label_job_title)) })
                    OutlinedTextField(value = editChannel, onValueChange = { editChannel = it }, label = { Text("지원 경로") })
                    OutlinedTextField(value = editUrl, onValueChange = { editUrl = it }, label = { Text(stringResource(R.string.label_notice_url)) })
                    OutlinedTextField(value = editMemo, onValueChange = { editMemo = it }, label = { Text(stringResource(R.string.label_memo_content)) }, maxLines = 4)
                }
            },
            confirmButton = {
                TextButton(
                    enabled = editCompany.isNotBlank() && editJob.isNotBlank(),
                    onClick = {
                        scope.launch {
                            viewModel.updateApplicationInfo(
                                currentApp.copy(
                                    companyName = editCompany,
                                    jobTitle = editJob,
                                    channel = editChannel,
                                    noticeUrl = editUrl,
                                    memo = editMemo,
                                    updatedAt = java.util.Date()
                                )
                            )
                        }
                        showEditInfoDialog = false
                    }
                ) { Text(stringResource(R.string.btn_save)) }
            },
            dismissButton = {
                TextButton(onClick = { showEditInfoDialog = false }) { Text(stringResource(R.string.btn_cancel)) }
            }
        )
    }

    if (showAddInterviewDialog) {
        var stage by remember { mutableStateOf("1차 면접") }
        var loc by remember { mutableStateOf("") }
        var method by remember { mutableStateOf("일대일") }
        var prepMemo by remember { mutableStateOf("") }
        var interviewDate by remember { mutableStateOf(java.util.Date()) }

        val cal = java.util.Calendar.getInstance().apply { time = interviewDate }
        val datePicker = android.app.DatePickerDialog(context, { _, y, m, d ->
            cal.set(y, m, d)
            interviewDate = cal.time
        }, cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH), cal.get(java.util.Calendar.DAY_OF_MONTH))

        val timePicker = android.app.TimePickerDialog(context, { _, h, min ->
            cal.set(java.util.Calendar.HOUR_OF_DAY, h)
            cal.set(java.util.Calendar.MINUTE, min)
            interviewDate = cal.time
        }, cal.get(java.util.Calendar.HOUR_OF_DAY), cal.get(java.util.Calendar.MINUTE), false)

        AlertDialog(
            onDismissRequest = { showAddInterviewDialog = false },
            title = { Text("면접 일정 등록") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = stage, onValueChange = { stage = it }, label = { Text("면접 차수 *") })
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { datePicker.show() }, modifier = Modifier.weight(1f)) {
                            Text(java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(interviewDate))
                        }
                        OutlinedButton(onClick = { timePicker.show() }, modifier = Modifier.weight(1f)) {
                            Text(java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(interviewDate))
                        }
                    }
                    OutlinedTextField(value = loc, onValueChange = { loc = it }, label = { Text("장소/링크") })
                    OutlinedTextField(value = method, onValueChange = { method = it }, label = { Text("방식") })
                    OutlinedTextField(value = prepMemo, onValueChange = { prepMemo = it }, label = { Text("메모") })
                }
            },
            confirmButton = {
                TextButton(enabled = stage.isNotBlank(), onClick = {
                    scope.launch {
                        viewModel.addInterview(Interview(applicationId = applicationId, stage = stage, interviewDate = interviewDate, location = loc, method = method, preparations = prepMemo))
                    }
                    showAddInterviewDialog = false
                }) { Text("등록") }
            },
            dismissButton = { TextButton(onClick = { showAddInterviewDialog = false }) { Text(stringResource(R.string.btn_cancel)) } }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.title_application_detail),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
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
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
        ) {
            // [1] Company and Status Hero
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = currentApp.companyName,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = currentApp.jobTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { viewModel.toggleFavorite(currentApp) }) {
                                Icon(
                                    imageVector = if (currentApp.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = null,
                                    tint = if (currentApp.isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        AssistChip(
                            onClick = { showStatusDialog = true },
                            label = { Text(currentApp.currentStatus.displayName, fontWeight = FontWeight.Bold) },
                            leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            colors = AssistChipDefaults.assistChipColors(
                                labelColor = MaterialTheme.colorScheme.primary,
                                leadingIconContentColor = MaterialTheme.colorScheme.primary
                            ),
                            border = AssistChipDefaults.assistChipBorder(
                                enabled = true,
                                borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            )
                        )
                    }
                }
            }

            // [2] Schedule Section
            item {
                SectionHeader(title = stringResource(R.string.label_schedule), icon = Icons.Default.CalendarMonth)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        InfoRow(label = stringResource(R.string.label_applied_date), value = dateFormat.format(currentApp.appliedDate))
                        InfoRow(label = stringResource(R.string.label_deadline_date), value = currentApp.deadlineDate?.let { dateFormat.format(it) } ?: "상시/미정")
                    }
                }
            }

            // [3] Documents Section
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    SectionHeader(title = stringResource(R.string.label_docs), icon = Icons.Default.Description)
                    Row {
                        IconButton(onClick = { showLinkDocDialog = true }) { Icon(Icons.Default.Link, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                        IconButton(onClick = { pdfLauncher.launch("application/pdf") }) { Icon(Icons.Default.FileUpload, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                    }
                }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SnapshotItemView(stringResource(R.string.career_cover_letter), currentApp.coverLetterSnapshot) { t, c -> showSnapshotDialog = t to c }
                        SnapshotItemView(stringResource(R.string.career_resume), currentApp.resumeSnapshot) { t, c -> showSnapshotDialog = t to c }
                        SnapshotItemView(stringResource(R.string.career_portfolio), currentApp.portfolioSnapshot) { t, c -> showSnapshotDialog = t to c }

                        if (currentApp.attachedPdfName != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { currentApp.attachedPdfPath?.let { CommonUtils.openFile(context, it) } }
                                    .padding(vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(20.dp), tint = Color(0xFFE91E63))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${stringResource(R.string.label_attached_pdf)}: ${currentApp.attachedPdfName}",
                                    color = MaterialTheme.colorScheme.primary,
                                    textDecoration = TextDecoration.Underline,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }

            // [4] Interviews Section
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    SectionHeader(title = stringResource(R.string.label_interviews), icon = Icons.Default.Groups)
                    IconButton(onClick = { showAddInterviewDialog = true }) {
                        Icon(Icons.Default.AddCircleOutline, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
                if (interviews.isEmpty()) {
                    Text(
                        "등록된 면접 일정이 없습니다.",
                        modifier = Modifier.padding(start = 4.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                interviews.forEach { interview ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text(text = interview.stage, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                IconButton(
                                    onClick = {
                                        scope.launch { viewModel.removeInterview(interview) }
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.outline)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = dateFormat.format(interview.interviewDate), style = MaterialTheme.typography.bodySmall)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Place, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.outline)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = interview.location.ifBlank { "장소 미정" }, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }

            // [5] Links and Memo Section
            item {
                SectionHeader(title = stringResource(R.string.label_links_memo), icon = Icons.Default.Link)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (currentApp.noticeUrl.isNotBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.label_notice_url),
                                    color = MaterialTheme.colorScheme.primary,
                                    textDecoration = TextDecoration.Underline,
                                    modifier = Modifier.clickable { CommonUtils.openUrl(context, currentApp.noticeUrl) },
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                        Text(
                            text = currentApp.memo.ifBlank { "작성된 메모가 없습니다." },
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (currentApp.memo.isBlank()) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // [6] Actions
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = { showEditInfoDialog = true },
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.btn_edit_info))
                    }
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                viewModel.deleteApplication(currentApp)
                                onBack()
                            }
                        },
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.btn_delete_all))
                    }
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String, icon: ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun SnapshotItemView(label: String, json: String?, onClick: (String, String) -> Unit) {
    if (json == null) return
    val content = remember(json) {
        try {
            val jsonElement = kotlinx.serialization.json.Json.parseToJsonElement(json)
            val obj = jsonElement as kotlinx.serialization.json.JsonObject
            val sb = StringBuilder()
            obj["title"]?.let { sb.append("제목: ${(it as kotlinx.serialization.json.JsonPrimitive).content}\n") }
            obj["version"]?.let { sb.append("버전: ${(it as kotlinx.serialization.json.JsonPrimitive).content}\n") }
            obj["memo"]?.let { sb.append("\n${(it as kotlinx.serialization.json.JsonPrimitive).content}\n") }
            obj["questions"]?.let {
                val qArr = it as kotlinx.serialization.json.JsonArray
                qArr.forEach { q ->
                    val qObj = q as kotlinx.serialization.json.JsonObject
                    sb.append("\nQ: ${qObj["question"]?.let { (it as kotlinx.serialization.json.JsonPrimitive).content }}\n")
                    sb.append("A: ${qObj["answer"]?.let { (it as kotlinx.serialization.json.JsonPrimitive).content }}\n")
                }
            }
            sb.toString()
        } catch (_: Exception) { json }
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(label, content) }
            .padding(vertical = 4.dp)
    ) {
        Icon(Icons.AutoMirrored.Filled.InsertDriveFile, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.secondary)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "$label (스냅샷 보기)",
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
