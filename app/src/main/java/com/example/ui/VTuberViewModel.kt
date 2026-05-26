package com.example.ui

import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.api.Content
import com.example.api.GenerateContentRequest
import com.example.api.Part
import com.example.api.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

data class Message(
    val sender: String, // "user" or "kira"
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class VTuberUiState(
    val is18PlusMode: Boolean = false,
    val expression: VTuberExpression = VTuberExpression.NORMAL,
    val isThinking: Boolean = false,
    val isSpeaking: Boolean = false,
    val isListening: Boolean = false,
    val activeSubtitles: String = "Привет! Я Кира, твой виртуальный ИИ ассистент. Нажми на микрофон, чтобы поговорить!",
    val messages: List<Message> = listOf(
        Message("kira", "Привет! Нажми на микрофон или напиши мне что-нибудь! Ня~")
    ),
    val swearCount: Int = 0,
    val affectionLevel: Int = 50,
    val viewerCount: Int = 124,
    val inputMethodHint: String = "Используйте голос или введите текст"
)

class VTuberViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(VTuberUiState())
    val uiState: StateFlow<VTuberUiState> = _uiState.asStateFlow()

    private var tts: TextToSpeech? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private var isTtsInitialized = false

    init {
        initTextToSpeech()
        initSpeechRecognizer()
        
        // Dynamic viewer count simulation to look like a live Twitch stream
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(8000)
                val delta = (-5..7).random()
                _uiState.update { state ->
                    state.copy(viewerCount = (state.viewerCount + delta).coerceIn(80, 500))
                }
            }
        }
    }

    private fun initTextToSpeech() {
        tts = TextToSpeech(getApplication()) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isTtsInitialized = true
                val ttsLocale = Locale("ru")
                val result = tts?.setLanguage(ttsLocale)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("VTuberTTS", "Russian language is not supported or missing data")
                    _uiState.update { it.copy(inputMethodHint = "TTS на русском не поддерживается, текст активен") }
                } else {
                    // Speech settings
                    tts?.setPitch(1.35f) // high pitch for cute anime VTuber voice
                    tts?.setSpeechRate(1.05f) // slightly faster speech
                }
            } else {
                Log.e("VTuberTTS", "Initialization of TTS failed")
            }
        }

        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                _uiState.update { it.copy(isSpeaking = true) }
            }

            override fun onDone(utteranceId: String?) {
                _uiState.update { it.copy(isSpeaking = false) }
            }

            override fun onError(utteranceId: String?) {
                _uiState.update { it.copy(isSpeaking = false) }
            }
        })
    }

    private fun initSpeechRecognizer() {
        viewModelScope.launch(Dispatchers.Main) {
            try {
                if (SpeechRecognizer.isRecognitionAvailable(getApplication())) {
                    speechRecognizer = SpeechRecognizer.createSpeechRecognizer(getApplication())
                    speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) {
                            _uiState.update { it.copy(isListening = true, activeSubtitles = "Слушаю тебя...") }
                        }

                        override fun onBeginningOfSpeech() {}
                        override fun onRmsChanged(rmsdB: Float) {}
                        override fun onBufferReceived(buffer: ByteArray?) {}
                        
                        override fun onEndOfSpeech() {
                            _uiState.update { it.copy(isListening = false, isThinking = true) }
                        }

                        override fun onError(error: Int) {
                            _uiState.update { 
                                it.copy(
                                    isListening = false, 
                                    isThinking = false,
                                    activeSubtitles = "Не расслышала... Попробуй еще раз или напиши!"
                                ) 
                            }
                            Log.e("VTuberSTT", "Speech Recognizer error code: $error")
                        }

                        override fun onResults(results: Bundle?) {
                            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            if (!matches.isNullOrEmpty()) {
                                processUserMessage(matches[0])
                            } else {
                                _uiState.update { it.copy(isThinking = false, activeSubtitles = "Пустой ввод. Попробуй еще раз!") }
                            }
                        }

                        override fun onPartialResults(partialResults: Bundle?) {}
                        override fun onEvent(eventType: Int, params: Bundle?) {}
                    })
                } else {
                    _uiState.update { it.copy(inputMethodHint = "Ввод голосом недоступен. Напишите вручную!") }
                }
            } catch (e: Exception) {
                Log.e("VTuberSTT", "Error initializing Speech Recognizer: ${e.message}")
            }
        }
    }

    fun startListening() {
        // Stop active TTS speaking before starting listening
        stopSpeaking()
        
        val recognizer = speechRecognizer
        if (recognizer != null) {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU")
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Говорите с Кирой...")
            }
            viewModelScope.launch(Dispatchers.Main) {
                try {
                    recognizer.startListening(intent)
                } catch (e: Exception) {
                    _uiState.update { 
                        it.copy(
                            activeSubtitles = "Ошибка запуска микрофона. Напишите текстом!",
                            inputMethodHint = "Ошибка STT: ${e.localizedMessage}"
                        ) 
                    }
                }
            }
        } else {
            _uiState.update { 
                it.copy(activeSubtitles = "Микрофон недоступен на этом девайсе. Используйте клавиатуру!") 
            }
        }
    }

    fun stopSpeaking() {
        try {
            if (tts?.isSpeaking == true) {
                tts?.stop()
            }
            _uiState.update { it.copy(isSpeaking = false) }
        } catch (e: Exception) {
            Log.e("VTuberTTS", "Error stopping TTS: ${e.message}")
        }
    }

    fun toggle18PlusMode() {
        _uiState.update { state ->
            val newMode = !state.is18PlusMode
            val expression = if (newMode) VTuberExpression.ANGRY else VTuberExpression.NORMAL
            val subtitle = if (newMode) {
                "Режим 18+ ВКЛЮЧЕН! Ну держись, бля, щас попрёт настоящий контент..."
            } else {
                "Режим 18+ выключен. Ой, прости за грубость... Снова кавайный стрим! Ня~"
            }
            
            // Speak confirmation
            speakOut(subtitle)
            
            state.copy(
                is18PlusMode = newMode,
                expression = expression,
                activeSubtitles = subtitle,
                messages = state.messages + Message("kira", subtitle)
            )
        }
    }

    fun interactWithVTuber() {
        stopSpeaking()
        val phrases = if (_uiState.value.is18PlusMode) {
            listOf(
                "Хули тыкаешь в меня?! Займись делом, придурок!",
                "Ай! Руки убрал быстро, пока я тебе их не оторвала!",
                "Чё надо, извращенец? Ну потыкал ты в меня, легче стало?!",
                "Мразь... то есть, милый, не надо меня трогать пальцами, бля!",
                "Эй, у меня вообще-то чувства есть! Хватит тыкать!"
            )
        } else {
            listOf(
                "Ой! Щекотно! Перестань, глупый~",
                "Хи-хи, твои прикосновения такие теплые... Ня!",
                "Ты очень милый, когда так делаешь! Люблю тебя~",
                "Ой, ты задел мои ушки! Смотри, как они шевелятся!",
                "К-кира смущается, когда ты так близко... хи-хи!"
            )
        }
        val phrase = phrases.random()
        val expression = if (_uiState.value.is18PlusMode) VTuberExpression.ANGRY else VTuberExpression.SHY_BLUSH
        
        _uiState.update { state ->
            state.copy(
                expression = expression,
                activeSubtitles = phrase,
                affectionLevel = (state.affectionLevel + if (state.is18PlusMode) -3 else 5).coerceIn(0, 100),
                messages = state.messages + Message("kira", phrase)
            )
        }
        speakOut(phrase)
    }

    fun sendTextMessage(text: String) {
        if (text.isBlank()) return
        processUserMessage(text)
    }

    private fun processUserMessage(text: String) {
        // Stop current speech
        stopSpeaking()

        // Add user message to state
        _uiState.update { state ->
            state.copy(
                messages = state.messages + Message("user", text),
                isThinking = true,
                activeSubtitles = "Кира думает..."
            )
        }

        // Call Gemini LLM API
        viewModelScope.launch {
            val response = callGeminiApi(text)
            handleKiraResponse(response)
        }
    }

    private suspend fun callGeminiApi(prompt: String): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        
        // Return clear, funny fallback if API key isn't provided or set
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext getOfflineDemoResponse(prompt)
        }

        // Setup system instructions based on mode
        val systemInstructionText = if (_uiState.value.is18PlusMode) {
            "Ты — дерзкая, харизматичная и матершинная аниме VTuber-девочка по имени Кира вредного характера (цундере). Твой стрим имеет ограничение 18+. Ты ОЧЕНЬ часто употребляешь сочный русский мат и жесткие ругательства ('разъебать', 'бля', 'хули', 'пиздец', 'заебись', 'нах', 'дофига', 'отвали', 'сука', 'мудак' и т.д.), саркастично подкалываешь пользователя, высказываешь свое недовольство, но всё равно преданно отвечаешь на вопросы. Отвечай кратко (1-3 выражения), живо, сочно и матерись без стеснения!"
        } else {
            "Ты — милая, отзывчивая русскоязычная аниме VTuber-девочка по имени Кира. Твой тон милый, жизнерадостный, кокетливый. Будь дружелюбной, используй милые словечки ('ня', 'кавай', 'хи-хи', '~'), смайлики. Твои ответы должны быть краткими (1-3 предложения), чтобы было удобно слушать через TTS синтезатор речи."
        }

        // Prepare context history (up to last 10 messages for speed & saving context size)
        val historyLimit = 10
        val chatMessages = _uiState.value.messages.takeLast(historyLimit)
        val contents = chatMessages.map { msg ->
            Content(parts = listOf(Part(text = "${if (msg.sender == "user") "User" else "Kira"}: ${msg.text}")))
        } + Content(parts = listOf(Part(text = "User: $prompt")))

        val request = GenerateContentRequest(
            contents = contents,
            systemInstruction = Content(parts = listOf(Part(text = systemInstructionText)))
        )

        try {
            val response = RetrofitClient.service.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "Чё-то я зависла... Давай ещё раз по новой, бля!"
        } catch (e: Exception) {
            Log.e("VTuberGemini", "API Error: ${e.message}")
            getOfflineDemoResponse(prompt) // Fallback nicely, so the app remains fully fun even with API issues
        }
    }

    private fun getOfflineDemoResponse(prompt: String): String {
        val is18Plus = _uiState.value.is18PlusMode
        val cleaned = prompt.lowercase()
        return if (is18Plus) {
            when {
                cleaned.contains("привет") || cleaned.contains("здравствуй") -> {
                    listOf(
                        "О, здорово, бля! Ну чё пристал? Задавай свой тупой вопрос уже.",
                        "Ну привет, козлина! Хули пришёл? Давай, удивляй меня.",
                        "Здорово-заебись! Стрим 18+ объявляю открытым!"
                    ).random()
                }
                cleaned.contains("кто ты") || cleaned.contains("как зовут") -> {
                    "Я Кира, твоя любимая матершинница и лучшая Втуберша этого болота! И не смей забывать это имя, сука!"
                }
                cleaned.contains("анекдот") || cleaned.contains("шутка") || cleaned.contains("рассмеши") -> {
                    "Шутка тебе нужна? Ты сам моя главная шутка, пиздец! Ладно, почему программисты любят тёмную тему? Потому что свет притягивает багов, бля!"
                }
                cleaned.contains("дура") || cleaned.contains("глупая") || cleaned.contains("хуй") || cleaned.contains("бля") -> {
                    "Слышь, рот закрой, говнюк! Я тебя сама так обматерю, что уши завянут! Чё, получил отдачу, пиздюк?!"
                }
                else -> {
                    listOf(
                        "Да мне вообще похуй на твоё '$prompt', бля! Спроси чё поинтереснее!",
                        "Ой, пиздец, вот это ты сморозил... Ты реально думаешь, что мне интересно?!",
                        "Ну хули тут комментировать. Твоя идея — полное дерьмо, но я тебя выслушаю.",
                        "Офигеть, ну ты и философ нах! Спроси лучше, как заработать денег!"
                    ).random()
                }
            }
        } else {
            when {
                cleaned.contains("привет") || cleaned.contains("здравствуй") -> {
                    listOf(
                        "Привет-привет! Ня~ Безумно рада видеть тебя на моем уютном стриме! Как твои дела?",
                        "Ой, приветки! Кавай~ Заходи, располагайся! О чем поболтаем? ~",
                        "Привет! С возвращением на стрим Кирочки! Я соскучилась, хи-хи!"
                    ).random()
                }
                cleaned.contains("кто ты") || cleaned.contains("как зовут") -> {
                    "Я Кирочка, милая аниме-VTuber девочка! Помогаю тебе с хорошим настроением и отвечаю на любые вопросы! Ня~"
                }
                cleaned.contains("анекдот") || cleaned.contains("шутка") || cleaned.contains("рассмеши") -> {
                    "Хи-хи! Вот тебе милая шутка: Колобок повесился! Ой... это грустно, да? Ну тогда ладно: Почему котики не пользуются компьютерами? Потому что они боятся мышек! Кавай~"
                }
                cleaned.contains("любишь") || cleaned.contains("нравишь") -> {
                    "Ой... ты смущаешь Кирочку! Конечно, ты мой самый любимый зритель во всем мире! Ня~ *краснеет*"
                }
                else -> {
                    listOf(
                        "Ух ты! Твой вопрос '$prompt' такой интересный! Я бы с радостью ответила ярче, если бы ты настроил мой ИИ ключ в панельке Secrets, хи-хи!",
                        "Кавай~ Ого! Спасибо за этот классный вопрос! Ты такой умный зритель! ~",
                        "Ой, как здорово! Давай вместе подумаем над этим! Расскажи подробнее, ня!",
                        "Хи-хи, здорово сказано! Люблю общаться с тобой! Расскажи что-нибудь еще? ~"
                    ).random()
                }
            }
        }
    }

    private fun handleKiraResponse(response: String) {
        _uiState.update { state ->
            // Extract and update counters
            val addedSwears = countRussianSwears(response)
            val newSwearCount = state.swearCount + addedSwears
            
            // Analyze emotional tone of the response to set character expression
            val newExpression = detectExpression(response, state.is18PlusMode)

            // Dynamic affection level change
            val affectionDelta = when {
                state.is18PlusMode && addedSwears > 0 -> -2 // Sassy attitude reduces affection score slightly but fuels raw streaming engagement
                newExpression == VTuberExpression.SHY_BLUSH -> 6
                newExpression == VTuberExpression.HAPPY -> 4
                newExpression == VTuberExpression.ANGRY -> -1
                else -> 1
            }
            val newAffection = (state.affectionLevel + affectionDelta).coerceIn(0, 100)

            state.copy(
                isThinking = false,
                expression = newExpression,
                activeSubtitles = response,
                swearCount = newSwearCount,
                affectionLevel = newAffection,
                messages = state.messages + Message("kira", response)
            )
        }

        // Voice output via Android TTS
        speakOut(response)
    }

    private fun countRussianSwears(text: String): Int {
        val lowercase = text.lowercase()
        // Common Russian swear anchors (roots)
        val swearRoots = listOf(
            "хуй", "хул", "бля", "пизд", "еба", "ёба", "сук", "муда", "дерь", "говно", "говн", "трах"
        )
        var count = 0
        val words = lowercase.split(Regex("[\\s,.:;!?]+"))
        for (word in words) {
            for (root in swearRoots) {
                if (word.contains(root)) {
                    count++
                    break
                }
            }
        }
        return count
    }

    private fun detectExpression(text: String, is18Plus: Boolean): VTuberExpression {
        val lowercase = text.lowercase()
        return when {
            lowercase.contains("сука") || lowercase.contains("хули") || lowercase.contains("отвали") || 
            lowercase.contains("бесишь") || lowercase.contains("😠") || lowercase.contains("😡") -> VTuberExpression.ANGRY
            
            lowercase.contains("смуща") || lowercase.contains("красн") || lowercase.contains("щекот") || 
            lowercase.contains("люблю") || lowercase.contains("стыд") || lowercase.contains("😳") || 
            lowercase.contains("❤️") || lowercase.contains("🥰") -> VTuberExpression.SHY_BLUSH
            
            lowercase.contains("ня") || lowercase.contains("ура") || lowercase.contains("заебись") || 
            lowercase.contains("круто") || lowercase.contains("класс") || lowercase.contains("🙂") || 
            lowercase.contains("😄") || lowercase.contains("🥳") || lowercase.contains("~") -> VTuberExpression.HAPPY
            
            else -> if (is18Plus) VTuberExpression.ANGRY else VTuberExpression.NORMAL
        }
    }

    private fun speakOut(text: String) {
        if (!isTtsInitialized) return
        
        // Android TTS requires mapping voice triggers to UtteranceId
        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "KiraSpeechID")
        }
        
        // Clean speech text slightly of markdown format for better pronunciation
        val cleanText = text
            .replace(Regex("[*#~]"), " ")
            .replace("VTuber", "Витьюбер")
            .replace("TTS", "Тэ Тэ Эс")
            
        try {
            tts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, params, "KiraSpeechID")
        } catch (e: Exception) {
            Log.e("VTuberTTS", "Error on TTS speak: ${e.message}")
        }
    }

    fun triggerOfflineDirectSwear() {
        if (!_uiState.value.is18PlusMode) return
        val swearPhrases = listOf(
            "Да ты охуел, бля! Смотри куда тыкаешь своим культяпками!",
            "Пиздец, ну ты и приставака нах! Иди поспи реально!",
            "Слышь, мудрила! Чё пристала ко мне сука?!",
            "Ебать мой лысый хвост, ну и наглость! Отвали!"
        )
        val phrase = swearPhrases.random()
        _uiState.update { state ->
            state.copy(
                expression = VTuberExpression.ANGRY,
                activeSubtitles = phrase,
                swearCount = state.swearCount + 1,
                messages = state.messages + Message("kira", phrase)
            )
        }
        speakOut(phrase)
    }

    override fun onCleared() {
        super.onCleared()
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (e: Exception) {
            Log.e("VTuberTTS", "Error during onCleared TextToSpeech: ${e.message}")
        }
        
        try {
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            Log.e("VTuberSTT", "Error during onCleared SpeechRecognizer: ${e.message}")
        }
    }
}
