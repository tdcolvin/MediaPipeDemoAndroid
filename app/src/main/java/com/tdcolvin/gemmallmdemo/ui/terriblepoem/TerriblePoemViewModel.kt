package com.tdcolvin.gemmallmdemo.ui.terriblepoem

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.genai.llminference.GraphOptions
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInference.LlmInferenceOptions
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession.LlmInferenceSessionOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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

    private var llmInference: LlmInference? = null
    private var llmInferenceSession: LlmInferenceSession? = null

    init {
        // Load the LlmInference object
        viewModelScope.launch(Dispatchers.IO) {

            val options = LlmInferenceOptions.builder()
                .setModelPath("/data/local/tmp/llm/gemma3_4b.task")
                .setMaxTokens(1000)
                .setMaxNumImages(1)
                .build()

            val inferenceSessionOptions = LlmInferenceSessionOptions.builder()
                .setTopK(10)
                .setTemperature(0.8f)
                .setGraphOptions(GraphOptions.builder().setEnableVisionModality(true).build())
                .build()

            try {
                llmInference = LlmInference.createFromOptions(application, options)
                llmInferenceSession = LlmInferenceSession.createFromOptions(llmInference, inferenceSessionOptions)

                uiState.update { it.copy(loaded = true, loadingError = null) }
            }
            catch (th: Throwable) {
                uiState.update { it.copy(loaded = true, loadingError = th) }
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

        generatePoemFromPrompt("Roast me! Be ruthless, savage and above all very funny.\n" +
                "Give your answer as a 4 line poem.\n" +
                "Respond only with the 4 lines of the poem. Do not include any other text.", roastImage)
    }

    private fun generatePoemFromPrompt(promptText: String, promptImage: Bitmap?) {
        if (!uiState.value.poemComplete) {
            return
        }

        uiState.update {
            it.copy(poemComplete = false, poemVerse = "", reactions = "")
        }

        // Tell the MediaPipe library to start generating text
        viewModelScope.launch(Dispatchers.IO) {
            llmInferenceSession?.addQueryChunk(promptText)
            promptImage?.let { llmInferenceSession?.addImage(BitmapImageBuilder(it).build()) }
            llmInferenceSession?.generateResponseAsync { partialResult, done ->
                uiState.update {
                    it.copy(
                        poemComplete = done,
                        poemVerse = (it.poemVerse ?: "") + partialResult
                    )
                }
            }
        }
    }

    fun addReaction(reaction: String) {
        uiState.update { it.copy(reactions = it.reactions + " " + reaction) }
    }
}