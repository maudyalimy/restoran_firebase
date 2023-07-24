package com.pnj.restoran_firebase.restoran

import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.pnj.restoran_firebase.MainActivity
import com.pnj.restoran_firebase.R
import com.pnj.restoran_firebase.databinding.ActivityAddMakananBinding
import java.io.ByteArrayOutputStream
import java.util.Calendar

class AddMakananActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddMakananBinding
    private val firestoreDatabase = FirebaseFirestore.getInstance()

    private val REQ_CAM = 101
    private lateinit var imgUri : Uri
    private var dataGambar: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAddMakananBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.TxtAddTglPesan.setOnClickListener {
            val c = Calendar.getInstance()
            val year = c.get(Calendar.YEAR)
            val month = c.get(Calendar.MONTH)
            val day = c.get(Calendar.DAY_OF_MONTH)
            
            val dpd = DatePickerDialog(this,
            DatePickerDialog.OnDateSetListener { view, year, monthOfYear, dayOfYear ->
                binding.TxtAddTglPesan.setText("" + year + "_" + monthOfYear + "__" + dayOfYear)
            }, year, month, day)

            dpd.show()


    }
        binding.BtnAddMakanan.setOnClickListener {
            addMakanan()
        }

        binding.BtnImgMakanan.setOnClickListener {
            openCamera()
        }

    }

    fun addMakanan() {
        var harga : String = binding.TxtAddHarga.text.toString()
        var nama : String = binding.TxtAddNama.text.toString()
        var tgl_makanan : String = binding.TxtAddTglPesan.text.toString()

        var jm : String = ""
        if (binding.RdnEditJKB.isChecked) {
            jm = "Basah"
        }
        else if (binding.RdnEditJKk.isChecked) {
            jm = "Kering"
        }

        var tambahan = ArrayList<String>()
        if (binding.ChkSaus.isChecked) {
            tambahan.add("saus")
        }
        if (binding.ChkSambal.isChecked) {
            tambahan.add("sambal")
        }
        if (binding.ChkMayonaise.isChecked) {
            tambahan.add("mayonaise")
        }

        val tambahan_string = tambahan.joinToString("|")

        val makanan: MutableMap<String, Any> = HashMap()
        makanan["harga"] = harga
        makanan["nama"] = nama
        makanan["jenis_makanan"] = jm
        makanan["tambahan"] = tambahan_string

        firestoreDatabase.collection("makanan").add(makanan)
            .addOnSuccessListener {
                val intentMain = Intent(this, MainActivity::class.java)
                startActivity(intentMain)
            }

        if (dataGambar != null) {
            uploadPictFirebase(dataGambar!!, "${harga}_${nama}")

            firestoreDatabase.collection("makanan").add(makanan)
                .addOnSuccessListener {
                    val intentMain = Intent(this, MainActivity::class.java)
                    startActivity(intentMain)
                }
        }
    }

    private fun openCamera() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { intent ->
            this.packageManager?.let {
                intent?.resolveActivity(it).also {
                    startActivityForResult(intent, REQ_CAM)
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_CAM && resultCode == RESULT_OK) {
            dataGambar = data?.extras?.get("data") as Bitmap
            binding.BtnImgMakanan.setImageBitmap(dataGambar)
        }
    }

    private fun uploadPictFirebase(img_bitmap: Bitmap, file_name: String) {
        val baos = ByteArrayOutputStream()
        val ref = FirebaseStorage.getInstance().reference.child("img_makanan/${file_name}.jpg")
        img_bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)

        val img = baos.toByteArray()
        ref.putBytes(img)
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    ref.downloadUrl.addOnCompleteListener { Task ->
                        Task.result.let { Uri ->
                            imgUri = Uri
                            binding.BtnImgMakanan.setImageBitmap(img_bitmap)
                        }
                    }
                }
            }
    }




}