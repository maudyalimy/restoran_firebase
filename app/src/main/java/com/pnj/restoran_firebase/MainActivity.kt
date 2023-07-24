package com.pnj.restoran_firebase

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.*
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.pnj.restoran_firebase.auth.SettingsActivity
import com.pnj.restoran_firebase.chat.ChatActivity
import com.pnj.restoran_firebase.databinding.ActivityMainBinding
import com.pnj.restoran_firebase.restoran.AddMakananActivity
import com.pnj.restoran_firebase.restoran.Makanan
import com.pnj.restoran_firebase.restoran.MakananAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.lang.Exception

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private lateinit var makananRecyclerView: RecyclerView
    private var makananArrayList: ArrayList<Makanan> = arrayListOf()
    private lateinit var makananAdapter: MakananAdapter
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        makananRecyclerView = binding.makananListView
        makananRecyclerView.layoutManager = LinearLayoutManager(this)
        makananRecyclerView.setHasFixedSize(true)
        load_data()
        makananAdapter = MakananAdapter(makananArrayList)

        makananRecyclerView.adapter = makananAdapter


        swipeDelete()

        binding.btnAddMakanan.setOnClickListener {
            val intentMain = Intent(this, AddMakananActivity::class.java)
            startActivity(intentMain)
        }

        binding.txtSearchMakanan.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                val keyword = binding.txtSearchMakanan.text.toString()
                if (keyword.isNotEmpty()) {
                    search_data(keyword)
                }
                else {
                    load_data()
                }
            }

            override fun afterTextChanged(p0: Editable?) {}
        })

        binding.bottomNavigation.setOnItemSelectedListener {
            when(it.itemId) {
                R.id.nav_bottom_home -> {
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                }
                R.id.nav_bottom_setting -> {
                    val intent = Intent(this, SettingsActivity::class.java)
                    startActivity(intent)
                }
                R.id.nav_bottom_chat -> {
                    val intent = Intent(this, ChatActivity::class.java)
                    startActivity(intent)
                }
            }
            true
        }
    }

    private fun load_data() {
        makananArrayList.clear()
        db = FirebaseFirestore.getInstance()
        db.collection("makanan").
                addSnapshotListener(object : EventListener<QuerySnapshot> {
                    override fun onEvent(value: QuerySnapshot?, error: FirebaseFirestoreException?) {
                        if(error != null){
                            Log.e("Firestore Error", error.message.toString())
                            return
                        }
                        for (dc: DocumentChange in value?.documentChanges!!){
                            if (dc.type == DocumentChange.Type.ADDED)
                                makananArrayList.add(dc.document.toObject(Makanan::class.java))
                        }
                        makananAdapter.notifyDataSetChanged()

                    }
                })
    }

    private  fun search_data(keyword :String) {
        makananArrayList.clear()

        db = FirebaseFirestore.getInstance()

        val query = db.collection("makanan")
            .orderBy("nama")
            .startAt(keyword)
            .get()
        query.addOnSuccessListener {
            makananArrayList.clear()
            for (document in it) {
                makananArrayList.add(document.toObject(Makanan::class.java))
            }
        }
    }

    private fun deleteMakanan(makanan: Makanan, doc_id:String) {
        val builder = AlertDialog.Builder(this)
        builder.setMessage("Apakah ${makanan.nama} ingin dihaps ?")
            .setCancelable(false)
            .setPositiveButton("Yes") { dialog, id ->
                lifecycleScope.launch {
                    db.collection("pasien")
                        .document(doc_id).delete()
                    Toast.makeText(
                        applicationContext,
                        makanan.nama.toString() + "is deleted",
                        Toast.LENGTH_LONG
                    ).show()
                    load_data()

                    deleteFoto("img_makanan/${makanan.harga}_${makanan.nama}.jpg")
                }
            }
            .setNegativeButton("No") { dialog, id ->
                dialog.dismiss()
                load_data()
            }
        val alerts = builder.create()
        alerts.show()
    }

    private fun swipeDelete() {
        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0,
        ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                lifecycleScope.launch {
                    val makanan = makananArrayList[position]
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
                                deleteMakanan(makanan, document.id)
                                load_data()
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(
                                        applicationContext,
                                        e.message.toString(),
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        }
                    }
                    else {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                applicationContext,
                                "User yang ingin di hapus tidak ditemukan",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            }
        })
    }

    private fun deleteFoto(file_name: String) {
        val storage = Firebase.storage
        val storageRef = storage.reference
        val deleteFileRef = storageRef.child(file_name)
        if (deleteFileRef != null) {
            deleteFileRef.delete().addOnSuccessListener {
                Log.e("deleted", "succes")
            }.addOnFailureListener {
                Log.e("deleted", "failed")
            }
        }
    }

}