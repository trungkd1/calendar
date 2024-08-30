package com.trungkieu.mycalendar.interfaces

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.trungkieu.mycalendar.entity.EventTypeEntity

@Dao
interface EventTypesDao {

    @Query("SElECT * FROM event_type ORDER BY title ASC")
    fun getEventTypes(): List<EventTypeEntity>

    @Query("SELECT * FROM event_type WHERE id = :id")
    fun gewtEventTypeWithId(id: Long) : EventTypeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdate(eventType: EventTypeEntity): Long
}