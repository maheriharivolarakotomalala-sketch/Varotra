package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sales")
data class Sale(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val invoiceNumber: String,
    val customerId: Long? = null,
    val customerName: String = "Client comptoir",
    val customerPhone: String = "",
    val totalAmount: Long = 0, // Total en Ariary
    val isPaid: Boolean = true, // Payé ou À crédit
    val paymentMethod: String = "Espèces", // Espèces, MVola, Orange Money, Airtel Money, Virement
    val timestamp: Long = System.currentTimeMillis(),
    val notes: String = ""
)
