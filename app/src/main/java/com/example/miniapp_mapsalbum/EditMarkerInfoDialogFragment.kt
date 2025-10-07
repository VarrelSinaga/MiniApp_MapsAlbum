package com.example.miniapp_mapsalbum

import android.content.Context
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.exifinterface.media.ExifInterface
import androidx.fragment.app.DialogFragment
import com.example.miniapp_mapsalbum.data.Memory
import java.io.File
import android.graphics.Bitmap
import android.util.Log
import android.view.WindowManager

class EditMarkerInfoDialogFragment : DialogFragment() {

    private var memory: Memory? = null
    // (Deklarasi view lainnya)
    private lateinit var etTitle: EditText
    private lateinit var tvLatLng: EditText
    private lateinit var ivMemoryImage: ImageView
    private lateinit var btnPickPhoto: Button
    private lateinit var btnSave: Button
    private lateinit var btnCancel: Button
    private lateinit var btnDelete: Button

    // DIUBAH: Listener lebih jelas, onMemoryDeleted
    interface EditMarkerInfoListener {
        fun onMemorySaved(memory: Memory)
        fun onMemoryDeleted(memory: Memory)
        fun onPickImageClicked(memoryToEdit: Memory?)
    }

    private var listener: EditMarkerInfoListener? = null

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            // Atur lebar dialog menjadi 90% dari lebar layar
            val width = (resources.displayMetrics.widthPixels * 0.90).toInt()
            // Biarkan tinggi menyesuaikan dengan konten
            val height = WindowManager.LayoutParams.WRAP_CONTENT
            setLayout(width, height)
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as? EditMarkerInfoListener
        if (listener == null) {
            throw ClassCastException("$context must implement EditMarkerInfoListener")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // (Sama seperti kode Anda)
        return inflater.inflate(R.layout.dialog_edit_marker_info, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // ... (Inisialisasi semua View Anda di sini seperti kode sebelumnya)
        etTitle = view.findViewById(R.id.et_marker_title)
        tvLatLng = view.findViewById(R.id.tv_marker_latlng)
        ivMemoryImage = view.findViewById(R.id.iv_memory_image)
        btnPickPhoto = view.findViewById(R.id.btn_pick_photo)
        btnSave = view.findViewById(R.id.btn_save_marker)
        btnCancel = view.findViewById(R.id.btn_cancel)
        btnDelete = view.findViewById(R.id.btn_delete_marker)

        val currentMemory = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arguments?.getParcelable(ARG_MEMORY, Memory::class.java)
        } else {
            @Suppress("DEPRECATION")
            arguments?.getParcelable(ARG_MEMORY)
        } ?: return

        this.memory = currentMemory

        etTitle.setText(currentMemory.title)
        tvLatLng.setText("Lat: ${currentMemory.latitude}, Lng: ${currentMemory.longitude}")
        currentMemory.imagePath?.let { updateImageView(it) }

        btnPickPhoto.setOnClickListener { listener?.onPickImageClicked(currentMemory) }

        btnSave.setOnClickListener {
            val title = etTitle.text.toString()
            if (title.isBlank()) {
                etTitle.error = "Judul tidak boleh kosong"
                return@setOnClickListener
            }
            currentMemory.title = title
            listener?.onMemorySaved(currentMemory)
            dismiss()
        }

        btnCancel.setOnClickListener { dismiss() }

        // DIUBAH: Logika tombol Hapus
        if (currentMemory.id == 0L) {
            btnDelete.visibility = View.GONE
        } else {
            btnDelete.visibility = View.VISIBLE
            btnDelete.text = "HAPUS"
            btnDelete.setOnClickListener {
                listener?.onMemoryDeleted(currentMemory)
                dismiss()
            }
        }
    }

    fun updateImageView(imagePath: String?) {
        // (Logika updateImageView Anda sudah bagus, tidak perlu diubah)
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
                    Log.e("EditMarkerInfo", "Error rotating image: ${e.message}")
                    ivMemoryImage.setImageBitmap(bitmap)
                }
                ivMemoryImage.visibility = View.VISIBLE
            }
        }
    }

    companion object {
        private const val ARG_MEMORY = "memory"
        fun newInstance(memory: Memory) = EditMarkerInfoDialogFragment().apply {
            arguments = Bundle().apply { putParcelable(ARG_MEMORY, memory) }
        }
    }
}