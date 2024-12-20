package com.example.streamease.helper

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper


class DatabaseHelper(context: Context?) :
    SQLiteOpenHelper(context, "SignLog.db", null, 1) {
    override fun onCreate(MyDatabase: SQLiteDatabase) {
        MyDatabase.execSQL("create Table users(email TEXT primary key, password TEXT)")
    }

    override fun onUpgrade(MyDB: SQLiteDatabase, i: Int, i1: Int) {
        MyDB.execSQL("drop Table if exists users")
    }

    fun insertData(email: String?, password: String?): Boolean {
        val MyDatabase = this.writableDatabase
        val contentValues = ContentValues()
        contentValues.put("email", email)
        contentValues.put("password", password)
        val result = MyDatabase.insert("users", null, contentValues)

        return if (result == -1L) {
            false
        } else {
            true
        }
    }

    fun checkEmail(email: String): Boolean {
        val MyDatabase = this.writableDatabase
        val cursor = MyDatabase.rawQuery("Select * from users where email = ?", arrayOf(email))

        return if (cursor.count > 0) {
            true
        } else {
            false
        }
    }

    fun checkEmailPassword(email: String, password: String): Boolean {
        val MyDatabase = this.writableDatabase
        val cursor = MyDatabase.rawQuery(
            "Select * from users where email = ? and password = ?",
            arrayOf(email, password)
        )

        return if (cursor.count > 0) {
            true
        } else {
            false
        }
    }

    companion object {
        const val databaseName: String = "SignLog.db"
    }
}