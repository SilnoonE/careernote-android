package com.thirtytwo_cereernote.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import com.thirtytwo_cereernote.viewmodel.CareerViewModel
import com.thirtytwo_cereernote.viewmodel.RecentActivity
import java.text.SimpleDateFormat
import java.util.Locale

private data class CareerMenuItem(val titleResId: Int, val id: String, val icon: ImageVector)

@Composable
fun CareerScreen(
    onMenuItemClick: (String, Int) -> Unit,
    onRecentItemClick: (String, Long) -> Unit,
    viewModel: CareerViewModel = hiltViewModel()
) {
    val summary by viewModel.careerSummary.collectAsState()
    val recentActivities by viewModel.recentActivities.collectAsState()
    val dateFormat = SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.getDefault())

    val menuItems = listOf(
        CareerMenuItem(R.string.career_cover_letter, "cover_letter", Icons.Default.Description),
        CareerMenuItem(R.string.career_resume, "resume", Icons.Default.Assignment),
        CareerMenuItem(R.string.career_portfolio, "portfolio", Icons.Default.Language),
        CareerMenuItem(R.string.career_project, "project", Icons.Default.Code),
        CareerMenuItem(R.string.career_certification, "certification", Icons.Default.Verified),
        CareerMenuItem(R.string.career_interview_question, "interview_question", Icons.Default.QuestionAnswer),
        CareerMenuItem(R.string.career_experience, "experience", Icons.Default.Work),
        CareerMenuItem(R.string.career_education, "education", Icons.Default.School)
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 메뉴 그리드 (2열)
        items(menuItems) { item ->
            val count = summary.counts[item.id] ?: 0
            CareerMenuCard(
                item = item,
                count = count,
                onClick = { onMenuItemClick(item.id, item.titleResId) }
            )
        }

        if (recentActivities.isNotEmpty()) {
            item(span = { GridItemSpan(2) }) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "최근 기록",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            items(recentActivities, span = { GridItemSpan(2) }) { activity ->
                RecentActivityItem(
                    activity = activity,
                    formattedDate = dateFormat.format(activity.date),
                    onClick = { onRecentItemClick(activity.categoryId, activity.id) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CareerMenuCard(
    item: CareerMenuItem,
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .height(110.dp)
            .padding(bottom = 2.dp), // Extra room for shadow
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            BadgedBox(
                badge = {
                    if (count > 0) {
                        Badge(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ) {
                            Text(count.toString())
                        }
                    }
                }
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(item.titleResId),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun RecentActivityItem(
    activity: RecentActivity,
    formattedDate: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp) // Room for shadow
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = activity.typeName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = activity.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
