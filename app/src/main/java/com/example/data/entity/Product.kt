package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val category: String,
    val purchasePrice: Long, // Prix d'achat en Ariary
    val sellingPrice: Long,  // Prix de vente en Ariary
    val stockQuantity: Int,  // Quantité en stock
    val alertThreshold: Int = 5, // Seuil d'alerte stock bas
    val unit: String = "unité",  // unité, kg, litre, paquet, sac, boîte
    val photoUri: String? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    val isLowStock: Boolean
        get() = stockQuantity <= alertThreshold

    val profitMargin: Long
        get() = sellingPrice - purchasePrice
}
