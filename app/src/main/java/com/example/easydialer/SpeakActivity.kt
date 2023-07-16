package com.example.easydialer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.speech.RecognizerIntent
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.example.easydialer.data.ContactViewModel


class SpeakActivity : AppCompatActivity() {

    private lateinit var mContactViewModel: ContactViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_speak)

        val btn = findViewById<Button>(R.id.btnSpeak2)
        btn.setOnClickListener {
            startVoiceRecognitionActivity()
        }

        mContactViewModel = ViewModelProvider(this).get(ContactViewModel::class.java)
    }

    val VOICE_RECOGNITION_REQUEST_CODE = 1234

    fun startVoiceRecognitionActivity() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )
        intent.putExtra(
            RecognizerIntent.EXTRA_PROMPT,
            "Speech recognition demo"
        )
        startActivityForResult(intent, VOICE_RECOGNITION_REQUEST_CODE)

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == VOICE_RECOGNITION_REQUEST_CODE && resultCode == RESULT_OK) {
            mContactViewModel.readAllData
            val matches = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)

            if (matches != null) {
                for (x in mContactViewModel.readAllData.value!!)
                {
                    if (matches.contains(x.name)) {
                        val callIntent = Intent(Intent.ACTION_CALL)
                        callIntent.data = Uri.parse("tel:" + x.phone)
                        ContextCompat.startActivity(this, callIntent, null)
                        break
                    }
                }
            }
        }
    }

}