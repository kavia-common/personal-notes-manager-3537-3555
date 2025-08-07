package org.example.app.ui

import android.app.Activity
import android.app.AlertDialog
import android.content.ContentValues
import android.database.Cursor
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import org.example.app.Note
import org.example.app.NotesContentProvider
import org.example.app.R
import org.example.app.NotesDatabaseHelper

// PUBLIC_INTERFACE
class NoteDetailActivity : Activity() {

    private var noteId: Long = 0
    private var isNew: Boolean = true
    private lateinit var titleView: EditText
    private lateinit var contentView: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.Theme_NotesMinimal)
        setContentView(R.layout.activity_note_detail)

        val toolbar = findViewById<TextView>(R.id.detailToolbar)
        toolbar?.text = getString(R.string.edit_note)
        toolbar?.setOnClickListener { finish() }

        titleView = findViewById(R.id.detailTitle)
        contentView = findViewById(R.id.detailContent)
        noteId = intent.getLongExtra(EXTRA_NOTE_ID, 0)
        isNew = noteId == 0L

        if (!isNew) loadNote(noteId)

        findViewById<Button>(R.id.btnSave).setOnClickListener { saveNote() }
        findViewById<Button>(R.id.btnDelete).setOnClickListener { confirmDelete() }
    }

    private fun loadNote(id: Long) {
        val cursor: Cursor? = contentResolver.query(
            NotesContentProvider.CONTENT_URI,
            null,
            "${NotesDatabaseHelper.COL_ID}=?",
            arrayOf(id.toString()),
            null
        )
        cursor?.use {
            if (it.moveToFirst()) {
                titleView.setText(it.getString(it.getColumnIndexOrThrow(NotesDatabaseHelper.COL_TITLE)))
                contentView.setText(it.getString(it.getColumnIndexOrThrow(NotesDatabaseHelper.COL_CONTENT)))
            }
        }
    }

    private fun saveNote() {
        val values = ContentValues().apply {
            put(NotesDatabaseHelper.COL_TITLE, titleView.text?.toString() ?: "")
            put(NotesDatabaseHelper.COL_CONTENT, contentView.text?.toString() ?: "")
            put(NotesDatabaseHelper.COL_UPDATED, System.currentTimeMillis())
            if (isNew) put(NotesDatabaseHelper.COL_CREATED, System.currentTimeMillis())
        }
        if (isNew) {
            val uri = contentResolver.insert(NotesContentProvider.CONTENT_URI, values)
            if (uri == null) Toast.makeText(this, "Failed to save", Toast.LENGTH_SHORT).show()
        } else {
            contentResolver.update(
                NotesContentProvider.CONTENT_URI.buildUpon().appendPath(noteId.toString()).build(),
                values, null, null
            )
        }
        finish()
    }

    private fun confirmDelete() {
        if (isNew) {
            finish()
            return
        }
        AlertDialog.Builder(this)
            .setMessage("Delete this note?")
            .setPositiveButton(R.string.delete) { _, _ -> deleteNote() }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun deleteNote() {
        contentResolver.delete(
            NotesContentProvider.CONTENT_URI.buildUpon().appendPath(noteId.toString()).build(),
            null, null
        )
        finish()
    }

    companion object {
        const val EXTRA_NOTE_ID = "extra_note_id"
    }
}
