package com.thirtytwo_cereernote.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.thirtytwo_cereernote.R
import com.thirtytwo_cereernote.data.model.ApplicationStatus
import com.thirtytwo_cereernote.ui.component.ApplicationCard
import com.thirtytwo_cereernote.viewmodel.ApplicationsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApplicationsScreen(
    onAddClick: () -> Unit,
    onItemClick: (Long) -> Unit,
    viewModel: ApplicationsViewModel = hiltViewModel()
) {
    val applications by viewModel.applications.collectAsState()
    val isInitialEmpty by viewModel.isInitialEmpty.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val statusFilter by viewModel.statusFilter.collectAsState()
    val favoriteFilter by viewModel.favoriteFilter.collectAsState()
    val sortBy by viewModel.sortBy.collectAsState()

    var showSortMenu by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface).padding(top = 8.dp)) {
                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.searchQuery.value = it },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    placeholder = { Text("회사명, 직무, 메모 검색") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = null)
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                // Filters Row
                LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    item {
                        FilterChip(
                            selected = favoriteFilter,
                            onClick = { viewModel.favoriteFilter.value = !favoriteFilter },
                            label = { Text("관심 기업") },
                            leadingIcon = if (favoriteFilter) {
                                { Icon(Icons.Default.FilterList, contentDescription = null, modifier = Modifier.size(18.dp)) }
                            } else null
                        )
                    }
                    
                    items(ApplicationStatus.entries) { status ->
                        FilterChip(
                            selected = statusFilter == status,
                            onClick = {
                                viewModel.statusFilter.value = if (statusFilter == status) null else status
                            },
                            label = { Text(status.displayName) }
                        )
                    }
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when(sortBy) {
                            "appliedDate" -> "지원일순"
                            "deadlineDate" -> "마감일순"
                            "updatedAt" -> "수정일순"
                            else -> "정렬"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.clickable { showSortMenu = true }
                    )
                    IconButton(onClick = { showSortMenu = true }) {
                        Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "정렬", modifier = Modifier.size(20.dp))
                    }
                    DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                        DropdownMenuItem(text = { Text("지원일순") }, onClick = { viewModel.sortBy.value = "appliedDate"; showSortMenu = false })
                        DropdownMenuItem(text = { Text("마감일순") }, onClick = { viewModel.sortBy.value = "deadlineDate"; showSortMenu = false })
                        DropdownMenuItem(text = { Text("수정일순") }, onClick = { viewModel.sortBy.value = "updatedAt"; showSortMenu = false })
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(top = 4.dp), thickness = 0.5.dp)
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick) {
                Icon(Icons.Default.Add, contentDescription = stringResource(id = R.string.add_application))
            }
        }
    ) { innerPadding ->
        if (isInitialEmpty) {
            Box(
                modifier = Modifier.padding(innerPadding).fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.AutoMirrored.Filled.EventNote, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "등록된 지원 기록이 없습니다.", style = MaterialTheme.typography.bodyLarge)
                    TextButton(onClick = onAddClick) { Text("첫 기록 시작하기") }
                }
            }
        } else if (applications.isEmpty()) {
            Box(
                modifier = Modifier.padding(innerPadding).fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "검색 결과가 없습니다.", color = MaterialTheme.colorScheme.outline)
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(innerPadding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(applications, key = { it.id }) { application ->
                    ApplicationCard(
                        application = application,
                        onClick = { onItemClick(application.id) }
                    )
                }
            }
        }
    }
}

