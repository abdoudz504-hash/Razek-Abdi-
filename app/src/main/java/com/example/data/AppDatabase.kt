package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.*
import com.example.data.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        ProductEntity::class,
        CustomerEntity::class,
        SaleEntity::class,
        SaleItemEntity::class,
        CustomerPaymentEntity::class,
        ExpenseEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun customerDao(): CustomerDao
    abstract fun customerPaymentDao(): CustomerPaymentDao
    abstract fun saleDao(): SaleDao
    abstract fun expenseDao(): ExpenseDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "rz_tasyir_store.db"
                )
                .fallbackToDestructiveMigration()
                .fallbackToDestructiveMigrationOnDowngrade()
                .build()
                INSTANCE = instance
                instance
            }
        }

        suspend fun seedInitialDataIfEmpty(db: AppDatabase) {
            try {
                if (db.productDao().getCount() == 0) {
                    populateInitialData(db)
                }
            } catch (e: Exception) {
                android.util.Log.e("AppDatabase", "Error seeding initial data", e)
            }
        }

        private suspend fun populateInitialData(db: AppDatabase) {
            val sampleProducts = listOf(
                ProductEntity(
                    name = "زيت المائدة إيليو 5 لتر",
                    barcode = "6130001001",
                    category = "مواد غذائية",
                    purchasePrice = 580.0,
                    sellingPrice = 650.0,
                    stockQuantity = 24,
                    minStockAlert = 5,
                    unit = "قارورة"
                ),
                ProductEntity(
                    name = "سكر أبيض 1 كلغ",
                    barcode = "6130001002",
                    category = "مواد غذائية",
                    purchasePrice = 85.0,
                    sellingPrice = 100.0,
                    stockQuantity = 45,
                    minStockAlert = 10,
                    unit = "كيس"
                ),
                ProductEntity(
                    name = "قهوة فاميكو 250غ",
                    barcode = "6130001003",
                    category = "مواد غذائية",
                    purchasePrice = 190.0,
                    sellingPrice = 230.0,
                    stockQuantity = 30,
                    minStockAlert = 6,
                    unit = "علبة"
                ),
                ProductEntity(
                    name = "حليب معقم كانديا 1 لتر",
                    barcode = "6130001004",
                    category = "ألبان ومشتقاتها",
                    purchasePrice = 110.0,
                    sellingPrice = 130.0,
                    stockQuantity = 18,
                    minStockAlert = 8,
                    unit = "علبة"
                ),
                ProductEntity(
                    name = "جبن شيدار طازج 200غ",
                    barcode = "6130001005",
                    category = "ألبان ومشتقاتها",
                    purchasePrice = 240.0,
                    sellingPrice = 300.0,
                    stockQuantity = 3, // Low stock for alert demonstration
                    minStockAlert = 5,
                    unit = "قطعة"
                ),
                ProductEntity(
                    name = "مياه معدنية لالة خديجة 1.5 لتر",
                    barcode = "6130001006",
                    category = "مشروبات",
                    purchasePrice = 35.0,
                    sellingPrice = 50.0,
                    stockQuantity = 60,
                    minStockAlert = 12,
                    unit = "قارورة"
                ),
                ProductEntity(
                    name = "مشروب غازي حمود بوعلام 1 لتر",
                    barcode = "6130001007",
                    category = "مشروبات",
                    purchasePrice = 90.0,
                    sellingPrice = 120.0,
                    stockQuantity = 2, // Very low stock
                    minStockAlert = 6,
                    unit = "قارورة"
                ),
                ProductEntity(
                    name = "شامبو دوف 400 مل",
                    barcode = "6130001008",
                    category = "منظفات وعناية",
                    purchasePrice = 380.0,
                    sellingPrice = 480.0,
                    stockQuantity = 14,
                    minStockAlert = 4,
                    unit = "عبوة"
                ),
                ProductEntity(
                    name = "مسحوق غسيل أومـو 2.5 كلغ",
                    barcode = "6130001009",
                    category = "منظفات وعناية",
                    purchasePrice = 550.0,
                    sellingPrice = 670.0,
                    stockQuantity = 8,
                    minStockAlert = 3,
                    unit = "كيس"
                ),
                ProductEntity(
                    name = "بسكويت برينس بالشوكولاتة",
                    barcode = "6130001010",
                    category = "حلويات ومسليات",
                    purchasePrice = 55.0,
                    sellingPrice = 70.0,
                    stockQuantity = 0, // Out of stock
                    minStockAlert = 10,
                    unit = "علبة"
                )
            )
            db.productDao().insertAll(sampleProducts)

            val sampleCustomers = listOf(
                CustomerEntity(
                    name = "محمد بن علي",
                    phone = "0661234567",
                    totalDebt = 4500.0,
                    notes = "جار المحل - يدفع شهرياً"
                ),
                CustomerEntity(
                    name = "كريم سعيدي",
                    phone = "0550987654",
                    totalDebt = 1850.0,
                    notes = "زبون وفي"
                ),
                CustomerEntity(
                    name = "عمر مرابط",
                    phone = "0770112233",
                    totalDebt = 0.0,
                    notes = "دفع نقدي دائماً"
                )
            )
            db.customerDao().insertAll(sampleCustomers)

            // Add sample expense
            db.expenseDao().insert(
                ExpenseEntity(
                    title = "فاتورة الكهرباء لشهر أوت",
                    category = "كهرباء وماء",
                    amount = 3200.0,
                    timestamp = System.currentTimeMillis() - 86400000L * 2,
                    notes = "سونلغاز"
                )
            )
            db.expenseDao().insert(
                ExpenseEntity(
                    title = "أكياس بلاستيكية ومستلزمات التغليف",
                    category = "مستلزمات",
                    amount = 1500.0,
                    timestamp = System.currentTimeMillis() - 86400000L,
                    notes = "شراء بالجملة"
                )
            )
        }
    }
}
