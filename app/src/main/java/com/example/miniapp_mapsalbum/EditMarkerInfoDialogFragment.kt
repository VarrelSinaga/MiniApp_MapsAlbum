package com.example.miniapp_mapsalbum

import android.app.Dialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.exifinterface.media.ExifInterface
import androidx.fragment.app.DialogFragment
import com.example.miniapp_mapsalbum.data.Memory
import java.io.File

class EditMarkerInfoDialogFragment : DialogFragment() {

    private var memory: Memory? = null
    private lateinit var etTitle: EditText
    private lateinit var tvLatLng: EditText
    private lateinit var ivMemoryImage: ImageView
    private lateinit var btnPickPhoto: Button
    private lateinit var btnSave: Button
    private lateinit var btnCancel: Button
    private lateinit var btnDelete: Button

    // MODIFIKASI: Ganti onMemoryDeleted menjadi onMemoryReset
    interface EditMarkerInfoListener {
        fun onMemorySaved(memory: Memory)
        fun onMemoryReset(memory: Memory)
        fun onPickImageClicked(memoryToEdit: Memory?)
    }

    private var listener: EditMarkerInfoListener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            listener = context as EditMarkerInfoListener
        } catch (e: ClassCastException) {
            throw ClassCastException("$context must implement EditMarkerInfoListener")
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        return dialog
    }

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
        return inflater.inflate(R.layout.dialog_edit_marker_info, container, false)
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

        etTitle = view.findViewById(R.id.et_marker_title)
        tvLatLng = view.findViewById(R.id.tv_marker_latlng)
        ivMemoryImage = view.findViewById(R.id.iv_memory_image)
        btnPickPhoto = view.findViewById(R.id.btn_pick_photo)
        btnSave = view.findViewById(R.id.btn_save_marker)
        btnCancel = view.findViewById(R.id.btn_cancel)
        btnDelete = view.findViewById(R.id.btn_delete_marker)

        etTitle.setText(currentMemory.title)
        tvLatLng.setText("Lat: ${currentMemory.latitude}, Lng: ${currentMemory.longitude}")

        currentMemory.imagePath?.let { path ->
            updateImageView(path)
        } ?: run { ivMemoryImage.visibility = View.GONE }


        btnPickPhoto.setOnClickListener {
            listener?.onPickImageClicked(currentMemory)
        }

        btnSave.setOnClickListener {
            currentMemory.title = etTitle.text.toString()
            listener?.onMemorySaved(currentMemory)
            dismiss()
        }

        btnCancel.setOnClickListener {
            dismiss()
        }

        // MODIFIKASI: Logika tombol HAPUS diubah menjadi RESET
        btnDelete.setOnClickListener {
            if (currentMemory.id != 0L) {
                // Panggil fungsi onMemoryReset yang baru
                listener?.onMemoryReset(currentMemory)
                dismiss()
            } else {
                Toast.makeText(context, "Tidak bisa mereset kenangan yang belum disimpan.", Toast.LENGTH_SHORT).show()
                dismiss()
            }
        }
        // MODIFIKASI: Ubah teks tombolnya
        btnDelete.text = "RESET"


        if (currentMemory.id == 0L) {
            btnDelete.visibility = View.GONE
        }
    }

    fun updateImageView(imagePath: String?) {
        if (imagePath != null) {
            val file = File(imagePath)
            if (file.exists()) {
                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
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
                    Log.e("EditMarkerInfo", "Error reading EXIF or rotating image: ${e.message}")
                    ivMemoryImage.setImageBitmap(bitmap)
                }

                ivMemoryImage.visibility = View.VISIBLE
            } else {
                ivMemoryImage.visibility = View.GONE
            }
        } else {
            ivMemoryImage.visibility = View.GONE
        }
    }

    companion object {
        private const val ARG_MEMORY = "memory"

        fun newInstance(memory: Memory): EditMarkerInfoDialogFragment {
            val fragment = EditMarkerInfoDialogFragment()
            val args = Bundle().apply {
                putParcelable(ARG_MEMORY, memory)
            }
            fragment.arguments = args
            return fragment
        }
    }
}