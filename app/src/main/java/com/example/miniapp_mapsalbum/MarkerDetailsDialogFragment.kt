package com.example.miniapp_mapsalbum

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

// (Import lainnya sama seperti kode Anda)
import java.io.File

class MarkerDetailsDialogFragment : DialogFragment() {

    // BARU: Listener khusus untuk dialog ini
    interface MarkerDetailsListener {
        fun onEditMemoryClicked(memory: Memory)
    }

    private var listener: MarkerDetailsListener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as? MarkerDetailsListener
        if (listener == null) {
            throw ClassCastException("$context must implement MarkerDetailsListener")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_marker_details, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // (Inisialisasi View sama seperti kode Anda)
        val tvTitle: TextView = view.findViewById(R.id.tv_detail_title)
        val tvLatLng: TextView = view.findViewById(R.id.tv_detail_latlng)
        val ivMemoryImage: ImageView = view.findViewById(R.id.iv_detail_image)
        val btnEdit: Button = view.findViewById(R.id.btn_detail_edit)
        val btnClose: Button = view.findViewById(R.id.btn_detail_close)

        val currentMemory = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arguments?.getParcelable(ARG_MEMORY, Memory::class.java)
        } else {
            @Suppress("DEPRECATION")
            arguments?.getParcelable(ARG_MEMORY)
        } ?: return

        tvTitle.text = currentMemory.title
        tvLatLng.text = "Lat: ${currentMemory.latitude}, Lng: ${currentMemory.longitude}"

        // (Logika rotasi gambar Anda sudah bagus, tidak perlu diubah)
        currentMemory.imagePath?.let { path ->
            val file = File(path)
            if (file.exists()) {
                val bitmap = BitmapFactory.decodeFile(file.absolutePath)

                // Cek orientasi EXIF dan putar Bitmap jika perlu
                try {
                    val exif = ExifInterface(file.absolutePath)
                    val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                    val matrix = android.graphics.Matrix()
                    when (orientation) {
                        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                    }
                    val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                    ivMemoryImage.setImageBitmap(rotatedBitmap)
                } catch (e: Exception) {
                    Log.e("MarkerDetails", "Error rotating image: ${e.message}")
                    ivMemoryImage.setImageBitmap(bitmap)
                }
                ivMemoryImage.visibility = View.VISIBLE
            }
        }

        btnEdit.setOnClickListener {
            listener?.onEditMemoryClicked(currentMemory)
            dismiss()
        }

        btnClose.setOnClickListener { dismiss() }
    }

    companion object {
        private const val ARG_MEMORY = "memory"
        fun newInstance(memory: Memory) = MarkerDetailsDialogFragment().apply {
            arguments = Bundle().apply { putParcelable(ARG_MEMORY, memory) }
        }
    }
}