package com.tdcolvin.gemmallmdemo.ui.terriblepoem

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import androidx.core.graphics.scale
import kotlinx.coroutines.flow.onCompletion

data class TerriblePoemUiState(
    val loaded: Boolean = false,
    val loadingError: Throwable? = null,

    val roastImage: Bitmap? = null,

    val poemTitle: String? = null,
    val poemVerse: String? = null,
    val poemComplete: Boolean = true,

    val reactions: String = "",
)

class TerriblePoemViewModel(application: Application): AndroidViewModel(application) {
    val uiState = MutableStateFlow(TerriblePoemUiState())

    private var engine: Engine? = null

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val modelPath = "/data/local/tmp/llm/gemma-4-E4B-it-gpu.litertlm"
            val cacheDir = getApplication<Application>().cacheDir.path
            try {
                val gpuEngine = Engine(
                    EngineConfig(
                        modelPath = modelPath,
                        backend = Backend.GPU(),
                        visionBackend = Backend.GPU(),
                        cacheDir = cacheDir
                    )
                )
                gpuEngine.initialize()
                engine = gpuEngine
                uiState.update { it.copy(loaded = true, loadingError = null) }
            }
            catch (gpuError: Throwable) {
                Log.w("litert", "GPU initialization failed, falling back to CPU", gpuError)
                try {
                    val cpuEngine = Engine(
                        EngineConfig(
                            modelPath = modelPath,
                            backend = Backend.CPU(),
                            visionBackend = Backend.CPU(),
                            cacheDir = cacheDir
                        )
                    )
                    cpuEngine.initialize()
                    engine = cpuEngine
                    uiState.update { it.copy(loaded = true, loadingError = null) }
                }
                catch (cpuError: Throwable) {
                    Log.e("litert", "Error loading model", cpuError)
                    uiState.update { it.copy(loaded = true, loadingError = cpuError) }
                }
            }
        }
    }

    // Called when the user presses the Generate Terrible Poem button
    fun generateTerriblePoemFromSubject(poemSubject: String) {
        uiState.update { it.copy(roastImage = null, poemTitle = "\"${poemSubject}\"") }

        val prompt = "Write a single verse, terrible poem about the following subject: " +
                "${poemSubject}. It should be exactly 4 lines long and it should at least attempt to " +
                "rhyme. It should be intentionally terrible, with bonus points for some forced or " +
                "awkward rhymes, clunky meter, melodrama, overly flowery and pretentious language, " +
                "silly imagery, or factual inaccuracy.\n" +
                "Respond only with the 4 lines of the poem. Do not include any other text."

        generatePoemFromPrompt(prompt, null)
    }

    fun generateRoast(roastImage: Bitmap) {
        uiState.update { it.copy(roastImage = roastImage, poemTitle = null) }

        generatePoemFromPrompt("Roast my outfit with! Be ruthless, savage and above all very funny.\n" +
                "Pick out bits of my outfit that don't go, or patterns that are old fashioned or ugly.\n" +
                "What unflattering thing does it make me look like? What do my clothes say about me?\n" +
                "Don't say 'Oh honey', and keep your response to within about 50 words.\n" +
                "It's all in the name of fun! Respond only with your roast. Do not include any other text.", roastImage)
    }

    fun readTextFromImage(image: Bitmap) {
        uiState.update { it.copy(roastImage = image, poemTitle = null) }

        generatePoemFromPrompt("Read all the text from this image. Respond only with the text you have found, or with the words 'no text found' if there is none.", image)
    }

    private fun generatePoemFromPrompt(promptText: String, promptImage: Bitmap?) {
        if (!uiState.value.poemComplete) {
            return
        }

        val activeEngine = engine ?: return

        uiState.update {
            it.copy(poemComplete = false, poemVerse = "", reactions = "")
        }

        viewModelScope.launch(Dispatchers.IO) {
            val promptImageJpg =
                promptImage?.run {
                    val scaled = scale(768, 768)
                    val stream = ByteArrayOutputStream()
                    scaled.compress(Bitmap.CompressFormat.JPEG, 85, stream)
                    stream.toByteArray()
                }

            try {
                val conversationConfig = ConversationConfig(maxOutputToken = 1000)

                val conversation = activeEngine.createConversation(conversationConfig)
                conversation.sendMessageAsync(Contents.of(
                    listOfNotNull(
                        Content.Text(promptText),
                        promptImageJpg?.let { Content.ImageBytes(it) }
                    )
                ))
                    .catch { e ->
                        Log.e("litert", "Error generating text", e)
                    }
                    .onCompletion {
                        uiState.update {
                            it.copy(poemComplete = true)
                        }
                    }
                    .collect { msg ->
                        uiState.update {
                            it.copy(
                                poemComplete = false,
                                poemVerse = (it.poemVerse ?: "")
                                        + msg.contents.contents.firstOrNull()?.toString()
                            )
                        }
                    }
            }
            catch (e: Exception) {
                Log.e("litert", "Error setting up conversation", e)
            }
        }
    }

    fun addReaction(reaction: String) {
        uiState.update { it.copy(reactions = it.reactions + " " + reaction) }
    }
}