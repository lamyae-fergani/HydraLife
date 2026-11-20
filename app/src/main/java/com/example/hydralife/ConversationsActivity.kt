package com.example.hydralife

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.hydralife.data.ConversationEntity
import com.example.hydralife.data.HydrationDatabase
import com.example.hydralife.data.HydrationRepository
import com.example.hydralife.utils.SettingsDrawerManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ConversationsActivity : AppCompatActivity() {

    private lateinit var repository: HydrationRepository
    private lateinit var ivMenuBtn: ImageView
    private lateinit var rvConversations: RecyclerView
    private lateinit var layoutEmptyHistory: LinearLayout
    private lateinit var btnNewChatFromHistory: MaterialButton
    private lateinit var bottomNavigationView: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_conversations)

        val dao = HydrationDatabase.getDatabase(this).hydrationDao()
        repository = HydrationRepository(dao)

        ivMenuBtn = findViewById(R.id.ivMenuBtn)
        rvConversations = findViewById(R.id.rvConversations)
        layoutEmptyHistory = findViewById(R.id.layoutEmptyHistory)
        btnNewChatFromHistory = findViewById(R.id.btnNewChatFromHistory)
        bottomNavigationView = findViewById(R.id.bottomNavigationView)

        ivMenuBtn.setOnClickListener {
            SettingsDrawerManager.showDrawer(this)
        }

        rvConversations.layoutManager = LinearLayoutManager(this)

        bottomNavigationView.selectedItemId = R.id.nav_ai_assistant
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> {
                    navigateTo(MainActivity::class.java)
                    true
                }
                R.id.nav_objectives -> {
                    navigateTo(ObjectivesActivity::class.java)
                    true
                }
                R.id.nav_ai_assistant -> {
                    navigateTo(AiAssistantActivity::class.java)
                    true
                }
                R.id.nav_account -> {
                    navigateTo(MyAccountActivity::class.java)
                    true
                }
                else -> false
            }
        }

        btnNewChatFromHistory.setOnClickListener {
            createNewChatAndOpen()
        }

        loadConversations()
    }

    private fun loadConversations() {
        lifecycleScope.launch {
            repository.getAllConversations().collectLatest { list ->
                if (list.isEmpty()) {
                    layoutEmptyHistory.visibility = android.view.View.VISIBLE
                    rvConversations.visibility = android.view.View.GONE
                } else {
                    layoutEmptyHistory.visibility = android.view.View.GONE
                    rvConversations.visibility = android.view.View.VISIBLE
                    rvConversations.adapter = ConversationsAdapter(
                        list,
                        onConvClick = { conv -> openConversation(conv.id) },
                        onRenameClick = { conv -> showRenameDialog(conv) },
                        onDeleteClick = { conv -> showDeleteConfirmationDialog(conv) }
                    )
                }
            }
        }
    }

    private fun createNewChatAndOpen() {
        lifecycleScope.launch {
            val newId = repository.createNewConversation("New Chat")
            openConversation(newId)
        }
    }

    private fun openConversation(conversationId: Long) {
        val intent = Intent(this, AiAssistantActivity::class.java).apply {
            putExtra("EXTRA_CONVERSATION_ID", conversationId)
        }
        startActivity(intent)
        finish()
    }

    private fun showRenameDialog(conv: ConversationEntity) {
        val input = EditText(this).apply {
            setText(conv.title)
            hint = "Conversation Title"
        }
        AlertDialog.Builder(this)
            .setTitle("Rename Conversation")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val newTitle = input.text.toString().trim()
                if (newTitle.isNotEmpty()) {
                    lifecycleScope.launch {
                        repository.updateConversationTitle(conv.id, newTitle)
                        Toast.makeText(this@ConversationsActivity, "Renamed successfully", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun showDeleteConfirmationDialog(conv: ConversationEntity) {
        AlertDialog.Builder(this)
            .setTitle("Delete Conversation")
            .setMessage("Are you sure you want to delete '${conv.title}'?")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    repository.deleteConversation(conv.id)
                    Toast.makeText(this@ConversationsActivity, "Conversation deleted", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun navigateTo(targetActivity: Class<*>) {
        val intent = Intent(this, targetActivity).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(intent)
        overridePendingTransition(0, 0)
        finish()
    }
}
