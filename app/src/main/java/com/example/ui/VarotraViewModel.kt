package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.entity.Customer
import com.example.data.entity.Product
import com.example.data.entity.Sale
import com.example.data.entity.SaleItem
import com.example.data.entity.SaleWithItems
import com.example.data.repository.VarotraRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.util.Calendar

data class CartItem(
    val product: Product,
    val quantity: Int
) {
    val totalPrice: Long
        get() = product.sellingPrice * quantity
}

enum class SalePeriodFilter(val label: String) {
    TODAY("Aujourd'hui"),
    WEEK("Cette semaine"),
    ALL("Toutes les ventes")
}

data class DashboardUiState(
    val todayRevenue: Long = 0,
    val weekRevenue: Long = 0,
    val monthRevenue: Long = 0,
    val todaySalesCount: Int = 0,
    val lowStockProducts: List<Product> = emptyList(),
    val recentSales: List<SaleWithItems> = emptyList(),
    val totalProductsCount: Int = 0,
    val totalCustomersCount: Int = 0
)

class VarotraViewModel(
    application: Application,
    private val repository: VarotraRepository
) : AndroidViewModel(application) {

    // 1. Products State
    val allProducts: StateFlow<List<Product>> = repository.allProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val lowStockProducts: StateFlow<List<Product>> = repository.lowStockProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _productSearchQuery = MutableStateFlow("")
    val productSearchQuery: StateFlow<String> = _productSearchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("Tous")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _onlyLowStockFilter = MutableStateFlow(false)
    val onlyLowStockFilter: StateFlow<Boolean> = _onlyLowStockFilter.asStateFlow()

    val filteredProducts: StateFlow<List<Product>> = combine(
        allProducts,
        _productSearchQuery,
        _selectedCategory,
        _onlyLowStockFilter
    ) { products, query, category, onlyLowStock ->
        products.filter { product ->
            val matchesQuery = query.isBlank() || product.name.contains(query, ignoreCase = true) || product.category.contains(query, ignoreCase = true)
            val matchesCategory = category == "Tous" || product.category.equals(category, ignoreCase = true)
            val matchesLowStock = !onlyLowStock || product.isLowStock
            matchesQuery && matchesCategory && matchesLowStock
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 2. Customers State
    val allCustomers: StateFlow<List<Customer>> = repository.allCustomers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _customerSearchQuery = MutableStateFlow("")
    val customerSearchQuery: StateFlow<String> = _customerSearchQuery.asStateFlow()

    val filteredCustomers: StateFlow<List<Customer>> = combine(
        allCustomers,
        _customerSearchQuery
    ) { customers, query ->
        if (query.isBlank()) {
            customers
        } else {
            customers.filter {
                it.name.contains(query, ignoreCase = true) ||
                        it.phone.contains(query, ignoreCase = true) ||
                        it.address.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedCustomerDetails = MutableStateFlow<Customer?>(null)
    val selectedCustomerDetails: StateFlow<Customer?> = _selectedCustomerDetails.asStateFlow()

    private val _customerSalesHistory = MutableStateFlow<List<SaleWithItems>>(emptyList())
    val customerSalesHistory: StateFlow<List<SaleWithItems>> = _customerSalesHistory.asStateFlow()

    // 3. Sales State
    val allSales: StateFlow<List<SaleWithItems>> = repository.allSales
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _salesPeriodFilter = MutableStateFlow(SalePeriodFilter.ALL)
    val salesPeriodFilter: StateFlow<SalePeriodFilter> = _salesPeriodFilter.asStateFlow()

    private val _salesSearchQuery = MutableStateFlow("")
    val salesSearchQuery: StateFlow<String> = _salesSearchQuery.asStateFlow()

    val filteredSales: StateFlow<List<SaleWithItems>> = combine(
        allSales,
        _salesPeriodFilter,
        _salesSearchQuery
    ) { sales, period, query ->
        val (todayStart, todayEnd) = VarotraRepository.getTodayBounds()
        val (weekStart, weekEnd) = VarotraRepository.getWeekBounds()

        sales.filter { saleWithItems ->
            val sale = saleWithItems.sale
            val matchesPeriod = when (period) {
                SalePeriodFilter.TODAY -> sale.timestamp in todayStart..todayEnd
                SalePeriodFilter.WEEK -> sale.timestamp in weekStart..weekEnd
                SalePeriodFilter.ALL -> true
            }
            val matchesQuery = query.isBlank() ||
                    sale.invoiceNumber.contains(query, ignoreCase = true) ||
                    sale.customerName.contains(query, ignoreCase = true) ||
                    saleWithItems.items.any { it.productName.contains(query, ignoreCase = true) }

            matchesPeriod && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    data class RevenueStats(
        val todayRevenue: Long = 0,
        val weekRevenue: Long = 0,
        val monthRevenue: Long = 0,
        val todaySalesCount: Int = 0
    )

    private val revenueStats: kotlinx.coroutines.flow.Flow<RevenueStats> = combine(
        repository.getTodayRevenue(),
        repository.getWeekRevenue(),
        repository.getMonthRevenue(),
        repository.getTodaySalesCount()
    ) { todayRev, weekRev, monthRev, todayCount ->
        RevenueStats(
            todayRevenue = todayRev ?: 0L,
            weekRevenue = weekRev ?: 0L,
            monthRevenue = monthRev ?: 0L,
            todaySalesCount = todayCount
        )
    }

    // 4. Dashboard metrics UI State
    val dashboardState: StateFlow<DashboardUiState> = combine(
        revenueStats,
        lowStockProducts,
        allSales,
        repository.totalProductsCount,
        repository.customerCount
    ) { stats, lowStock, sales, prodCount, custCount ->
        DashboardUiState(
            todayRevenue = stats.todayRevenue,
            weekRevenue = stats.weekRevenue,
            monthRevenue = stats.monthRevenue,
            todaySalesCount = stats.todaySalesCount,
            lowStockProducts = lowStock,
            recentSales = sales.take(5),
            totalProductsCount = prodCount,
            totalCustomersCount = custCount
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardUiState())

    // 5. New Sale (Cart) State
    private val _cart = MutableStateFlow<Map<Long, CartItem>>(emptyMap())
    val cart: StateFlow<Map<Long, CartItem>> = _cart.asStateFlow()

    private val _newSaleCustomer = MutableStateFlow<Customer?>(null)
    val newSaleCustomer: StateFlow<Customer?> = _newSaleCustomer.asStateFlow()

    private val _newSaleCustomerName = MutableStateFlow("Client comptoir")
    val newSaleCustomerName: StateFlow<String> = _newSaleCustomerName.asStateFlow()

    private val _newSaleCustomerPhone = MutableStateFlow("")
    val newSaleCustomerPhone: StateFlow<String> = _newSaleCustomerPhone.asStateFlow()

    private val _newSaleIsPaid = MutableStateFlow(true)
    val newSaleIsPaid: StateFlow<Boolean> = _newSaleIsPaid.asStateFlow()

    private val _newSalePaymentMethod = MutableStateFlow("Espèces")
    val newSalePaymentMethod: StateFlow<String> = _newSalePaymentMethod.asStateFlow()

    private val _newSaleNotes = MutableStateFlow("")
    val newSaleNotes: StateFlow<String> = _newSaleNotes.asStateFlow()

    val cartTotalAmount: StateFlow<Long> = cart.combine(MutableStateFlow(0)) { itemsMap, _ ->
        itemsMap.values.sumOf { it.totalPrice }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val cartTotalCount: StateFlow<Int> = cart.combine(MutableStateFlow(0)) { itemsMap, _ ->
        itemsMap.values.sumOf { it.quantity }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // --- Actions ---

    // Product search & filters
    fun setProductSearchQuery(query: String) {
        _productSearchQuery.value = query
    }

    fun setSelectedCategory(category: String) {
        _selectedCategory.value = category
    }

    fun toggleOnlyLowStockFilter() {
        _onlyLowStockFilter.update { !it }
    }

    fun saveProduct(
        id: Long = 0,
        name: String,
        category: String,
        purchasePrice: Long,
        sellingPrice: Long,
        stockQuantity: Int,
        alertThreshold: Int,
        unit: String,
        photoUri: String? = null
    ) {
        viewModelScope.launch {
            val product = Product(
                id = id,
                name = name.trim(),
                category = category.trim().ifBlank { "Général" },
                purchasePrice = purchasePrice,
                sellingPrice = sellingPrice,
                stockQuantity = stockQuantity,
                alertThreshold = alertThreshold,
                unit = unit.trim().ifBlank { "unité" },
                photoUri = photoUri
            )
            if (id == 0L) {
                repository.insertProduct(product)
            } else {
                repository.updateProduct(product)
            }
        }
    }

    fun deleteProduct(product: Product) {
        viewModelScope.launch {
            repository.deleteProduct(product)
        }
    }

    fun updateProductStock(product: Product, newStock: Int) {
        viewModelScope.launch {
            repository.updateStock(product.id, newStock)
        }
    }

    // Customer operations
    fun setCustomerSearchQuery(query: String) {
        _customerSearchQuery.value = query
    }

    fun selectCustomerForDetails(customer: Customer?) {
        _selectedCustomerDetails.value = customer
        if (customer != null) {
            viewModelScope.launch {
                repository.getSalesForCustomer(customer.id).collect { sales ->
                    _customerSalesHistory.value = sales
                }
            }
        } else {
            _customerSalesHistory.value = emptyList()
        }
    }

    fun saveCustomer(
        id: Long = 0,
        name: String,
        phone: String = "",
        address: String = "",
        notes: String = ""
    ) {
        viewModelScope.launch {
            val customer = Customer(
                id = id,
                name = name.trim(),
                phone = phone.trim(),
                address = address.trim(),
                notes = notes.trim()
            )
            if (id == 0L) {
                repository.insertCustomer(customer)
            } else {
                repository.updateCustomer(customer)
            }
        }
    }

    fun deleteCustomer(customer: Customer) {
        viewModelScope.launch {
            repository.deleteCustomer(customer)
        }
    }

    // Sales operations
    fun setSalesPeriodFilter(filter: SalePeriodFilter) {
        _salesPeriodFilter.value = filter
    }

    fun setSalesSearchQuery(query: String) {
        _salesSearchQuery.value = query
    }

    fun deleteSale(sale: Sale) {
        viewModelScope.launch {
            repository.deleteSale(sale)
        }
    }

    // Cart / New Sale actions
    fun selectCustomerForNewSale(customer: Customer?) {
        _newSaleCustomer.value = customer
        if (customer != null) {
            _newSaleCustomerName.value = customer.name
            _newSaleCustomerPhone.value = customer.phone
        } else {
            _newSaleCustomerName.value = "Client comptoir"
            _newSaleCustomerPhone.value = ""
        }
    }

    fun setNewSaleCustomerName(name: String) {
        _newSaleCustomerName.value = name
    }

    fun setNewSaleCustomerPhone(phone: String) {
        _newSaleCustomerPhone.value = phone
    }

    fun setNewSaleIsPaid(isPaid: Boolean) {
        _newSaleIsPaid.value = isPaid
    }

    fun setNewSalePaymentMethod(method: String) {
        _newSalePaymentMethod.value = method
    }

    fun setNewSaleNotes(notes: String) {
        _newSaleNotes.value = notes
    }

    fun addProductToCart(product: Product) {
        _cart.update { currentMap ->
            val existing = currentMap[product.id]
            val newQty = (existing?.quantity ?: 0) + 1
            currentMap + (product.id to CartItem(product, newQty))
        }
    }

    fun removeProductFromCart(productId: Long) {
        _cart.update { currentMap ->
            val existing = currentMap[productId] ?: return@update currentMap
            if (existing.quantity > 1) {
                currentMap + (productId to existing.copy(quantity = existing.quantity - 1))
            } else {
                currentMap - productId
            }
        }
    }

    fun setCartItemQuantity(productId: Long, quantity: Int) {
        _cart.update { currentMap ->
            val existing = currentMap[productId] ?: return@update currentMap
            if (quantity <= 0) {
                currentMap - productId
            } else {
                currentMap + (productId to existing.copy(quantity = quantity))
            }
        }
    }

    fun clearCart() {
        _cart.value = emptyMap()
        _newSaleCustomer.value = null
        _newSaleCustomerName.value = "Client comptoir"
        _newSaleCustomerPhone.value = ""
        _newSaleIsPaid.value = true
        _newSalePaymentMethod.value = "Espèces"
        _newSaleNotes.value = ""
    }

    fun checkoutSale(onSaleCreated: (SaleWithItems) -> Unit) {
        val cartItemsList = _cart.value.values.toList()
        if (cartItemsList.isEmpty()) return

        val totalAmount = cartItemsList.sumOf { it.totalPrice }
        val cal = Calendar.getInstance()
        val datePart = String.format("%04d%02d%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH))
        val randomSuffix = (100..999).random()
        val invoiceNumber = "FAC-$datePart-$randomSuffix"

        val sale = Sale(
            invoiceNumber = invoiceNumber,
            customerId = _newSaleCustomer.value?.id,
            customerName = _newSaleCustomerName.value.ifBlank { "Client comptoir" },
            customerPhone = _newSaleCustomerPhone.value,
            totalAmount = totalAmount,
            isPaid = _newSaleIsPaid.value,
            paymentMethod = _newSalePaymentMethod.value,
            timestamp = System.currentTimeMillis(),
            notes = _newSaleNotes.value
        )

        val saleItems = cartItemsList.map { cartItem ->
            SaleItem(
                productId = cartItem.product.id,
                productName = cartItem.product.name,
                quantity = cartItem.quantity,
                unitPrice = cartItem.product.sellingPrice,
                totalPrice = cartItem.totalPrice,
                unit = cartItem.product.unit
            )
        }

        viewModelScope.launch {
            val saleId = repository.createSaleWithItems(sale, saleItems)
            val createdSaleWithItems = SaleWithItems(
                sale = sale.copy(id = saleId),
                items = saleItems.map { it.copy(saleId = saleId) }
            )
            clearCart()
            onSaleCreated(createdSaleWithItems)
        }
    }

    companion object {
        fun provideFactory(application: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val db = AppDatabase.getDatabase(application, kotlinx.coroutines.GlobalScope)
                    val repository = VarotraRepository(
                        productDao = db.productDao(),
                        customerDao = db.customerDao(),
                        saleDao = db.saleDao()
                    )
                    return VarotraViewModel(application, repository) as T
                }
            }
    }
}
