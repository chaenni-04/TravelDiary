package com.example.traveldiary.fragment

import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.exifinterface.media.ExifInterface
import androidx.fragment.app.Fragment
import com.example.traveldiary.R
import com.example.traveldiary.databinding.FragmentMapBinding
import com.example.traveldiary.db.DBHelper
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class MapFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!
    private var googleMap: GoogleMap? = null
    private lateinit var dbHelper: DBHelper
    private var isMapReady = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dbHelper = DBHelper(requireContext())

        val mapFragment = SupportMapFragment.newInstance()
        childFragmentManager.beginTransaction()
            .replace(R.id.map_container, mapFragment)
            .commit()
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        isMapReady = true

        val korea = LatLng(36.5, 127.5)
        googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(korea, 7f))

        loadMarkers()
    }

    override fun onResume() {
        super.onResume()
        // 지도 준비됐을 때만 호출
        if (isMapReady) {
            loadMarkers()
        }
    }

    private fun loadMarkers() {
        try {
            googleMap?.clear()
            val records = dbHelper.getAll()
            var markerCount = 0

            for (record in records) {
                if (record.photoUri.isNotEmpty()) {
                    val latLng = getGpsFromPhoto(record.photoUri)
                    if (latLng != null) {
                        googleMap?.addMarker(
                            MarkerOptions()
                                .position(latLng)
                                .title(record.place)
                                .snippet(record.visitDate)
                                .icon(
                                    BitmapDescriptorFactory.defaultMarker(
                                        BitmapDescriptorFactory.HUE_GREEN
                                    )
                                )
                        )
                        markerCount++
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getGpsFromPhoto(photoUri: String): LatLng? {
        return try {
            val uri = Uri.parse(photoUri)
            val stream = requireContext().contentResolver
                .openInputStream(uri) ?: return null
            val exif = ExifInterface(stream)
            val latLong = FloatArray(2)
            val hasLatLong = exif.getLatLong(latLong)
            stream.close()

            if (hasLatLong) {
                LatLng(latLong[0].toDouble(), latLong[1].toDouble())
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}