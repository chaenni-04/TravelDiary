package com.example.traveldiary

import android.Manifest
import android.app.DatePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.traveldiary.databinding.ActivityAddEditBinding
import com.example.traveldiary.db.DBHelper
import com.example.traveldiary.model.TravelRecord
import java.io.File
import java.util.Calendar

class AddEditActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddEditBinding
    private lateinit var dbHelper: DBHelper

    private var selectedPhotoUri: String = ""
    private var cameraImageUri: Uri? = null
    private var editRecordId: Int = -1

    // 카메라 런처
    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                selectedPhotoUri = cameraImageUri?.toString() ?: ""
                showPreview()
            } else {
                Toast.makeText(this, "사진 촬영이 취소되었습니다", Toast.LENGTH_SHORT).show()
            }
        }

    // 갤러리 런처
    private val galleryLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let {
                try {
                    contentResolver.takePersistableUriPermission(
                        it, Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (e: Exception) { }
                selectedPhotoUri = it.toString()
                showPreview()
            } ?: Toast.makeText(this, "사진 선택이 취소되었습니다", Toast.LENGTH_SHORT).show()
        }

    // 권한 요청 런처
    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                launchCamera()
            } else {
                Toast.makeText(
                    this,
                    "카메라 권한이 거부되었습니다\n설정에서 권한을 허용해주세요",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbHelper = DBHelper(this)

        // 툴바 설정
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // 수정 모드 확인
        editRecordId = intent.getIntExtra("record_id", -1)
        if (editRecordId != -1) {
            supportActionBar?.title = "여행 기록 수정"
            loadRecord(editRecordId)
        } else {
            supportActionBar?.title = "새 여행 기록"
        }

        // 날짜 선택
        binding.etDate.setOnClickListener { showDatePicker() }

        // 사진 선택 버튼
        binding.btnPickImage.setOnClickListener { showImagePickerDialog() }

        // 저장 버튼
        binding.btnSave.setOnClickListener { saveRecord() }
    }

    private fun loadRecord(id: Int) {
        try {
            val record = dbHelper.getById(id)
            if (record != null) {
                binding.etPlace.setText(record.place)
                binding.etDate.setText(record.visitDate)
                binding.etMemo.setText(record.memo)
                selectedPhotoUri = record.photoUri
                if (selectedPhotoUri.isNotEmpty()) showPreview()
            } else {
                Toast.makeText(this, "기록을 불러올 수 없습니다", Toast.LENGTH_SHORT).show()
                finish()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "오류가 발생했습니다", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun showDatePicker() {
        try {
            val cal = Calendar.getInstance()
            DatePickerDialog(
                this,
                { _, year, month, day ->
                    binding.etDate.setText(
                        String.format("%04d-%02d-%02d", year, month + 1, day)
                    )
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        } catch (e: Exception) {
            Toast.makeText(this, "날짜 선택 중 오류가 발생했습니다", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showImagePickerDialog() {
        AlertDialog.Builder(this)
            .setTitle("사진 선택")
            .setItems(arrayOf("📷  카메라로 촬영", "🖼️  갤러리에서 선택")) { _, which ->
                when (which) {
                    0 -> checkCameraPermission()
                    1 -> galleryLauncher.launch(arrayOf("image/*"))
                }
            }
            .show()
    }

    private fun checkCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED -> launchCamera()
            else -> permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun launchCamera() {
        try {
            val photoFile = File(
                getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                "photo_${System.currentTimeMillis()}.jpg"
            )
            cameraImageUri = FileProvider.getUriForFile(
                this, "${packageName}.fileprovider", photoFile
            )
            cameraImageUri?.let {
                cameraLauncher.launch(it)
            } ?: Toast.makeText(this, "카메라를 실행할 수 없습니다", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "카메라 실행 중 오류가 발생했습니다", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showPreview() {
        try {
            val stream = contentResolver.openInputStream(Uri.parse(selectedPhotoUri))
            val bitmap = BitmapFactory.decodeStream(stream)
            if (bitmap != null) {
                binding.ivPreview.setImageBitmap(bitmap)
                binding.ivPreview.visibility = View.VISIBLE
                binding.layoutPhotoPlaceholder.visibility = View.GONE
            } else {
                Toast.makeText(this, "이미지를 불러올 수 없습니다", Toast.LENGTH_SHORT).show()
                selectedPhotoUri = ""
            }
        } catch (e: Exception) {
            Toast.makeText(this, "이미지를 불러올 수 없습니다", Toast.LENGTH_SHORT).show()
            selectedPhotoUri = ""
        }
    }

    private fun saveRecord() {
        try {
            val place = binding.etPlace.text.toString().trim()
            val date = binding.etDate.text.toString().trim()
            val memo = binding.etMemo.text.toString().trim()

            if (place.isEmpty()) {
                binding.etPlace.error = "여행지명을 입력하세요"
                binding.etPlace.requestFocus()
                return
            }
            if (date.isEmpty()) {
                Toast.makeText(this, "날짜를 선택하세요", Toast.LENGTH_SHORT).show()
                return
            }

            val record = TravelRecord(
                no = if (editRecordId != -1) editRecordId else 0,
                place = place,
                visitDate = date,
                memo = memo,
                photoUri = selectedPhotoUri
            )

            if (editRecordId != -1) {
                dbHelper.update(record)
                Toast.makeText(this, "수정되었습니다", Toast.LENGTH_SHORT).show()
            } else {
                dbHelper.insert(record)
                Toast.makeText(this, "저장되었습니다", Toast.LENGTH_SHORT).show()
            }

            finish()

        } catch (e: Exception) {
            Toast.makeText(this, "저장 중 오류가 발생했습니다", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}