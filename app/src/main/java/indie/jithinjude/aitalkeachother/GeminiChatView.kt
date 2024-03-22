package indie.jithinjude.aitalkeachother

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aallam.openai.api.BetaOpenAI
import com.aallam.openai.api.chat.ChatCompletion
import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun GeminiChatView(
    apiKey: String
) {
    val initMessage = "Ask one short question about Global warming."

    val coroutineScope = rememberCoroutineScope()
    val lazyColumnListState = rememberLazyListState()
    val isGemini = remember { mutableStateOf(false) }
    val chatDataList = remember { mutableStateOf(listOf(ChatMember(memberType = MemberType.CHAT_GPT, text = initMessage))) }
    val isFirstMessage = remember { mutableStateOf(true) }


    val generativeModel = GenerativeModel(
        modelName = "gemini-pro",
        apiKey = apiKey
    )
    val chat = generativeModel.startChat(history = listOf(
        content(role = "user") { text("Only ask one question at a time. Never repeat question, instead create a new question from related topic. Nothing else.") },
        content(role = "model") { text("Sure") }
    ))

    suspend fun getGeminiResponse(geminiQuery: String) {
        val response = chat.sendMessage(geminiQuery)
        chatDataList.value += listOf(
            ChatMember(
                MemberType.GEMINI,
                response.text ?: ""
            )
        )
        lazyColumnListState.animateScrollToItem(chatDataList.value.size - 1)
    }

    @OptIn(BetaOpenAI::class)
    suspend fun getChatGPTResponse(gptQuery: String) {
        val openAI = OpenAI(BuildConfig.openAiApiKey)
        val chatCompletionRequest = ChatCompletionRequest(
            model = ModelId("gpt-3.5-turbo"),
            messages = listOf(
                ChatMessage(
                    role = ChatRole.User,
                    content = gptQuery + ". Give answer in 1 or 2 lines. Also tell something about related topic in 1 or 2 lines. Nothing else."
                )
            )
        )

        val completion: ChatCompletion = openAI.chatCompletion(chatCompletionRequest)

        val response = completion.choices.first().message?.content

        chatDataList.value += listOf(
            ChatMember(
                MemberType.CHAT_GPT,
                response ?: ""
            )
        )
        lazyColumnListState.animateScrollToItem(chatDataList.value.size - 1)
    }

    LaunchedEffect(chatDataList.value) {
        coroutineScope.launch {
            isGemini.value = !isGemini.value
            if(isFirstMessage.value){
                isFirstMessage.value = false
                getGeminiResponse(initMessage)
            } else {
                try {
                    delay(10000)
                    if(isGemini.value){
                        getGeminiResponse(chatDataList.value.last().text)
                    } else {
                        getChatGPTResponse(chatDataList.value.last().text)
                    }
                } catch (ex: Exception) {
                    Log.d("AI_ERROR", "AI_ERROR :=> ${ex.message}")
                    ex.printStackTrace()
                }
            }
        }
    }
    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize(),
            state = lazyColumnListState
        ) {
            items(chatDataList.value) { chat ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            imageVector = if (chat.memberType == MemberType.GEMINI) Icons.Rounded.Face else Icons.Rounded.AccountCircle,
                            contentDescription = "Send"
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = chat.text, fontSize = 20.sp)
                    }
                }
            }
        }
    }
}
data class ChatMember(var memberType: MemberType, var text: String)
enum class MemberType {GEMINI, CHAT_GPT}