package indie.jithinjude.aitalkeachother

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
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
    val initMessage = "Ask a question about academy awards."

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
        content(role = "user") { text("Only ask one question at a time.") },
        content(role = "model") { text("Sure") },
        content(role = "user") { text("Do not add any type of formatting or listing. Nothing else.") },
        content(role = "model") { text("Which is your favorite movie?") },
        content(role = "user") { text("My favorite movie is the Star wars franchise.") },
        content(role = "model") { text("Which one among the franchise?") },
        content(role = "user") { text("Episode V – The Empire Strikes Back is a nice one.") },
        content(role = "model") { text("Why did you like that movie?") },
        content(role = "user") { text(" It had a more serious and mature tone compared to the upbeat original film, creating a sense of real threat and danger.") },
        content(role = "model") { text("Who directed the movie Episode V – The Empire Strikes Back?") },
        content(role = "user") { text("Irvin Kershner.") },
        content(role = "model") { text("Tell me the names of movies directed by Irvin Kershner other than Episode V – The Empire Strikes Back.") },
        content(role = "user") { text("Never Say Never Again, is one of his famous movie") },
        content(role = "model") { text("Tell me some facts about Never Say Never Again movie?") },
        content(role = "user") { text("This was the very first film appearance of Rowan Atkinson, who later became famous as Mr. Bean.") },
        content(role = "model") { text("Mr. Bean! Who is that?") },
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
        val openAI = OpenAI("")
        val chatCompletionRequest = ChatCompletionRequest(
            model = ModelId("gpt-3.5-turbo"),
            messages = listOf(
                ChatMessage(
                    role = ChatRole.User,
                    content = gptQuery + ". Give answer in 1 or 2 lines. Also tell something about related topic in 1 or 2 lines. Give answer as sentence not points. Do not add any type of formatting or listing. Nothing else."
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
                        Image(
                            painter = if (chat.memberType == MemberType.GEMINI) painterResource(id = R.drawable.ic_gemini) else painterResource(
                                id = R.drawable.ic_chat_gpt
                            ),
                            contentDescription = "icon",
                            modifier = Modifier.size(24.dp)
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