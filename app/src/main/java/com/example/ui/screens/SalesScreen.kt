package com.example.ui.screens

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.Customer
import com.example.data.entity.Product
import com.example.data.entity.Sale
import com.example.data.entity.SaleWithItems
import com.example.pdf.InvoicePdfGenerator
import com.example.ui.CartItem
import com.example.ui.SalePeriodFilter
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.PaymentStatusBadge
import com.example.ui.components.SaleCreatedSuccessDialog
import com.example.ui.components.VarotraAppHeader
import com.example.ui.theme.AlertRed
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmberGold
import com.example.ui.theme.OnAmberContainer
import com.example.ui.theme.PrimaryContainer
import com.example.ui.theme.PrimaryTeal
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SuccessGreenLight
import com.example.util.Formatters

@Composable
fun SalesScreen(
    sales: List<SaleWithItems>,
    products: List<Product>,
    customers: List<Customer>,
    cart: Map<Long, CartItem>,
    cartTotalAmount: Long,
    cartTotalCount: Int,
    selectedCustomer: Customer?,
    customerName: String,
    customerPhone: String,
    isPaid: Boolean,
    paymentMethod: String,
    notes: String,
    periodFilter: SalePeriodFilter,
    searchQuery: String,
    initialTab: Int = 0, // 0 = Nouvelle Vente, 1 = Historique
    onPeriodFilterChange: (SalePeriodFilter) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSelectCustomer: (Customer?) -> Unit,
    onCustomerNameChange: (String) -> Unit,
    onCustomerPhoneChange: (String) -> Unit,
    onIsPaidChange: (Boolean) -> Unit,
    onPaymentMethodChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onAddToCart: (Product) -> Unit,
    onRemoveFromCart: (Long) -> Unit,
    onSetCartQuantity: (Long, Int) -> Unit,
    onClearCart: () -> Unit,
    onCheckout: (onCreated: (SaleWithItems) -> Unit) -> Unit,
    onDeleteSale: (Sale) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(initialTab) }
    var createdSaleForDialog by remember { mutableStateOf<SaleWithItems?>(null) }
    var saleToDelete by remember { mutableStateOf<Sale?>(null) }
    var selectedSaleDetails by remember { mutableStateOf<SaleWithItems?>(null) }

    Scaffold(
        topBar = {
            Column {
                VarotraAppHeader(
                    title = "Ventes & Facturation",
                    subtitle = if (selectedTab == 0) "Création de vente rapide" else "Historique et Factures PDF"
                )

                // Main Mode Switcher Tabs
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = PrimaryTeal
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.AddShoppingCart,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Nouvelle Vente ${if (cartTotalCount > 0) "($cartTotalCount)" else ""}",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        },
                        modifier = Modifier.testTag("tab_new_sale")
                    )

                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.History,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Historique (${sales.size})",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        },
                        modifier = Modifier.testTag("tab_sales_history")
                    )
                }
            }
        },
        modifier = modifier.testTag("sales_screen")
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (selectedTab == 0) {
                // Tab 0: New Sale Creator
                NewSaleView(
                    products = products,
                    customers = customers,
                    cart = cart,
                    cartTotalAmount = cartTotalAmount,
                    selectedCustomer = selectedCustomer,
                    customerName = customerName,
                    customerPhone = customerPhone,
                    isPaid = isPaid,
                    paymentMethod = paymentMethod,
                    notes = notes,
                    onSelectCustomer = onSelectCustomer,
                    onCustomerNameChange = onCustomerNameChange,
                    onCustomerPhoneChange = onCustomerPhoneChange,
                    onIsPaidChange = onIsPaidChange,
                    onPaymentMethodChange = onPaymentMethodChange,
                    onNotesChange = onNotesChange,
                    onAddToCart = onAddToCart,
                    onRemoveFromCart = onRemoveFromCart,
                    onSetCartQuantity = onSetCartQuantity,
                    onClearCart = onClearCart,
                    onValidateSale = {
                        onCheckout { createdSale ->
                            createdSaleForDialog = createdSale
                        }
                    }
                )
            } else {
                // Tab 1: Sales History
                SalesHistoryView(
                    sales = sales,
                    periodFilter = periodFilter,
                    searchQuery = searchQuery,
                    onPeriodFilterChange = onPeriodFilterChange,
                    onSearchQueryChange = onSearchQueryChange,
                    onViewDetails = { selectedSaleDetails = it },
                    onDeleteSale = { saleToDelete = it.sale },
                    onGeneratePdf = { saleWithItems ->
                        val pdfFile = InvoicePdfGenerator.generateInvoicePdf(context, saleWithItems)
                        if (pdfFile != null) {
                            InvoicePdfGenerator.shareInvoicePdf(context, pdfFile)
                        }
                    }
                )
            }
        }
    }

    // Success Dialog on Sale Creation with PDF Action Buttons
    if (createdSaleForDialog != null) {
        SaleCreatedSuccessDialog(
            saleWithItems = createdSaleForDialog!!,
            onDismiss = { createdSaleForDialog = null },
            onViewPdf = {
                val file = InvoicePdfGenerator.generateInvoicePdf(context, createdSaleForDialog!!)
                if (file != null) {
                    InvoicePdfGenerator.viewInvoicePdf(context, file)
                }
            },
            onSharePdf = {
                val file = InvoicePdfGenerator.generateInvoicePdf(context, createdSaleForDialog!!)
                if (file != null) {
                    InvoicePdfGenerator.shareInvoicePdf(context, file)
                }
            },
            onNewSale = {
                createdSaleForDialog = null
                selectedTab = 0
            }
        )
    }

    // Sale Details Dialog
    if (selectedSaleDetails != null) {
        SaleDetailsDialog(
            saleWithItems = selectedSaleDetails!!,
            onDismiss = { selectedSaleDetails = null },
            onSharePdf = {
                val file = InvoicePdfGenerator.generateInvoicePdf(context, selectedSaleDetails!!)
                if (file != null) {
                    InvoicePdfGenerator.shareInvoicePdf(context, file)
                }
            }
        )
    }

    // Delete Sale Confirmation Dialog
    if (saleToDelete != null) {
        ConfirmDeleteDialog(
            title = "Supprimer cette vente ?",
            message = "Voulez-vous supprimer la facture N° ${saleToDelete?.invoiceNumber} ? Le montant total est de ${saleToDelete?.let { Formatters.formatAriary(it.totalAmount) }}.",
            onConfirm = {
                saleToDelete?.let { onDeleteSale(it) }
                saleToDelete = null
            },
            onDismiss = { saleToDelete = null }
        )
    }
}

@Composable
fun NewSaleView(
    products: List<Product>,
    customers: List<Customer>,
    cart: Map<Long, CartItem>,
    cartTotalAmount: Long,
    selectedCustomer: Customer?,
    customerName: String,
    customerPhone: String,
    isPaid: Boolean,
    paymentMethod: String,
    notes: String,
    onSelectCustomer: (Customer?) -> Unit,
    onCustomerNameChange: (String) -> Unit,
    onCustomerPhoneChange: (String) -> Unit,
    onIsPaidChange: (Boolean) -> Unit,
    onPaymentMethodChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onAddToCart: (Product) -> Unit,
    onRemoveFromCart: (Long) -> Unit,
    onSetCartQuantity: (Long, Int) -> Unit,
    onClearCart: () -> Unit,
    onValidateSale: () -> Unit
) {
    var productSearchQuery by remember { mutableStateOf("") }
    var showCustomerPicker by remember { mutableStateOf(false) }

    val filteredProducts = remember(products, productSearchQuery) {
        if (productSearchQuery.isBlank()) products
        else products.filter {
            it.name.contains(productSearchQuery, ignoreCase = true) ||
                    it.category.contains(productSearchQuery, ignoreCase = true)
        }
    }

    val paymentMethods = listOf("Espèces", "MVola", "Orange Money", "Airtel Money", "Virement")

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("new_sale_view"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Client Selector Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                tint = PrimaryTeal,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "CLIENT :",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        TextButton(
                            onClick = { showCustomerPicker = true },
                            modifier = Modifier.testTag("select_customer_button")
                        ) {
                            Text(if (selectedCustomer == null) "Choisir un client" else "Changer")
                        }
                    }

                    if (selectedCustomer != null) {
                        Surface(
                            color = PrimaryContainer,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = selectedCustomer.name,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    if (selectedCustomer.phone.isNotBlank()) {
                                        Text(
                                            text = selectedCustomer.phone,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = { onSelectCustomer(null) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Clear, contentDescription = "Retirer client")
                                }
                            }
                        }
                    } else {
                        // Quick default "Client comptoir" with option to type name
                        OutlinedTextField(
                            value = customerName,
                            onValueChange = onCustomerNameChange,
                            label = { Text("Nom du client") },
                            placeholder = { Text("Client comptoir") },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("client_name_input")
                        )
                    }
                }
            }
        }

        // 2. Panier / Cart Section (Always visible if items > 0)
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (cart.isNotEmpty()) AmberContainer else MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.ShoppingCart,
                                contentDescription = null,
                                tint = if (cart.isNotEmpty()) OnAmberContainer else PrimaryTeal,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "PANIER (${cart.values.sumOf { it.quantity }} articles)",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                                color = if (cart.isNotEmpty()) OnAmberContainer else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        if (cart.isNotEmpty()) {
                            TextButton(
                                onClick = onClearCart,
                                modifier = Modifier.testTag("clear_cart_button")
                            ) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = AlertRed, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Vider", color = AlertRed, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (cart.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Le panier est vide. Touchez un produit ci-dessous pour l'ajouter.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        // Cart Items List
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            cart.values.forEach { cartItem ->
                                CartRowItem(
                                    cartItem = cartItem,
                                    onIncrease = { onAddToCart(cartItem.product) },
                                    onDecrease = { onRemoveFromCart(cartItem.product.id) },
                                    onDelete = { onSetCartQuantity(cartItem.product.id, 0) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Grand Total Box
                        Surface(
                            color = PrimaryTeal,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "TOTAL À PAYER :",
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = Formatters.formatAriary(cartTotalAmount),
                                    color = Color.White,
                                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Payment Mode & Validation (Only if cart is not empty)
        if (cart.isNotEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "RÈGLEMENT & PAIEMENT",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Payment Methods Chips
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            paymentMethods.forEach { method ->
                                FilterChip(
                                    selected = paymentMethod == method,
                                    onClick = { onPaymentMethodChange(method) },
                                    label = { Text(method) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = PrimaryTeal,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Is Paid Switch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (isPaid) "Marqué comme PAYÉ" else "Vente À CRÉDIT",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isPaid) SuccessGreen else AlertRed
                                    )
                                )
                                Text(
                                    text = if (isPaid) "Montant encaissé immédiatement" else "En attente de règlement",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Switch(
                                checked = isPaid,
                                onCheckedChange = onIsPaidChange,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = SuccessGreen
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // BIG VALIDATION BUTTON
                        Button(
                            onClick = onValidateSale,
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(vertical = 16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("validate_sale_button")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "VALIDER LA VENTE (${Formatters.formatAriary(cartTotalAmount)})",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. Products Selector Catalog
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "CATALOGUE PRODUITS (Touchez + pour ajouter)",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = productSearchQuery,
                    onValueChange = { productSearchQuery = it },
                    placeholder = { Text("Filtrer par nom de produit...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = PrimaryTeal) },
                    trailingIcon = {
                        if (productSearchQuery.isNotBlank()) {
                            IconButton(onClick = { productSearchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Effacer")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("catalog_search_input")
                )
            }
        }

        // Products Grid / List in New Sale
        items(filteredProducts, key = { it.id }) { product ->
            CatalogProductQuickItem(
                product = product,
                inCartQty = cart[product.id]?.quantity ?: 0,
                onAddToCart = { onAddToCart(product) }
            )
        }
    }

    // Customer Picker Dialog
    if (showCustomerPicker) {
        CustomerPickerDialog(
            customers = customers,
            onDismiss = { showCustomerPicker = false },
            onSelect = { customer ->
                onSelectCustomer(customer)
                showCustomerPicker = false
            }
        )
    }
}

@Composable
fun CartRowItem(
    cartItem: CartItem,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Product info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = cartItem.product.name,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "${Formatters.formatAriary(cartItem.product.sellingPrice)} / ${cartItem.product.unit}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Subtotal
            Text(
                text = Formatters.formatAriary(cartItem.totalPrice),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold, color = PrimaryTeal),
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            // Quantity controls (+ and - big buttons)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Minus
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = CircleShape,
                    modifier = Modifier
                        .size(36.dp)
                        .clickable { onDecrease() }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Remove,
                            contentDescription = "Diminuer",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Quantity number
                Box(
                    modifier = Modifier
                        .width(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${cartItem.quantity}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black)
                    )
                }

                // Plus
                Surface(
                    color = PrimaryTeal,
                    shape = CircleShape,
                    modifier = Modifier
                        .size(36.dp)
                        .clickable { onIncrease() }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Augmenter",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CatalogProductQuickItem(
    product: Product,
    inCartQty: Int,
    onAddToCart: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (inCartQty > 0) PrimaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onAddToCart() }
            .testTag("quick_item_${product.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = Formatters.formatAriary(product.sellingPrice),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.ExtraBold, color = PrimaryTeal)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "• Stock : ${product.stockQuantity} ${product.unit}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (product.isLowStock) AlertRed else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Big Add Button with counter if already in cart
            Button(
                onClick = onAddToCart,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (inCartQty > 0) PrimaryTeal else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (inCartQty > 0) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                ),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                modifier = Modifier.height(38.dp)
            ) {
                if (inCartQty > 0) {
                    Text(
                        text = "$inCartQty dans panier",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                } else {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Ajouter", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                }
            }
        }
    }
}

@Composable
fun SalesHistoryView(
    sales: List<SaleWithItems>,
    periodFilter: SalePeriodFilter,
    searchQuery: String,
    onPeriodFilterChange: (SalePeriodFilter) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onViewDetails: (SaleWithItems) -> Unit,
    onDeleteSale: (SaleWithItems) -> Unit,
    onGeneratePdf: (SaleWithItems) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text("Rechercher (N° Facture, client, article)...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = PrimaryTeal) },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Effacer")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .testTag("sales_search_input")
        )

        // Filter Period Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SalePeriodFilter.values().forEach { filter ->
                FilterChip(
                    selected = periodFilter == filter,
                    onClick = { onPeriodFilterChange(filter) },
                    label = { Text(filter.label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PrimaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }

        // List
        if (sales.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.ReceiptLong,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Aucune vente trouvée",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(sales, key = { it.sale.id }) { saleWithItems ->
                    SaleHistoryCard(
                        saleWithItems = saleWithItems,
                        onClick = { onViewDetails(saleWithItems) },
                        onPdfClick = { onGeneratePdf(saleWithItems) },
                        onDelete = { onDeleteSale(saleWithItems) }
                    )
                }
            }
        }
    }
}

@Composable
fun SaleHistoryCard(
    saleWithItems: SaleWithItems,
    onClick: () -> Unit,
    onPdfClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sale = saleWithItems.sale
    val itemsSummary = saleWithItems.items.joinToString(", ") { "${it.quantity}x ${it.productName}" }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("sale_card_${sale.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header row: Invoice # and Total
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = sale.invoiceNumber,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = Formatters.formatDateTime(sale.timestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = Formatters.formatAriary(sale.totalAmount),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        color = PrimaryTeal
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Client & Payment status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Client : ${sale.customerName}",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )

                PaymentStatusBadge(isPaid = sale.isPaid, paymentMethod = sale.paymentMethod)
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Items summary
            Text(
                text = itemsSummary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(10.dp))
            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(8.dp))

            // Action Row: Big "Générer Facture PDF" Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Supprimer vente",
                        tint = AlertRed.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Big prominent "Facture PDF" button as requested
                Button(
                    onClick = onPdfClick,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier
                        .height(38.dp)
                        .testTag("generate_pdf_btn_${sale.id}")
                ) {
                    Icon(
                        Icons.Default.PictureAsPdf,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Générer Facture PDF",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}

@Composable
fun CustomerPickerDialog(
    customers: List<Customer>,
    onDismiss: () -> Unit,
    onSelect: (Customer) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filtered = if (query.isBlank()) customers else customers.filter {
        it.name.contains(query, ignoreCase = true) || it.phone.contains(query, ignoreCase = true)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sélectionner un Client", fontWeight = FontWeight.Bold) },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        },
        confirmButton = {},
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(350.dp)
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Rechercher...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                if (filtered.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Aucun client trouvé", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(filtered) { customer ->
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelect(customer) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(customer.name, fontWeight = FontWeight.Bold)
                                        if (customer.phone.isNotBlank()) {
                                            Text(
                                                customer.phone,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Icon(Icons.Default.Check, contentDescription = null, tint = PrimaryTeal)
                                }
                            }
                        }
                    }
                }
            }
        }
    )
}

@Composable
fun SaleDetailsDialog(
    saleWithItems: SaleWithItems,
    onDismiss: () -> Unit,
    onSharePdf: () -> Unit
) {
    val sale = saleWithItems.sale

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Facture ${sale.invoiceNumber}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black)
                )
                Text(
                    text = Formatters.formatDateTime(sale.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onSharePdf,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal)
            ) {
                Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Partager Facture PDF")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Fermer")
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(360.dp)
            ) {
                // Client Info Box
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(text = "Client : ${sale.customerName}", fontWeight = FontWeight.Bold)
                        if (sale.customerPhone.isNotBlank()) {
                            Text(text = "Tél : ${sale.customerPhone}", style = MaterialTheme.typography.bodySmall)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        PaymentStatusBadge(isPaid = sale.isPaid, paymentMethod = sale.paymentMethod)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "ARTICLES ACHETÉS",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Items list
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(saleWithItems.items) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.productName, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text(
                                    "${item.quantity} ${item.unit} x ${Formatters.formatAriary(item.unitPrice)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                Formatters.formatAriary(item.totalPrice),
                                fontWeight = FontWeight.Bold,
                                color = PrimaryTeal
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Total
                Surface(
                    color = AmberContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("TOTAL :", fontWeight = FontWeight.Bold, color = OnAmberContainer)
                        Text(
                            Formatters.formatAriary(sale.totalAmount),
                            fontWeight = FontWeight.Black,
                            style = MaterialTheme.typography.titleMedium,
                            color = OnAmberContainer
                        )
                    }
                }
            }
        }
    )
}
