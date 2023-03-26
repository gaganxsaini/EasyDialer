package com.example.easydialer

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.easydialer.data.Contact
import com.example.easydialer.data.ContactViewModel
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class AddContactActivity : AppCompatActivity() {
    private lateinit var btnSave : Button
    private lateinit var btnBrowse : Button
    private lateinit var mContactViewModel : ContactViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_contact)
        btnSave = findViewById(R.id.btnSave)
        btnBrowse = findViewById(R.id.btnBrowsePic)

        mContactViewModel = ViewModelProvider(this).get(ContactViewModel::class.java)

        btnSave.setOnClickListener{
            saveFile()
            insertDataToDatabase()
        }

        btnBrowse.setOnClickListener{
            var intent = Intent()
            intent.type = "image/*"
            intent.action = Intent.ACTION_GET_CONTENT
            startActivityForResult(Intent.createChooser(intent, "Choose Image"), 101)
        }
    }

    var fileUri : Uri? = null

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 101 && resultCode == RESULT_OK && data != null) {
            fileUri = data.data

            try {
                val img = findViewById<ImageView>(R.id.imgTemp)
                val imgFile = File(fileUri?.path)

                val inputStream: InputStream = contentResolver.openInputStream(fileUri!!) ?: return
                val bitmap1: Bitmap = BitmapFactory.decodeStream(inputStream)
                img.setImageBitmap(bitmap1)
            } finally {

            }
        }
    }

    private var fileName : String? = null

    private fun saveFile() {
        fileName = fileUri?.let { getFileName(it) }
        insertInPrivateStorage(fileName!!)
    }

    private fun getFileName(uri : Uri) : String? {
        var result : String? = null
        if (uri.scheme.equals("content")) {
            var cursor = contentResolver.query(uri, null, null, null, null)
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    var col_index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (col_index >= 0)
                        result = cursor.getString(col_index)
                }
            } finally {
                cursor?.close()
            }
        }

        if (result == null) {
            var cut = uri.path?.lastIndexOf('/')
            if (cut != null && cut != -1) {
                result = uri.path?.substring(cut + 1)
            }
        }

        return result
    }

    private fun insertInPrivateStorage(name : String) {
        val inputStream: InputStream = contentResolver.openInputStream(fileUri!!) ?: return

        val fos : FileOutputStream = openFileOutput(name, MODE_APPEND)

        inputStream.use { input ->
            fos.use { output ->
                val buffer = ByteArray(4 * 1024) // buffer size
                while (true) {
                    val byteCount = input.read(buffer)
                    if (byteCount < 0) break
                    output.write(buffer, 0, byteCount)
                }
                output.flush()
            }
        }


    }

    private fun insertDataToDatabase(){
        val txtName = findViewById<EditText>(R.id.editTextName)
        val txtPhone = findViewById<EditText>(R.id.editTextPhone)
        val name = txtName.text.toString()
        val phone = txtPhone.text.toString()
        val contact = Contact(0, name, phone, fileName)
        mContactViewModel.addContact(contact)
        Toast.makeText(this, "Successfully added", Toast.LENGTH_LONG).show()
    }
}