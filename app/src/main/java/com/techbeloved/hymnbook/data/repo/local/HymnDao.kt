package com.techbeloved.hymnbook.data.repo.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.techbeloved.hymnbook.data.model.Hymn
import com.techbeloved.hymnbook.data.model.HymnTitle
import io.reactivex.Flowable

@Dao
interface HymnDao {
    @Query("SELECT * FROM hymns ORDER BY :sortBy ASC")
    fun getAllHymns(@Hymn.Companion.ColumnName sortBy: String): Flowable<List<Hymn>>

    @Query("SELECT * FROM hymns WHERE num = :number")
    fun getHymnByNumber(number: Int): Flowable<Hymn>

    @Query("SELECT * FROM hymn_title ORDER BY :sortBy ASC")
    fun getAllHymnTitles(@Hymn.Companion.ColumnName sortBy: String): Flowable<List<HymnTitle>>

    @Insert
    fun insertAll(hymns: List<Hymn>)

    @Insert
    fun insert(hymn: Hymn)

    @Delete
    fun delete(hymn: Hymn)

    @Query("DELETE FROM hymns WHERE num = :hymnNo")
    fun deleteByNumber(hymnNo: Int)

    @Query("DELETE FROM hymns")
    fun deleteAll()
}