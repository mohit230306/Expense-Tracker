package com.mohit.expensetracker

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

data class Tx(val title:String, val amount:Double, val type:String, val category:String, val date:String)

class MainActivity : AppCompatActivity() {
    private val txs = mutableListOf<Tx>()
    private lateinit var adapter: TxAdapter
    private val prefs by lazy { getSharedPreferences("expense_data", Context.MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        load()
        adapter = TxAdapter(txs)
        findViewById<RecyclerView>(R.id.list).apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = this@MainActivity.adapter
        }
        findViewById<View>(R.id.addBtn).setOnClickListener { showAddDialog() }
        updateSummary()
    }

    private fun showAddDialog() {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(50,20,50,10) }
        val title = EditText(this).apply { hint="Description (e.g. Tea)" }
        val amount = EditText(this).apply { hint="Amount"; inputType=2 }
        val type = Spinner(this).apply { adapter=ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, arrayOf("Expense","Income")) }
        val category = EditText(this).apply { hint="Category (Food, Travel, Salary...)" }
        box.addView(title); box.addView(amount); box.addView(type); box.addView(category)
        AlertDialog.Builder(this).setTitle("Add Transaction").setView(box)
            .setPositiveButton("Save") { _,_ ->
                val a = amount.text.toString().toDoubleOrNull()
                if (a == null || a <= 0) { Toast.makeText(this,"Enter a valid amount",Toast.LENGTH_SHORT).show(); return@setPositiveButton }
                val d = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())
                txs.add(0, Tx(title.text.toString().ifBlank{"Untitled"}, a, type.selectedItem.toString(), category.text.toString().ifBlank{"Other"}, d))
                save(); adapter.notifyDataSetChanged(); updateSummary()
            }.setNegativeButton("Cancel", null).show()
    }

    private fun updateSummary() {
        val balance = txs.sumOf { if(it.type=="Income") it.amount else -it.amount }
        val spent = txs.filter { it.type=="Expense" }.sumOf { it.amount }
        findViewById<TextView>(R.id.balance).text = "Balance: ₹%.2f".format(balance)
        findViewById<TextView>(R.id.monthly).text = "Expenses: ₹%.2f".format(spent)
    }

    private fun save() {
        val arr=JSONArray()
        txs.forEach { t -> arr.put(JSONObject().put("title",t.title).put("amount",t.amount).put("type",t.type).put("category",t.category).put("date",t.date)) }
        prefs.edit().putString("txs",arr.toString()).apply()
    }
    private fun load() {
        val arr=JSONArray(prefs.getString("txs","[]"))
        for(i in 0 until arr.length()) { val o=arr.getJSONObject(i); txs.add(Tx(o.getString("title"),o.getDouble("amount"),o.getString("type"),o.getString("category"),o.getString("date"))) }
    }
}

class TxAdapter(private val items: List<Tx>) : RecyclerView.Adapter<TxAdapter.H>() {
    class H(v:View):RecyclerView.ViewHolder(v) { val title=v.findViewById<TextView>(R.id.title); val amount=v.findViewById<TextView>(R.id.amount); val meta=v.findViewById<TextView>(R.id.meta) }
    override fun onCreateViewHolder(p:ViewGroup,v:Int)=H(LayoutInflater.from(p.context).inflate(R.layout.item_transaction,p,false))
    override fun getItemCount()=items.size
    override fun onBindViewHolder(h:H,p:Int) { val t=items[p]; h.title.text=t.title; h.amount.text=(if(t.type=="Income") "+ ₹" else "- ₹")+"%.2f".format(t.amount); h.meta.text="${t.category} • ${t.date}" }
}
