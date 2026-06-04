package com.example.traveldiary.fragment

import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.traveldiary.AddEditActivity
import com.example.traveldiary.DetailActivity
import com.example.traveldiary.adapter.TravelAdapter
import com.example.traveldiary.databinding.FragmentListBinding
import com.example.traveldiary.db.DBHelper
import com.example.traveldiary.model.TravelRecord
import android.widget.Toast

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

        // 선택 개수 업데이트 콜백
        adapter.onSelectionChanged = {
            updateSelectCount()
        }

        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // FAB
        binding.fabAdd.setOnClickListener {
            val intent = Intent(requireContext(), AddEditActivity::class.java)
            startActivity(intent)
        }

        // 전체 선택 텍스트
        binding.tvSelectAll.setOnClickListener {
            if (adapter.selectedItems.size == records.size) {
                // 전체 선택 상태면 전체 해제
                adapter.clearSelection()
            } else {
                adapter.selectAll()
            }
            updateSelectCount()
        }

        // 취소 텍스트
        binding.tvCancelSelect.setOnClickListener {
            exitSelectMode()
        }

        // 선택 항목 삭제 버튼
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

    // 선택 개수 업데이트
    private fun updateSelectCount() {
        val count = adapter.selectedItems.size
        binding.tvSelectCount.text = "${count}개 선택됨"

        // 전체 선택 텍스트 변경
        if (count == records.size && records.isNotEmpty()) {
            binding.tvSelectAll.text = "전체 해제"
        } else {
            binding.tvSelectAll.text = "전체 선택"
        }
    }

    // 선택 삭제 모드 진입
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

    // 선택 삭제 모드 종료
    fun exitSelectMode() {
        adapter.clearSelectMode()
        binding.layoutSelectTopBar.visibility = View.GONE
        binding.layoutSelectMode.visibility = View.GONE
        binding.fabAdd.visibility = View.VISIBLE
    }

    fun isSelectMode() = adapter.isSelectMode

    private fun showContextMenu(record: TravelRecord) {
        AlertDialog.Builder(requireContext())
            .setTitle("📍 ${record.place}")
            .setItems(arrayOf("✏️  수정", "🗑️  삭제")) { _, which ->
                when (which) {
                    0 -> {
                        val intent = Intent(requireContext(), AddEditActivity::class.java)
                        intent.putExtra("record_id", record.no)
                        startActivity(intent)
                    }
                    1 -> showDeleteDialog(record)
                }
            }
            .show()
    }

    private fun updateInfoFragment() {
        val infoFragment = parentFragmentManager
            .findFragmentByTag("info") as? InfoFragment
        infoFragment?.updateStats()
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

    fun deleteAll() {
        dbHelper.deleteAll()
        loadData()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}