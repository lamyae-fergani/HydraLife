package com.example.hydralife

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.example.hydralife.data.UserSessionManager
import com.google.android.material.button.MaterialButton

class OnboardingActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var btnNext: MaterialButton
    private lateinit var btnSkip: Button
    private lateinit var btnLogIn: Button
    private lateinit var layoutDots: LinearLayout

    private val pages = listOf(
        OnboardingPage(
            R.string.onboard_page1_title,
            R.string.onboard_page1_desc,
            R.drawable.ic_onboard_track
        ),
        OnboardingPage(
            R.string.onboard_page2_title,
            R.string.onboard_page2_desc,
            R.drawable.ic_onboard_goal
        ),
        OnboardingPage(
            R.string.onboard_page3_title,
            R.string.onboard_page3_desc,
            R.drawable.ic_onboard_ai
        )
    )

    private lateinit var dots: Array<ImageView?>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        viewPager = findViewById(R.id.viewPagerOnboarding)
        btnNext = findViewById(R.id.btnNext)
        btnSkip = findViewById(R.id.btnSkip)
        btnLogIn = findViewById(R.id.btnLogIn)
        layoutDots = findViewById(R.id.layoutDots)

        val adapter = OnboardingPagerAdapter(pages)
        viewPager.adapter = adapter

        setupDots(0)

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateDots(position)
                if (position == pages.size - 1) {
                    btnNext.text = getString(R.string.onboard_get_started)
                } else {
                    btnNext.text = getString(R.string.onboard_continue)
                }
            }
        })

        btnNext.setOnClickListener {
            val current = viewPager.currentItem
            if (current < pages.size - 1) {
                viewPager.currentItem = current + 1
            } else {
                navigateToAuth()
            }
        }

        btnSkip.setOnClickListener {
            navigateToAuth()
        }

        btnLogIn.setOnClickListener {
            navigateToAuth()
        }
    }

    private fun setupDots(currentPage: Int) {
        layoutDots.removeAllViews()
        dots = arrayOfNulls(pages.size)
        for (i in pages.indices) {
            dots[i] = ImageView(this).apply {
                setImageResource(if (i == currentPage) R.drawable.dot_active else R.drawable.dot_inactive)
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(8, 0, 8, 0)
                }
                layoutParams = params
            }
            layoutDots.addView(dots[i])
        }
    }

    private fun updateDots(currentPage: Int) {
        for (i in pages.indices) {
            dots[i]?.setImageResource(if (i == currentPage) R.drawable.dot_active else R.drawable.dot_inactive)
        }
    }

    private fun navigateToAuth() {
        val sessionManager = UserSessionManager(this)
        sessionManager.setOnboardingCompleted(true)
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }
}
