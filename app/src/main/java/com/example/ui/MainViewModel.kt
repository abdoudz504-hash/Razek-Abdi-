package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.StoreRepository
import com.example.data.model.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

sealed class BarcodeScanResult {
    data class AddedNew(val product: ProductEntity) : BarcodeScanResult()
    data class AlreadyInCart(val product: ProductEntity, val currentQuantity: Int) : BarcodeScanResult()
    data class NotFound(val barcode: String) : BarcodeScanResult()
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: StoreRepository

    init {
        val db = AppDatabase.getDatabase(application)
        repository = StoreRepository(
            productDao = db.productDao(),
            customerDao = db.customerDao(),
            customerPaymentDao = db.customerPaymentDao(),
            saleDao = db.saleDao(),
            expenseDao = db.expenseDao()
        )
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            AppDatabase.seedInitialDataIfEmpty(db)
        }
    }

    // Currency symbol
    val currency = "د.ج"

    // Data streams
    val products: StateFlow<List<ProductEntity>> = repository.allProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customers: StateFlow<List<CustomerEntity>> = repository.allCustomers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sales: StateFlow<List<SaleEntity>> = repository.allSales
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val expenses: StateFlow<List<ExpenseEntity>> = repository.allExpenses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Cart state
    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems.asStateFlow()

    private val _selectedCustomerForSale = MutableStateFlow<CustomerEntity?>(null)
    val selectedCustomerForSale: StateFlow<CustomerEntity?> = _selectedCustomerForSale.asStateFlow()

    // Last completed sale (for receipt viewing)
    private val _lastSale = MutableStateFlow<SaleEntity?>(null)
    val lastSale: StateFlow<SaleEntity?> = _lastSale.asStateFlow()

    // Active customer for detailed ledger
    private val _activeCustomer = MutableStateFlow<CustomerEntity?>(null)
    val activeCustomer: StateFlow<CustomerEntity?> = _activeCustomer.asStateFlow()

    // Customer payments flow for active customer
    val activeCustomerPayments: StateFlow<List<CustomerPaymentEntity>> = _activeCustomer
        .flatMapLatest { customer ->
            if (customer != null) repository.getPaymentsForCustomer(customer.id)
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeCustomerSales: StateFlow<List<SaleEntity>> = _activeCustomer
        .flatMapLatest { customer ->
            if (customer != null) repository.getSalesByCustomer(customer.id)
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Cart operations: Scan barcode once per product.
    // If the product is already in the cart, do NOT auto-increment!
    // Returning AlreadyInCart allows manual hand adjustment (+ / -).
    fun scanBarcode(barcode: String): BarcodeScanResult {
        val trimmed = barcode.trim()
        if (trimmed.isEmpty()) return BarcodeScanResult.NotFound(barcode)
        val product = products.value.find { it.barcode.equals(trimmed, ignoreCase = true) }
            ?: return BarcodeScanResult.NotFound(trimmed)

        val current = _cartItems.value.toMutableList()
        val index = current.indexOfFirst { it.product.id == product.id }
        return if (index >= 0) {
            // Already in cart: Do NOT increment automatically
            BarcodeScanResult.AlreadyInCart(product = product, currentQuantity = current[index].quantity)
        } else {
            // New item: Add once with quantity 1
            current.add(CartItem(product = product, quantity = 1))
            _cartItems.value = current
            BarcodeScanResult.AddedNew(product = product)
        }
    }

    fun addToCart(product: ProductEntity) {
        val current = _cartItems.value.toMutableList()
        val index = current.indexOfFirst { it.product.id == product.id }
        if (index >= 0) {
            val item = current[index]
            current[index] = item.copy(quantity = item.quantity + 1)
        } else {
            current.add(CartItem(product = product, quantity = 1))
        }
        _cartItems.value = current
    }

    fun updateCartQuantity(productId: Long, qty: Int) {
        if (qty <= 0) {
            removeFromCart(productId)
            return
        }
        val current = _cartItems.value.toMutableList()
        val index = current.indexOfFirst { it.product.id == productId }
        if (index >= 0) {
            current[index] = current[index].copy(quantity = qty)
            _cartItems.value = current
        }
    }

    fun removeFromCart(productId: Long) {
        _cartItems.value = _cartItems.value.filterNot { it.product.id == productId }
    }

    fun clearCart() {
        _cartItems.value = emptyList()
        _selectedCustomerForSale.value = null
    }

    fun selectCustomerForSale(customer: CustomerEntity?) {
        _selectedCustomerForSale.value = customer
    }

    fun completeSale(
        discount: Double,
        paidAmount: Double,
        paymentType: String,
        notes: String,
        onSuccess: (SaleEntity) -> Unit
    ) {
        val currentItems = _cartItems.value
        if (currentItems.isEmpty()) return

        val customer = _selectedCustomerForSale.value
        viewModelScope.launch {
            val sale = repository.checkoutSale(
                items = currentItems,
                customerId = customer?.id,
                customerName = customer?.name ?: "زبون عادي",
                discount = discount,
                paidAmount = paidAmount,
                paymentType = paymentType,
                notes = notes
            )
            _lastSale.value = sale
            clearCart()
            onSuccess(sale)
        }
    }

    // Product operations
    fun saveProduct(product: ProductEntity, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            if (product.id == 0L) {
                repository.insertProduct(product)
            } else {
                repository.updateProduct(product)
            }
            onDone()
        }
    }

    fun deleteProduct(product: ProductEntity) {
        viewModelScope.launch {
            repository.deleteProduct(product)
        }
    }

    fun quickAddStock(productId: Long, quantity: Int) {
        viewModelScope.launch {
            repository.quickAddStock(productId, quantity)
        }
    }

    // Customer operations
    fun saveCustomer(customer: CustomerEntity, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            if (customer.id == 0L) {
                repository.insertCustomer(customer)
            } else {
                repository.updateCustomer(customer)
            }
            onDone()
        }
    }

    fun deleteCustomer(customer: CustomerEntity) {
        viewModelScope.launch {
            repository.deleteCustomer(customer)
        }
    }

    fun setActiveCustomer(customer: CustomerEntity?) {
        _activeCustomer.value = customer
    }

    fun recordPayment(customerId: Long, amount: Double, notes: String, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repository.recordCustomerPayment(customerId, amount, notes)
            // Update active customer if currently viewed
            val current = _activeCustomer.value
            if (current != null && current.id == customerId) {
                val newDebt = (current.totalDebt - amount).coerceAtLeast(0.0)
                _activeCustomer.value = current.copy(totalDebt = newDebt)
            }
            onDone()
        }
    }

    // Expense operations
    fun saveExpense(expense: ExpenseEntity, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repository.insertExpense(expense)
            onDone()
        }
    }

    fun deleteExpense(expense: ExpenseEntity) {
        viewModelScope.launch {
            repository.deleteExpense(expense)
        }
    }

    // Sale items query for receipt
    suspend fun getSaleItems(saleId: Long): List<SaleItemEntity> {
        return repository.getSaleItemsDirect(saleId)
    }

    // Reports & KPI aggregations
    data class ShopKpis(
        val todaySales: Double = 0.0,
        val todayCost: Double = 0.0,
        val todayExpenses: Double = 0.0,
        val todayNetProfit: Double = 0.0,
        val totalDebts: Double = 0.0,
        val lowStockCount: Int = 0,
        val totalProductsCount: Int = 0,
        val totalStockCostValue: Double = 0.0,
        val totalStockRetailValue: Double = 0.0,
        val monthSales: Double = 0.0,
        val monthNetProfit: Double = 0.0
    )

    val kpis: StateFlow<ShopKpis> = combine(
        products,
        customers,
        sales,
        expenses
    ) { prodList, custList, saleList, expList ->
        val cal = Calendar.getInstance()
        val now = cal.timeInMillis

        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startOfToday = cal.timeInMillis

        cal.set(Calendar.DAY_OF_MONTH, 1)
        val startOfMonth = cal.timeInMillis

        val todaySalesList = saleList.filter { it.timestamp >= startOfToday }
        val todaySalesAmount = todaySalesList.sumOf { it.totalAmount }
        val todayCostAmount = todaySalesList.sumOf { it.costAmount }

        val todayExpensesList = expList.filter { it.timestamp >= startOfToday }
        val todayExpensesAmount = todayExpensesList.sumOf { it.amount }

        val todayNetProfit = (todaySalesAmount - todayCostAmount - todayExpensesAmount)

        val monthSalesList = saleList.filter { it.timestamp >= startOfMonth }
        val monthSalesAmount = monthSalesList.sumOf { it.totalAmount }
        val monthCostAmount = monthSalesList.sumOf { it.costAmount }
        val monthExpensesList = expList.filter { it.timestamp >= startOfMonth }
        val monthExpensesAmount = monthExpensesList.sumOf { it.amount }
        val monthNetProfit = (monthSalesAmount - monthCostAmount - monthExpensesAmount)

        val totalDebts = custList.sumOf { it.totalDebt }
        val lowStock = prodList.count { it.isLowStock || it.isOutOfStock }
        val stockCostValue = prodList.sumOf { it.stockQuantity * it.purchasePrice }
        val stockRetailValue = prodList.sumOf { it.stockQuantity * it.sellingPrice }

        ShopKpis(
            todaySales = todaySalesAmount,
            todayCost = todayCostAmount,
            todayExpenses = todayExpensesAmount,
            todayNetProfit = todayNetProfit,
            totalDebts = totalDebts,
            lowStockCount = lowStock,
            totalProductsCount = prodList.size,
            totalStockCostValue = stockCostValue,
            totalStockRetailValue = stockRetailValue,
            monthSales = monthSalesAmount,
            monthNetProfit = monthNetProfit
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ShopKpis())
}
