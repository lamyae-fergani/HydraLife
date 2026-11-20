package com.example.hydralife

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.hydralife.data.HydrationDatabase
import com.example.hydralife.data.HydrationRepository
import com.example.hydralife.utils.SettingsDrawerManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.slider.Slider
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Locale

class ObjectivesActivity : AppCompatActivity() {

    private lateinit var repository: HydrationRepository
    private lateinit var ivMenuBtn: ImageView
    private lateinit var tvCurrentGoalValue: TextView
    private lateinit var sliderGoal: Slider
    private lateinit var btnDecGoal: MaterialButton
    private lateinit var btnIncGoal: MaterialButton
    private lateinit var btnSaveGoal: MaterialButton
    private lateinit var tvMorningAmount: TextView
    private lateinit var tvAfternoonAmount: TextView
    private lateinit var tvEveningAmount: TextView
    private lateinit var rvGoalHistory: RecyclerView
    private lateinit var bottomNavigationView: BottomNavigationView

    private var selectedGoalMl = 2000

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_objectives)

        val dao = HydrationDatabase.getDatabase(this).hydrationDao()
        repository = HydrationRepository(dao)

        ivMenuBtn = findViewById(R.id.ivMenuBtn)
        tvCurrentGoalValue = findViewById(R.id.tvCurrentGoalValue)
        sliderGoal = findViewById(R.id.sliderGoal)
        btnDecGoal = findViewById(R.id.btnDecGoal)
        btnIncGoal = findViewById(R.id.btnIncGoal)
        btnSaveGoal = findViewById(R.id.btnSaveGoal)
        tvMorningAmount = findViewById(R.id.tvMorningAmount)
        tvAfternoonAmount = findViewById(R.id.tvAfternoonAmount)
        tvEveningAmount = findViewById(R.id.tvEveningAmount)
        rvGoalHistory = findViewById(R.id.rvGoalHistory)
        bottomNavigationView = findViewById(R.id.bottomNavigationView)

        ivMenuBtn.setOnClickListener {
            SettingsDrawerManager.showDrawer(this)
        }

        rvGoalHistory.layoutManager = LinearLayoutManager(this)

        bottomNavigationView.selectedItemId = R.id.nav_objectives
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> {
                    navigateTo(MainActivity::class.java)
                    true
                }
                R.id.nav_objectives -> true
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

        sliderGoal.addOnChangeListener { _, value, _ ->
            selectedGoalMl = value.toInt()
            updateGoalDisplay(selectedGoalMl)
        }

        btnDecGoal.setOnClickListener {
            selectedGoalMl = maxOf(1200, selectedGoalMl - 100)
            sliderGoal.value = selectedGoalMl.toFloat()
            updateGoalDisplay(selectedGoalMl)
        }

        btnIncGoal.setOnClickListener {
            selectedGoalMl = minOf(3600, selectedGoalMl + 100)
            sliderGoal.value = selectedGoalMl.toFloat()
            updateGoalDisplay(selectedGoalMl)
        }

        btnSaveGoal.setOnClickListener {
            lifecycleScope.launch {
                repository.updateGoal(selectedGoalMl)
                Toast.makeText(this@ObjectivesActivity, "Goal saved successfully!", Toast.LENGTH_SHORT).show()
            }
        }

        loadData()
    }

    private fun loadData() {
        lifecycleScope.launch {
            val goalEntity = repository.getOrCreateGoalToday()
            selectedGoalMl = goalEntity.goalMl
            sliderGoal.value = selectedGoalMl.toFloat()
            updateGoalDisplay(selectedGoalMl)

            val morning = repository.getConsumedForSessionToday("Morning")
            val afternoon = repository.getConsumedForSessionToday("Afternoon")
            val evening = repository.getConsumedForSessionToday("Evening")

            tvMorningAmount.text = "$morning ml"
            tvAfternoonAmount.text = "$afternoon ml"
            tvEveningAmount.text = "$evening ml"
        }

        lifecycleScope.launch {
            repository.getAllGoalsHistory().collectLatest { history ->
                rvGoalHistory.adapter = GoalHistoryAdapter(history)
            }
        }
    }

    private fun updateGoalDisplay(goalMl: Int) {
        val goalL = String.format(Locale.getDefault(), "%.1f L", goalMl / 1000.0)
        tvCurrentGoalValue.text = "$goalMl ml ($goalL)"
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
