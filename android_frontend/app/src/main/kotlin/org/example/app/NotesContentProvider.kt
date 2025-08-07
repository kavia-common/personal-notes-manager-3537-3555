package org.example.app

import android.content.ContentProvider
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.UriMatcher
import android.database.Cursor
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import android.net.Uri

// PUBLIC_INTERFACE
class NotesContentProvider : ContentProvider() {
    private lateinit var dbHelper: NotesDatabaseHelper

    override fun onCreate(): Boolean {
        dbHelper = NotesDatabaseHelper(context!!)
        return true
    }

    override fun query(
        uri: Uri, projection: Array<String>?, selection: String?, selectionArgs: Array<String>?, sortOrder: String?
    ): Cursor? {
        val database = dbHelper.readableDatabase
        val cursor: Cursor = when (uriMatcher.match(uri)) {
            NOTES -> database.query(NotesDatabaseHelper.TABLE_NAME, projection, selection, selectionArgs, null, null, sortOrder)
            NOTE_ID -> {
                val id = ContentUris.parseId(uri)
                database.query(
                    NotesDatabaseHelper.TABLE_NAME, projection,
                    "${NotesDatabaseHelper.COL_ID}=?", arrayOf(id.toString()), null, null, sortOrder
                )
            }
            else -> throw IllegalArgumentException("Unknown URI $uri")
        }
        cursor.setNotificationUri(context!!.contentResolver, uri)
        return cursor
    }

    override fun getType(uri: Uri): String? =
        when (uriMatcher.match(uri)) {
            NOTES -> "vnd.android.cursor.dir/vnd.org.example.app.note"
            NOTE_ID -> "vnd.android.cursor.item/vnd.org.example.app.note"
            else -> throw IllegalArgumentException("Unknown URI $uri")
        }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        val database = dbHelper.writableDatabase
        when (uriMatcher.match(uri)) {
            NOTES -> {
                val id = database.insert(NotesDatabaseHelper.TABLE_NAME, null, values)
                if (id > 0) {
                    val noteUri = ContentUris.withAppendedId(CONTENT_URI, id)
                    context!!.contentResolver.notifyChange(noteUri, null)
                    return noteUri
                }
                throw SQLException("Failed to add record into $uri")
            }
            else -> throw IllegalArgumentException("Unknown URI $uri")
        }
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int {
        val database = dbHelper.writableDatabase
        val count: Int = when (uriMatcher.match(uri)) {
            NOTES -> database.delete(NotesDatabaseHelper.TABLE_NAME, selection, selectionArgs)
            NOTE_ID -> {
                val id = ContentUris.parseId(uri)
                database.delete(
                    NotesDatabaseHelper.TABLE_NAME,
                    "${NotesDatabaseHelper.COL_ID}=?", arrayOf(id.toString())
                )
            }
            else -> throw IllegalArgumentException("Unknown URI $uri")
        }
        context!!.contentResolver.notifyChange(uri, null)
        return count
    }

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<String>?): Int {
        val database = dbHelper.writableDatabase
        val rowsUpdated: Int = when (uriMatcher.match(uri)) {
            NOTES -> database.update(NotesDatabaseHelper.TABLE_NAME, values, selection, selectionArgs)
            NOTE_ID -> {
                val id = ContentUris.parseId(uri)
                database.update(
                    NotesDatabaseHelper.TABLE_NAME, values,
                    "${NotesDatabaseHelper.COL_ID}=?", arrayOf(id.toString())
                )
            }
            else -> throw IllegalArgumentException("Unknown URI $uri")
        }
        context!!.contentResolver.notifyChange(uri, null)
        return rowsUpdated
    }

    companion object {
        const val AUTHORITY = "org.example.app.notesprovider"
        val CONTENT_URI: Uri = Uri.parse("content://$AUTHORITY/notes")
        private const val NOTES = 1
        private const val NOTE_ID = 2
        private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH).apply {
            addURI(AUTHORITY, "notes", NOTES)
            addURI(AUTHORITY, "notes/#", NOTE_ID)
        }
    }
}
