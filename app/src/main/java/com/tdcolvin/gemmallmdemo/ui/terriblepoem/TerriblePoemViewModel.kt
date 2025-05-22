package com.tdcolvin.gemmallmdemo.ui.terriblepoem

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInference.LlmInferenceOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TerriblePoemUiState(
    val loaded: Boolean = false,
    val loadingError: Throwable? = null,
    val poemTitle: String? = null,
    val poemVerse: String? = null,
    val poemComplete: Boolean = true,
    val reactions: String = "",
)

class TerriblePoemViewModel(application: Application): AndroidViewModel(application) {
    val uiState = MutableStateFlow(TerriblePoemUiState())

    private var llmInference: LlmInference? = null

    init {
        // Load the LlmInference object
        viewModelScope.launch(Dispatchers.IO) {

            val options = LlmInferenceOptions.builder()
                .setModelPath("/data/local/tmp/llm/gemma3_1b.task")
                .setMaxTokens(500)
                .build()

            try {
                llmInference = LlmInference.createFromOptions(application, options)
                uiState.update { it.copy(loaded = true, loadingError = null) }
            }
            catch (th: Throwable) {
                uiState.update { it.copy(loaded = true, loadingError = th) }
            }
        }
    }

    // Called when the user presses the Generate Terrible Poem button
    fun generateTerriblePoem(poemSubject: String) {
        val prompt = "Write a single verse, terrible poem about the following subject: " +
                "${poemSubject}. It should be 4 lines long and almost, but not quite, rhyme. It " +
                "should be intentionally terrible, with bonus points for some factual inaccuracies. " +
                "Respond only with the 4 lines of the poem. Do not include any other text."

        uiState.update {
            it.copy(poemComplete = false, poemVerse = "", poemTitle = poemSubject, reactions = "")
        }

        // Tell the MediaPipe library to start generating text
        viewModelScope.launch(Dispatchers.IO) {
            llmInference?.generateResponseAsync(prompt) { partialResult, done ->
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