package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.entity.Sale
import com.example.data.entity.SaleItem
import com.example.data.entity.SaleWithItems
import kotlinx.coroutines.flow.Flow

@Dao
interface SaleDao {
    @Transaction
    @Query("SELECT * FROM sales ORDER BY timestamp DESC")
    fun getAllSalesWithItems(): Flow<List<SaleWithItems>>

    @Transaction
    @Query("SELECT * FROM sales WHERE customerId = :customerId ORDER BY timestamp DESC")
    fun getSalesForCustomer(customerId: Long): Flow<List<SaleWithItems>>

    @Transaction
    @Query("SELECT * FROM sales WHERE id = :id")
    fun getSaleWithItemsById(id: Long): Flow<SaleWithItems?>

    @Transaction
    @Query("SELECT * FROM sales WHERE id = :id")
    suspend fun getSaleWithItemsByIdDirect(id: Long): SaleWithItems?

    @Query("SELECT * FROM sales WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp DESC")
    fun getSalesBetween(startTime: Long, endTime: Long): Flow<List<Sale>>

    @Query("SELECT COUNT(*) FROM sales WHERE timestamp >= :startOfDay AND timestamp <= :endOfDay")
    fun getTodaySalesCount(startOfDay: Long, endOfDay: Long): Flow<Int>

    @Query("SELECT SUM(totalAmount) FROM sales WHERE timestamp >= :startOfDay AND timestamp <= :endOfDay")
    fun getTodayRevenue(startOfDay: Long, endOfDay: Long): Flow<Long?>

    @Query("SELECT SUM(totalAmount) FROM sales WHERE timestamp >= :startOfWeek AND timestamp <= :endOfWeek")
    fun getWeekRevenue(startOfWeek: Long, endOfWeek: Long): Flow<Long?>

    @Query("SELECT SUM(totalAmount) FROM sales WHERE timestamp >= :startOfMonth AND timestamp <= :endOfMonth")
    fun getMonthRevenue(startOfMonth: Long, endOfMonth: Long): Flow<Long?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSale(sale: Sale): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSaleItems(items: List<SaleItem>)

    @Update
    suspend fun updateSale(sale: Sale)

    @Delete
    suspend fun deleteSale(sale: Sale)
}
