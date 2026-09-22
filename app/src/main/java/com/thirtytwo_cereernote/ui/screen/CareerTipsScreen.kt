package com.thirtytwo_cereernote.ui.screen

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.thirtytwo_cereernote.R
import com.thirtytwo_cereernote.data.model.CareerTip
import com.thirtytwo_cereernote.data.model.TipCategory
import com.thirtytwo_cereernote.viewmodel.CareerTipsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CareerTipsScreen(
    onCategoryClick: (TipCategory) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.career_tips_title),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(top = 8.dp) // Lower for stability
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.career_tips_hero_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stringResource(R.string.career_tips_hero_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                        lineHeight = 22.sp
                    )
                }
            }

            item {
                CategoryHubCard(
                    title = stringResource(R.string.career_tips_market),
                    subtitle = "2026년 최신 고용 시장 데이터 분석",
                    icon = Icons.Default.AutoGraph,
                    color = Color(0xFF2196F3),
                    onClick = { onCategoryClick(TipCategory.MARKET) }
                )
            }

            item {
                CategoryHubCard(
                    title = stringResource(R.string.career_tips_job_guide),
                    subtitle = "직무별 핵심 역량과 포트폴리오 가이드",
                    icon = Icons.Default.WorkHistory,
                    color = Color(0xFF673AB7),
                    onClick = { onCategoryClick(TipCategory.JOB_GUIDE) }
                )
            }

            item {
                CategoryHubCard(
                    title = stringResource(R.string.career_tips_interview),
                    subtitle = "합격을 부르는 면접 전략과 체크리스트",
                    icon = Icons.Default.InterpreterMode,
                    color = Color(0xFFE91E63),
                    onClick = { onCategoryClick(TipCategory.INTERVIEW) }
                )
            }

            item {
                CategoryHubCard(
                    title = stringResource(R.string.career_tips_career_change),
                    subtitle = "성공적인 이직을 위한 커리어 관리 팁",
                    icon = Icons.Default.RocketLaunch,
                    color = Color(0xFF4CAF50),
                    onClick = { onCategoryClick(TipCategory.CAREER_CHANGE) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun CategoryHubCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .clip(RoundedCornerShape(28.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Background Decoration Circle - Fixed positioning and size
            Surface(
                modifier = Modifier
                    .size(140.dp)
                    .align(Alignment.CenterEnd)
                    .offset(x = 40.dp),
                shape = CircleShape,
                color = color.copy(alpha = 0.06f)
            ) {}

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = color.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(32.dp))
                    }
                }

                Spacer(modifier = Modifier.width(20.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = color.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CareerTipsDetailScreen(
    categoryName: String,
    onBack: () -> Unit,
    viewModel: CareerTipsViewModel = hiltViewModel()
) {
    val category = try { TipCategory.valueOf(categoryName) } catch (_: Exception) { TipCategory.MARKET }
    val tips by viewModel.tips.collectAsState()
    val filteredTips = tips.filter { it.category == category }

    val expandedStates = remember { mutableStateMapOf<String, Boolean>() }

    val (title, icon, color) = when(category) {
        TipCategory.MARKET -> Triple(stringResource(R.string.career_tips_market), Icons.Default.AutoGraph, Color(0xFF2196F3))
        TipCategory.JOB_GUIDE -> Triple(stringResource(R.string.career_tips_job_guide), Icons.Default.WorkHistory, Color(0xFF673AB7))
        TipCategory.INTERVIEW -> Triple(stringResource(R.string.career_tips_interview), Icons.Default.InterpreterMode, Color(0xFFE91E63))
        TipCategory.CAREER_CHANGE -> Triple(stringResource(R.string.career_tips_career_change), Icons.Default.RocketLaunch, Color(0xFF4CAF50))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp) // Lower for stability
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = color.copy(alpha = 0.1f)
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            color.copy(alpha = 0.1f),
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    ) {
                        Surface(
                            color = color.copy(alpha = 0.15f),
                            shape = CircleShape,
                            modifier = Modifier.size(60.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(32.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(20.dp))
                        Column {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "성공적인 커리어를 위한 맞춤 가이드",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                items(filteredTips) { tip ->
                    val isExpanded = expandedStates[tip.id] ?: (filteredTips.indexOf(tip) == 0)
                    RichTipCard(
                        tip = tip,
                        accentColor = color,
                        isExpanded = isExpanded,
                        onExpandToggle = { expandedStates[tip.id] = !isExpanded }
                    )
                }

                item { Spacer(modifier = Modifier.height(40.dp)) }
            }
        }
    }
}

@Composable
fun RichTipCard(
    tip: CareerTip,
    accentColor: Color,
    isExpanded: Boolean,
    onExpandToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .clip(RoundedCornerShape(24.dp))
            .clickable { onExpandToggle() },
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            if (tip.badge != null) {
                Surface(
                    color = accentColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Text(
                        text = tip.badge,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = accentColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Text(
                text = tip.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (tip.subtitle != null) {
                Text(
                    text = tip.subtitle,
                    style = MaterialTheme.typography.bodyLarge,
                    color = accentColor,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 16.dp),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            )

            if (isExpanded) {
                RichTextBody(tip.body, accentColor)

                if (tip.source != null) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "출처 · ${tip.source}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }

                Text(
                    text = "접기 ∧",
                    style = MaterialTheme.typography.labelMedium,
                    color = accentColor.copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    textAlign = TextAlign.Center
                )
            } else {
                val previewText = remember(tip.body) {
                    tip.body.split("\n")
                        .map { it.trim() }
                        .filter { it.isNotBlank() }
                        .map { line ->
                            line.replace(Regex("^###\\s*"), "")
                                .replace(Regex("^■\\s*"), "")
                                .replace(Regex("^[-•①②③④⑤]\\s*"), "")
                                .trim()
                        }
                        .firstOrNull { it.isNotEmpty() }
                        ?: tip.body.take(100).replace("\n", " ")
                }
                Text(
                    text = previewText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 22.sp
                )
                Text(
                    text = "자세히 보기 ∨",
                    style = MaterialTheme.typography.labelMedium,
                    color = accentColor,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun RichTextBody(body: String, accentColor: Color) {
    Column {
        body.split("\n").forEach { line ->
            when {
                line.startsWith("### ") -> {
                    Text(
                        text = line.substring(4),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                    )
                }
                line.startsWith("■ ") -> {
                    Text(
                        text = line,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = accentColor,
                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                    )
                }
                line.startsWith("- ") || line.startsWith("• ") -> {
                    Row(modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)) {
                        Text(text = "• ", style = MaterialTheme.typography.bodyMedium, color = accentColor)
                        Text(
                            text = if (line.length > 2) line.substring(2) else "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                line.isEmpty() -> {
                    Spacer(modifier = Modifier.height(8.dp))
                }
                else -> {
                    Text(
                        text = line,
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 24.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }
            }
        }
    }
}
