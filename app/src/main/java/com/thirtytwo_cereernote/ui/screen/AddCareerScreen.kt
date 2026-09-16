package com.thirtytwo_cereernote.ui.screen

import android.app.DatePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.thirtytwo_cereernote.R
import com.thirtytwo_cereernote.viewmodel.CareerViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCareerScreen(
    id: String,
    itemId: Long = 0L,
    onBack: () -> Unit,
    viewModel: CareerViewModel = hiltViewModel()
) {
    val drafts by viewModel.drafts.collectAsState()
    val draftKey = "${id}_$itemId"
    val currentDraft = drafts[draftKey]

    var field1 by rememberSaveable { mutableStateOf("") }
    var field2 by rememberSaveable { mutableStateOf("") }
    var field3 by rememberSaveable { mutableStateOf("") }
    var field4 by rememberSaveable { mutableStateOf("") }
    var field5 by rememberSaveable { mutableStateOf("") }
    var field6 by rememberSaveable { mutableStateOf("") }
    var field7 by rememberSaveable { mutableStateOf("") }
    var field8 by rememberSaveable { mutableStateOf("") }
    var field9 by rememberSaveable { mutableStateOf("") }
    
    var field1Error by remember { mutableStateOf(false) }
    var field2Error by remember { mutableStateOf(false) }
    var field3Error by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(itemId > 0) }
    var isInitialized by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    // Load existing data if editing
    LaunchedEffect(itemId) {
        if (itemId > 0) {
            isLoading = true
            when (id) {
                "cover_letter" -> viewModel.getCoverLetterById(itemId)?.let { field1 = it.title; field2 = it.companyName; field3 = it.memo }
                "resume" -> viewModel.getResumeById(itemId)?.let { field1 = it.title; field2 = it.version; field3 = it.memo }
                "portfolio" -> viewModel.getPortfolioById(itemId)?.let { field1 = it.title; field2 = it.url; field3 = it.memo }
                "project" -> viewModel.getProjectById(itemId)?.let { field1 = it.name; field2 = it.role; field3 = it.techStack; field4 = it.description; field5 = it.problem; field6 = it.outcome }
                "certification" -> viewModel.getCertificationById(itemId)?.let { field1 = it.name; field2 = it.issuer; field3 = it.score; field4 = it.grade; field5 = it.acquisitionDate?.time?.toString() ?: "" }
                "interview_question" -> viewModel.getInterviewQuestionById(itemId)?.let { field1 = it.category; field2 = it.question; field3 = it.myAnswer; field4 = it.betterAnswer }
                "experience" -> viewModel.getExperienceById(itemId)?.let { field1 = it.companyName; field2 = it.jobTitle; field3 = it.description; field4 = it.outcome; field5 = it.startDate.time.toString(); field6 = it.endDate?.time?.toString() ?: ""; field7 = it.isCurrent.toString() }
                "education" -> viewModel.getEducationById(itemId)?.let { field1 = it.name; field2 = it.institution; field3 = it.status; field4 = it.description; field5 = it.startDate.time.toString(); field6 = it.endDate?.time?.toString() ?: "" }
            }
            isLoading = false
        }
        isInitialized = true
    }

    // Load draft
    LaunchedEffect(isInitialized, currentDraft) {
        if (isInitialized && currentDraft != null) {
            if (field1.isEmpty() && field2.isEmpty() && field3.isEmpty() && field4.isEmpty()) {
                field1 = currentDraft.field1
                field2 = currentDraft.field2
                field3 = currentDraft.field3
                field4 = currentDraft.field4
                field5 = currentDraft.field5
                field6 = currentDraft.field6
                field7 = currentDraft.field7
                field8 = currentDraft.field8
                field9 = currentDraft.field9
            }
        }
    }

    // Auto save draft
    LaunchedEffect(field1, field2, field3, field4, field5, field6, field7, field8, field9) {
        if (isInitialized) {
            viewModel.saveDraft(id, itemId, field1, field2, field3, field4, field5, field6, field7, field8, field9)
        }
    }

    val isFormValid = when(id) {
        "cover_letter" -> field1.isNotBlank() && field2.isNotBlank() && field3.isNotBlank()
        "interview_question" -> field2.isNotBlank() && field3.isNotBlank()
        "experience" -> field1.isNotBlank() && field2.isNotBlank()
        "education" -> field1.isNotBlank() && field2.isNotBlank()
        else -> field1.isNotBlank()
    }

    fun validate(): Boolean {
        field1Error = field1.isBlank()
        field2Error = when(id) {
            "cover_letter", "experience", "education" -> field2.isBlank()
            "interview_question" -> field2.isBlank()
            else -> false
        }
        field3Error = when(id) {
            "cover_letter", "interview_question" -> field3.isBlank()
            else -> false
        }
        return !field1Error && !field2Error && !field3Error
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (itemId > 0) R.string.title_edit_record else R.string.title_new_record)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { innerPadding ->
        if (isLoading) {
            Box(modifier = Modifier.padding(innerPadding).fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
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
                if (currentDraft != null && itemId == 0L) {
                    Text(text = stringResource(R.string.msg_draft_loaded), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                }
                
                when (id) {
                    "cover_letter" -> {
                        OutlinedTextField(value = field1, onValueChange = { field1 = it; field1Error = false }, label = { Text(stringResource(R.string.label_cl_title)) }, modifier = Modifier.fillMaxWidth(), isError = field1Error)
                        OutlinedTextField(value = field2, onValueChange = { field2 = it; field2Error = false }, label = { Text(stringResource(R.string.label_cl_company)) }, modifier = Modifier.fillMaxWidth(), isError = field2Error)
                        OutlinedTextField(value = field3, onValueChange = { field3 = it; field3Error = false }, label = { Text(stringResource(R.string.label_cl_content)) }, modifier = Modifier.fillMaxWidth(), minLines = 8, isError = field3Error)
                    }
                    "resume" -> {
                        OutlinedTextField(value = field1, onValueChange = { field1 = it; field1Error = false }, label = { Text(stringResource(R.string.label_rs_title)) }, modifier = Modifier.fillMaxWidth(), isError = field1Error)
                        OutlinedTextField(value = field2, onValueChange = { field2 = it }, label = { Text(stringResource(R.string.label_rs_version)) }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = field3, onValueChange = { field3 = it }, label = { Text(stringResource(R.string.label_rs_memo)) }, modifier = Modifier.fillMaxWidth(), minLines = 3)
                    }
                    "portfolio" -> {
                        OutlinedTextField(value = field1, onValueChange = { field1 = it; field1Error = false }, label = { Text(stringResource(R.string.label_pt_title)) }, modifier = Modifier.fillMaxWidth(), isError = field1Error)
                        OutlinedTextField(value = field2, onValueChange = { field2 = it }, label = { Text(stringResource(R.string.label_pt_url)) }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = field3, onValueChange = { field3 = it }, label = { Text(stringResource(R.string.label_rs_memo)) }, modifier = Modifier.fillMaxWidth(), minLines = 3)
                    }
                    "project" -> {
                        OutlinedTextField(value = field1, onValueChange = { field1 = it; field1Error = false }, label = { Text(stringResource(R.string.label_pj_name)) }, modifier = Modifier.fillMaxWidth(), isError = field1Error)
                        OutlinedTextField(value = field2, onValueChange = { field2 = it }, label = { Text(stringResource(R.string.label_pj_role)) }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = field3, onValueChange = { field3 = it }, label = { Text(stringResource(R.string.label_pj_tech)) }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = field4, onValueChange = { field4 = it }, label = { Text(stringResource(R.string.label_pj_action)) }, modifier = Modifier.fillMaxWidth(), minLines = 4)
                        OutlinedTextField(value = field5, onValueChange = { field5 = it }, label = { Text(stringResource(R.string.label_pj_situation)) }, modifier = Modifier.fillMaxWidth(), minLines = 3)
                        OutlinedTextField(value = field6, onValueChange = { field6 = it }, label = { Text(stringResource(R.string.label_pj_result)) }, modifier = Modifier.fillMaxWidth(), minLines = 3)
                    }
                    "certification" -> {
                        OutlinedTextField(value = field1, onValueChange = { field1 = it; field1Error = false }, label = { Text(stringResource(R.string.label_cert_name)) }, modifier = Modifier.fillMaxWidth(), isError = field1Error)
                        OutlinedTextField(value = field2, onValueChange = { field2 = it }, label = { Text(stringResource(R.string.label_cert_issuer)) }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = field3, onValueChange = { field3 = it }, label = { Text(stringResource(R.string.label_cert_score)) }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = field4, onValueChange = { field4 = it }, label = { Text(stringResource(R.string.label_cert_grade)) }, modifier = Modifier.fillMaxWidth())
                        
                        DatePickerField(label = stringResource(R.string.label_acquired_date), value = field5, onValueChange = { field5 = it }, dateFormat = dateFormat)
                    }
                    "interview_question" -> {
                        OutlinedTextField(value = field1, onValueChange = { field1 = it }, label = { Text(stringResource(R.string.label_iq_category)) }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = field2, onValueChange = { field2 = it; field2Error = false }, label = { Text(stringResource(R.string.label_iq_question)) }, modifier = Modifier.fillMaxWidth(), isError = field2Error)
                        OutlinedTextField(value = field3, onValueChange = { field3 = it; field3Error = false }, label = { Text(stringResource(R.string.label_iq_answer)) }, modifier = Modifier.fillMaxWidth(), minLines = 5, isError = field3Error)
                        OutlinedTextField(value = field4, onValueChange = { field4 = it }, label = { Text(stringResource(R.string.label_iq_better)) }, modifier = Modifier.fillMaxWidth(), minLines = 5)
                    }
                    "experience" -> {
                        OutlinedTextField(value = field1, onValueChange = { field1 = it; field1Error = false }, label = { Text(stringResource(R.string.label_exp_company)) }, modifier = Modifier.fillMaxWidth(), isError = field1Error)
                        OutlinedTextField(value = field2, onValueChange = { field2 = it; field2Error = false }, label = { Text(stringResource(R.string.label_exp_job)) }, modifier = Modifier.fillMaxWidth(), isError = field2Error)
                        
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = field7 == "true", onCheckedChange = { field7 = it.toString() })
                            Text(stringResource(R.string.label_currently_working))
                        }

                        DatePickerField(label = stringResource(R.string.label_start_date), value = field5, onValueChange = { field5 = it }, dateFormat = dateFormat)
                        if (field7 != "true") {
                            DatePickerField(label = stringResource(R.string.label_end_date), value = field6, onValueChange = { field6 = it }, dateFormat = dateFormat)
                        }

                        OutlinedTextField(value = field3, onValueChange = { field3 = it }, label = { Text(stringResource(R.string.label_exp_work)) }, modifier = Modifier.fillMaxWidth(), minLines = 5)
                        OutlinedTextField(value = field4, onValueChange = { field4 = it }, label = { Text(stringResource(R.string.label_exp_outcome)) }, modifier = Modifier.fillMaxWidth(), minLines = 3)
                    }
                    "education" -> {
                        OutlinedTextField(value = field1, onValueChange = { field1 = it; field1Error = false }, label = { Text(stringResource(R.string.label_edu_name)) }, modifier = Modifier.fillMaxWidth(), isError = field1Error)
                        OutlinedTextField(value = field2, onValueChange = { field2 = it; field2Error = false }, label = { Text(stringResource(R.string.label_edu_inst)) }, modifier = Modifier.fillMaxWidth(), isError = field2Error)
                        
                        DatePickerField(label = stringResource(R.string.label_start_date), value = field5, onValueChange = { field5 = it }, dateFormat = dateFormat)
                        DatePickerField(label = stringResource(R.string.label_end_date), value = field6, onValueChange = { field6 = it }, dateFormat = dateFormat)

                        OutlinedTextField(value = field3, onValueChange = { field3 = it }, label = { Text(stringResource(R.string.label_edu_status)) }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = field4, onValueChange = { field4 = it }, label = { Text(stringResource(R.string.label_edu_desc)) }, modifier = Modifier.fillMaxWidth(), minLines = 3)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        if (validate()) {
                            coroutineScope.launch {
                                val success = when (id) {
                                    "cover_letter" -> {
                                        if (itemId > 0) {
                                            viewModel.getCoverLetterById(itemId)?.let { viewModel.updateCoverLetter(it.copy(title = field1, companyName = field2, memo = field3)) } ?: false
                                        } else {
                                            viewModel.saveCoverLetter(field1, field2, field3) > 0
                                        }
                                    }
                                    "resume" -> {
                                        if (itemId > 0) {
                                            viewModel.getResumeById(itemId)?.let { viewModel.updateResume(it.copy(title = field1, version = field2, memo = field3)) } ?: false
                                        } else {
                                            viewModel.saveResume(field1, field2, field3) > 0
                                        }
                                    }
                                    "portfolio" -> {
                                        if (itemId > 0) {
                                            viewModel.getPortfolioById(itemId)?.let { viewModel.updatePortfolio(it.copy(title = field1, url = field2, memo = field3)) } ?: false
                                        } else {
                                            viewModel.savePortfolio(field1, field2, field3) > 0
                                        }
                                    }
                                    "project" -> {
                                        if (itemId > 0) {
                                            viewModel.getProjectById(itemId)?.let { viewModel.updateProject(it.copy(name = field1, role = field2, techStack = field3, description = field4, problem = field5, outcome = field6)) } ?: false
                                        } else {
                                            viewModel.saveProject(field1, field2, field3, field4, field5, field6) > 0
                                        }
                                    }
                                    "certification" -> {
                                        val acquiredDate = field5.toLongOrNull()?.let { Date(it) }
                                        if (itemId > 0) {
                                            viewModel.getCertificationById(itemId)?.let { viewModel.updateCertification(it.copy(name = field1, issuer = field2, score = field3, grade = field4, acquisitionDate = acquiredDate)) } ?: false
                                        } else {
                                            viewModel.saveCertification(field1, field2, field3, field4, acquiredDate) > 0
                                        }
                                    }
                                    "interview_question" -> {
                                        if (itemId > 0) {
                                            viewModel.getInterviewQuestionById(itemId)?.let { viewModel.updateInterviewQuestion(it.copy(category = field1, question = field2, myAnswer = field3, betterAnswer = field4)) } ?: false
                                        } else {
                                            viewModel.saveInterviewQuestion(field1, field2, field3, field4) > 0
                                        }
                                    }
                                    "experience" -> {
                                        val start = field5.toLongOrNull()?.let { Date(it) } ?: Date()
                                        val end = field6.toLongOrNull()?.let { Date(it) }
                                        val current = field7 == "true"
                                        if (itemId > 0) {
                                            viewModel.getExperienceById(itemId)?.let { viewModel.updateExperience(it.copy(companyName = field1, jobTitle = field2, description = field3, outcome = field4, startDate = start, endDate = if (current) null else end, isCurrent = current)) } ?: false
                                        } else {
                                            viewModel.saveExperience(field1, field2, field3, field4, start, end, current) > 0
                                        }
                                    }
                                    "education" -> {
                                        val start = field5.toLongOrNull()?.let { Date(it) } ?: Date()
                                        val end = field6.toLongOrNull()?.let { Date(it) }
                                        if (itemId > 0) {
                                            viewModel.getEducationById(itemId)?.let { viewModel.updateEducation(it.copy(name = field1, institution = field2, status = field3, description = field4, startDate = start, endDate = end)) } ?: false
                                        } else {
                                            viewModel.saveEducation(field1, field2, field3, field4, start, end) > 0
                                        }
                                    }
                                    else -> false
                                }
                                if (success) {
                                    viewModel.removeDraft(id, itemId)
                                    onBack()
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                    enabled = isFormValid
                ) {
                    Text(stringResource(if (itemId > 0) R.string.btn_save_complete else R.string.btn_save))
                }
            }
        }
    }
}

@Composable
fun DatePickerField(label: String, value: String, onValueChange: (String) -> Unit, dateFormat: SimpleDateFormat) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    value.toLongOrNull()?.let { calendar.timeInMillis = it }
    
    val datePicker = DatePickerDialog(
        context,
        { _, y, m, d ->
            val cal = Calendar.getInstance()
            cal.set(y, m, d)
            onValueChange(cal.timeInMillis.toString())
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    OutlinedTextField(
        value = value.toLongOrNull()?.let { dateFormat.format(Date(it)) } ?: "",
        onValueChange = {},
        readOnly = true,
        label = { Text(label) },
        trailingIcon = {
            IconButton(onClick = { datePicker.show() }) {
                Icon(Icons.Default.CalendarToday, contentDescription = null)
            }
        },
        modifier = Modifier.fillMaxWidth().clickable { datePicker.show() }
    )
}
