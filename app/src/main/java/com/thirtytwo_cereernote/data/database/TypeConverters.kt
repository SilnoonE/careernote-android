package com.thirtytwo_cereernote.data.database

import androidx.room.TypeConverter
import com.thirtytwo_cereernote.data.model.ApplicationStatus
import com.thirtytwo_cereernote.data.model.CompanySize
import com.thirtytwo_cereernote.data.model.EmploymentType
import com.thirtytwo_cereernote.data.model.WorkMode
import java.util.Date

class DateConverters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
}

class EnumConverters {
    @TypeConverter
    fun fromApplicationStatus(value: ApplicationStatus): String = value.name

    @TypeConverter
    fun toApplicationStatus(value: String): ApplicationStatus = ApplicationStatus.valueOf(value)

    @TypeConverter
    fun fromEmploymentType(value: EmploymentType): String = value.name

    @TypeConverter
    fun toEmploymentType(value: String): EmploymentType = EmploymentType.valueOf(value)

    @TypeConverter
    fun fromWorkMode(value: WorkMode): String = value.name

    @TypeConverter
    fun toWorkMode(value: String): WorkMode = WorkMode.valueOf(value)

    @TypeConverter
    fun fromCompanySize(value: CompanySize): String = value.name

    @TypeConverter
    fun toCompanySize(value: String): CompanySize = CompanySize.valueOf(value)
}
