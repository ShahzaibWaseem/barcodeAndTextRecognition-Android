package com.shahzaib.vision

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity


class RecognizedActivity: AppCompatActivity() {
    private lateinit var recognizedTextHistory: ArrayList<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.recognized_text)

        val actionBar = supportActionBar
        actionBar?.setTitle(R.string.recognized_text)
        val listView: ListView = findViewById(R.id.recognizedListView)

        val intent = intent
        recognizedTextHistory = intent.getStringArrayListExtra("recognizedTextHistory") as ArrayList<String>

        val listItems = arrayOfNulls<String>(recognizedTextHistory.size)
        for (i in 0 until recognizedTextHistory.size){
            listItems[i] = recognizedTextHistory[i]
        }

        val adapter = ArrayAdapter(this, R.layout.listview_items, listItems)
        listView.adapter = adapter
    }
}