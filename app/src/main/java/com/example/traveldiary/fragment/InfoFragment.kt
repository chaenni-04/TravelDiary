package com.example.traveldiary.fragment

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import com.example.traveldiary.databinding.FragmentInfoBinding
import com.example.traveldiary.db.DBHelper

class InfoFragment : Fragment() {

    private var _binding: FragmentInfoBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateStats()
    }

    override fun onResume() {
        super.onResume()
        updateStats()
    }

    private fun updateStats() {
        val dbHelper = DBHelper(requireContext())
        val allRecords = dbHelper.getAll()
        binding.tvTotalCount.text = allRecords.size.toString()
        binding.tvPhotoCount.text = allRecords.count { it.photoUri.isNotEmpty() }.toString()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}