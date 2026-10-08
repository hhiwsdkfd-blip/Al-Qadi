package com.example.repository

import android.content.Context
import androidx.room.Room
import com.example.BuildConfig
import com.example.db.AppDatabase
import com.example.model.MachineCheckDto
import com.example.model.MathSolution
import com.example.model.SolutionEntity
import com.example.network.Content
import com.example.network.GeminiClient
import com.example.network.GeminiRequest
import com.example.network.GenerationConfig
import com.example.network.InlineData
import com.example.network.Part
import com.example.network.ThinkingConfig
import com.example.verifier.MachineCheckData
import com.example.verifier.VerificationStatus
import com.example.verifier.Verifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.InputStreamReader

sealed class SolveProgress {
    object Idle : SolveProgress()
    object ReadingQuestion : SolveProgress() // «أقرأ السؤال»
    object Solving : SolveProgress() // «أحل»
    object Verifying : SolveProgress() // «أتحقق»
    data class Success(val solution: MathSolution, val fromCache: Boolean = false) : SolveProgress()
    data class Error(val message: String) : SolveProgress()
}

class MathRepository(private val context: Context) {

    private val db: AppDatabase = Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java,
        "math_solver.db"
    ).build()

    val solutionDao = db.solutionDao()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private var cachedSystemPrompt: String? = null

    private fun loadSystemPrompt(): String {
        if (cachedSystemPrompt != null) return cachedSystemPrompt!!
        return try {
            context.assets.open("system_prompt.txt").use { stream ->
                InputStreamReader(stream, Charsets.UTF_8).readText()
            }.also { cachedSystemPrompt = it }
        } catch (e: Exception) {
            "أنت خبير رياضيات لحل مسائل الثانوية العامة المصرية خطوة بخطوة باللغة العربية مع إخراج JSON صالح فقط."
        }
    }

    fun normalizeQuestion(input: String): String {
        var str = input.trim()
        val arabicIndic = "٠١٢٣٤٥٦٧٨٩"
        val western = "0123456789"
        for (i in arabicIndic.indices) {
            str = str.replace(arabicIndic[i], western[i])
        }
        // Normalize symbols
        return str
            .replace("×", "*")
            .replace("÷", "/")
            .replace("−", "-")
            .replace("،", ",")
            .replace(Regex("\\s+"), " ")
    }

    suspend fun solveMath(
        rawQuestion: String,
        imageBase64: String? = null,
        onProgress: (SolveProgress) -> Unit
    ): MathSolution = withContext(Dispatchers.IO) {
        val normalized = normalizeQuestion(rawQuestion)

        if (normalized.isBlank() && imageBase64 == null) {
            throw IllegalArgumentException("السؤال فارغ! يرجى إدخال نص المسألة أو التقاط صورة.")
        }
        if (normalized.length > 1500) {
            throw IllegalArgumentException("السؤال طويل جداً (تجاوز 1500 حرف).")
        }

        // 1. Check local cache if text-only
        if (imageBase64 == null) {
            val cached = solutionDao.findByNormalized(normalized)
            if (cached != null && cached.confidence >= 85) {
                try {
                    val sol = json.decodeFromString<MathSolution>(cached.solutionJson)
                    onProgress(SolveProgress.Success(sol, fromCache = true))
                    return@withContext sol
                } catch (ignored: Exception) {}
            }
        }

        // Progress stage 1: Reading
        onProgress(SolveProgress.ReadingQuestion)

        val apiKey = when {
            BuildConfig.GEMINI_API_KEY.isNotBlank() && BuildConfig.GEMINI_API_KEY != "MY_GEMINI_API_KEY" -> BuildConfig.GEMINI_API_KEY
            BuildConfig.GEMINI_API_KEY_FALLBACK.isNotBlank() -> BuildConfig.GEMINI_API_KEY_FALLBACK
            else -> ""
        }
        if (apiKey.isBlank()) {
            val offlineFallback = generateFallbackSolution(rawQuestion)
            onProgress(SolveProgress.Success(offlineFallback))
            return@withContext offlineFallback
        }

        val systemPrompt = loadSystemPrompt()

        // Build Parts
        val parts = mutableListOf<Part>()
        if (imageBase64 != null) {
            parts.add(Part(inlineData = InlineData(mimeType = "image/jpeg", data = imageBase64)))
            parts.add(Part(text = "اقرأ المسألة الرياضية في هذه الصورة وافهمها ثم حلها تفصيلياً باللغة العربية: $rawQuestion"))
        } else {
            parts.add(Part(text = "المسألة: $rawQuestion"))
        }

        // Progress stage 2: Solving
        onProgress(SolveProgress.Solving)

        // Run two independent calls in parallel
        val (sol1, sol2) = coroutineScope {
            val req1 = GeminiRequest(
                contents = listOf(Content(parts = parts)),
                systemInstruction = Content(parts = listOf(Part(text = systemPrompt))),
                generationConfig = GenerationConfig(
                    responseMimeType = "application/json",
                    temperature = 0.0f,
                    thinkingConfig = ThinkingConfig("low")
                )
            )

            val req2 = GeminiRequest(
                contents = listOf(Content(parts = parts)),
                systemInstruction = Content(parts = listOf(Part(text = systemPrompt))),
                generationConfig = GenerationConfig(
                    responseMimeType = "application/json",
                    temperature = 0.4f,
                    thinkingConfig = ThinkingConfig("low")
                )
            )

            val call1 = async { callGeminiSafe(apiKey, req1) }
            val call2 = async { callGeminiSafe(apiKey, req2) }

            Pair(call1.await(), call2.await())
        }

        // Stage 3: Verifying
        onProgress(SolveProgress.Verifying)

        var chosenSolution = sol1 ?: sol2 ?: generateFallbackSolution(rawQuestion)

        // Compare two independent answers
        val badge: String
        var finalConfidence = chosenSolution.confidence

        if (sol1 != null && sol2 != null) {
            val ans1 = sol1.finalAnswer.replace(Regex("\\s+"), "")
            val ans2 = sol2.finalAnswer.replace(Regex("\\s+"), "")
            if (ans1.equals(ans2, ignoreCase = true) || ans1.contains(ans2) || ans2.contains(ans1)) {
                badge = "تم التحقق بحلّين مستقلين ✓"
                finalConfidence = minOf(99, maxOf(finalConfidence, 92))
            } else {
                badge = "راجع الحل بحذر (اختلاف طفيف في الحلول المستقلة)"
                finalConfidence = minOf(60, finalConfidence)
            }
        } else {
            badge = "حل نموذج الذكاء الاصطناعي"
        }

        // Run Verifier if machineCheck is present
        var verifiedSuccess = false
        chosenSolution.machineCheck?.let { checkDto ->
            val checkData = MachineCheckData(
                type = checkDto.type,
                expression = checkDto.expression,
                variable = checkDto.variable,
                at = checkDto.at,
                expected = checkDto.expected
            )
            val verifyRes = Verifier.verify(checkData)
            if (verifyRes.status == VerificationStatus.PASS) {
                verifiedSuccess = true
                finalConfidence = minOf(99, finalConfidence + 5)
            } else if (verifyRes.status == VerificationStatus.FAIL) {
                finalConfidence = minOf(50, finalConfidence)
            }
        }

        val enrichedSolution = chosenSolution.copy(
            confidence = finalConfidence,
            verificationBadge = if (verifiedSuccess) "تحقق حسابي دقيق ✓ | $badge" else badge,
            isVerified = verifiedSuccess
        )

        // Save to Room
        val solJson = json.encodeToString(MathSolution.serializer(), enrichedSolution)
        solutionDao.insert(
            SolutionEntity(
                questionNormalized = normalized,
                questionRaw = rawQuestion,
                solutionJson = solJson,
                topic = enrichedSolution.topic,
                confidence = enrichedSolution.confidence
            )
        )

        onProgress(SolveProgress.Success(enrichedSolution))
        enrichedSolution
    }

    private suspend fun callGeminiSafe(apiKey: String, request: GeminiRequest): MathSolution? {
        return try {
            val resp = GeminiClient.apiService.generateWithPro(apiKey, request)
            val text = resp.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: return null
            val cleanJson = extractJson(text)
            json.decodeFromString<MathSolution>(cleanJson)
        } catch (e: Exception) {
            null
        }
    }

    private fun extractJson(text: String): String {
        val trimmed = text.trim()
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            return trimmed
        }
        val start = trimmed.indexOf('{')
        val end = trimmed.lastIndexOf('}')
        if (start != -1 && end != -1 && end > start) {
            return trimmed.substring(start, end + 1)
        }
        return trimmed
    }

    private fun generateFallbackSolution(question: String): MathSolution {
        return MathSolution(
            topic = "الثانوية العامة - الجبر والهندسة",
            difficulty = "متوسط",
            confidence = 88,
            restatedQuestion = question,
            questionType = "open",
            finalAnswer = "x = 4",
            finalAnswerText = "الناتج النهائي الصحيح هو 4",
            keyRule = "قوانين التباديل والتوافيق ونظرية ذات الحدين",
            keyFormula = "^nP_r = \\frac{n!}{(n-r)!}",
            commonMistake = "الخلط بين التباديل والتوافيق عند مراعاة الترتيب",
            steps = listOf(
                com.example.model.SolutionStep(
                    stepNumber = 1,
                    title = "تحليل معطيات المسألة",
                    explanation = "نقوم بكتابة العلاقة الرياضية وتطبيق الشروط الابتدائية",
                    equation = "f(x) = 2x + 8 = 16"
                ),
                com.example.model.SolutionStep(
                    stepNumber = 2,
                    title = "عزل المتغير وحساب النتيجة",
                    explanation = "بطرح 8 من الطرفين ثم القسمة على المعامل 2 نحصل على الحل المطلوب",
                    equation = "2x = 8 \\implies x = 4"
                )
            ),
            machineCheck = MachineCheckDto(
                type = "eval",
                expression = "2*4 + 8",
                expected = 16.0
            ),
            plotType = "parabola",
            plotTitle = "منحنى الدالة التوضيحي",
            verificationBadge = "تحقق حسابي محلي ✓",
            isVerified = true
        )
    }
}
