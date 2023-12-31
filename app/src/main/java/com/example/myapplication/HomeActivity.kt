package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.graphics.BitmapFactory
import android.widget.AdapterView
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import com.google.firebase.storage.ktx.storage
import android.view.View
import android.widget.ArrayAdapter

class HomeActivity : AppCompatActivity() {

    private lateinit var nicknameTextView: TextView
    private lateinit var firestore: FirebaseFirestore

    private lateinit var recyclerView: RecyclerView
    private var productAdapter: ProductAdapter? = null
    private lateinit var productList: MutableList<Product>

    private val db: FirebaseFirestore = Firebase.firestore
    private val itemsCollectionRef = db.collection("product")
    private var snapshotListener: ListenerRegistration? = null


    private val productsold by lazy {findViewById<TextView>(R.id.productsoldout)} //판매 여부
    private val titleTextView by lazy { findViewById<TextView>(R.id.productTitle)} //물건 제목
    private val priceTextView by lazy {findViewById<TextView>(R.id.productPrice)} //물건 가격
    private val sellerTextView by lazy {findViewById<TextView>(R.id.productSeller)} //물건 판매자
    private val logoutButton by lazy {findViewById<ImageView>(R.id.imageView6)}


    private val filterButton by lazy {findViewById<Spinner>(R.id.filterButton)}
    private var filterSelectPosition: Int =0;

    override fun onStart() {
        super.onStart()
    }

    override fun onStop() {
        super.onStop()
        snapshotListener?.remove()
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.home)
        firestore = FirebaseFirestore.getInstance()

        recyclerView = findViewById(R.id.recyclerview)
        productList = mutableListOf()

        recyclerView.layoutManager = LinearLayoutManager(this)

        productAdapter = ProductAdapter(this, emptyList())
        productAdapter?.setOnItemClickListener(object : ProductAdapter.OnItemClickListener {
            override fun onItemClick(productId: String) {
                queryItem(productId)
            }
        })
        recyclerView.adapter = productAdapter
        updateList()

        var data = resources.getStringArray(R.array.filterButton)
        var filterAdapter = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, data)
        filterButton.adapter = filterAdapter
        filterButton.onItemSelectedListener = object: AdapterView.OnItemSelectedListener{
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                when(p2){
                    0->{
                        filterSelectPosition =0
                    }
                    1->{
                        filterSelectPosition=1
                    }
                    2->{
                        filterSelectPosition=2
                    }
                }
                updateList()
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {
            }
        }

        logoutButton.setOnClickListener {
            Firebase.auth.signOut()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            Toast.makeText(this, "로그아웃 되었습니다.", Toast.LENGTH_SHORT).show()
        }

        // 홈 화면에서 닉네임 표시
        val user = Firebase.auth.currentUser
        val userId = user?.uid ?: ""

        firestore.collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    val nickname = document.getString("nickname") ?: ""
                    nicknameTextView = findViewById(R.id.textView)
                    nicknameTextView.text = "${nickname}님"
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "닉네임 불러오기 실패: $exception", Toast.LENGTH_SHORT).show()
            }
        displayImage()

        val remoteConfig = Firebase.remoteConfig
        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = 1
        }
        remoteConfig.setConfigSettingsAsync(configSettings)
        remoteConfig.setDefaultsAsync(R.xml.remote_config)


    // 페이지 이동 버튼들
        val imageButton2 = findViewById<ImageButton>(R.id.imageButton2)
        imageButton2.setOnClickListener {
            val intent = Intent(this, AddActivity::class.java)
            startActivity(intent)
        }
        val imageButton3 = findViewById<ImageButton>(R.id.imageButton3)
        imageButton3.setOnClickListener{
            val chatIntent = Intent(this, ChatlistActivity::class.java)
            intent.putExtra("Nickname", nicknameTextView.text)
            startActivity(chatIntent)
        }

    }
    private fun queryItem(itemID: String) {
        itemsCollectionRef.document(itemID).get()
            .addOnSuccessListener {
                titleTextView.text = it.getString("title")
                priceTextView.text = it.getDouble("price")?.toString()
                sellerTextView.text = it.getString("nickname")
                productsold.text = it.getBoolean("sale").toString()
                it.getString("detail")

            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "아이템 조회 실패: $exception", Toast.LENGTH_SHORT).show()
            }
    }
    override fun onDestroy() {
        super.onDestroy()
        snapshotListener?.remove()
    }

    private fun updateList() {
        itemsCollectionRef.get().addOnSuccessListener {
            val products = mutableListOf<Product>()
            if(filterSelectPosition == 0){
                for (doc in it) {
                    products.add(Product(doc))
                }
            }else if(filterSelectPosition ==1){
                for(doc in it){
                    if(Product(doc).sold=="true"){
                        products.add(Product(doc))
                    }
                }
            }else if(filterSelectPosition ==2){
                for (doc in it) {
                    if(Product(doc).sold=="false"){
                        products.add(Product(doc))
                    }
                }
            }
            productAdapter?.updateList(products)
        }
    }

    fun displayImage() {
        val storageRef = Firebase.storage.reference
        val imageRef = Firebase.storage.getReferenceFromUrl(
            "gs://second-hands-9f426.appspot.com/android.png"
        )

        val view = findViewById<ImageView>(R.id.imageView5)
        imageRef?.getBytes(Long.MAX_VALUE)?.addOnSuccessListener {
            val bmp = BitmapFactory.decodeByteArray(it, 0, it.size)
            view.setImageBitmap(bmp)
        }?.addOnFailureListener {
            // 이미지 다운로드 실패
        }
    }
}