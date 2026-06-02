package com.example.traveldiary.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.traveldiary.model.TravelRecord

class DBHelper(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    companion object {
        private const val DB_NAME = "TravelDiary.db"
        private const val DB_VERSION = 1
        const val TABLE = "travel_records"
        const val COL_NO = "no"
        const val COL_PLACE = "place"
        const val COL_DATE = "visit_date"
        const val COL_MEMO = "memo"
        const val COL_PHOTO = "photo_uri"
    }

    override fun onCreate(db: SQLiteDatabase) {
        try {
            val sql = "CREATE TABLE $TABLE (" +
                    "`$COL_NO` INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "`$COL_PLACE` TEXT, " +
                    "`$COL_DATE` TEXT, " +
                    "`$COL_MEMO` TEXT, " +
                    "`$COL_PHOTO` TEXT)"
            db.execSQL(sql)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        try {
            db.execSQL("DROP TABLE IF EXISTS $TABLE")
            onCreate(db)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // CREATE
    fun insert(record: TravelRecord): Long {
        return try {
            val db = writableDatabase
            val values = ContentValues().apply {
                put(COL_PLACE, record.place)
                put(COL_DATE, record.visitDate)
                put(COL_MEMO, record.memo)
                put(COL_PHOTO, record.photoUri)
            }
            db.insert(TABLE, null, values)
        } catch (e: Exception) {
            e.printStackTrace()
            -1L
        }
    }

    // READ ALL
    fun getAll(orderBy: String = "$COL_NO DESC"): List<TravelRecord> {
        return try {
            val db = readableDatabase
            val cursor = db.query(TABLE, null, null, null, null, null, orderBy)
            val list = mutableListOf<TravelRecord>()
            with(cursor) {
                while (moveToNext()) {
                    list.add(
                        TravelRecord(
                            no = getInt(getColumnIndexOrThrow(COL_NO)),
                            place = getString(getColumnIndexOrThrow(COL_PLACE)) ?: "",
                            visitDate = getString(getColumnIndexOrThrow(COL_DATE)) ?: "",
                            memo = getString(getColumnIndexOrThrow(COL_MEMO)) ?: "",
                            photoUri = getString(getColumnIndexOrThrow(COL_PHOTO)) ?: ""
                        )
                    )
                }
                close()
            }
            list
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    // READ ONE
    fun getById(no: Int): TravelRecord? {
        return try {
            val db = readableDatabase
            val cursor = db.query(
                TABLE, null,
                "$COL_NO = ?", arrayOf(no.toString()),
                null, null, null
            )
            if (cursor.moveToFirst()) {
                val record = TravelRecord(
                    no = cursor.getInt(cursor.getColumnIndexOrThrow(COL_NO)),
                    place = cursor.getString(cursor.getColumnIndexOrThrow(COL_PLACE)) ?: "",
                    visitDate = cursor.getString(cursor.getColumnIndexOrThrow(COL_DATE)) ?: "",
                    memo = cursor.getString(cursor.getColumnIndexOrThrow(COL_MEMO)) ?: "",
                    photoUri = cursor.getString(cursor.getColumnIndexOrThrow(COL_PHOTO)) ?: ""
                )
                cursor.close()
                record
            } else {
                cursor.close()
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // UPDATE
    fun update(record: TravelRecord): Int {
        return try {
            val db = writableDatabase
            val values = ContentValues().apply {
                put(COL_PLACE, record.place)
                put(COL_DATE, record.visitDate)
                put(COL_MEMO, record.memo)
                put(COL_PHOTO, record.photoUri)
            }
            db.update(TABLE, values, "$COL_NO = ?", arrayOf(record.no.toString()))
        } catch (e: Exception) {
            e.printStackTrace()
            -1
        }
    }

    // DELETE ONE
    fun delete(no: Int): Int {
        return try {
            val db = writableDatabase
            db.delete(TABLE, "$COL_NO = ?", arrayOf(no.toString()))
        } catch (e: Exception) {
            e.printStackTrace()
            -1
        }
    }

    // DELETE ALL
    fun deleteAll(): Int {
        return try {
            val db = writableDatabase
            db.delete(TABLE, null, null)
        } catch (e: Exception) {
            e.printStackTrace()
            -1
        }
    }

    fun getCount(): Int {
        return try {
            val db = readableDatabase
            val cursor = db.rawQuery("SELECT COUNT(*) FROM $TABLE", null)
            cursor.moveToFirst()
            val count = cursor.getInt(0)
            cursor.close()
            count
        } catch (e: Exception) {
            e.printStackTrace()
            0
        }
    }

    fun getPhotoCount(): Int {
        return try {
            val db = readableDatabase
            val cursor = db.rawQuery(
                "SELECT COUNT(*) FROM $TABLE WHERE $COL_PHOTO != ''", null
            )
            cursor.moveToFirst()
            val count = cursor.getInt(0)
            cursor.close()
            count
        } catch (e: Exception) {
            e.printStackTrace()
            0
        }
    }
}