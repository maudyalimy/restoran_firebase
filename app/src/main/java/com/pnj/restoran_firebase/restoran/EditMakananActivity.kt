package com.pnj.restoran_firebase.restoran

import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import com.pnj.restoran_firebase.MainActivity
import com.pnj.restoran_firebase.R
import com.pnj.restoran_firebase.databinding.ActivityEditMakananBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.sql.Timestamp


class EditMakananActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditMakananBinding
    private val db = FirebaseFirestore.getInstance()

    private val REQ_CAM = 101
    private lateinit var imgUri : Uri
    private var dataGambar: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityEditMakananBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val (year, month, day, curr_makanan) = setDefaultValue()

        binding.TxtEditTglPesan.setOnClickListener {
            val dpd = DatePickerDialog(this,
            DatePickerDialog.OnDateSetListener { view, year, monthOfYear, dayOfMonth ->
                binding.TxtEditTglPesan.setText(
                    "" + year + "_" + (monthOfYear + 1) + "_" + dayOfMonth)
            }, year.toString().toInt(), month.toString().toInt(), day.toString().toInt()

            )
            dpd.show()
        }

        binding.BtnEditMakanan.setOnClickListener {
            val new_data_makanan = newMakanan()
            updateMakanan(curr_makanan as Makanan, new_data_makanan)

            val intentMain = Intent(this, MainActivity::class.java)
            startActivity(intentMain)
            finish()
        }

        showFoto()

        binding.BtnImgMakanan.setOnClickListener{
            openCamera()
        }
    }

    fun setDefaultValue(): Array<Any> {
        val intent = intent
        val harga = intent.getStringExtra("harga").toString()
        val nama = intent.getStringExtra("nama").toString()
        val tgl_pesan = intent.getStringExtra("tgl_pesan").toString()
        val jenis_makanan = intent.getStringExtra("jenis_makanan").toString()
        val tambahan = intent.getStringExtra("tambahan").toString()

        binding.TxtEditHarga.setText(harga)
        binding.TxtEditNama.setText(nama)
        binding.TxtEditTglPesan.setText(tgl_pesan)

        val tgl_split = intent.getStringExtra("tgl_pesan")
            .toString().split("_").toTypedArray()
        val year = tgl_split[0].toInt()
        val month = tgl_split[1].toInt() - 1
        val day = tgl_split[2].toInt()
        if (jenis_makanan == "Basah") {
            binding.RdnEditJKB.isChecked = true
        } else if (jenis_makanan == "Kering") {
            binding.RdnEditJKK.isChecked = true
        }
        val tambahan_makanan = tambahan.split("|").toTypedArray()
        for (p in tambahan_makanan) {
            if (p == "saus") {
                binding.ChkEditSaus.isChecked = true
            } else if (p == "sambal") {
                binding.ChkEditSambal.isChecked = true
            } else if (p == "mayonaise") {
                binding.ChkEditMayonaise.isChecked = true
            }
        }
        val curr_makanan = Makanan(harga, nama, jenis_makanan, tgl_pesan, tambahan)
        return arrayOf(year, month, day, curr_makanan)
    }

    fun newMakanan(): Map<String, Any> {
        var harga : String = binding.TxtEditHarga.text.toString()
        var nama : String = binding.TxtEditNama.text.toString()
        var tgl_pesan: String = binding.TxtEditTglPesan.text.toString()

        var jm : String = ""
        if(binding.RdnEditJKB.isChecked) {
            jm = "Basah"
        }
        else if(binding.RdnEditJKK.isChecked) {
            jm = "Kering"
        }

        var tambahan = ArrayList<String>()
        if (binding.ChkEditSaus.isChecked){
            tambahan.add("saus")
        }
        if (binding.ChkEditSambal.isChecked){
            tambahan.add("sambal")
        }
        if (binding.ChkEditMayonaise.isChecked){
            tambahan.add("smayonaise")
        }

        if (dataGambar != null) {
            uploadPictFirebase(dataGambar!!, "${harga}_${nama}")
        }

        val makanan_string = tambahan.joinToString("|")

        val makanan = mutableMapOf<String, Any>()
        makanan["harga"] = harga
        makanan["nama"] = nama
        makanan["tgl_pesan"] = tgl_pesan
        makanan["jenis_makanan"] = jm
        makanan["tambahan"] = makanan_string

        return makanan
    }

    private fun updateMakanan(makanan: Makanan, newMakananMap: Map<String, Any>) =
        CoroutineScope(Dispatchers.IO).launch {
            val personQuery = db.collection("makanan")
                .whereEqualTo("harga", makanan.harga)
                .whereEqualTo("nama", makanan.nama)
                .whereEqualTo("jenis_makanan", makanan.jenis_makanan)
                .whereEqualTo("tambahan", makanan.tambahan)
                .get()
                .await()
            if (personQuery.documents.isNotEmpty()) {
                for (document in personQuery) {
                    try {
                        db.collection("makanan").document(document.id).set(
                            newMakananMap,
                            SetOptions.merge()
                        )
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@EditMakananActivity,
                            e.message, Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
            else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@EditMakananActivity,
                    "No persons matched the query.", Toast.LENGTH_LONG).show()
                }
            }
        }

    fun showFoto() {
        val intent = intent
        val harga = intent.getStringExtra("harga").toString()
        val nama = intent.getStringExtra("nama").toString()

        val storageRef = FirebaseStorage.getInstance().reference.child("img_makanan/${harga}_${nama}.jpg")
        val localfile = File.createTempFile("tempImage", "jpg")
        storageRef.getFile(localfile).addOnSuccessListener {
            val bitmap = BitmapFactory.decodeFile(localfile.absolutePath)
            binding.BtnImgMakanan.setImageBitmap(bitmap)
        }.addOnFailureListener {
            Log.e("foto ?", "gagal")
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