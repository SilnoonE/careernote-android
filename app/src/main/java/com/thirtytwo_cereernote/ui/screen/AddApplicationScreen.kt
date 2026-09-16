package com.thirtytwo_cereernote.ui.screen

import android.app.DatePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.thirtytwo_cereernote.R
import com.thirtytwo_cereernote.data.model.*
import com.thirtytwo_cereernote.viewmodel.ApplicationsViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddApplicationScreen(
    onBack: () -> Unit,
    viewModel: ApplicationsViewModel = hiltViewModel()
) {
    val drafts by viewModel.drafts.collectAsState()
    val currentDraft = drafts["application_0"]

    var companyName by remember { mutableStateOf("") }
    var jobTitle by remember { mutableStateOf("") }
    var status by remember { mutableStateOf(ApplicationStatus.INTERESTED) }
    var employmentType by remember { mutableStateOf(EmploymentType.FULL_TIME) }
    var deadlineDate by remember { mutableStateOf<Date?>(null) }
    
    var isInitialized by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val scope = rememberCoroutineScope()

    // Load Draft
    LaunchedEffect(currentDraft) {
        if (!isInitialized && currentDraft != null) {
            companyName = currentDraft.field1
            jobTitle = currentDraft.field2
            try { status = ApplicationStatus.valueOf(currentDraft.field3) } catch (_: Exception) {}
            if (currentDraft.field4.isNotBlank()) {
                try { deadlineDate = dateFormat.parse(currentDraft.field4) } catch (_: Exception) {}
            }
            isInitialized = true
        } else if (!isInitialized) {
            isInitialized = true
        }
    }

    // Save Draft
    LaunchedEffect(companyName, jobTitle, status, deadlineDate) {
        if (isInitialized) {
            viewModel.saveDraft(
                companyName, 
                jobTitle, 
                status.name, 
                deadlineDate?.let { dateFormat.format(it) } ?: ""
            )
        }
    }

    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val cal = Calendar.getInstance()
            cal.set(year, month, dayOfMonth, 0, 0, 0)
            cal.set(Calendar.MILLISECOND, 0)
            deadlineDate = cal.time
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_add_application)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            if (currentDraft != null) {
                Text(stringResource(R.string.msg_draft_loaded), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
            }

            Text(stringResource(R.string.label_basic_info), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            
            OutlinedTextField(
                value = companyName,
                onValueChange = { companyName = it },
                label = { Text(stringResource(R.string.label_company_name)) },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = jobTitle,
                onValueChange = { jobTitle = it },
                label = { Text(stringResource(R.string.label_job_title)) },
                modifier = Modifier.fillMaxWidth()
            )

            HorizontalDivider()
            Text(stringResource(R.string.label_status_schedule), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            var statusExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = statusExpanded,
                onExpandedChange = { statusExpanded = !statusExpanded }
            ) {
                OutlinedTextField(
                    value = status.displayName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.label_status)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusExpanded) },
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = statusExpanded,
                    onDismissRequest = { statusExpanded = false }
                ) {
                    ApplicationStatus.entries.forEach { s ->
                        DropdownMenuItem(
                            text = { Text(s.displayName) },
                            onClick = {
                                status = s
                                statusExpanded = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = deadlineDate?.let { dateFormat.format(it) } ?: "",
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.label_deadline)) },
                trailingIcon = {
                    IconButton(onClick = { datePickerDialog.show() }) {
                        Icon(Icons.Default.CalendarToday, contentDescription = null)
                    }
                },
                modifier = Modifier.fillMaxWidth().clickable { datePickerDialog.show() }
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (companyName.isNotBlank() && jobTitle.isNotBlank()) {
                        scope.launch {
                            viewModel.addApplication(
                                Application(
                                    companyName = companyName,
                                    jobTitle = jobTitle,
                                    currentStatus = status,
                                    deadlineDate = deadlineDate,
                                    employmentType = employmentType
                                )
                            )
                            onBack()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = companyName.isNotBlank() && jobTitle.isNotBlank()
            ) {
                Text(stringResource(R.string.btn_save_record), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}
