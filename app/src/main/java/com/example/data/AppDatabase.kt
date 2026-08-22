package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.CustomerDao
import com.example.data.dao.ProductDao
import com.example.data.dao.SaleDao
import com.example.data.entity.Customer
import com.example.data.entity.Product
import com.example.data.entity.Sale
import com.example.data.entity.SaleItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

@Database(
    entities = [
        Product::class,
        Customer::class,
        Sale::class,
        SaleItem::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun productDao(): ProductDao
    abstract fun customerDao(): CustomerDao
    abstract fun saleDao(): SaleDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "varotra_database"
                )
                    .addCallback(AppDatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class AppDatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {

        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateInitialData(database)
                }
            }
        }

        suspend fun populateInitialData(database: AppDatabase) {
            val productDao = database.productDao()
            val customerDao = database.customerDao()
            val saleDao = database.saleDao()

            // 1. Initial Products
            val sampleProducts = listOf(
                Product(
                    name = "Riz Makalioka (Sac 25kg)",
                    category = "Alimentation",
                    purchasePrice = 75000,
                    sellingPrice = 85000,
                    stockQuantity = 14,
                    alertThreshold = 5,
                    unit = "sac"
                ),
                Product(
                    name = "Huile de table 1 Litre",
                    category = "Alimentation",
                    purchasePrice = 7800,
                    sellingPrice = 9500,
                    stockQuantity = 28,
                    alertThreshold = 10,
                    unit = "bouteille"
                ),
                Product(
                    name = "Sucre roux 1 kg",
                    category = "Alimentation",
                    purchasePrice = 3500,
                    sellingPrice = 4200,
                    stockQuantity = 45,
                    alertThreshold = 15,
                    unit = "kg"
                ),
                Product(
                    name = "Savon Nosy Bleu",
                    category = "Hygiène & Entretien",
                    purchasePrice = 1100,
                    sellingPrice = 1500,
                    stockQuantity = 60,
                    alertThreshold = 20,
                    unit = "morceau"
                ),
                Product(
                    name = "Lait concentré Socolait 390g",
                    category = "Alimentation",
                    purchasePrice = 3800,
                    sellingPrice = 4600,
                    stockQuantity = 18,
                    alertThreshold = 8,
                    unit = "boîte"
                ),
                Product(
                    name = "Café Robusta Moulu 250g",
                    category = "Boissons",
                    purchasePrice = 4000,
                    sellingPrice = 5200,
                    stockQuantity = 22,
                    alertThreshold = 6,
                    unit = "sachet"
                ),
                Product(
                    name = "Pâtes Nouilles 500g",
                    category = "Alimentation",
                    purchasePrice = 2200,
                    sellingPrice = 2800,
                    stockQuantity = 3, // LOW STOCK to trigger alert banner
                    alertThreshold = 8,
                    unit = "paquet"
                ),
                Product(
                    name = "Piles Alcalines AA (Pack de 4)",
                    category = "Divers & Maison",
                    purchasePrice = 2800,
                    sellingPrice = 4000,
                    stockQuantity = 2, // LOW STOCK
                    alertThreshold = 5,
                    unit = "pack"
                ),
                Product(
                    name = "Jus Naturel Pok-Pok 1L",
                    category = "Boissons",
                    purchasePrice = 5000,
                    sellingPrice = 6500,
                    stockQuantity = 12,
                    alertThreshold = 4,
                    unit = "bouteille"
                ),
                Product(
                    name = "Bougies Blanches (Boîte de 6)",
                    category = "Divers & Maison",
                    purchasePrice = 1800,
                    sellingPrice = 2500,
                    stockQuantity = 1, // LOW STOCK
                    alertThreshold = 5,
                    unit = "boîte"
                )
            )

            productDao.insertAll(sampleProducts)

            // 2. Initial Customers
            val sampleCustomers = listOf(
                Customer(
                    name = "Mme Razafindratsimba",
                    phone = "034 12 345 67",
                    address = "Analakely, Antananarivo",
                    notes = "Client fidèle - Épicerie de quartier"
                ),
                Customer(
                    name = "Rabe Jean",
                    phone = "032 88 765 43",
                    address = "Ankorondrano",
                    notes = "Règlement souvent par MVola"
                ),
                Customer(
                    name = "Restaurant Le Baobab (Andry)",
                    phone = "033 45 678 90",
                    address = "Isoraka, Antananarivo",
                    notes = "Achats en gros hebdomadaires"
                )
            )

            val customerId1 = customerDao.insertCustomer(sampleCustomers[0])
            val customerId2 = customerDao.insertCustomer(sampleCustomers[1])
            customerDao.insertCustomer(sampleCustomers[2])

            // 3. Initial Sample Sales (Today & Recent)
            val now = System.currentTimeMillis()
            val cal = Calendar.getInstance()
            val todayStr = String.format("%04d%02d%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH))

            // Sale 1: Today
            val sale1 = Sale(
                invoiceNumber = "FAC-$todayStr-001",
                customerId = customerId1,
                customerName = "Mme Razafindratsimba",
                customerPhone = "034 12 345 67",
                totalAmount = 94500,
                isPaid = true,
                paymentMethod = "Espèces",
                timestamp = now - (2 * 3600 * 1000), // 2 hours ago
                notes = "Livré en boutique"
            )
            val saleId1 = saleDao.insertSale(sale1)
            saleDao.insertSaleItems(
                listOf(
                    SaleItem(
                        saleId = saleId1,
                        productId = 1,
                        productName = "Riz Makalioka (Sac 25kg)",
                        quantity = 1,
                        unitPrice = 85000,
                        totalPrice = 85000,
                        unit = "sac"
                    ),
                    SaleItem(
                        saleId = saleId1,
                        productId = 2,
                        productName = "Huile de table 1 Litre",
                        quantity = 1,
                        unitPrice = 9500,
                        totalPrice = 9500,
                        unit = "bouteille"
                    )
                )
            )

            // Sale 2: Today (Fast counter sale)
            val sale2 = Sale(
                invoiceNumber = "FAC-$todayStr-002",
                customerId = null,
                customerName = "Client comptoir",
                customerPhone = "",
                totalAmount = 14200,
                isPaid = true,
                paymentMethod = "MVola",
                timestamp = now - (45 * 60 * 1000), // 45 min ago
                notes = ""
            )
            val saleId2 = saleDao.insertSale(sale2)
            saleDao.insertSaleItems(
                listOf(
                    SaleItem(
                        saleId = saleId2,
                        productId = 4,
                        productName = "Savon Nosy Bleu",
                        quantity = 2,
                        unitPrice = 1500,
                        totalPrice = 3000,
                        unit = "morceau"
                    ),
                    SaleItem(
                        saleId = saleId2,
                        productId = 3,
                        productName = "Sucre roux 1 kg",
                        quantity = 1,
                        unitPrice = 4200,
                        totalPrice = 4200,
                        unit = "kg"
                    ),
                    SaleItem(
                        saleId = saleId2,
                        productId = 6,
                        productName = "Café Robusta Moulu 250g",
                        quantity = 1,
                        unitPrice = 5200,
                        totalPrice = 5200,
                        unit = "sachet"
                    ),
                    SaleItem(
                        saleId = saleId2,
                        productId = 7,
                        productName = "Bougies Blanches (Boîte de 6)",
                        quantity = 1,
                        unitPrice = 2500,
                        totalPrice = 2500,
                        unit = "boîte"
                    )
                )
            )

            // Sale 3: Yesterday
            val yesterdayCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
            val yesterdayStr = String.format("%04d%02d%02d", yesterdayCal.get(Calendar.YEAR), yesterdayCal.get(Calendar.MONTH) + 1, yesterdayCal.get(Calendar.DAY_OF_MONTH))
            val sale3 = Sale(
                invoiceNumber = "FAC-$yesterdayStr-001",
                customerId = customerId2,
                customerName = "Rabe Jean",
                customerPhone = "032 88 765 43",
                totalAmount = 26000,
                isPaid = true,
                paymentMethod = "MVola",
                timestamp = yesterdayCal.timeInMillis,
                notes = "Paiement confirmé"
            )
            val saleId3 = saleDao.insertSale(sale3)
            saleDao.insertSaleItems(
                listOf(
                    SaleItem(
                        saleId = saleId3,
                        productId = 9,
                        productName = "Jus Naturel Pok-Pok 1L",
                        quantity = 4,
                        unitPrice = 6500,
                        totalPrice = 26000,
                        unit = "bouteille"
                    )
                )
            )
        }
    }
}
