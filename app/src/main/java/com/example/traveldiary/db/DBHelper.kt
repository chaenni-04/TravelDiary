package com.example.traveldiary.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.traveldiary.model.TravelRecord

class DBHelper(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    companion object {
        private const val DB_NAME = "TravelDiary.db"
        private const val DB_VERSION = 2  // 버전 올림
        const val TABLE = "travel_records"
        const val COL_NO = "no"
        const val COL_PLACE = "place"
        const val COL_DATE = "visit_date"
        const val COL_MEMO = "memo"
        const val COL_PHOTO = "photo_uri"
        const val COL_PINNED = "is_pinned"
    }

    override fun onCreate(db: SQLiteDatabase) {
        try {
            val sql = "CREATE TABLE $TABLE (" +
                    "`$COL_NO` INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "`$COL_PLACE` TEXT, " +
                    "`$COL_DATE` TEXT, " +
                    "`$COL_MEMO` TEXT, " +
                    "`$COL_PHOTO` TEXT, " +
                    "`$COL_PINNED` INTEGER DEFAULT 0)"
            db.execSQL(sql)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        try {
            if (oldVersion < 2) {
                // 기존 데이터 유지하면서 컬럼만 추가
                db.execSQL("ALTER TABLE $TABLE ADD COLUMN `$COL_PINNED` INTEGER DEFAULT 0")
            }
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
                put(COL_PINNED, record.isPinned)
            }
            db.insert(TABLE, null, values)
        } catch (e: Exception) {
            e.printStackTrace()
            -1L
        }
    }

    // READ ALL - 고정 항목 먼저, 그 다음 정렬 기준 적용
    fun getAll(orderBy: String = "$COL_NO DESC"): List<TravelRecord> {
        return try {
            val db = readableDatabase
            val cursor = db.query(
                TABLE, null, null, null, null, null,
                "$COL_PINNED DESC, $orderBy"  // 고정 먼저, 그 다음 정렬
            )
            val list = mutableListOf<TravelRecord>()
            with(cursor) {
                while (moveToNext()) {
                    list.add(
                        TravelRecord(
                            no = getInt(getColumnIndexOrThrow(COL_NO)),
                            place = getString(getColumnIndexOrThrow(COL_PLACE)) ?: "",
                            visitDate = getString(getColumnIndexOrThrow(COL_DATE)) ?: "",
                            memo = getString(getColumnIndexOrThrow(COL_MEMO)) ?: "",
                            photoUri = getString(getColumnIndexOrThrow(COL_PHOTO)) ?: "",
                            isPinned = getInt(getColumnIndexOrThrow(COL_PINNED))
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
                    photoUri = cursor.getString(cursor.getColumnIndexOrThrow(COL_PHOTO)) ?: "",
                    isPinned = cursor.getInt(cursor.getColumnIndexOrThrow(COL_PINNED))
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
                put(COL_PINNED, record.isPinned)
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

    // PIN / UNPIN
    fun updatePin(no: Int, isPinned: Int): Int {
        return try {
            val db = writableDatabase
            val values = ContentValues().apply {
                put(COL_PINNED, isPinned)
            }
            db.update(TABLE, values, "$COL_NO = ?", arrayOf(no.toString()))
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