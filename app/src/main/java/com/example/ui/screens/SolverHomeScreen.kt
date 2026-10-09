package com.example.ui.screens

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.MathSolution
import com.example.ui.components.AdvancedMathKeyboard
import com.example.ui.components.MathSolutionWebView
import com.example.viewmodel.MathViewModel
import com.example.viewmodel.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SolverHomeScreen(
    viewModel: MathViewModel,
    uiState: UiState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var textInput by remember { mutableStateOf(TextFieldValue(uiState.rawQuestion)) }

    // Sync state
    LaunchedEffect(uiState.rawQuestion) {
        if (textInput.text != uiState.rawQuestion) {
            textInput = TextFieldValue(uiState.rawQuestion)
        }
    }

    // Photo picker launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val bitmap: Bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, it))
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, it)
                }
                viewModel.setImageBitmap(bitmap)
            } catch (e: Exception) {
                // handle error
            }
        }
    }

    val sampleQuestions = listOf(
        "إذا كان ع = 1 + جذر(3) ت ، أوجد المقياس والسعة والصورة المثلثية للعدد ع",
        "أوجد الحد الخالي من س في مفكوك (س + 1/س)^10",
        "احسب نهاية: نها(س→0) [جا(3س) / س]",
        "إذا كان ن ل 2 = 30 ، أوجد قيمة ن",
        "أوجد بؤرة ودليل ومعادلة القطع المكافئ الذي رأسه نقطة الأصل ويمر بالنقطة (2، 4)"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "المساعد الرياضي الشامل",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        ) {
                            Text(
                                "3 ث علمي",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                },
                actions = {
                    // Daily limit badge
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            text = "المتبقي اليوم: ${uiState.remainingToday}/${uiState.dailyLimit}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Sample questions quick chips
            Text(
                "مسائل نموذجية سريعة للتدريب:",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                sampleQuestions.forEach { sample ->
                    SuggestionChip(
                        onClick = {
                            textInput = TextFieldValue(sample)
                            viewModel.updateQuestionText(sample)
                        },
                        label = { Text(sample.take(28) + "...", fontSize = 12.sp) },
                        modifier = Modifier.padding(end = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Main Input Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // Type selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("نوع السؤال:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Row {
                            listOf("auto" to "تلقائي", "mcq" to "اختيار", "true_false" to "صح/خطأ", "open" to "مقالي").forEach { (code, label) ->
                                FilterChip(
                                    selected = uiState.questionType == code,
                                    onClick = { viewModel.updateQuestionType(code) },
                                    label = { Text(label, fontSize = 11.sp) },
                                    modifier = Modifier.padding(horizontal = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Multiline Text Field
                    OutlinedTextField(
                        value = textInput,
                        onValueChange = {
                            if (it.text.length <= 1500) {
                                textInput = it
                                viewModel.updateQuestionText(it.text)
                            }
                        },
                        label = { Text("اكتب مسألتك الرياضية هنا أو اختر الرموز من اللوحة...") },
                        placeholder = { Text("مثال: إذا كان ع = 1 + ت ، احسب ع^8 باستخدام نظرية ديموافر") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp, max = 240.dp)
                            .testTag("question_input_field"),
                        trailingIcon = {
                            if (textInput.text.isNotEmpty()) {
                                IconButton(
                                    onClick = {
                                        textInput = TextFieldValue("")
                                        viewModel.updateQuestionText("")
                                    },
                                    modifier = Modifier.testTag("clear_text_button")
                                ) {
                                    Icon(Icons.Default.Clear, contentDescription = "مسح")
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Char counter & Camera/Image OCR trigger
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            FilledTonalIconButton(
                                onClick = { photoPickerLauncher.launch("image/*") },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("ocr_upload_button")
                            ) {
                                Icon(Icons.Default.PhotoCamera, contentDescription = "رفع صورة للمسألة OCR", modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            if (uiState.selectedImageBase64 != null) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text("تم إرفاق صورة ✓", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "حذف الصورة",
                                            modifier = Modifier
                                                .size(14.dp)
                                                .clickable { viewModel.clearImage() }
                                        )
                                    }
                                }
                            }
                        }

                        Text(
                            "${textInput.text.length} / 1500 حرف",
                            fontSize = 11.sp,
                            color = if (textInput.text.length > 1400) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Comprehensive Math Keyboard & Symbol Input
                    AdvancedMathKeyboard(
                        textFieldValue = textInput,
                        onValueChange = {
                            textInput = it
                            viewModel.updateQuestionText(it.text)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Solve Button
            Button(
                onClick = { viewModel.solveCurrentQuestion() },
                enabled = !uiState.isSolving && (textInput.text.isNotBlank() || uiState.selectedImageBase64 != null),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("solve_button")
            ) {
                if (uiState.isSolving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.5.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(uiState.progressStage.ifBlank { "جاري الحل..." }, fontSize = 15.sp)
                } else {
                    Icon(Icons.Default.Calculate, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("حــــل الـمـسـألـة خـطـوة بـخـطـوة", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Error display
            AnimatedVisibility(visible = uiState.errorMessage != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            uiState.errorMessage ?: "",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // Results Section
            uiState.currentSolution?.let { solution ->
                Spacer(modifier = Modifier.height(20.dp))
                SolutionResultCard(
                    solution = solution,
                    onEditQuestion = {
                        // User wants to edit restated question
                        textInput = TextFieldValue(solution.restatedQuestion)
                        viewModel.updateQuestionText(solution.restatedQuestion)
                    },
                    onShare = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "حل مسألة: ${solution.topic}")
                            putExtra(Intent.EXTRA_TEXT, "سؤال: ${solution.restatedQuestion}\n\nالإجابة النهائية: ${solution.finalAnswerText}\n\nتم الحل بواسطة المساعد الرياضي الشامل")
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "مشاركة الحل"))
                    },
                    onCopy = {
                        clipboardManager.setText(AnnotatedString("${solution.restatedQuestion}\n\n${solution.finalAnswerText}\n${solution.finalAnswer}"))
                    }
                )
            }
        }
    }
}

@Composable
fun SolutionResultCard(
    solution: MathSolution,
    onEditQuestion: () -> Unit,
    onShare: () -> Unit,
    onCopy: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth().testTag("solution_result_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Topic & Verification Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "المحور: ${solution.topic}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                // Confidence badge with color
                val confColor = when {
                    solution.confidence >= 85 -> Color(0xFF10B981)
                    solution.confidence >= 70 -> Color(0xFFF59E0B)
                    else -> Color(0xFFEF4444)
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = confColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "دقة الحل: ${solution.confidence}%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = confColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            if (!solution.verificationBadge.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Verified,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            solution.verificationBadge,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Restated question box
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("السؤال بعد الفهم والصياغة:", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                        Text(solution.restatedQuestion, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                    TextButton(onClick = onEditQuestion) {
                        Text("عدّل سؤالي", fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // KaTeX Interactive HTML WebView Rendering
            MathSolutionWebView(solution = solution)

            Spacer(modifier = Modifier.height(14.dp))

            // Action buttons row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                OutlinedButton(onClick = onCopy, shape = RoundedCornerShape(8.dp)) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("نسخ", fontSize = 12.sp)
                }
                OutlinedButton(onClick = onShare, shape = RoundedCornerShape(8.dp)) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("مشاركة", fontSize = 12.sp)
                }
            }
        }
    }
}
