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
                val intent = Intent(requireContext(), DetailActivity::class.java)
                intent.putExtra("record_id", record.no)
                startActivity(intent)
            },
            onLongClick = { record ->
                showContextMenu(record)
            }
        )

        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())

        binding.fabAdd.setOnClickListener {
            val intent = Intent(requireContext(), AddEditActivity::class.java)
            startActivity(intent)
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

        // 빈 목록 처리
        if (records.isEmpty()) {
            binding.layoutEmpty.visibility = View.VISIBLE
            binding.recyclerView.visibility = View.GONE
        } else {
            binding.layoutEmpty.visibility = View.GONE
            binding.recyclerView.visibility = View.VISIBLE
        }
    }

    // AlertDialog 방식 컨텍스트 메뉴
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

    private fun showDeleteDialog(record: TravelRecord) {
        AlertDialog.Builder(requireContext())
            .setTitle("삭제 확인")
            .setMessage("'${record.place}' 여행 기록을\n삭제하시겠습니까?")
            .setPositiveButton("삭제") { _, _ ->
                dbHelper.delete(record.no)
                loadData()
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