package com.example.data.repository

import com.example.data.dao.CustomerDao
import com.example.data.dao.ProductDao
import com.example.data.dao.SaleDao
import com.example.data.entity.Customer
import com.example.data.entity.Product
import com.example.data.entity.Sale
import com.example.data.entity.SaleItem
import com.example.data.entity.SaleWithItems
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class VarotraRepository(
    private val productDao: ProductDao,
    private val customerDao: CustomerDao,
    private val saleDao: SaleDao
) {
    // Products
    val allProducts: Flow<List<Product>> = productDao.getAllProducts()
    val lowStockProducts: Flow<List<Product>> = productDao.getLowStockProducts()
    val totalProductsCount: Flow<Int> = productDao.getTotalProductsCount()
    val lowStockCount: Flow<Int> = productDao.getLowStockCount()

    fun getProductById(id: Long): Flow<Product?> = productDao.getProductById(id)

    suspend fun insertProduct(product: Product): Long = productDao.insertProduct(product)
    suspend fun updateProduct(product: Product) = productDao.updateProduct(product)
    suspend fun deleteProduct(product: Product) = productDao.deleteProduct(product)
    suspend fun updateStock(productId: Long, newStock: Int) = productDao.updateStock(productId, newStock)

    // Customers
    val allCustomers: Flow<List<Customer>> = customerDao.getAllCustomers()
    val customerCount: Flow<Int> = customerDao.getCustomerCount()

    fun getCustomerById(id: Long): Flow<Customer?> = customerDao.getCustomerById(id)
    suspend fun insertCustomer(customer: Customer): Long = customerDao.insertCustomer(customer)
    suspend fun updateCustomer(customer: Customer) = customerDao.updateCustomer(customer)
    suspend fun deleteCustomer(customer: Customer) = customerDao.deleteCustomer(customer)

    // Sales
    val allSales: Flow<List<SaleWithItems>> = saleDao.getAllSalesWithItems()

    fun getSalesForCustomer(customerId: Long): Flow<List<SaleWithItems>> =
        saleDao.getSalesForCustomer(customerId)

    fun getSaleById(id: Long): Flow<SaleWithItems?> = saleDao.getSaleWithItemsById(id)
    suspend fun getSaleByIdDirect(id: Long): SaleWithItems? = saleDao.getSaleWithItemsByIdDirect(id)

    suspend fun createSaleWithItems(
        sale: Sale,
        items: List<SaleItem>
    ): Long {
        val saleId = saleDao.insertSale(sale)
        val itemsWithSaleId = items.map { it.copy(saleId = saleId) }
        saleDao.insertSaleItems(itemsWithSaleId)

        // Decrement stock for each product
        for (item in items) {
            productDao.decrementStock(item.productId, item.quantity)
        }

        return saleId
    }

    suspend fun deleteSale(sale: Sale) = saleDao.deleteSale(sale)

    // Dashboard metrics
    fun getTodaySalesCount(): Flow<Int> {
        val (start, end) = getTodayBounds()
        return saleDao.getTodaySalesCount(start, end)
    }

    fun getTodayRevenue(): Flow<Long?> {
        val (start, end) = getTodayBounds()
        return saleDao.getTodayRevenue(start, end)
    }

    fun getWeekRevenue(): Flow<Long?> {
        val (start, end) = getWeekBounds()
        return saleDao.getWeekRevenue(start, end)
    }

    fun getMonthRevenue(): Flow<Long?> {
        val (start, end) = getMonthBounds()
        return saleDao.getMonthRevenue(start, end)
    }

    companion object {
        fun getTodayBounds(): Pair<Long, Long> {
            val cal = Calendar.getInstance()
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val start = cal.timeInMillis

            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            cal.set(Calendar.MILLISECOND, 999)
            val end = cal.timeInMillis

            return Pair(start, end)
        }

        fun getWeekBounds(): Pair<Long, Long> {
            val cal = Calendar.getInstance()
            cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val start = cal.timeInMillis

            cal.add(Calendar.DAY_OF_WEEK, 6)
            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            cal.set(Calendar.MILLISECOND, 999)
            val end = cal.timeInMillis

            return Pair(start, end)
        }

        fun getMonthBounds(): Pair<Long, Long> {
            val cal = Calendar.getInstance()
            cal.set(Calendar.DAY_OF_MONTH, 1)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val start = cal.timeInMillis

            cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            cal.set(Calendar.MILLISECOND, 999)
            val end = cal.timeInMillis

            return Pair(start, end)
        }
    }
}
