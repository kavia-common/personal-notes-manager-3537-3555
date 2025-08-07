package org.example.app

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.SimpleAdapter
import android.widget.TextView
import org.example.app.NotesContentProvider
import org.example.app.Note
import org.example.app.NotesDatabaseHelper
import org.example.app.ui.NoteDetailActivity
import android.database.Cursor

// PUBLIC_INTERFACE
class MainActivity : Activity() {

    private lateinit var notesListView: ListView
    private lateinit var searchBar: EditText
    private lateinit var addButton: Button
    private lateinit var emptyLabel: TextView
    private var notes: MutableList<Note> = mutableListOf()
    private var listAdapter: SimpleAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.Theme_NotesMinimal)
        setContentView(R.layout.activity_main)

        // top bar
        val toolbar = findViewById<TextView>(R.id.toolbar)
        toolbar.text = getString(R.string.notes_title)

        searchBar = findViewById(R.id.searchView)
        notesListView = findViewById(R.id.notesRecyclerView)
        emptyLabel = findViewById(R.id.emptyTextView)
        addButton = findViewById(R.id.fabAddNote)

        searchBar.setOnEditorActionListener { v, _, _ ->
            filterNotes(v.text?.toString() ?: "")
            true
        }

        addButton.setOnClickListener {
            startActivity(Intent(this, NoteDetailActivity::class.java))
        }

        notesListView.setOnItemClickListener { _, _, position, _ ->
            val selectedNote = notes[position]
            val intent = Intent(this, NoteDetailActivity::class.java)
            intent.putExtra(NoteDetailActivity.EXTRA_NOTE_ID, selectedNote.id)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        loadNotes()
    }

    private fun filterNotes(query: String) {
        val filtered = if (query.isBlank()) notes
            else notes.filter { it.title.contains(query, true) || it.content.contains(query, true) }
        updateAdapter(filtered)
    }

    private fun loadNotes() {
        notes.clear()
        val projection = arrayOf(
            NotesDatabaseHelper.COL_ID,
            NotesDatabaseHelper.COL_TITLE,
            NotesDatabaseHelper.COL_CONTENT,
            NotesDatabaseHelper.COL_CREATED,
            NotesDatabaseHelper.COL_UPDATED
        )
        val cursor: Cursor? = contentResolver.query(
            NotesContentProvider.CONTENT_URI,
            projection,
            null, null,
            NotesDatabaseHelper.COL_UPDATED + " DESC"
        )
        cursor?.use {
            while (it.moveToNext()) {
                notes.add(Note(
                    id = it.getLong(it.getColumnIndexOrThrow(NotesDatabaseHelper.COL_ID)),
                    title = it.getString(it.getColumnIndexOrThrow(NotesDatabaseHelper.COL_TITLE)),
                    content = it.getString(it.getColumnIndexOrThrow(NotesDatabaseHelper.COL_CONTENT)),
                    createdAt = it.getLong(it.getColumnIndexOrThrow(NotesDatabaseHelper.COL_CREATED)),
                    updatedAt = it.getLong(it.getColumnIndexOrThrow(NotesDatabaseHelper.COL_UPDATED))
                ))
            }
        }
        filterNotes(searchBar.text?.toString() ?: "")
    }

    private fun updateAdapter(noteList: List<Note>) {
        val data = noteList.map {
            mapOf(
                "title" to (if (it.title.isNotEmpty()) it.title else getString(R.string.note_untitled)),
                "date" to android.text.format.DateFormat.format("MMM d, yyyy", it.updatedAt).toString()
            )
        }
        listAdapter = SimpleAdapter(
            this,
            data,
            android.R.layout.simple_list_item_2,
            arrayOf("title", "date"),
            intArrayOf(android.R.id.text1, android.R.id.text2)
        )
        notesListView.adapter = listAdapter
        emptyLabel.visibility = if (data.isEmpty()) TextView.VISIBLE else TextView.GONE
    }
}
