package com.example.traveldiary

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.addCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.traveldiary.databinding.ActivityMainBinding
import com.example.traveldiary.db.DBHelper
import com.example.traveldiary.fragment.InfoFragment
import com.example.traveldiary.fragment.ListFragment
import com.example.traveldiary.fragment.MapFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val listFragment = ListFragment()
    private val mapFragment = MapFragment()
    private val infoFragment = InfoFragment()
    private var activeFragment: androidx.fragment.app.Fragment = listFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(true)

        onBackPressedDispatcher.addCallback(this) {
            if (supportFragmentManager.backStackEntryCount > 0) {
                supportFragmentManager.popBackStack()
            } else {
                finish()
            }
        }

        // Fragment 초기 설정
        supportFragmentManager.beginTransaction()
            .add(R.id.fragment_container, infoFragment, "info").hide(infoFragment)
            .add(R.id.fragment_container, mapFragment, "map").hide(mapFragment)
            .add(R.id.fragment_container, listFragment, "list")
            .commit()

        // BottomNavigationView
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_list -> {
                    switchFragment(listFragment)
                    supportActionBar?.title = "여행 다이어리"
                    true
                }
                R.id.nav_map -> {
                    switchFragment(mapFragment)
                    supportActionBar?.title = "여행 지도"
                    true
                }
                R.id.nav_info -> {
                    switchFragment(infoFragment)
                    supportActionBar?.title = "앱 정보"
                    true
                }
                else -> false
            }
        }
    }

    private fun switchFragment(fragment: androidx.fragment.app.Fragment) {
        if (fragment == activeFragment) return
        supportFragmentManager.beginTransaction()
            .hide(activeFragment)
            .show(fragment)
            .addToBackStack(null)
            .commit()
        activeFragment = fragment
        invalidateOptionsMenu()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.option_menu, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val isListFragment = activeFragment is ListFragment
        menu.findItem(R.id.menu_sort_date).isVisible = isListFragment
        menu.findItem(R.id.menu_sort_name).isVisible = isListFragment
        menu.findItem(R.id.menu_delete_all).isVisible = isListFragment
        menu.findItem(R.id.menu_app_info).isVisible = true
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_sort_date -> {
                listFragment.loadData("${DBHelper.COL_DATE} DESC")
                true
            }
            R.id.menu_sort_name -> {
                listFragment.loadData("${DBHelper.COL_PLACE} ASC")
                true
            }
            R.id.menu_delete_all -> {
                AlertDialog.Builder(this)
                    .setTitle("전체 삭제")
                    .setMessage("모든 여행 기록을 삭제하시겠습니까?\n이 작업은 되돌릴 수 없습니다.")
                    .setPositiveButton("삭제") { _, _ ->
                        listFragment.deleteAll()
                    }
                    .setNegativeButton("취소", null)
                    .show()
                true
            }
            R.id.menu_app_info -> {
                binding.bottomNav.selectedItemId = R.id.nav_info
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}