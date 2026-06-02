package com.example.traveldiary

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.traveldiary.databinding.ActivityDetailBinding
import com.example.traveldiary.db.DBHelper
import com.example.traveldiary.model.TravelRecord

class DetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetailBinding
    private lateinit var dbHelper: DBHelper
    private var currentRecord: TravelRecord? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbHelper = DBHelper(this)

        // 툴바 설정
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val recordId = intent.getIntExtra("record_id", -1)
        if (recordId == -1) {
            finish()
            return
        }

        loadRecord(recordId)

        // 수정 버튼
        binding.btnEdit.setOnClickListener {
            val intent = Intent(this, AddEditActivity::class.java)
            intent.putExtra("record_id", currentRecord?.no ?: -1)
            startActivity(intent)
        }

        // 삭제 버튼
        binding.btnDelete.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("삭제 확인")
                .setMessage("'${currentRecord?.place}' 여행 기록을\n삭제하시겠습니까?")
                .setPositiveButton("삭제") { _, _ ->
                    currentRecord?.let { dbHelper.delete(it.no) }
                    Toast.makeText(this, "삭제되었습니다", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .setNegativeButton("취소", null)
                .show()
        }
    }

    override fun onResume() {
        super.onResume()
        currentRecord?.let { loadRecord(it.no) }
    }

    private fun loadRecord(id: Int) {
        currentRecord = dbHelper.getById(id) ?: run {
            finish()
            return
        }
        val record = currentRecord!!

        supportActionBar?.title = record.place
        binding.tvDetailPlace.text = record.place
        binding.tvDetailDate.text = record.visitDate
        binding.tvDetailMemo.text = record.memo.ifEmpty { "메모가 없습니다." }

        // 사진 로드
        if (record.photoUri.isNotEmpty()) {
            try {
                val stream = contentResolver.openInputStream(Uri.parse(record.photoUri))
                val bitmap = BitmapFactory.decodeStream(stream)
                binding.ivDetailPhoto.setImageBitmap(bitmap)
                binding.ivDetailPhoto.visibility = View.VISIBLE
                binding.layoutNoPhotoDetail.visibility = View.GONE
            } catch (e: Exception) {
                binding.ivDetailPhoto.visibility = View.GONE
                binding.layoutNoPhotoDetail.visibility = View.VISIBLE
            }
        } else {
            binding.ivDetailPhoto.visibility = View.GONE
            binding.layoutNoPhotoDetail.visibility = View.VISIBLE
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