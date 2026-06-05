package com.example.traveldiary.adapter

import android.graphics.BitmapFactory
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
import androidx.core.net.toUri

class TravelAdapter(
    private val records: MutableList<TravelRecord>,
    private val onClick: (TravelRecord) -> Unit,
    private val onLongClick: (TravelRecord) -> Unit
) : RecyclerView.Adapter<TravelAdapter.ViewHolder>() {

    var isSelectMode = false
    val selectedItems = mutableSetOf<Int>()
    var onSelectionChanged: (() -> Unit)? = null

    class ViewHolder(val binding: ItemTravelBinding) :
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

        // 고정 아이콘 표시
        binding.tvPinIcon.visibility =
            if (record.isPinned == 1) View.VISIBLE else View.GONE

        // 선택 모드 UI
        if (isSelectMode) {
            binding.checkbox.visibility = View.VISIBLE
            binding.tvArrow.visibility = View.GONE
            binding.checkbox.isChecked = selectedItems.contains(record.no)
        } else {
            binding.checkbox.visibility = View.GONE
            binding.tvArrow.visibility = View.VISIBLE
            binding.checkbox.isChecked = false
        }

        // 클릭
        binding.root.setOnClickListener {
            if (isSelectMode) {
                toggleSelection(position, record.no)
            } else {
                onClick(record)
            }
        }

        // 롱클릭
        binding.root.setOnLongClickListener {
            if (!isSelectMode) onLongClick(record)
            true
        }

        // 체크박스 클릭
        binding.checkbox.setOnClickListener {
            toggleSelection(position, record.no)
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
                        .openInputStream(record.photoUri.toUri())
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

    // 체크박스 토글
    private fun toggleSelection(position: Int, no: Int) {
        if (selectedItems.contains(no)) {
            selectedItems.remove(no)
        } else {
            selectedItems.add(no)
        }
        notifyItemChanged(position)
        onSelectionChanged?.invoke()
    }

    override fun getItemCount() = records.size

    // 전체 데이터 교체
    fun updateData(newList: List<TravelRecord>) {
        val oldSize = records.size
        records.clear()
        records.addAll(newList)
        val newSize = records.size

        when {
            oldSize == newSize -> notifyItemRangeChanged(0, newSize)
            oldSize < newSize -> {
                notifyItemRangeChanged(0, oldSize)
                notifyItemRangeInserted(oldSize, newSize - oldSize)
            }
            else -> {
                notifyItemRangeChanged(0, newSize)
                notifyItemRangeRemoved(newSize, oldSize - newSize)
            }
        }
    }

    // 전체 선택
    fun selectAll() {
        records.forEach { selectedItems.add(it.no) }
        notifyItemRangeChanged(0, records.size)
        onSelectionChanged?.invoke()
    }

    // 전체 해제
    fun clearSelection() {
        selectedItems.clear()
        notifyItemRangeChanged(0, records.size)
        onSelectionChanged?.invoke()
    }

    // 선택 모드 종료
    fun clearSelectMode() {
        isSelectMode = false
        selectedItems.clear()
        notifyItemRangeChanged(0, records.size)
    }
}