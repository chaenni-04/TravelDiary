package com.example.traveldiary.adapter

import android.graphics.BitmapFactory
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.traveldiary.databinding.ItemTravelBinding
import com.example.traveldiary.model.TravelRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TravelAdapter(
    private val records: MutableList<TravelRecord>,
    private val onClick: (TravelRecord) -> Unit,
    private val onLongClick: (TravelRecord) -> Unit
) : RecyclerView.Adapter<TravelAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemTravelBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTravelBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val record = records[position]
        val binding = holder.binding

        binding.tvPlace.text = record.place
        binding.tvDate.text = record.visitDate
        binding.tvMemoPreview.text = record.memo.ifEmpty { "메모 없음" }

        // 클릭
        binding.root.setOnClickListener { onClick(record) }

        // 롱클릭 (AlertDialog 방식 컨텍스트 메뉴)
        binding.root.setOnLongClickListener {
            onLongClick(record)
            true
        }

        // 사진이 있으면 코루틴으로 비동기 로딩 (가산점)
        if (record.photoUri.isNotEmpty()) {
            binding.layoutNoPhoto.visibility = View.GONE
            binding.progressBar.visibility = View.VISIBLE
            binding.ivThumbnail.visibility = View.INVISIBLE

            CoroutineScope(Dispatchers.IO).launch {
                val bitmap = try {
                    val context = holder.itemView.context
                    context.contentResolver
                        .openInputStream(Uri.parse(record.photoUri))
                        ?.use { stream -> BitmapFactory.decodeStream(stream) }
                } catch (e: Exception) {
                    null
                }

                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                    binding.ivThumbnail.visibility = View.VISIBLE
                    if (bitmap != null) {
                        binding.ivThumbnail.setImageBitmap(bitmap)
                    } else {
                        binding.layoutNoPhoto.visibility = View.VISIBLE
                        binding.ivThumbnail.visibility = View.GONE
                    }
                }
            }
        } else {
            binding.progressBar.visibility = View.GONE
            binding.ivThumbnail.visibility = View.GONE
            binding.layoutNoPhoto.visibility = View.VISIBLE
        }
    }

    override fun getItemCount() = records.size

    fun updateData(newList: List<TravelRecord>) {
        records.clear()
        records.addAll(newList)
        notifyDataSetChanged()
    }
}