package com.nit.crm.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        User::class,
        Customer::class,
        Product::class,
        Invoice::class,
        InvoiceItem::class,
        Payment::class,
        ServiceRecord::class,
        InstallationRecord::class,
        SyncQueue::class,
        ActivityLog::class
    ],
    version = 7,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun customerDao(): CustomerDao
    abstract fun productDao(): ProductDao
    abstract fun invoiceDao(): InvoiceDao
    abstract fun invoiceItemDao(): InvoiceItemDao
    abstract fun paymentDao(): PaymentDao
    abstract fun serviceRecordDao(): ServiceRecordDao
    abstract fun installationRecordDao(): InstallationRecordDao
    abstract fun syncQueueDao(): SyncQueueDao
    abstract fun activityLogDao(): ActivityLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "nit_crm_database"
                )
                .fallbackToDestructiveMigration(true)
                .build()
                INSTANCE = instance
                instance
            }
        }

        suspend fun prepopulateProducts(database: AppDatabase) {
            val productDao = database.productDao()
            // Check if products exist before inserting
            // Simple check to prevent overwriting modified data
            val existingProducts = database.productDao().getProductById("1")
            if (existingProducts == null) {
                val initialProducts = listOf(
                    Product("1", "CCTV-DOM-05", "Dome CCTV Camera 5MP", 75.0, "Cameras", 50, 45.0, "1 Year"),
                    Product("2", "CCTV-BUL-08", "Bullet Outdoor CCTV Camera 4K", 120.0, "Cameras", 35, 75.0, "1 Year"),
                    Product("3", "NVR-PRO-08", "Professional 8-Channel NVR", 199.99, "Recorders", 15, 120.0, "2 Years"),
                    Product("4", "HDD-SUR-02", "Surveillance Hard Drive 2TB", 85.0, "Storage", 25, 55.0, "2 Years"),
                    Product("5", "SWI-POE-08", "PoE Gigabit Switch 8-Port", 55.0, "Networking", 20, 35.0, "1 Year"),
                    Product("6", "CAB-CAT-100", "Cat6 Shielded Cable 100m", 40.0, "Accessories", 40, 22.0, "No Warranty"),
                    Product("7", "ACC-BNC-10", "BNC Connectors Pack of 10", 12.50, "Accessories", 100, 6.0, "No Warranty"),
                    Product("8", "PWR-12V-10A", "CCTV Power Supply 12V 10A", 30.0, "Accessories", 30, 18.0, "6 Months")
                )
                productDao.insertProducts(initialProducts)
            }
            
            // Pre-populate an active user session if not exists
            val userDao = database.userDao()
            val currentUser = userDao.getCurrentUserSync()
            if (currentUser == null) {
                userDao.insertUser(
                    User(
                        id = "u_1",
                        name = "Sarah Connor",
                        email = "sarah.c@nitcrm.com",
                        role = "Owner", // "Technician", "Sales", "Owner"
                        isLoggedIn = true
                    )
                )
            }
        }
    }
}
