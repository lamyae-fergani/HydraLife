package com.example.hydralife

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.hydralife.data.HydrationDatabase
import com.example.hydralife.data.HydrationRepository
import com.example.hydralife.utils.SettingsDrawerManager
import com.example.hydralife.utils.WaterBottleView
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var repository: HydrationRepository
    private lateinit var ivMenuBtn: ImageView
    private lateinit var tvGreeting: TextView
    private lateinit var tvDate: TextView
    private lateinit var waterBottleView: WaterBottleView
    private lateinit var tvConsumed: TextView
    private lateinit var tvGoal: TextView
    private lateinit var tvPercentage: TextView
    private lateinit var tvRemaining: TextView
    private lateinit var barChartWeekly: BarChart
    private lateinit var bottomNavigationView: BottomNavigationView

    private lateinit var btnAdd200: MaterialButton
    private lateinit var btnAdd250: MaterialButton
    private lateinit var btnAdd500: MaterialButton
    private lateinit var btnAddCustom: MaterialButton

    private var currentConsumedMl = 0
    private var dailyGoalMl = 2000

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val dao = HydrationDatabase.getDatabase(this).hydrationDao()
        repository = HydrationRepository(dao)

        ivMenuBtn = findViewById(R.id.ivMenuBtn)
        tvGreeting = findViewById(R.id.tvGreeting)
        tvDate = findViewById(R.id.tvDate)
        waterBottleView = findViewById(R.id.waterBottleView)
        tvConsumed = findViewById(R.id.tvConsumed)
        tvGoal = findViewById(R.id.tvGoal)
        tvPercentage = findViewById(R.id.tvPercentage)
        tvRemaining = findViewById(R.id.tvRemaining)
        barChartWeekly = findViewById(R.id.barChartWeekly)
        bottomNavigationView = findViewById(R.id.bottomNavigationView)

        btnAdd200 = findViewById(R.id.btnAdd200)
        btnAdd250 = findViewById(R.id.btnAdd250)
        btnAdd500 = findViewById(R.id.btnAdd500)
        btnAddCustom = findViewById(R.id.btnAddCustom)

        waterBottleView.setProgress(0f, animate = false)

        ivMenuBtn.setOnClickListener {
            SettingsDrawerManager.showDrawer(this)
        }

        val currentDate = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date())
        tvDate.text = currentDate

        bottomNavigationView.selectedItemId = R.id.nav_dashboard
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> true
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

        btnAdd200.setOnClickListener { addWater(200) }
        btnAdd250.setOnClickListener { addWater(250) }
        btnAdd500.setOnClickListener { addWater(500) }
        btnAddCustom.setOnClickListener { showCustomWaterDialog() }

        loadDashboardData()
    }

    override fun onResume() {
        super.onResume()
        loadDashboardData()
    }

    private fun loadDashboardData() {
        lifecycleScope.launch {
            currentConsumedMl = repository.getTotalConsumedToday()
            val goalEntity = repository.getOrCreateGoalToday()
            dailyGoalMl = goalEntity.goalMl

            updateHydrationUI()
            setupWeeklyChart()
        }
    }

    private fun addWater(amountMl: Int) {
        lifecycleScope.launch {
            repository.addWaterEntry(amountMl)
            loadDashboardData()
            Toast.makeText(this@MainActivity, "+${amountMl}ml added successfully", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showCustomWaterDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Add Custom Water Quantity")

        val input = EditText(this).apply {
            hint = "Enter amount in ml (e.g. 300)"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
        }
        builder.setView(input)

        builder.setPositiveButton("Add") { _, _ ->
            val text = input.text.toString().trim()
            if (text.isNotEmpty()) {
                val amount = text.toIntOrNull()
                if (amount != null && amount > 0) {
                    addWater(amount)
                } else {
                    Toast.makeText(this, "Please enter a valid positive number", Toast.LENGTH_SHORT).show()
                }
            }
        }
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
        builder.show()
    }

    private fun updateHydrationUI() {
        val consumedL = String.format(Locale.getDefault(), "%.1f L", currentConsumedMl / 1000.0)
        val goalL = String.format(Locale.getDefault(), "%.1f L Goal", dailyGoalMl / 1000.0)
        val remainingMl = maxOf(0, dailyGoalMl - currentConsumedMl)
        val remainingStr = if (remainingMl >= 1000) {
            String.format(Locale.getDefault(), "%.1f L", remainingMl / 1000.0)
        } else {
            "$remainingMl ml"
        }

        val percentage = if (dailyGoalMl > 0) ((currentConsumedMl.toFloat() / dailyGoalMl.toFloat()) * 100).toInt() else 0
        val fillProgress = if (dailyGoalMl > 0) {
            (currentConsumedMl.toFloat() / dailyGoalMl.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }

        tvConsumed.text = consumedL
        tvGoal.text = "of $goalL"
        tvPercentage.text = "$percentage%"
        tvRemaining.text = remainingStr

        waterBottleView.setProgress(fillProgress, animate = true)
    }

    private fun setupWeeklyChart() {
        lifecycleScope.launch {
            val weeklyMap = repository.getWeeklyConsumptionMap()
            val calendar = Calendar.getInstance()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val dayLabelFormat = SimpleDateFormat("EEE", Locale.getDefault())

            val entries = mutableListOf<BarEntry>()
            val days = mutableListOf<String>()

            for (i in 6 downTo 0) {
                val cal = (calendar.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -i) }
                val dateStr = dateFormat.format(cal.time)
                val dayLabel = dayLabelFormat.format(cal.time)
                val totalMl = weeklyMap[dateStr] ?: 0
                val totalL = totalMl / 1000.0f

                val xIndex = (6 - i).toFloat()
                entries.add(BarEntry(xIndex, totalL))
                days.add(dayLabel)
            }

            val dataSet = BarDataSet(entries, "Daily Water (L)").apply {
                color = getColor(R.color.primary)
                valueTextColor = getColor(R.color.on_surface)
                valueTextSize = 12f
            }

            val barData = BarData(dataSet).apply {
                barWidth = 0.5f
            }

            barChartWeekly.data = barData
            barChartWeekly.description.isEnabled = false
            barChartWeekly.legend.isEnabled = false
            barChartWeekly.setDrawValueAboveBar(true)
            barChartWeekly.setFitBars(true)
            barChartWeekly.setPinchZoom(false)
            barChartWeekly.setScaleEnabled(false)
            barChartWeekly.setDoubleTapToZoomEnabled(false)

            // X Axis
            barChartWeekly.xAxis.apply {
                valueFormatter = IndexAxisValueFormatter(days)
                granularity = 1f
                isGranularityEnabled = true
                position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                textColor = getColor(R.color.on_surface)
                textSize = 12f
            }

            // Left Axis
            barChartWeekly.axisLeft.apply {
                axisMinimum = 0f
                axisMaximum = maxOf(4.0f, (dailyGoalMl / 1000.0f) + 1.0f)
                textColor = getColor(R.color.on_surface)
                textSize = 12f
                barChartWeekly.axisLeft.removeAllLimitLines()

                // Goal Reference Line
                val goalL = dailyGoalMl / 1000.0f
                val goalLimitLine = LimitLine(goalL, "Goal: ${String.format(Locale.getDefault(), "%.1fL", goalL)}").apply {
                    lineWidth = 2f
                    lineColor = getColor(R.color.secondary)
                    textColor = getColor(R.color.secondary)
                    textSize = 12f
                }
                addLimitLine(goalLimitLine)
            }

            // Right Axis
            barChartWeekly.axisRight.isEnabled = false

            barChartWeekly.invalidate()
        }
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
