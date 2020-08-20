package com.shahzaib.vision

import android.content.Context
import android.os.Bundle
import android.view.textservice.SentenceSuggestionsInfo
import android.view.textservice.SpellCheckerSession
import android.view.textservice.SpellCheckerSession.SpellCheckerSessionListener
import android.view.textservice.SuggestionsInfo
import android.view.textservice.TextInfo
import android.view.textservice.TextServicesManager
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity


class RecognizedActivity: AppCompatActivity(), SpellCheckerSessionListener {
    private lateinit var recognizedTextHistory: ArrayList<String>
    private lateinit var spellChecker: SpellCheckerSession

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

    override fun onGetSuggestions(arg0: Array<SuggestionsInfo?>?) {
        // TODO Auto-generated method stub
    }

    override fun onGetSentenceSuggestions(arg0: Array<SentenceSuggestionsInfo?>?) {
        // TODO Auto-generated method stub
    }

    override fun onResume() {
        super.onResume()
//        val textServiceManager: TextServicesManager = getSystemService(Context.TEXT_SERVICES_MANAGER_SERVICE) as TextServicesManager
//        spellChecker = textServiceManager.newSpellCheckerSession(null, null, this, true)
    }

    override fun onPause() {
        super.onPause()
        spellChecker.close()
    }
}