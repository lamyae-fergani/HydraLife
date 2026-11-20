package com.example.hydralife

import android.Manifest
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.hydralife.data.HydrationDatabase
import com.example.hydralife.data.HydrationRepository
import com.example.hydralife.data.UserSessionManager
import com.example.hydralife.utils.SettingsDrawerManager
import com.example.hydralife.utils.WeatherApiService
import com.example.hydralife.utils.WeatherInfo
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AiAssistantActivity : AppCompatActivity() {

    private lateinit var repository: HydrationRepository
    private lateinit var sessionManager: UserSessionManager
    private lateinit var ivMenuBtn: ImageView
    private lateinit var rvChatMessages: RecyclerView
    private lateinit var layoutEmptyState: View
    private lateinit var etMessageInput: TextInputEditText
    private lateinit var btnSend: MaterialButton
    private lateinit var bottomNavigationView: BottomNavigationView

    private lateinit var cardWeather: MaterialCardView
    private lateinit var tvWeatherCity: TextView
    private lateinit var tvWeatherDetails: TextView
    private lateinit var ivRefreshWeather: ImageView

    private lateinit var btnNewChat: MaterialButton
    private lateinit var btnHistory: MaterialButton
    private lateinit var btnTips: MaterialButton
    private lateinit var btnSuggestion1: MaterialButton
    private lateinit var btnSuggestion2: MaterialButton
    private lateinit var btnSuggestion3: MaterialButton

    private val messageList = mutableListOf<ChatMessage>()
    private lateinit var chatAdapter: ChatAdapter

    private val geminiApiService = GeminiApiService()
    private var isGenerating = false
    private var currentConversationId: Long = -1L
    private var currentWeatherInfo: WeatherInfo? = null

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            fetchDeviceLocationAndWeather()
        } else {
            Toast.makeText(this, "Location permission denied. Using saved city.", Toast.LENGTH_SHORT).show()
            fetchWeatherData()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ai_assistant)

        val dao = HydrationDatabase.getDatabase(this).hydrationDao()
        repository = HydrationRepository(dao)
        sessionManager = UserSessionManager(this)

        ivMenuBtn = findViewById(R.id.ivMenuBtn)
        rvChatMessages = findViewById(R.id.rvChatMessages)
        layoutEmptyState = findViewById(R.id.layoutEmptyState)
        etMessageInput = findViewById(R.id.etMessageInput)
        btnSend = findViewById(R.id.btnSend)
        bottomNavigationView = findViewById(R.id.bottomNavigationView)

        cardWeather = findViewById(R.id.cardWeather)
        tvWeatherCity = findViewById(R.id.tvWeatherCity)
        tvWeatherDetails = findViewById(R.id.tvWeatherDetails)
        ivRefreshWeather = findViewById(R.id.ivRefreshWeather)

        btnNewChat = findViewById(R.id.btnNewChat)
        btnHistory = findViewById(R.id.btnHistory)
        btnTips = findViewById(R.id.btnTips)
        btnSuggestion1 = findViewById(R.id.btnSuggestion1)
        btnSuggestion2 = findViewById(R.id.btnSuggestion2)
        btnSuggestion3 = findViewById(R.id.btnSuggestion3)

        ivMenuBtn.setOnClickListener {
            SettingsDrawerManager.showDrawer(this)
        }

        cardWeather.setOnClickListener {
            showChangeCityDialog()
        }

        ivRefreshWeather.setOnClickListener {
            fetchDeviceLocationAndWeather()
        }

        chatAdapter = ChatAdapter(messageList)
        rvChatMessages.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        rvChatMessages.adapter = chatAdapter

        currentConversationId = intent.getLongExtra("EXTRA_CONVERSATION_ID", -1L)
        if (currentConversationId != -1L) {
            loadExistingMessages(currentConversationId)
        }

        bottomNavigationView.selectedItemId = R.id.nav_ai_assistant
        bottomNavigationView.setOnItemSelectedListener { item ->
            if (isGenerating) return@setOnItemSelectedListener false
            when (item.itemId) {
                R.id.nav_dashboard -> {
                    navigateTo(MainActivity::class.java)
                    true
                }
                R.id.nav_objectives -> {
                    navigateTo(ObjectivesActivity::class.java)
                    true
                }
                R.id.nav_ai_assistant -> true
                R.id.nav_account -> {
                    navigateTo(MyAccountActivity::class.java)
                    true
                }
                else -> false
            }
        }

        btnNewChat.setOnClickListener {
            if (!isGenerating) {
                currentConversationId = -1L
                messageList.clear()
                chatAdapter.notifyDataSetChanged()
                layoutEmptyState.visibility = View.VISIBLE
            }
        }

        btnHistory.setOnClickListener {
            if (!isGenerating) {
                val intent = Intent(this, ConversationsActivity::class.java)
                startActivity(intent)
                finish()
            }
        }

        btnTips.setOnClickListener {
            if (!isGenerating) {
                showHydrationTipsDialog()
            }
        }

        btnSend.setOnClickListener {
            if (isGenerating) return@setOnClickListener
            val text = etMessageInput.text.toString().trim()
            if (text.isNotEmpty()) {
                sendMessage(text)
                etMessageInput.text?.clear()
            }
        }

        btnSuggestion1.setOnClickListener { if (!isGenerating) sendMessage(btnSuggestion1.text.toString()) }
        btnSuggestion2.setOnClickListener { if (!isGenerating) sendMessage(btnSuggestion2.text.toString()) }
        btnSuggestion3.setOnClickListener { if (!isGenerating) sendMessage(btnSuggestion3.text.toString()) }

        // Attempt automatic location detection on start
        checkLocationPermissionAndFetch()
    }

    private fun checkLocationPermissionAndFetch() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fetchDeviceLocationAndWeather()
        } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun fetchDeviceLocationAndWeather() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            fetchWeatherData()
            return
        }

        tvWeatherCity.text = "Detecting location..."
        tvWeatherDetails.text = getString(R.string.loading_weather)

        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        val location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

        if (location != null) {
            lifecycleScope.launch {
                val weather = WeatherApiService.getWeatherDataByCoords(location.latitude, location.longitude, "Current Location")
                currentWeatherInfo = weather
                if (weather != null) {
                    tvWeatherCity.text = weather.cityName
                    tvWeatherDetails.text = "${weather.temperature}°C, ${weather.conditionText} (Feels like ${weather.apparentTemperature}°C)"
                } else {
                    fetchWeatherData()
                }
            }
        } else {
            fetchWeatherData()
        }
    }

    private fun fetchWeatherData() {
        val cityName = sessionManager.getSelectedCity()
        tvWeatherCity.text = cityName
        tvWeatherDetails.text = getString(R.string.loading_weather)

        lifecycleScope.launch {
            val weather = WeatherApiService.getWeatherData(cityName)
            currentWeatherInfo = weather
            if (weather != null) {
                tvWeatherCity.text = weather.cityName
                tvWeatherDetails.text = "${weather.temperature}°C, ${weather.conditionText} (Feels like ${weather.apparentTemperature}°C)"
            } else {
                tvWeatherDetails.text = "Weather unavailable"
            }
        }
    }

    private fun showChangeCityDialog() {
        val input = EditText(this).apply {
            setText(sessionManager.getSelectedCity())
            hint = getString(R.string.city_hint)
        }
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.change_city_title))
            .setView(input)
            .setPositiveButton(getString(R.string.save)) { _, _ ->
                val newCity = input.text.toString().trim()
                if (newCity.isNotEmpty()) {
                    sessionManager.setSelectedCity(newCity)
                    fetchWeatherData()
                    Toast.makeText(this, "City updated to $newCity", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(getString(R.string.cancel)) { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun showHydrationTipsDialog() {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.dialog_hydration_tips, null)
        dialog.setContentView(view)

        val tvResource1 = view.findViewById<TextView>(R.id.tvResource1)
        val tvResource2 = view.findViewById<TextView>(R.id.tvResource2)
        val btnShareAdvice = view.findViewById<MaterialButton>(R.id.btnShareAdvice)

        tvResource1.setOnClickListener {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.mayoclinic.org/healthy-lifestyle/nutrition-and-healthy-eating/in-depth/water/art-20044256"))
                startActivity(intent)
            } catch (_: ActivityNotFoundException) {
                Toast.makeText(this, "No web browser available", Toast.LENGTH_SHORT).show()
            }
        }

        tvResource2.setOnClickListener {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.cdc.gov/healthyweight/healthy_eating/water-and-healthier-drinks.html"))
                startActivity(intent)
            } catch (_: ActivityNotFoundException) {
                Toast.makeText(this, "No web browser available", Toast.LENGTH_SHORT).show()
            }
        }

        btnShareAdvice.setOnClickListener {
            val shareText = getString(R.string.share_advice_text)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, getString(R.string.tips_dialog_title))
                putExtra(Intent.EXTRA_TEXT, shareText)
            }
            try {
                startActivity(Intent.createChooser(intent, getString(R.string.share_chooser_title)))
            } catch (_: ActivityNotFoundException) {
                Toast.makeText(this, "No sharing app available", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    private fun loadExistingMessages(convId: Long) {
        lifecycleScope.launch {
            val savedMessages = repository.getMessagesForConversationList(convId)
            if (savedMessages.isNotEmpty()) {
                layoutEmptyState.visibility = View.GONE
                messageList.clear()
                for (msg in savedMessages) {
                    messageList.add(ChatMessage(text = msg.text, isUser = msg.isUser, timestamp = msg.timestamp))
                }
                chatAdapter.notifyDataSetChanged()
                rvChatMessages.scrollToPosition(messageList.size - 1)
            }
        }
    }

    private fun setInputEnabled(enabled: Boolean) {
        isGenerating = !enabled
        btnSend.isEnabled = enabled
        etMessageInput.isEnabled = enabled
        btnNewChat.isEnabled = enabled
        btnHistory.isEnabled = enabled
        btnTips.isEnabled = enabled
        btnSuggestion1.isEnabled = enabled
        btnSuggestion2.isEnabled = enabled
        btnSuggestion3.isEnabled = enabled
    }

    private fun sendMessage(userText: String) {
        if (isGenerating) return
        setInputEnabled(false)
        layoutEmptyState.visibility = View.GONE

        messageList.add(ChatMessage(userText, isUser = true))
        val userMsgIndex = messageList.size - 1
        chatAdapter.notifyItemInserted(userMsgIndex)

        // Add typing indicator bubble
        val typingBubble = ChatMessage("HydraLife AI is thinking...", isUser = false, isLoadingBubble = true)
        messageList.add(typingBubble)
        val typingIndex = messageList.size - 1
        chatAdapter.notifyItemInserted(typingIndex)
        rvChatMessages.scrollToPosition(typingIndex)

        lifecycleScope.launch {
            try {
                // Create conversation if this is a new chat session
                if (currentConversationId == -1L) {
                    currentConversationId = repository.createNewConversation(userText)
                } else {
                    val existingMsgs = repository.getMessagesForConversationList(currentConversationId)
                    if (existingMsgs.size <= 1) {
                        repository.updateConversationTitleFromFirstMessage(currentConversationId, userText)
                    }
                }

                // Save user message to Room
                repository.saveChatMessage(currentConversationId, userText, isUser = true)

                val consumedMl = repository.getTotalConsumedToday()
                val goalEntity = repository.getOrCreateGoalToday()
                val goalMl = goalEntity.goalMl
                val progressPercent = if (goalMl > 0) ((consumedMl.toFloat() / goalMl.toFloat()) * 100).toInt() else 0

                val weatherContext = currentWeatherInfo?.let {
                    "Current Weather in ${it.cityName}: Temperature ${it.temperature}°C (Feels like ${it.apparentTemperature}°C), Condition: ${it.conditionText}."
                } ?: "WARNING: Live weather data is currently UNAVAILABLE. If the user asks about the weather, you MUST inform them clearly that live weather data is currently unavailable and you cannot provide current temperature. Do not invent weather numbers."

                val apiKey = BuildConfig.GEMINI_API_KEY
                if (apiKey.isEmpty()) {
                    val errorMsg = "Gemini API key is not configured. Please add GEMINI_API_KEY=your_key in local.properties."
                    withContext(Dispatchers.Main) {
                        removeTypingBubble()
                        messageList.add(ChatMessage(errorMsg, isUser = false))
                        chatAdapter.notifyItemInserted(messageList.size - 1)
                        rvChatMessages.scrollToPosition(messageList.size - 1)
                    }
                    return@launch
                }

                // Gather recent conversation history (optimized to last 6 messages excluding typing bubble)
                val historyWindow = messageList.filter { !it.isLoadingBubble }.takeLast(6)
                val historyBuilder = StringBuilder()
                for (msg in historyWindow) {
                    val role = if (msg.isUser) "User" else "HydraLife AI"
                    historyBuilder.append("$role: ${msg.text}\n")
                }

                val prompt = """
                    You are HydraLife AI, a smart hydration assistant.
                    Status: Consumed $consumedMl ml of $goalMl ml goal ($progressPercent%).
                    Weather Info: $weatherContext
                    Language Rule: Reply in EXACTLY the same language/script as the user's latest message (Darija Latin/Arabic, French, English, Arabic). Do not switch languages.
                    History:
                    ${historyBuilder.toString()}
                    User: "$userText"
                    Instructions: Be concise, natural, and directly answer using hydration and weather stats. If Weather Info indicates data is UNAVAILABLE, state so honestly without inventing values. Use Markdown. No raw JSON/tokens.
                """.trimIndent()

                val aiReply = geminiApiService.generateContent(apiKey, prompt)

                // Save AI response to Room
                repository.saveChatMessage(currentConversationId, aiReply, isUser = false)

                withContext(Dispatchers.Main) {
                    removeTypingBubble()
                    messageList.add(ChatMessage(aiReply, isUser = false))
                    chatAdapter.notifyItemInserted(messageList.size - 1)
                    rvChatMessages.scrollToPosition(messageList.size - 1)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    removeTypingBubble()
                    val errorMsg = "Failed to communicate with Gemini AI: ${e.localizedMessage ?: "Unknown error"}. Please check your connection."
                    messageList.add(ChatMessage(errorMsg, isUser = false))
                    chatAdapter.notifyItemInserted(messageList.size - 1)
                    rvChatMessages.scrollToPosition(messageList.size - 1)
                }
            } finally {
                withContext(Dispatchers.Main) {
                    setInputEnabled(true)
                }
            }
        }
    }

    private fun removeTypingBubble() {
        val typingIdx = messageList.indexOfFirst { it.isLoadingBubble }
        if (typingIdx != -1) {
            messageList.removeAt(typingIdx)
            chatAdapter.notifyItemRemoved(typingIdx)
        }
    }

    private fun navigateTo(targetActivity: Class<*>) {
        if (isGenerating) return
        val intent = Intent(this, targetActivity).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(intent)
        overridePendingTransition(0, 0)
        finish()
    }
}
