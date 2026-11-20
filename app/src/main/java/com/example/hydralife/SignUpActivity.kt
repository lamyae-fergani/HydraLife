package com.example.hydralife

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.hydralife.data.UserSessionManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.util.Calendar

class SignUpActivity : AppCompatActivity() {

    private lateinit var tilSignUpEmail: TextInputLayout
    private lateinit var tilDob: TextInputLayout
    private lateinit var tilSignUpPassword: TextInputLayout
    private lateinit var tilConfirmPassword: TextInputLayout
    private lateinit var etSignUpEmail: TextInputEditText
    private lateinit var etDob: TextInputEditText
    private lateinit var etSignUpPassword: TextInputEditText
    private lateinit var etConfirmPassword: TextInputEditText
    private lateinit var btnSignUp: MaterialButton
    private lateinit var tvGoToLogin: TextView

    private var selectedDobCalendar: Calendar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        tilSignUpEmail = findViewById(R.id.tilSignUpEmail)
        tilDob = findViewById(R.id.tilDob)
        tilSignUpPassword = findViewById(R.id.tilSignUpPassword)
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword)
        etSignUpEmail = findViewById(R.id.etSignUpEmail)
        etDob = findViewById(R.id.etDob)
        etSignUpPassword = findViewById(R.id.etSignUpPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        btnSignUp = findViewById(R.id.btnSignUp)
        tvGoToLogin = findViewById(R.id.tvGoToLogin)

        etDob.setOnClickListener {
            showDatePickerDialog()
        }

        btnSignUp.setOnClickListener {
            if (validateForm()) {
                val email = etSignUpEmail.text.toString().trim()
                val name = email.substringBefore("@").replaceFirstChar { it.uppercase() }
                UserSessionManager(this).saveUser(name, email)

                Toast.makeText(this, getString(R.string.signup_success), Toast.LENGTH_SHORT).show()
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            }
        }

        tvGoToLogin.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
            val dobCal = Calendar.getInstance().apply {
                set(selectedYear, selectedMonth, selectedDay)
            }
            selectedDobCalendar = dobCal
            val formattedDate = String.format("%04d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay)
            etDob.setText(formattedDate)
            tilDob.error = null
        }, year - 20, month, day)

        datePickerDialog.datePicker.maxDate = System.currentTimeMillis()
        datePickerDialog.show()
    }

    private fun validateForm(): Boolean {
        var isValid = true

        val email = etSignUpEmail.text.toString().trim()
        val dobString = etDob.text.toString().trim()
        val password = etSignUpPassword.text.toString().trim()
        val confirmPassword = etConfirmPassword.text.toString().trim()

        if (email.isEmpty()) {
            tilSignUpEmail.error = getString(R.string.email_required)
            isValid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilSignUpEmail.error = getString(R.string.invalid_email)
            isValid = false
        } else {
            tilSignUpEmail.error = null
        }

        if (dobString.isEmpty()) {
            tilDob.error = getString(R.string.dob_required)
            isValid = false
        } else if (selectedDobCalendar != null) {
            val today = Calendar.getInstance()
            val age = today.get(Calendar.YEAR) - selectedDobCalendar!!.get(Calendar.YEAR)
            val hasBirthdayPassed = if (today.get(Calendar.YEAR) == selectedDobCalendar!!.get(Calendar.YEAR)) {
                false
            } else {
                today.get(Calendar.DAY_OF_YEAR) >= selectedDobCalendar!!.get(Calendar.DAY_OF_YEAR)
            }
            val actualAge = if (hasBirthdayPassed) age else age - 1

            if (actualAge < 18) {
                tilDob.error = getString(R.string.underage_error)
                isValid = false
            } else {
                tilDob.error = null
            }
        } else {
            tilDob.error = null
        }

        if (password.isEmpty()) {
            tilSignUpPassword.error = getString(R.string.password_required)
            isValid = false
        } else if (password.length < 6) {
            tilSignUpPassword.error = getString(R.string.password_min_length)
            isValid = false
        } else {
            tilSignUpPassword.error = null
        }

        if (confirmPassword.isEmpty()) {
            tilConfirmPassword.error = getString(R.string.confirm_password_required)
            isValid = false
        } else if (confirmPassword != password) {
            tilConfirmPassword.error = getString(R.string.passwords_dont_match)
            isValid = false
        } else {
            tilConfirmPassword.error = null
        }

        return isValid
    }
}
