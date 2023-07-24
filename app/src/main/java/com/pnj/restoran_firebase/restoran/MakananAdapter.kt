package com.pnj.restoran_firebase.restoran

import android.content.Intent
import android.graphics.BitmapFactory
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.storage.FirebaseStorage
import com.pnj.restoran_firebase.R
import java.io.File

class MakananAdapter(private val makananList: ArrayList<Makanan>) :
    RecyclerView.Adapter<MakananAdapter.MakananViewHolder>() {

    private lateinit var activity: AppCompatActivity

        class MakananViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val harga: TextView = itemView.findViewById(R.id.TVLHarga)
            val nama: TextView = itemView.findViewById(R.id.TVLNama)
            val jenis_makanan: TextView = itemView.findViewById(R.id.TVLJenisMakanan)
            val img_makanan: ImageView = itemView.findViewById(R.id.IMLGambarMakanan)
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MakananViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.makanan_list_layout, parent, false)
        return MakananViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: MakananViewHolder, position: Int) {
        val makanan: Makanan = makananList[position]
        holder.harga.text = makanan.harga
        holder.nama.text = makanan.nama
        holder.jenis_makanan.text = makanan.jenis_makanan

        holder.itemView.setOnClickListener {
            activity = it.context as AppCompatActivity
            activity.startActivity(Intent(activity, EditMakananActivity::class.java).apply {
                putExtra("harga", makanan.harga.toString())
                putExtra("nama", makanan.nama.toString())
                putExtra("jenis_makanan", makanan.jenis_makanan.toString())
                putExtra("tambahan", makanan.tambahan.toString())
            })
        }

        val storageRef = FirebaseStorage.getInstance().reference.child("img_makanan/${makanan.harga}_${makanan.nama}.jpg")
        val localfile = File.createTempFile("tempImage", "jpg")
        storageRef.getFile(localfile).addOnSuccessListener {
            val bitmap = BitmapFactory.decodeFile(localfile.absolutePath)
            holder.img_makanan.setImageBitmap(bitmap)
        }.addOnFailureListener {
            Log.e("foto ?", "gagal")
        }
    }

    override fun getItemCount(): Int {
        return makananList.size
    }
}