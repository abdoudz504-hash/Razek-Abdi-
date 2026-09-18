package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val barcode: String = "",
    val category: String = "عام",
    val purchasePrice: Double = 0.0,
    val sellingPrice: Double = 0.0,
    val stockQuantity: Int = 0,
    val minStockAlert: Int = 5,
    val unit: String = "قطعة",
    val updatedAt: Long = System.currentTimeMillis()
) {
    val isLowStock: Boolean
        get() = stockQuantity in 1..minStockAlert

    val isOutOfStock: Boolean
        get() = stockQuantity <= 0

    val profitMargin: Double
        get() = if (purchasePrice > 0) ((sellingPrice - purchasePrice) / purchasePrice) * 100 else 0.0
}

@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String = "",
    val totalDebt: Double = 0.0,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "sales")
data class SaleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val invoiceNumber: String,
    val customerId: Long? = null,
    val customerName: String = "زبون عادي",
    val totalAmount: Double = 0.0,
    val costAmount: Double = 0.0,
    val discountAmount: Double = 0.0,
    val paidAmount: Double = 0.0,
    val debtAmount: Double = 0.0,
    val paymentType: String = "CASH", // CASH, DEBT, CARD
    val timestamp: Long = System.currentTimeMillis(),
    val notes: String = ""
)

@Entity(
    tableName = "sale_items",
    foreignKeys = [
        ForeignKey(
            entity = SaleEntity::class,
            parentColumns = ["id"],
            childColumns = ["saleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("saleId")]
)
data class SaleItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val saleId: Long,
    val productId: Long,
    val productName: String,
    val unitPrice: Double,
    val purchasePrice: Double,
    val quantity: Int,
    val subtotal: Double
)

@Entity(tableName = "customer_payments")
data class CustomerPaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val customerId: Long,
    val amount: Double,
    val timestamp: Long = System.currentTimeMillis(),
    val notes: String = ""
)

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val category: String, // إيجار، كهرباء وماء، رواتب، نقل، صيانة، أخرى
    val amount: Double,
    val timestamp: Long = System.currentTimeMillis(),
    val notes: String = ""
)

data class CartItem(
    val product: ProductEntity,
    val quantity: Int = 1,
    val customPrice: Double = product.sellingPrice
) {
    val subtotal: Double
        get() = quantity * customPrice

    val costTotal: Double
        get() = quantity * product.purchasePrice
}
