package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.Product
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.StockBadge
import com.example.ui.components.VarotraAppHeader
import com.example.ui.theme.AlertRed
import com.example.ui.theme.AlertRedLight
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmberGold
import com.example.ui.theme.OnAmberContainer
import com.example.ui.theme.PrimaryContainer
import com.example.ui.theme.PrimaryTeal
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SuccessGreenLight
import com.example.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductsScreen(
    products: List<Product>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedCategory: String,
    onSelectCategory: (String) -> Unit,
    onlyLowStockFilter: Boolean,
    onToggleLowStockFilter: () -> Unit,
    onSaveProduct: (
        id: Long,
        name: String,
        category: String,
        purchasePrice: Long,
        sellingPrice: Long,
        stockQuantity: Int,
        alertThreshold: Int,
        unit: String,
        photoUri: String?
    ) -> Unit,
    onDeleteProduct: (Product) -> Unit,
    onAddToCart: (Product) -> Unit,
    showAddDialogInitially: Boolean = false,
    onAddDialogDismissed: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(showAddDialogInitially) }
    var productToEdit by remember { mutableStateOf<Product?>(null) }
    var productToDelete by remember { mutableStateOf<Product?>(null) }

    // If parent requested open dialog
    if (showAddDialogInitially && !showDialog) {
        showDialog = true
        productToEdit = null
    }

    val categories = listOf("Tous", "Alimentation", "Boissons", "Hygiène & Entretien", "Divers & Maison")

    Scaffold(
        topBar = {
            VarotraAppHeader(
                title = "Produits & Stocks",
                subtitle = "${products.size} article${if (products.size > 1) "s" else ""} enregistrés"
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    productToEdit = null
                    showDialog = true
                },
                containerColor = PrimaryTeal,
                contentColor = Color.White,
                modifier = Modifier
                    .padding(bottom = 70.dp)
                    .testTag("fab_add_product")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Ajouter Produit")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Nouveau Produit",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        modifier = modifier.testTag("products_screen")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Rechercher un produit...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = PrimaryTeal)
                },
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
                    .testTag("product_search_input")
            )

            // Filter Chips (Categories & Low Stock)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Low stock filter toggle
                FilterChip(
                    selected = onlyLowStockFilter,
                    onClick = onToggleLowStockFilter,
                    label = { Text("⚠️ Stock Bas") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AlertRedLight,
                        selectedLabelColor = AlertRed
                    ),
                    modifier = Modifier.testTag("filter_low_stock_chip")
                )

                // Category chips
                categories.forEach { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { onSelectCategory(category) },
                        label = { Text(category) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }

            // Products List
            if (products.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Inventory2,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (searchQuery.isNotBlank() || onlyLowStockFilter) "Aucun produit trouvé" else "Aucun produit dans le catalogue",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Touchez le bouton + pour enregistrer un article",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 140.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(products, key = { it.id }) { product ->
                        ProductItemCard(
                            product = product,
                            onEdit = {
                                productToEdit = product
                                showDialog = true
                            },
                            onDelete = { productToDelete = product },
                            onAddToCart = { onAddToCart(product) }
                        )
                    }
                }
            }
        }
    }

    // Add / Edit Product Dialog
    if (showDialog) {
        AddEditProductDialog(
            product = productToEdit,
            onDismiss = {
                showDialog = false
                productToEdit = null
                onAddDialogDismissed()
            },
            onSave = { id, name, category, buyPrice, sellPrice, stock, threshold, unit, photoUri ->
                onSaveProduct(id, name, category, buyPrice, sellPrice, stock, threshold, unit, photoUri)
                showDialog = false
                productToEdit = null
                onAddDialogDismissed()
            }
        )
    }

    // Delete Confirmation
    if (productToDelete != null) {
        ConfirmDeleteDialog(
            title = "Supprimer le produit ?",
            message = "Êtes-vous sûr de vouloir supprimer \"${productToDelete?.name}\" ? Cette action est irréversible.",
            onConfirm = {
                productToDelete?.let { onDeleteProduct(it) }
                productToDelete = null
            },
            onDismiss = { productToDelete = null }
        )
    }
}

@Composable
fun ProductItemCard(
    product: Product,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onAddToCart: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("product_card_${product.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Category Avatar Icon
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (product.isLowStock) AlertRedLight else PrimaryContainer
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (product.isLowStock) Icons.Default.Warning else Icons.Default.Inventory,
                        contentDescription = null,
                        tint = if (product.isLowStock) AlertRed else PrimaryTeal,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Product details
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = product.category,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    StockBadge(
                        stock = product.stockQuantity,
                        alertThreshold = product.alertThreshold,
                        unit = product.unit
                    )
                }

                // Prices
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = Formatters.formatAriary(product.sellingPrice),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            color = PrimaryTeal
                        )
                    )

                    if (product.purchasePrice > 0) {
                        Text(
                            text = "Achat : ${Formatters.formatAriary(product.purchasePrice)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Marge : +${Formatters.formatAriary(product.profitMargin)}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = SuccessGreen
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Delete
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Supprimer",
                        tint = AlertRed.copy(alpha = 0.8f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Edit
                OutlinedButton(
                    onClick = onEdit,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Modifier", modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Modifier", style = MaterialTheme.typography.labelMedium)
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Add to Sale
                Button(
                    onClick = onAddToCart,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                    modifier = Modifier
                        .height(34.dp)
                        .testTag("add_to_cart_btn_${product.id}")
                ) {
                    Icon(Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("+ Vente", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                }
            }
        }
    }
}

@Composable
fun AddEditProductDialog(
    product: Product?,
    onDismiss: () -> Unit,
    onSave: (
        id: Long,
        name: String,
        category: String,
        purchasePrice: Long,
        sellingPrice: Long,
        stockQuantity: Int,
        alertThreshold: Int,
        unit: String,
        photoUri: String?
    ) -> Unit
) {
    var name by remember { mutableStateOf(product?.name ?: "") }
    var category by remember { mutableStateOf(product?.category ?: "Alimentation") }
    var purchasePriceStr by remember { mutableStateOf(if (product != null && product.purchasePrice > 0) product.purchasePrice.toString() else "") }
    var sellingPriceStr by remember { mutableStateOf(product?.sellingPrice?.toString() ?: "") }
    var stockQuantityStr by remember { mutableStateOf(product?.stockQuantity?.toString() ?: "10") }
    var alertThresholdStr by remember { mutableStateOf(product?.alertThreshold?.toString() ?: "5") }
    var unit by remember { mutableStateOf(product?.unit ?: "unité") }

    val buyPrice = purchasePriceStr.toLongOrNull() ?: 0L
    val sellPrice = sellingPriceStr.toLongOrNull() ?: 0L
    val margin = sellPrice - buyPrice

    val units = listOf("unité", "kg", "litre", "sac", "paquet", "boîte", "morceau", "bouteille")
    val defaultCategories = listOf("Alimentation", "Boissons", "Hygiène & Entretien", "Divers & Maison", "Électronique", "Vêtements")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (product == null) "Ajouter un Produit" else "Modifier le Produit",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && sellPrice > 0) {
                        onSave(
                            product?.id ?: 0L,
                            name,
                            category,
                            buyPrice,
                            sellPrice,
                            stockQuantityStr.toIntOrNull() ?: 0,
                            alertThresholdStr.toIntOrNull() ?: 5,
                            unit,
                            null
                        )
                    }
                },
                enabled = name.isNotBlank() && sellPrice > 0,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal),
                modifier = Modifier.testTag("save_product_button")
            ) {
                Text("Enregistrer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Name
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nom du produit *") },
                        placeholder = { Text("Ex: Riz Makalioka 25kg") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("product_name_input")
                    )
                }

                // Category Selection
                item {
                    Column {
                        Text(
                            text = "Catégorie :",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            defaultCategories.forEach { cat ->
                                FilterChip(
                                    selected = category == cat,
                                    onClick = { category = cat },
                                    label = { Text(cat, fontSize = 12.sp) }
                                )
                            }
                        }
                    }
                }

                // Prices
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = purchasePriceStr,
                            onValueChange = { purchasePriceStr = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Prix d'achat (Ar)") },
                            placeholder = { Text("0") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("product_buy_price_input")
                        )

                        OutlinedTextField(
                            value = sellingPriceStr,
                            onValueChange = { sellingPriceStr = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Prix vente (Ar) *") },
                            placeholder = { Text("Ex: 5000") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("product_sell_price_input")
                        )
                    }
                }

                // Profit Margin live indicator
                if (sellPrice > 0) {
                    item {
                        Card(
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (margin >= 0) SuccessGreenLight else AlertRedLight
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Marge unitaire :",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (margin >= 0) Color(0xFF15803D) else AlertRed
                                )
                                Text(
                                    text = "${if (margin >= 0) "+" else ""}${Formatters.formatAriary(margin)}",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (margin >= 0) Color(0xFF15803D) else AlertRed
                                )
                            }
                        }
                    }
                }

                // Stock & Alert Threshold
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = stockQuantityStr,
                            onValueChange = { stockQuantityStr = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Stock actuel") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("product_stock_input")
                        )

                        OutlinedTextField(
                            value = alertThresholdStr,
                            onValueChange = { alertThresholdStr = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Seuil d'alerte") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("product_threshold_input")
                        )
                    }
                }

                // Unit selection
                item {
                    Column {
                        Text(
                            text = "Unité de mesure :",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            units.forEach { u ->
                                FilterChip(
                                    selected = unit == u,
                                    onClick = { unit = u },
                                    label = { Text(u, fontSize = 12.sp) }
                                )
                            }
                        }
                    }
                }
            }
        }
    )
}
