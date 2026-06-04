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

    var isSelectMode = false
    val selectedItems = mutableSetOf<Int>()
    var onSelectionChanged: (() -> Unit)? = null  // 추가

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

        if (isSelectMode) {
            binding.checkbox.visibility = View.VISIBLE
            binding.tvArrow.visibility = View.GONE
            binding.checkbox.isChecked = selectedItems.contains(record.no)
        } else {
            binding.checkbox.visibility = View.GONE
            binding.tvArrow.visibility = View.VISIBLE
            binding.checkbox.isChecked = false
        }

        binding.root.setOnClickListener {
            if (isSelectMode) {
                toggleSelection(record.no)
            } else {
                onClick(record)
            }
        }

        binding.root.setOnLongClickListener {
            if (!isSelectMode) onLongClick(record)
            true
        }

        binding.checkbox.setOnClickListener {
            toggleSelection(record.no)
        }

        // 코루틴 비동기 이미지 로딩
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

    private fun toggleSelection(no: Int) {
        if (selectedItems.contains(no)) {
            selectedItems.remove(no)
        } else {
            selectedItems.add(no)
        }
        notifyDataSetChanged()
        onSelectionChanged?.invoke()  // 콜백 호출
    }

    override fun getItemCount() = records.size

    fun updateData(newList: List<TravelRecord>) {
        records.clear()
        records.addAll(newList)
        notifyDataSetChanged()
    }

    fun selectAll() {
        records.forEach { selectedItems.add(it.no) }
        notifyDataSetChanged()
        onSelectionChanged?.invoke()
    }

    fun clearSelection() {
        selectedItems.clear()
        notifyDataSetChanged()
        onSelectionChanged?.invoke()
    }

    fun clearSelectMode() {
        isSelectMode = false
        selectedItems.clear()
        notifyDataSetChanged()
    }
}