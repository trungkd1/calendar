package com.trungkieu.mycalendar.interfaces

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.trungkieu.mycalendar.entity.TaskEntity

@Dao
interface TaskDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdate(task: TaskEntity): Long

    @Query("SELECT * FROM tasks WHERE task_id = :id AND start_ts = :startTs")
    fun getTaskWithIdAndTs(id: Long, startTs: Long): TaskEntity?

    @Query("DELETE FROM tasks WHERE task_id = :id AND start_ts = :startTs")
    fun deleteTaskWithIdAndTs(id: Long, startTs: Long)
}