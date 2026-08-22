package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inventory
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.Customer
import com.example.data.entity.SaleWithItems
import com.example.pdf.InvoicePdfGenerator
import com.example.ui.SalePeriodFilter
import com.example.ui.VarotraViewModel
import com.example.ui.screens.CustomersScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.ProductsScreen
import com.example.ui.screens.SaleDetailsDialog
import com.example.ui.screens.SalesScreen
import com.example.ui.theme.PrimaryTeal
import com.example.ui.theme.VarotraTheme

enum class Screen(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    DASHBOARD("Accueil", Icons.Filled.Home, Icons.Outlined.Home, "nav_dashboard"),
    PRODUCTS("Produits", Icons.Filled.Inventory, Icons.Outlined.Inventory, "nav_products"),
    CUSTOMERS("Clients", Icons.Filled.People, Icons.Outlined.People, "nav_customers"),
    SALES("Ventes", Icons.Filled.ReceiptLong, Icons.Outlined.ReceiptLong, "nav_sales")
}

class MainActivity : ComponentActivity() {

    private val viewModel: VarotraViewModel by viewModels {
        VarotraViewModel.provideFactory(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VarotraTheme {
                VarotraApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun VarotraApp(viewModel: VarotraViewModel) {
    val context = LocalContext.current
    var currentScreen by rememberSaveable { mutableStateOf(Screen.DASHBOARD) }
    var showAddProductDialogInitially by remember { mutableStateOf(false) }
    var showAddCustomerDialogInitially by remember { mutableStateOf(false) }
    var salesTabToOpen by remember { mutableIntStateOf(0) }
    var activeSaleDetails by remember { mutableStateOf<SaleWithItems?>(null) }

    // Collect UI States
    val dashboardState by viewModel.dashboardState.collectAsStateWithLifecycle()
    val products by viewModel.filteredProducts.collectAsStateWithLifecycle()
    val productSearchQuery by viewModel.productSearchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val onlyLowStockFilter by viewModel.onlyLowStockFilter.collectAsStateWithLifecycle()

    val customers by viewModel.filteredCustomers.collectAsStateWithLifecycle()
    val customerSearchQuery by viewModel.customerSearchQuery.collectAsStateWithLifecycle()
    val selectedCustomerDetails by viewModel.selectedCustomerDetails.collectAsStateWithLifecycle()
    val customerSalesHistory by viewModel.customerSalesHistory.collectAsStateWithLifecycle()

    val sales by viewModel.filteredSales.collectAsStateWithLifecycle()
    val salesPeriodFilter by viewModel.salesPeriodFilter.collectAsStateWithLifecycle()
    val salesSearchQuery by viewModel.salesSearchQuery.collectAsStateWithLifecycle()

    val cart by viewModel.cart.collectAsStateWithLifecycle()
    val cartTotalAmount by viewModel.cartTotalAmount.collectAsStateWithLifecycle()
    val cartTotalCount by viewModel.cartTotalCount.collectAsStateWithLifecycle()
    val newSaleCustomer by viewModel.newSaleCustomer.collectAsStateWithLifecycle()
    val newSaleCustomerName by viewModel.newSaleCustomerName.collectAsStateWithLifecycle()
    val newSaleCustomerPhone by viewModel.newSaleCustomerPhone.collectAsStateWithLifecycle()
    val newSaleIsPaid by viewModel.newSaleIsPaid.collectAsStateWithLifecycle()
    val newSalePaymentMethod by viewModel.newSalePaymentMethod.collectAsStateWithLifecycle()
    val newSaleNotes by viewModel.newSaleNotes.collectAsStateWithLifecycle()

    Scaffold(
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("bottom_nav_bar")
            ) {
                Screen.values().forEach { screen ->
                    val isSelected = currentScreen == screen
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            currentScreen = screen
                            if (screen == Screen.SALES) {
                                salesTabToOpen = 0
                            }
                        },
                        icon = {
                            if (screen == Screen.SALES && cartTotalCount > 0) {
                                BadgedBox(badge = { Badge { Text("$cartTotalCount") } }) {
                                    Icon(
                                        imageVector = if (isSelected) screen.selectedIcon else screen.unselectedIcon,
                                        contentDescription = screen.title
                                    )
                                }
                            } else if (screen == Screen.PRODUCTS && dashboardState.lowStockProducts.isNotEmpty()) {
                                BadgedBox(badge = { Badge { Text("${dashboardState.lowStockProducts.size}") } }) {
                                    Icon(
                                        imageVector = if (isSelected) screen.selectedIcon else screen.unselectedIcon,
                                        contentDescription = screen.title
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = if (isSelected) screen.selectedIcon else screen.unselectedIcon,
                                    contentDescription = screen.title
                                )
                            }
                        },
                        label = {
                            Text(
                                text = screen.title,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PrimaryTeal,
                            selectedTextColor = PrimaryTeal,
                            indicatorColor = PrimaryTeal.copy(alpha = 0.12f)
                        ),
                        modifier = Modifier.testTag(screen.testTag)
                    )
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                Screen.DASHBOARD -> {
                    DashboardScreen(
                        state = dashboardState,
                        onNavigateToNewSale = {
                            salesTabToOpen = 0
                            currentScreen = Screen.SALES
                        },
                        onNavigateToAddProduct = {
                            showAddProductDialogInitially = true
                            currentScreen = Screen.PRODUCTS
                        },
                        onNavigateToAddCustomer = {
                            showAddCustomerDialogInitially = true
                            currentScreen = Screen.CUSTOMERS
                        },
                        onNavigateToProducts = {
                            currentScreen = Screen.PRODUCTS
                        },
                        onNavigateToSales = {
                            salesTabToOpen = 1
                            currentScreen = Screen.SALES
                        },
                        onViewSaleDetails = { saleWithItems ->
                            activeSaleDetails = saleWithItems
                        }
                    )
                }

                Screen.PRODUCTS -> {
                    ProductsScreen(
                        products = products,
                        searchQuery = productSearchQuery,
                        onSearchQueryChange = viewModel::setProductSearchQuery,
                        selectedCategory = selectedCategory,
                        onSelectCategory = viewModel::setSelectedCategory,
                        onlyLowStockFilter = onlyLowStockFilter,
                        onToggleLowStockFilter = viewModel::toggleOnlyLowStockFilter,
                        onSaveProduct = viewModel::saveProduct,
                        onDeleteProduct = viewModel::deleteProduct,
                        onAddToCart = { product ->
                            viewModel.addProductToCart(product)
                            salesTabToOpen = 0
                            currentScreen = Screen.SALES
                        },
                        showAddDialogInitially = showAddProductDialogInitially,
                        onAddDialogDismissed = { showAddProductDialogInitially = false }
                    )
                }

                Screen.CUSTOMERS -> {
                    CustomersScreen(
                        customers = customers,
                        searchQuery = customerSearchQuery,
                        onSearchQueryChange = viewModel::setCustomerSearchQuery,
                        selectedCustomer = selectedCustomerDetails,
                        customerSalesHistory = customerSalesHistory,
                        onSelectCustomer = viewModel::selectCustomerForDetails,
                        onSaveCustomer = viewModel::saveCustomer,
                        onDeleteCustomer = viewModel::deleteCustomer,
                        onNewSaleForCustomer = { customer ->
                            viewModel.selectCustomerForNewSale(customer)
                            salesTabToOpen = 0
                            currentScreen = Screen.SALES
                        },
                        showAddDialogInitially = showAddCustomerDialogInitially,
                        onAddDialogDismissed = { showAddCustomerDialogInitially = false }
                    )
                }

                Screen.SALES -> {
                    SalesScreen(
                        sales = sales,
                        products = products,
                        customers = customers,
                        cart = cart,
                        cartTotalAmount = cartTotalAmount,
                        cartTotalCount = cartTotalCount,
                        selectedCustomer = newSaleCustomer,
                        customerName = newSaleCustomerName,
                        customerPhone = newSaleCustomerPhone,
                        isPaid = newSaleIsPaid,
                        paymentMethod = newSalePaymentMethod,
                        notes = newSaleNotes,
                        periodFilter = salesPeriodFilter,
                        searchQuery = salesSearchQuery,
                        initialTab = salesTabToOpen,
                        onPeriodFilterChange = viewModel::setSalesPeriodFilter,
                        onSearchQueryChange = viewModel::setSalesSearchQuery,
                        onSelectCustomer = viewModel::selectCustomerForNewSale,
                        onCustomerNameChange = viewModel::setNewSaleCustomerName,
                        onCustomerPhoneChange = viewModel::setNewSaleCustomerPhone,
                        onIsPaidChange = viewModel::setNewSaleIsPaid,
                        onPaymentMethodChange = viewModel::setNewSalePaymentMethod,
                        onNotesChange = viewModel::setNewSaleNotes,
                        onAddToCart = viewModel::addProductToCart,
                        onRemoveFromCart = viewModel::removeProductFromCart,
                        onSetCartQuantity = viewModel::setCartItemQuantity,
                        onClearCart = viewModel::clearCart,
                        onCheckout = viewModel::checkoutSale,
                        onDeleteSale = viewModel::deleteSale
                    )
                }
            }

            // Sale Details Dialog triggered from Dashboard recent sales
            if (activeSaleDetails != null) {
                SaleDetailsDialog(
                    saleWithItems = activeSaleDetails!!,
                    onDismiss = { activeSaleDetails = null },
                    onSharePdf = {
                        val file = InvoicePdfGenerator.generateInvoicePdf(context, activeSaleDetails!!)
                        if (file != null) {
                            InvoicePdfGenerator.shareInvoicePdf(context, file)
                        }
                    }
                )
            }
        }
    }
}
