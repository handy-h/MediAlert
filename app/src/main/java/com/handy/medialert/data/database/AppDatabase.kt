package com.handy.medialert.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.handy.medialert.data.dao.MedicationDao
import com.handy.medialert.data.dao.StockLogDao
import com.handy.medialert.data.entity.Medication
import com.handy.medialert.data.entity.StockLog
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Database(
    entities = [Medication::class, StockLog::class],
    version = 3,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun medicationDao(): MedicationDao
    abstract fun stockLogDao(): StockLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "medialert_database"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

val MIGRATION_1_2 = androidx.room.migration.Migration(1, 2) { database ->
    // v2: 添加 calendarEventId 字段用于追踪日历事件
    database.execSQL("ALTER TABLE medications ADD COLUMN calendarEventId INTEGER DEFAULT NULL")
}

val MIGRATION_2_3 = androidx.room.migration.Migration(2, 3) { database ->
    // v3: package_size 从 INTEGER 改为 REAL（支持小数，如每板 12.5 片）
    // 注意：列顺序必须与 Medication Entity 字段声明顺序一致，否则 INSERT SELECT 会错位
    database.execSQL("""
        CREATE TABLE medications_new (
            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            genericName TEXT NOT NULL,
            brandName TEXT,
            specification TEXT,
            packageUnit TEXT NOT NULL,
            dosageForm TEXT NOT NULL,
            packageSize REAL NOT NULL DEFAULT 0,
            currentStock REAL NOT NULL DEFAULT 0,
            frequencyType TEXT NOT NULL,
            frequencyValue INTEGER NOT NULL DEFAULT 1,
            dailyDosage REAL NOT NULL DEFAULT 0,
            startDate TEXT,
            isActive INTEGER NOT NULL DEFAULT 1,
            calendarEventId INTEGER,
            createdAt INTEGER NOT NULL DEFAULT 0
        )
    """)
    database.execSQL("INSERT INTO medications_new SELECT id, genericName, brandName, specification, packageUnit, dosageForm, CAST(package_size AS REAL), currentStock, frequencyType, frequencyValue, dailyDosage, startDate, isActive, calendarEventId, createdAt FROM medications")
    database.execSQL("DROP TABLE medications")
    database.execSQL("ALTER TABLE medications_new RENAME TO medications")
}

class Converters {
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE

    @TypeConverter
    fun fromLocalDate(date: LocalDate?): String? {
        return date?.format(formatter)
    }

    @TypeConverter
    fun toLocalDate(dateString: String?): LocalDate? {
        return dateString?.let { LocalDate.parse(it, formatter) }
    }
}