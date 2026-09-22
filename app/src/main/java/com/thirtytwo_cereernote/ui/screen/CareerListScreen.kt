package com.thirtytwo_cereernote.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.thirtytwo_cereernote.R
import com.thirtytwo_cereernote.data.model.*
import com.thirtytwo_cereernote.viewmodel.CareerViewModel
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CareerListScreen(
    id: String,
    title: String,
    onBack: () -> Unit,
    onAddClick: () -> Unit,
    onItemClick: (Long) -> Unit,
    viewModel: CareerViewModel = hiltViewModel()
) {
    val coverLetters by viewModel.coverLetters.collectAsState()
    val resumes by viewModel.resumes.collectAsState()
    val portfolios by viewModel.portfolios.collectAsState()
    val projects by viewModel.projects.collectAsState()
    val certifications by viewModel.certifications.collectAsState()
    val interviewQuestions by viewModel.interviewQuestions.collectAsState()
    val experiences by viewModel.experiences.collectAsState()
    val educations by viewModel.educations.collectAsState()

    val isEmpty = when (id) {
        "cover_letter" -> coverLetters.isEmpty()
        "resume" -> resumes.isEmpty()
        "portfolio" -> portfolios.isEmpty()
        "project" -> projects.isEmpty()
        "certification" -> certifications.isEmpty()
        "interview_question" -> interviewQuestions.isEmpty()
        "experience" -> experiences.isEmpty()
        "education" -> educations.isEmpty()
        else -> true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = title,
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddClick,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.btn_add)) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (isEmpty) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if(title.isNotBlank()) "$title 기록이 없습니다." else "기록이 없습니다.",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "첫 번째 기록을 남겨보세요!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = onAddClick) {
                        Text(stringResource(R.string.btn_add))
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    when (id) {
                        "cover_letter" -> items(coverLetters) { item ->
                            CoverLetterCard(item, onClick = { onItemClick(item.id) })
                        }
                        "resume" -> items(resumes) { item ->
                            ResumeCard(item, onClick = { onItemClick(item.id) })
                        }
                        "portfolio" -> items(portfolios) { item ->
                            PortfolioCard(item, onClick = { onItemClick(item.id) })
                        }
                        "project" -> items(projects) { item ->
                            ProjectCard(item, onClick = { onItemClick(item.id) })
                        }
                        "certification" -> items(certifications) { item ->
                            CertificationCard(item, onClick = { onItemClick(item.id) })
                        }
                        "interview_question" -> items(interviewQuestions) { item ->
                            InterviewQuestionCard(item, onClick = { onItemClick(item.id) })
                        }
                        "experience" -> items(experiences) { item ->
                            ExperienceCard(item, onClick = { onItemClick(item.id) })
                        }
                        "education" -> items(educations) { item ->
                            EducationCard(item, onClick = { onItemClick(item.id) })
                        }
                    }
                }
            }
        }
    }
}

// ... Cards remain same ...
@Composable
fun CoverLetterCard(item: CoverLetter, onClick: () -> Unit) {
    CareerBaseCard(onClick = onClick) {
        CareerHeader(title = item.title, icon = Icons.Default.Description, badge = "자기소개서")
        CareerInfoRow("기업명", item.companyName)
        CareerInfoRow("직무", item.jobTitle)
        CareerInfoRow("버전", item.version)
    }
}

@Composable
fun ResumeCard(item: Resume, onClick: () -> Unit) {
    CareerBaseCard(onClick = onClick) {
        CareerHeader(title = item.title, icon = Icons.AutoMirrored.Filled.Article, badge = "이력서")
        CareerInfoRow("버전", item.version)
        if (item.memo.isNotBlank()) {
            CareerInfoRow("메모", item.memo, maxLines = 1)
        }
    }
}

@Composable
fun PortfolioCard(item: Portfolio, onClick: () -> Unit) {
    CareerBaseCard(onClick = onClick) {
        CareerHeader(title = item.title, icon = Icons.Default.AccountTree, badge = "포트폴리오")
        CareerInfoRow("URL/경로", item.url.ifBlank { item.filePath })
        CareerInfoRow("버전", item.version)
    }
}

@Composable
fun ProjectCard(item: Project, onClick: () -> Unit) {
    CareerBaseCard(onClick = onClick) {
        CareerHeader(title = item.name, icon = Icons.Default.IntegrationInstructions, badge = "프로젝트")
        CareerInfoRow("역할", item.role)
        CareerInfoRow("기술 스택", item.techStack)
        CareerInfoRow("상세 설명", item.description, maxLines = 2)
    }
}

@Composable
fun CertificationCard(item: Certification, onClick: () -> Unit) {
    val dateFormat = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault())
    CareerBaseCard(onClick = onClick) {
        CareerHeader(title = item.name, icon = Icons.Default.Badge, badge = "자격증")
        CareerInfoRow("발행처", item.issuer)
        item.acquisitionDate?.let { CareerInfoRow("취득일", dateFormat.format(it)) }
        CareerInfoRow("점수/등급", listOfNotNull(item.score.takeIf { it.isNotBlank() }, item.grade.takeIf { it.isNotBlank() }).joinToString(" / "))
    }
}

@Composable
fun InterviewQuestionCard(item: InterviewQuestion, onClick: () -> Unit) {
    CareerBaseCard(onClick = onClick) {
        CareerHeader(title = item.question, icon = Icons.Default.QuestionAnswer, badge = item.category)
        CareerInfoRow("나의 답변", item.myAnswer, maxLines = 2)
        if (item.memo.isNotBlank()) {
            CareerInfoRow("메모", item.memo, maxLines = 1)
        }
    }
}

@Composable
fun ExperienceCard(item: CareerExperience, onClick: () -> Unit) {
    val dateFormat = SimpleDateFormat("yyyy.MM", Locale.getDefault())
    val period = "${dateFormat.format(item.startDate)} ~ ${if (item.isCurrent) "재직 중" else item.endDate?.let { dateFormat.format(it) } ?: ""}"
    CareerBaseCard(onClick = onClick) {
        CareerHeader(title = item.companyName, icon = Icons.Default.BusinessCenter, badge = item.employmentType.displayName)
        CareerInfoRow("직무", item.jobTitle)
        CareerInfoRow("기간", period)
        CareerInfoRow("주요 성과", item.outcome, maxLines = 2)
    }
}

@Composable
fun EducationCard(item: Education, onClick: () -> Unit) {
    val dateFormat = SimpleDateFormat("yyyy.MM", Locale.getDefault())
    val period = "${dateFormat.format(item.startDate)} ~ ${item.endDate?.let { dateFormat.format(it) } ?: ""}"
    CareerBaseCard(onClick = onClick) {
        CareerHeader(title = item.institution, icon = Icons.Default.School, badge = item.name)
        CareerInfoRow("상태", item.status)
        CareerInfoRow("기간", period)
    }
}

@Composable
private fun CareerBaseCard(onClick: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}

@Composable
private fun CareerHeader(title: String, icon: ImageVector, badge: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.padding(top = 2.dp)
            ) {
                Text(
                    text = badge,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun CareerInfoRow(label: String, value: String, maxLines: Int = 1) {
    if (value.isBlank()) return
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
            modifier = Modifier.width(70.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis
        )
    }
}
