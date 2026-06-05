package com.example.traveldiary.fragment

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.traveldiary.AddEditActivity
import com.example.traveldiary.DetailActivity
import com.example.traveldiary.adapter.TravelAdapter
import com.example.traveldiary.databinding.FragmentListBinding
import com.example.traveldiary.db.DBHelper
import com.example.traveldiary.model.TravelRecord

class ListFragment : Fragment() {

    private var _binding: FragmentListBinding? = null
    private val binding get() = _binding!!

    private lateinit var dbHelper: DBHelper
    private lateinit var adapter: TravelAdapter
    private val records = mutableListOf<TravelRecord>()

    var currentOrderBy = "${DBHelper.COL_NO} DESC"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dbHelper = DBHelper(requireContext())

        adapter = TravelAdapter(
            records = records,
            onClick = { record ->
                if (!adapter.isSelectMode) {
                    val intent = Intent(requireContext(), DetailActivity::class.java)
                    intent.putExtra("record_id", record.no)
                    startActivity(intent)
                }
            },
            onLongClick = { record ->
                showContextMenu(record)
            }
        )

        adapter.onSelectionChanged = {
            updateSelectCount()
        }

        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())

        binding.fabAdd.setOnClickListener {
            val intent = Intent(requireContext(), AddEditActivity::class.java)
            startActivity(intent)
        }

        binding.tvSelectAll.setOnClickListener {
            if (adapter.selectedItems.size == records.size) {
                adapter.clearSelection()
            } else {
                adapter.selectAll()
            }
            updateSelectCount()
        }

        binding.tvCancelSelect.setOnClickListener {
            exitSelectMode()
        }

        binding.btnDeleteSelected.setOnClickListener {
            if (adapter.selectedItems.isEmpty()) {
                AlertDialog.Builder(requireContext())
                    .setTitle("알림")
                    .setMessage("삭제할 항목을 선택해주세요.")
                    .setPositiveButton("확인", null)
                    .show()
                return@setOnClickListener
            }

            AlertDialog.Builder(requireContext())
                .setTitle("선택 삭제")
                .setMessage("선택한 ${adapter.selectedItems.size}개의 여행 기록을\n삭제하시겠습니까?")
                .setPositiveButton("삭제") { _, _ ->
                    adapter.selectedItems.forEach { no ->
                        dbHelper.delete(no)
                    }
                    exitSelectMode()
                    loadData()
                    updateInfoFragment()
                    Toast.makeText(requireContext(), "삭제되었습니다", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("취소", null)
                .show()
        }

        loadData()
    }

    override fun onResume() {
        super.onResume()
        loadData()
    }

    fun loadData(orderBy: String = currentOrderBy) {
        currentOrderBy = orderBy
        val list = dbHelper.getAll(orderBy)
        records.clear()
        records.addAll(list)
        adapter.notifyDataSetChanged()

        if (records.isEmpty()) {
            binding.layoutEmpty.visibility = View.VISIBLE
            binding.recyclerView.visibility = View.GONE
        } else {
            binding.layoutEmpty.visibility = View.GONE
            binding.recyclerView.visibility = View.VISIBLE
        }
    }

    private fun updateSelectCount() {
        val count = adapter.selectedItems.size
        binding.tvSelectCount.text = "${count}개 선택됨"
        if (count == records.size && records.isNotEmpty()) {
            binding.tvSelectAll.text = "전체 해제"
        } else {
            binding.tvSelectAll.text = "전체 선택"
        }
    }

    fun enterSelectMode() {
        adapter.isSelectMode = true
        adapter.selectedItems.clear()
        adapter.notifyDataSetChanged()
        binding.layoutSelectTopBar.visibility = View.VISIBLE
        binding.layoutSelectMode.visibility = View.VISIBLE
        binding.fabAdd.visibility = View.GONE
        binding.tvSelectCount.text = "0개 선택됨"
        binding.tvSelectAll.text = "전체 선택"
    }

    fun exitSelectMode() {
        adapter.clearSelectMode()
        binding.layoutSelectTopBar.visibility = View.GONE
        binding.layoutSelectMode.visibility = View.GONE
        binding.fabAdd.visibility = View.VISIBLE
    }

    fun isSelectMode() = adapter.isSelectMode

    // 롱클릭 컨텍스트 메뉴
    private fun showContextMenu(record: TravelRecord) {
        // 고정 상태에 따라 메뉴 텍스트 변경
        val pinText = if (record.isPinned == 1) "상단 고정 해제" else "상단 고정"

        AlertDialog.Builder(requireContext())
            .setTitle("📍 ${record.place}")
            .setItems(arrayOf(pinText, "메모 복사")) { _, which ->
                when (which) {
                    0 -> togglePin(record)
                    1 -> copyMemo(record)
                }
            }
            .show()
    }

    // 상단 고정 / 해제
    private fun togglePin(record: TravelRecord) {
        val newPinState = if (record.isPinned == 1) 0 else 1
        dbHelper.updatePin(record.no, newPinState)
        loadData()
        val message = if (newPinState == 1) "상단에 고정되었습니다" else "고정이 해제되었습니다"
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    // 메모 복사
    private fun copyMemo(record: TravelRecord) {
        try {
            val clipboard = requireContext()
                .getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val text = "여행지명: ${record.place}\n날짜: ${record.visitDate}\n메모: ${record.memo}"
            val clip = ClipData.newPlainText("여행 메모", text)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(requireContext(), "클립보드에 복사되었습니다", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "복사 중 오류가 발생했습니다", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showDeleteDialog(record: TravelRecord) {
        AlertDialog.Builder(requireContext())
            .setTitle("삭제 확인")
            .setMessage("'${record.place}' 여행 기록을\n삭제하시겠습니까?")
            .setPositiveButton("삭제") { _, _ ->
                dbHelper.delete(record.no)
                loadData()
                updateInfoFragment()
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun updateInfoFragment() {
        val infoFragment = parentFragmentManager
            .findFragmentByTag("info") as? InfoFragment
        infoFragment?.updateStats()
    }

    fun deleteAll() {
        dbHelper.deleteAll()
        loadData()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}