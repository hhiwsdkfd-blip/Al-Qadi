package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.SpaceBar
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class KeyItem(
    val label: String,
    val insertText: String = label,
    val cursorOffset: Int? = null,
    val isAction: Boolean = false
)

enum class KeyboardTab(val title: String) {
    GENERAL("أساسي وأرقام"),
    CALCULUS("تفاضل وتكامل"),
    ALGEBRA("جبر ومركبة"),
    TRIGONOMETRY("مثلثات"),
    PROBABILITY("احتمالات ومجموعات"),
    TEMPLATES("صيغ وزارية جاهزة")
}

@Composable
fun AdvancedMathKeyboard(
    textFieldValue: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(true) }
    var currentTab by remember { mutableStateOf(KeyboardTab.GENERAL) }

    fun insertText(insert: String, cursorOffset: Int? = null) {
        val text = textFieldValue.text
        val selection = textFieldValue.selection
        val start = selection.min
        val end = selection.max

        val newText = text.substring(0, start) + insert + text.substring(end)
        val newCursorPos = if (cursorOffset != null) {
            start + cursorOffset
        } else if (insert.contains("()")) {
            start + insert.indexOf("()") + 1
        } else {
            start + insert.length
        }

        onValueChange(
            textFieldValue.copy(
                text = newText,
                selection = TextRange(newCursorPos.coerceIn(0, newText.length))
            )
        )
    }

    fun backspace() {
        val text = textFieldValue.text
        val selection = textFieldValue.selection
        val start = selection.min
        val end = selection.max

        if (start != end) {
            val newText = text.substring(0, start) + text.substring(end)
            onValueChange(textFieldValue.copy(text = newText, selection = TextRange(start)))
        } else if (start > 0) {
            val newText = text.substring(0, start - 1) + text.substring(start)
            onValueChange(textFieldValue.copy(text = newText, selection = TextRange(start - 1)))
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("math_keyboard_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Keyboard,
                        contentDescription = "لوحة المفاتيح الرياضية",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "لوحة الرموز الرياضية التفاعلية (الثانوية اليمنية)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "تصغير" else "توسيع"
                    )
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column {
                    // Category Selection Tabs
                    ScrollableTabRow(
                        selectedTabIndex = currentTab.ordinal,
                        edgePadding = 4.dp,
                        modifier = Modifier.fillMaxWidth().testTag("keyboard_tabs")
                    ) {
                        KeyboardTab.values().forEach { tab ->
                            Tab(
                                selected = currentTab == tab,
                                onClick = { currentTab = tab },
                                text = {
                                    Text(
                                        text = tab.title,
                                        fontSize = 12.sp,
                                        fontWeight = if (currentTab == tab) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Keys Area
                    when (currentTab) {
                        KeyboardTab.GENERAL -> GeneralKeysGrid(onInsert = ::insertText)
                        KeyboardTab.CALCULUS -> CalculusKeysGrid(onInsert = ::insertText)
                        KeyboardTab.ALGEBRA -> AlgebraKeysGrid(onInsert = ::insertText)
                        KeyboardTab.TRIGONOMETRY -> TrigKeysGrid(onInsert = ::insertText)
                        KeyboardTab.PROBABILITY -> ProbabilityKeysGrid(onInsert = ::insertText)
                        KeyboardTab.TEMPLATES -> TemplatesGrid(onInsert = ::insertText)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Quick Action Row (Space, Backspace, Variables, Equals)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf(
                            KeyItem("س", "س"),
                            KeyItem("ص", "ص"),
                            KeyItem("ع", "ع"),
                            KeyItem("ت", "ت"),
                            KeyItem("=", " = "),
                            KeyItem("+", " + "),
                            KeyItem("−", " - "),
                            KeyItem("مسافة", " ", isAction = true)
                        ).forEach { key ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (key.isAction) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { insertText(key.insertText, key.cursorOffset) }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    if (key.insertText == " ") {
                                        Icon(Icons.Default.SpaceBar, contentDescription = "مسافة", modifier = Modifier.size(16.dp))
                                    } else {
                                        Text(key.label, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        // Backspace Key
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier
                                .width(46.dp)
                                .height(38.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { backspace() }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Backspace,
                                    contentDescription = "حذف حرف",
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KeyButton(
    key: KeyItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = containerColor,
        shadowElevation = 1.dp,
        modifier = modifier
            .height(42.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 4.dp)) {
            Text(
                text = key.label,
                fontSize = if (key.label.length > 5) 11.sp else 14.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun GeneralKeysGrid(onInsert: (String, Int?) -> Unit) {
    val keys = listOf(
        KeyItem("1"), KeyItem("2"), KeyItem("3"), KeyItem("4"), KeyItem("5"),
        KeyItem("6"), KeyItem("7"), KeyItem("8"), KeyItem("9"), KeyItem("0"),
        KeyItem("+"), KeyItem("−", " - "), KeyItem("×", " * "), KeyItem("÷", " / "), KeyItem("±", " ± "),
        KeyItem("س²", "س^2"), KeyItem("س³", "س^3"), KeyItem("س^ن", "س^()"), KeyItem("√س", "√(س)"), KeyItem("∛س", "∛(س)"),
        KeyItem("كسر أ/ب", "() / ()", 1), KeyItem("( )", "()", 1), KeyItem("[ ]", "[]", 1), KeyItem("{ }", "{}", 1), KeyItem("π", "π"),
        KeyItem("∞", "∞"), KeyItem("≤", " ≤ "), KeyItem("≥", " ≥ "), KeyItem("≠", " ≠ "), KeyItem("؛", " ؛ ")
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(5),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.height(180.dp)
    ) {
        items(keys) { key ->
            KeyButton(key = key, onClick = { onInsert(key.insertText, key.cursorOffset) })
        }
    }
}

@Composable
private fun CalculusKeysGrid(onInsert: (String, Int?) -> Unit) {
    val keys = listOf(
        KeyItem("∫ ءس", "∫ () ءس", 2),
        KeyItem("∫[أ،ب]", "∫ من _ إلى _ () ءس", 7),
        KeyItem("نها(س→ )", "نها س← ()", 7),
        KeyItem("س←0", "س←0"),
        KeyItem("س←∞", "س←∞"),
        KeyItem("د'(س)", "د'(س)"),
        KeyItem("د''(س)", "د''(س)"),
        KeyItem("د⁽ⁿ⁾(س)", "د⁽ⁿ⁾(س)"),
        KeyItem("ءص/ءس", "ءص/ءس"),
        KeyItem("ء²ص/ءس²", "ء²ص/ءس²"),
        KeyItem("هـ^س", "هـ^(س)"),
        KeyItem("هـ^-س", "هـ^(-س)"),
        KeyItem("لو(س)", "لو(س)"),
        KeyItem("لو_هـ", "لو_هـ(س)"),
        KeyItem("+ ث", " + ث"),
        KeyItem("نقطة حرجة", "نقطة حرجة عند س = "),
        KeyItem("نقطة انعطاف", "نقطة انعطاف ( ، )", 15),
        KeyItem("مماس", "معادلة المماس: ص - ص₁ = م(س - س₁)"),
        KeyItem("ناظم", "معادلة الناظم: ص - ص₁ = -1/م(س - س₁)"),
        KeyItem("قيمة قصوى", "قيمة قصوى عظمى/صغرى عند س = ")
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.height(180.dp)
    ) {
        items(keys) { key ->
            KeyButton(
                key = key,
                onClick = { onInsert(key.insertText, key.cursorOffset) },
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
private fun AlgebraKeysGrid(onInsert: (String, Int?) -> Unit) {
    val keys = listOf(
        KeyItem("ت", "ت"),
        KeyItem("ت² = -1", "ت² = -1"),
        KeyItem("ت³ = -ت", "ت³ = -ت"),
        KeyItem("ت⁴ = 1", "ت⁴ = 1"),
        KeyItem("|ع| مقياس", "|ع|"),
        KeyItem("ع̄ مرافق", "ع̄"),
        KeyItem("سعة هـ", "السعة هـ = "),
        KeyItem("[ر ، هـ]", "[ر ، هـ]"),
        KeyItem("ن!", "ن!"),
        KeyItem("ن ل ر", "ن ل ر"),
        KeyItem("ن ق ر", "ن ق ر"),
        KeyItem("مفكوك", "(س + ص)^ن"),
        KeyItem("رتبة الحد الخالي", "الحد الخالي من س هو ح_"),
        KeyItem("الحد الأوسط", "الحد الأوسط في مفكوك "),
        KeyItem("القطع المكافئ", "ص² = 4 أ س"),
        KeyItem("القطع الناقص", "(س²/أ²) + (ص²/ب²) = 1"),
        KeyItem("القطع الزائد", "(س²/أ²) - (ص²/ب²) = 1"),
        KeyItem("معادلة التقارب", "المستقيمان المقاربان: ص = ± (ب/أ) س")
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.height(180.dp)
    ) {
        items(keys) { key ->
            KeyButton(
                key = key,
                onClick = { onInsert(key.insertText, key.cursorOffset) },
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
private fun TrigKeysGrid(onInsert: (String, Int?) -> Unit) {
    val keys = listOf(
        KeyItem("جا(س)", "جا(س)"),
        KeyItem("جتا(س)", "جتا(س)"),
        KeyItem("ظا(س)", "ظا(س)"),
        KeyItem("ظتا(س)", "ظتا(س)"),
        KeyItem("قا(س)", "قا(س)"),
        KeyItem("قتا(س)", "قتا(س)"),
        KeyItem("جا²(س)", "جا²(س)"),
        KeyItem("جتا²(س)", "جتا²(س)"),
        KeyItem("ظا²(س)", "ظا²(س)"),
        KeyItem("جا² + جتا² = 1", "جا²(س) + جتا²(س) = 1"),
        KeyItem("1 + ظا² = قا²", "1 + ظا²(س) = قا²(س)"),
        KeyItem("جا(2س)", "جا(2س) = 2 جا(س) جتا(س)"),
        KeyItem("جتا(2س)", "جتا(2س) = جتا²(س) - جا²(س)"),
        KeyItem("π/2 (90°)", "π/2"),
        KeyItem("π (180°)", "π"),
        KeyItem("π/4 (45°)", "π/4")
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.height(180.dp)
    ) {
        items(keys) { key ->
            KeyButton(
                key = key,
                onClick = { onInsert(key.insertText, key.cursorOffset) },
                containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
private fun ProbabilityKeysGrid(onInsert: (String, Int?) -> Unit) {
    val keys = listOf(
        KeyItem("ل(أ)", "ل(أ)"),
        KeyItem("ل(ب)", "ل(ب)"),
        KeyItem("ل(أ ∩ ب)", "ل(أ ∩ ب)"),
        KeyItem("ل(أ ∪ ب)", "ل(أ ∪ ب)"),
        KeyItem("ل(أ|ب) شرطي", "ل(أ|ب)"),
        KeyItem("ل(أ̄) متمم", "ل(أ̄) = 1 - ل(أ)"),
        KeyItem("مستقلتان", "حادثتان مستقلتان: ل(أ ∩ ب) = ل(أ) × ل(ب)"),
        KeyItem("متنافيتان", "حادثتان متنافيتان: ل(أ ∩ ب) = 0"),
        KeyItem("فضاء العينة ف", "ف = {}"),
        KeyItem("حجر نرد", "رمي حجر نرد منتظم"),
        KeyItem("قطعة نقود", "رمي قطعة نقود (صورة، كتابة)"),
        KeyItem("كرات وصندوق", "سحب عشوائي مع الإرجاع / دون إرجاع")
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.height(180.dp)
    ) {
        items(keys) { key ->
            KeyButton(key = key, onClick = { onInsert(key.insertText, key.cursorOffset) })
        }
    }
}

@Composable
private fun TemplatesGrid(onInsert: (String, Int?) -> Unit) {
    val templates = listOf(
        KeyItem("إذا كانت د(س) = ... فإن د'(س) =", "إذا كانت د(س) = \nفإن المشتقة د'(س) = "),
        KeyItem("احسب قيمة النهاية: نها...", "احسب قيمة النهاية:\nنها (س ← 0) [ ]", 25),
        KeyItem("تكامل محدد من أ إلى ب", "احسب التكامل المحدد:\n∫ من 0 إلى 1 () ءس", 28),
        KeyItem("إذا كان ع = ... أوجد المقياس والسعة", "إذا كان العدد المركب ع = \nأوجد مقياسه وسعته والصورة القطبية."),
        KeyItem("في مفكوك (س + ص)^ن أوجد الحد الخالي", "في مفكوك (س + 1/س)^10 أوجد رتبة وقيمة الحد الخالي من س."),
        KeyItem("معادلة المماس والناظم عند النقطة", "أوجد معادلة مماس وناظم المنحنى ص = عند النقطة ( ، )."),
        KeyItem("أوجد معادلة القطع المخروطي", "أوجد معادلة القطع (المكافئ/الناقص/الزائد) الذي بؤرتاه ( ، ) ورأساه ( ، ).")
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.height(180.dp)
    ) {
        templates.forEach { t ->
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onInsert(t.insertText, t.cursorOffset) }
            ) {
                Box(contentAlignment = Alignment.CenterStart, modifier = Modifier.padding(horizontal = 8.dp)) {
                    Text(t.label, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                }
            }
        }
    }
}
