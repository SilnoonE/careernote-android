package com.thirtytwo_cereernote.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.thirtytwo_cereernote.R
import com.thirtytwo_cereernote.data.model.*
import com.thirtytwo_cereernote.viewmodel.CareerViewModel

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import com.thirtytwo_cereernote.util.CommonUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CareerDetailScreen(
    id: String,
    itemId: Long,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    viewModel: CareerViewModel = hiltViewModel()
) {
    val scope = rememberCoroutineScope()
    val dateFormat = remember { java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()) }
    val context = androidx.compose.ui.platform.LocalContext.current

    var itemState by remember { mutableStateOf<Any?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(id, itemId) {
        isLoading = true
        itemState = when(id) {
            "cover_letter" -> viewModel.getCoverLetterById(itemId)
            "resume" -> viewModel.getResumeById(itemId)
            "portfolio" -> viewModel.getPortfolioById(itemId)
            "project" -> viewModel.getProjectById(itemId)
            "certification" -> viewModel.getCertificationById(itemId)
            "interview_question" -> viewModel.getInterviewQuestionById(itemId)
            "experience" -> viewModel.getExperienceById(itemId)
            "education" -> viewModel.getEducationById(itemId)
            else -> null
        }
        isLoading = false
    }

    var showDeleteConfirm by remember { mutableStateOf(false) }

    val categoryTitle = remember(id) {
        when(id) {
            "cover_letter" -> "자기소개서"
            "resume" -> "이력서"
            "portfolio" -> "포트폴리오"
            "project" -> "프로젝트"
            "certification" -> "자격증"
            "interview_question" -> "면접 질문"
            "experience" -> "경력"
            "education" -> "교육"
            else -> "상세 보기"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = categoryTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp) // Lower for stability
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로가기")
                    }
                },
                actions = {
                    if (itemState != null) {
                        IconButton(
                            onClick = onEdit,
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "수정")
                        }
                        IconButton(
                            onClick = {
                                scope.launch {
                                    val newId = when (val item = itemState) {
                                        is CoverLetter -> viewModel.duplicateCoverLetter(item)
                                        is Resume -> viewModel.duplicateResume(item)
                                        is Portfolio -> viewModel.duplicatePortfolio(item)
                                        is Project -> viewModel.duplicateProject(item)
                                        is Certification -> viewModel.duplicateCertification(item)
                                        is InterviewQuestion -> viewModel.duplicateInterviewQuestion(item)
                                        is CareerExperience -> viewModel.duplicateExperience(item)
                                        is Education -> viewModel.duplicateEducation(item)
                                        else -> null
                                    }
                                    if (newId != null) onBack()
                                }
                            },
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "복제")
                        }
                        IconButton(
                            onClick = { showDeleteConfirm = true },
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "삭제", tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { innerPadding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (itemState == null) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.msg_no_record_found))
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (val item = itemState) {
                    is CoverLetter -> {
                        DetailItem(stringResource(R.string.label_item_title), item.title)
                        DetailItem(stringResource(R.string.label_item_company), item.companyName)
                        DetailItem(stringResource(R.string.label_item_content), item.memo)
                    }
                    is Resume -> {
                        DetailItem(stringResource(R.string.label_item_title), item.title)
                        DetailItem(stringResource(R.string.label_item_version), item.version)
                        DetailItem(stringResource(R.string.label_item_memo), item.memo)
                    }
                    is Portfolio -> {
                        DetailItem(stringResource(R.string.label_item_title), item.title)
                        DetailItem(stringResource(R.string.label_item_url), item.url, isLink = true, onClick = { CommonUtils.openUrl(context, item.url) })
                        DetailItem(stringResource(R.string.label_item_memo), item.memo)
                    }
                    is Project -> {
                        DetailItem(stringResource(R.string.label_item_pj_name), item.name)
                        DetailItem(stringResource(R.string.label_item_role), item.role)
                        DetailItem(stringResource(R.string.label_item_tech), item.techStack)
                        DetailItem(stringResource(R.string.label_item_action), item.description)
                        DetailItem(stringResource(R.string.label_item_situation), item.problem)
                        DetailItem(stringResource(R.string.label_item_result), item.outcome)
                    }
                    is Certification -> {
                        DetailItem(stringResource(R.string.label_item_cert_name), item.name)
                        DetailItem(stringResource(R.string.label_item_issuer), item.issuer)
                        DetailItem(stringResource(R.string.label_item_score_grade), "${item.score} / ${item.grade}")
                        DetailItem(stringResource(R.string.label_item_acquired_date), item.acquisitionDate?.let { d -> dateFormat.format(d) } ?: "-")
                    }
                    is InterviewQuestion -> {
                        DetailItem(stringResource(R.string.label_item_category), item.category)
                        DetailItem(stringResource(R.string.label_item_question), item.question)
                        DetailItem(stringResource(R.string.label_item_my_answer), item.myAnswer)
                        DetailItem(stringResource(R.string.label_item_better_answer), item.betterAnswer)
                    }
                    is CareerExperience -> {
                        DetailItem(stringResource(R.string.label_item_company), item.companyName)
                        DetailItem(stringResource(R.string.label_item_job), item.jobTitle)
                        DetailItem(stringResource(R.string.label_item_period), "${dateFormat.format(item.startDate)} ~ ${item.endDate?.let { d -> dateFormat.format(d) } ?: "재직 중"}")
                        DetailItem(stringResource(R.string.label_item_work), item.description)
                        DetailItem(stringResource(R.string.label_item_outcome), item.outcome)
                    }
                    is Education -> {
                        DetailItem(stringResource(R.string.label_item_edu_major), item.name)
                        DetailItem(stringResource(R.string.label_item_inst), item.institution)
                        DetailItem(stringResource(R.string.label_item_status), item.status)
                        DetailItem(stringResource(R.string.label_item_detail), item.description)
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.btn_delete)) },
            text = { Text(stringResource(R.string.msg_delete_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        when (val item = itemState) {
                            is CoverLetter -> viewModel.deleteCoverLetter(item)
                            is Resume -> viewModel.deleteResume(item)
                            is Portfolio -> viewModel.deletePortfolio(item)
                            is Project -> viewModel.deleteProject(item)
                            is Certification -> viewModel.deleteCertification(item)
                            is InterviewQuestion -> viewModel.deleteInterviewQuestion(item)
                            is CareerExperience -> viewModel.deleteExperience(item)
                            is Education -> viewModel.deleteEducation(item)
                        }
                        showDeleteConfirm = false
                        onBack()
                    }
                }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                    Text(stringResource(R.string.btn_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text(stringResource(R.string.btn_cancel)) }
            }
        )
    }
}

@Composable
fun DetailItem(label: String, value: String, isLink: Boolean = false, onClick: () -> Unit = {}) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = if(value.isBlank()) "-" else value,
            style = MaterialTheme.typography.bodyLarge,
            color = if (isLink && value.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            textDecoration = if (isLink && value.isNotBlank()) TextDecoration.Underline else null,
            modifier = if (isLink && value.isNotBlank()) Modifier.clickable { onClick() } else Modifier
        )
        HorizontalDivider(modifier = Modifier.padding(top = 12.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
    }
}
