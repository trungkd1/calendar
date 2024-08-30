package com.trungkieu.mycalendar.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "event_type", indices = [(Index(value = ["id"], unique = true))])
data class EventTypeEntity(
    @PrimaryKey(autoGenerate = true) var id: Long?,
    @ColumnInfo(name = "title") var title: String,
    @ColumnInfo(name = "color") var color: Int,
    @ColumnInfo(name = "caldav_calendar_id") var caldavCalendarId: Int = 0,
    @ColumnInfo(name = "caldav_display_name") var caldavDisplayName: String = "",
    @ColumnInfo(name = "caldav_email") var caldavEmail: String = "",
    @ColumnInfo(name = "type") var type: Int = 0
) {

    /** Get display title with content is :
     * if (caldavCalendarId == 0)
     *   title
     * else
     *   "$caldavDisplayName ($caldavEmail)"
     * */
    fun getDisplayTitle() = caldavCalendarId.takeIf { it != 0 }.let { "$caldavDisplayName ($caldavEmail)" } ?:  title

    fun isSyncedEventType() = caldavCalendarId != 0
}
