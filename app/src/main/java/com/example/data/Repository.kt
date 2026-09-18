package com.example.data

import com.example.data.dao.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StoreRepository(
    private val productDao: ProductDao,
    private val customerDao: CustomerDao,
    private val customerPaymentDao: CustomerPaymentDao,
    private val saleDao: SaleDao,
    private val expenseDao: ExpenseDao
) {
    // Products
    val allProducts: Flow<List<ProductEntity>> = productDao.getAllProducts()

    fun searchProducts(query: String): Flow<List<ProductEntity>> =
        productDao.searchProducts(query)

    suspend fun getProductByBarcode(barcode: String): ProductEntity? =
        productDao.getProductByBarcode(barcode)

    suspend fun insertProduct(product: ProductEntity): Long =
        productDao.insert(product)

    suspend fun updateProduct(product: ProductEntity) =
        productDao.update(product)

    suspend fun deleteProduct(product: ProductEntity) =
        productDao.delete(product)

    suspend fun quickAddStock(productId: Long, quantity: Int) =
        productDao.increaseStock(productId, quantity)

    // Customers
    val allCustomers: Flow<List<CustomerEntity>> = customerDao.getAllCustomers()

    fun getCustomerById(id: Long): Flow<CustomerEntity?> =
        customerDao.getCustomerById(id)

    suspend fun insertCustomer(customer: CustomerEntity): Long =
        customerDao.insert(customer)

    suspend fun updateCustomer(customer: CustomerEntity) =
        customerDao.update(customer)

    suspend fun deleteCustomer(customer: CustomerEntity) =
        customerDao.delete(customer)

    // Customer payments
    fun getPaymentsForCustomer(customerId: Long): Flow<List<CustomerPaymentEntity>> =
        customerPaymentDao.getPaymentsForCustomer(customerId)

    suspend fun recordCustomerPayment(customerId: Long, amount: Double, notes: String) {
        customerPaymentDao.insert(
            CustomerPaymentEntity(
                customerId = customerId,
                amount = amount,
                timestamp = System.currentTimeMillis(),
                notes = notes
            )
        )
        customerDao.reduceDebt(customerId, amount)
    }

    // Sales
    val allSales: Flow<List<SaleEntity>> = saleDao.getAllSales()

    fun getSaleItems(saleId: Long): Flow<List<SaleItemEntity>> =
        saleDao.getSaleItems(saleId)

    suspend fun getSaleItemsDirect(saleId: Long): List<SaleItemEntity> =
        saleDao.getSaleItemsDirect(saleId)

    fun getSalesByCustomer(customerId: Long): Flow<List<SaleEntity>> =
        saleDao.getSalesByCustomer(customerId)

    suspend fun checkoutSale(
        items: List<CartItem>,
        customerId: Long?,
        customerName: String,
        discount: Double,
        paidAmount: Double,
        paymentType: String,
        notes: String
    ): SaleEntity {
        val totalAmount = items.sumOf { it.subtotal }
        val costAmount = items.sumOf { it.costTotal }
        val finalAmount = (totalAmount - discount).coerceAtLeast(0.0)
        val debtAmount = (finalAmount - paidAmount).coerceAtLeast(0.0)

        val timestamp = System.currentTimeMillis()
        val invoiceNumber = "INV-" + SimpleDateFormat("yyMMdd-HHmmss", Locale.ENGLISH).format(Date(timestamp))

        val sale = SaleEntity(
            invoiceNumber = invoiceNumber,
            customerId = customerId,
            customerName = customerName.ifBlank { "زبون عادي" },
            totalAmount = finalAmount,
            costAmount = costAmount,
            discountAmount = discount,
            paidAmount = paidAmount,
            debtAmount = debtAmount,
            paymentType = paymentType,
            timestamp = timestamp,
            notes = notes
        )

        val saleId = saleDao.insertSale(sale)

        val saleItems = items.map {
            SaleItemEntity(
                saleId = saleId,
                productId = it.product.id,
                productName = it.product.name,
                unitPrice = it.customPrice,
                purchasePrice = it.product.purchasePrice,
                quantity = it.quantity,
                subtotal = it.subtotal
            )
        }
        saleDao.insertSaleItems(saleItems)

        // Decrease product stock
        for (item in items) {
            productDao.decreaseStock(item.product.id, item.quantity)
        }

        // If debt is recorded for a customer, update customer's debt
        if (customerId != null && debtAmount > 0) {
            customerDao.addDebt(customerId, debtAmount)
        }

        return sale.copy(id = saleId)
    }

    suspend fun deleteSale(sale: SaleEntity) {
        saleDao.deleteSale(sale)
    }

    // Expenses
    val allExpenses: Flow<List<ExpenseEntity>> = expenseDao.getAllExpenses()

    suspend fun insertExpense(expense: ExpenseEntity): Long =
        expenseDao.insert(expense)

    suspend fun deleteExpense(expense: ExpenseEntity) =
        expenseDao.delete(expense)
}
