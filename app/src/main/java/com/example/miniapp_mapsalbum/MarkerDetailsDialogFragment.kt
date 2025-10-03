package com.example.miniapp_mapsalbum // Sesuaikan dengan package Anda

import android.app.Dialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.exifinterface.media.ExifInterface
import androidx.fragment.app.DialogFragment
import com.example.miniapp_mapsalbum.data.Memory
import java.io.File

class MarkerDetailsDialogFragment : DialogFragment() {

    private var memory: Memory? = null
    private lateinit var tvTitle: TextView
    private lateinit var tvLatLng: TextView
    private lateinit var ivMemoryImage: ImageView
    private lateinit var btnEdit: Button
    private lateinit var btnClose: Button

    private var editListener: EditMarkerInfoDialogFragment.EditMarkerInfoListener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            editListener = context as EditMarkerInfoDialogFragment.EditMarkerInfoListener
        } catch (e: ClassCastException) {
            // Biarkan saja, tombol Edit akan menangani jika listener null
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        return dialog
    }

    // BARU: Tambahkan onStart untuk mengatur ukuran dialog
    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_marker_details, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val currentMemory = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arguments?.getParcelable(ARG_MEMORY, Memory::class.java)
        } else {
            @Suppress("DEPRECATION")
            arguments?.getParcelable(ARG_MEMORY)
        }

        if (currentMemory == null) {
            Toast.makeText(context, "Data kenangan tidak ditemukan.", Toast.LENGTH_SHORT).show()
            dismiss()
            return
        }

        this.memory = currentMemory

        // Inisialisasi View
        tvTitle = view.findViewById(R.id.tv_detail_title)
        tvLatLng = view.findViewById(R.id.tv_detail_latlng)
        ivMemoryImage = view.findViewById(R.id.iv_detail_image)
        btnEdit = view.findViewById(R.id.btn_detail_edit)
        btnClose = view.findViewById(R.id.btn_detail_close)

        tvTitle.text = currentMemory.title
        tvLatLng.text = "Lat: ${currentMemory.latitude}, Lng: ${currentMemory.longitude}"

        // MODIFIKASI DIMULAI DI SINI: Logika rotasi gambar ditambahkan
        currentMemory.imagePath?.let { path ->
            val file = File(path)
            if (file.exists()) {
                val bitmap = BitmapFactory.decodeFile(file.absolutePath)

                // Cek orientasi EXIF dan putar Bitmap jika perlu
                try {
                    val exif = ExifInterface(file.absolutePath)
                    val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                    val matrix = Matrix()
                    when (orientation) {
                        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                    }
                    val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                    ivMemoryImage.setImageBitmap(rotatedBitmap)
                } catch (e: Exception) {
                    Log.e("MarkerDetails", "Error rotating image: ${e.message}")
                    ivMemoryImage.setImageBitmap(bitmap) // Tampilkan tanpa rotasi jika gagal
                }

                ivMemoryImage.visibility = View.VISIBLE
            } else {
                ivMemoryImage.visibility = View.GONE
            }
        } ?: run { ivMemoryImage.visibility = View.GONE }
        // MODIFIKASI SELESAI

        btnEdit.setOnClickListener {
            dismiss() // Tutup dialog details
            // Buka dialog edit dengan data dari currentMemory
            val dialog = EditMarkerInfoDialogFragment.newInstance(currentMemory)
            dialog.show(parentFragmentManager, "EditMarkerInfoDialog")
        }

        btnClose.setOnClickListener {
            dismiss()
        }
    }

    companion object {
        private const val ARG_MEMORY = "memory"

        fun newInstance(memory: Memory): MarkerDetailsDialogFragment {
            val fragment = MarkerDetailsDialogFragment()
            val args = Bundle().apply {
                putParcelable(ARG_MEMORY, memory)
            }
            fragment.arguments = args
            return fragment
        }
    }
}