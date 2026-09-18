package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ProductEntity
import com.example.ui.MainViewModel
import com.example.ui.components.ArabicSearchBar
import com.example.ui.components.formatCurrency
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val products by viewModel.products.collectAsStateWithLifecycle()
    val kpis by viewModel.kpis.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("الكل") }
    var showOnlyLowStock by remember { mutableStateOf(false) }

    var productToEdit by remember { mutableStateOf<ProductEntity?>(null) }
    var isAddingNewProduct by remember { mutableStateOf(false) }
    var productForRestock by remember { mutableStateOf<ProductEntity?>(null) }
    var productToDelete by remember { mutableStateOf<ProductEntity?>(null) }

    val categories = remember(products) {
        listOf("الكل") + products.map { it.category }.distinct().filter { it.isNotBlank() }
    }

    val filteredProducts = remember(products, searchQuery, selectedCategory, showOnlyLowStock) {
        products.filter { product ->
            val matchesCategory = selectedCategory == "الكل" || product.category == selectedCategory
            val matchesSearch = searchQuery.isBlank() ||
                    product.name.contains(searchQuery, ignoreCase = true) ||
                    product.barcode.contains(searchQuery, ignoreCase = true)
            val matchesLowStock = !showOnlyLowStock || (product.isLowStock || product.isOutOfStock)
            matchesCategory && matchesSearch && matchesLowStock
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { isAddingNewProduct = true },
                containerColor = BrandEmeraldPrimary,
                contentColor = Color.Black,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("إضافة منتج", fontWeight = FontWeight.Bold) },
                modifier = Modifier
                    .testTag("add_product_fab")
                    .padding(bottom = 16.dp)
            )
        },
        containerColor = Color.Transparent,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Inventory Value Header Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BrandEmeraldDeep)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = BrandSurfaceCardDark),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.horizontalGradient(listOf(StockPurple.copy(alpha = 0.5f), BrandEmeraldPrimary.copy(alpha = 0.3f)))
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "قيمة المخزون (بسعر الشراء):",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondaryDark
                            )
                            Text(
                                text = formatCurrency(kpis.totalStockCostValue, viewModel.currency),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "القيمة البيعية المتوقعة:",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondaryDark
                            )
                            Text(
                                text = formatCurrency(kpis.totalStockRetailValue, viewModel.currency),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = BrandGold
                                )
                            )
                        }
                    }
                }

                ArabicSearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    placeholder = "بحث في المخزون بالاسم أو الباركود..."
                )

                // Filter Rows (Categories + Low Stock Toggle)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(categories) { cat ->
                            val isSelected = cat == selectedCategory
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedCategory = cat },
                                label = { Text(cat, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = StockPurple,
                                    selectedLabelColor = Color.White,
                                    containerColor = BrandSurfaceCardDark,
                                    labelColor = TextPrimaryDark
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    FilterChip(
                        selected = showOnlyLowStock,
                        onClick = { showOnlyLowStock = !showOnlyLowStock },
                        label = {
                            Text(
                                "تنبيهات النقص (${kpis.lowStockCount})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ExpenseRose,
                            selectedLabelColor = Color.White,
                            containerColor = if (kpis.lowStockCount > 0) ExpenseRose.copy(alpha = 0.2f) else BrandSurfaceCardDark,
                            labelColor = if (kpis.lowStockCount > 0) ExpenseRose else TextSecondaryDark
                        )
                    )
                }
            }

            // Products List
            if (filteredProducts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "لا توجد منتجات في المخزون تطابق المعايير",
                        color = TextSecondaryDark,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("inventory_products_list")
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredProducts, key = { it.id }) { product ->
                        InventoryProductCard(
                            product = product,
                            currency = viewModel.currency,
                            onEdit = { productToEdit = product },
                            onRestock = { productForRestock = product },
                            onDelete = { productToDelete = product }
                        )
                    }
                }
            }
        }
    }

    // Add / Edit Product Dialog
    if (isAddingNewProduct || productToEdit != null) {
        val initial = productToEdit ?: ProductEntity(name = "")
        ProductFormDialog(
            initialProduct = initial,
            currency = viewModel.currency,
            onDismiss = {
                isAddingNewProduct = false
                productToEdit = null
            },
            onSave = { savedProduct ->
                viewModel.saveProduct(savedProduct) {
                    isAddingNewProduct = false
                    productToEdit = null
                }
            }
        )
    }

    // Quick Restock Dialog
    productForRestock?.let { product ->
        RestockDialog(
            product = product,
            onDismiss = { productForRestock = null },
            onConfirm = { quantity ->
                viewModel.quickAddStock(product.id, quantity)
                productForRestock = null
            }
        )
    }

    // Delete Confirmation Dialog
    productToDelete?.let { product ->
        AlertDialog(
            onDismissRequest = { productToDelete = null },
            containerColor = Color(0xFF1E1010),
            shape = RoundedCornerShape(20.dp),
            title = { Text("تأكيد حذف المنتج", color = TextPrimaryDark, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "هل أنت متأكد من حذف المنتج: ${product.name} نهائياً من المخزون؟",
                    color = Color(0xFFFCA5A5)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteProduct(product)
                        productToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ExpenseRose)
                ) {
                    Text("حذف نهائي", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { productToDelete = null }) {
                    Text("إلغاء", color = TextSecondaryDark)
                }
            }
        )
    }
}

@Composable
fun InventoryProductCard(
    product: ProductEntity,
    currency: String,
    onEdit: () -> Unit,
    onRestock: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("inventory_item_${product.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BrandSurfaceCardDark),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.linearGradient(
                listOf(
                    if (product.isOutOfStock) ExpenseRose.copy(alpha = 0.6f)
                    else if (product.isLowStock) BrandGold.copy(alpha = 0.6f)
                    else BrandSurfaceBorderDark,
                    BrandSurfaceBorderDark
                )
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimaryDark,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Text(
                            text = product.category,
                            style = MaterialTheme.typography.labelSmall,
                            color = StockPurple
                        )
                        if (product.barcode.isNotBlank()) {
                            Text("•", color = TextSecondaryDark)
                            Text(
                                text = "بارcode: ${product.barcode}",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondaryDark
                            )
                        }
                    }
                }

                // Stock Badge
                val (badgeBg, badgeText) = when {
                    product.isOutOfStock -> Color(0xFF7F1D1D) to "نفد المخزون (0)"
                    product.isLowStock -> Color(0xFF78350F) to "منخفض: ${product.stockQuantity} ${product.unit}"
                    else -> Color(0xFF064E3B) to "${product.stockQuantity} ${product.unit}"
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(badgeBg)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }
            }

            HorizontalDivider(color = BrandSurfaceBorderDark)

            // Price Details & Profit Margin
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("سعر الشراء", style = MaterialTheme.typography.labelSmall, color = TextSecondaryDark)
                    Text(formatCurrency(product.purchasePrice, currency), color = TextPrimaryDark, fontWeight = FontWeight.SemiBold)
                }
                Column {
                    Text("سعر البيع", style = MaterialTheme.typography.labelSmall, color = TextSecondaryDark)
                    Text(formatCurrency(product.sellingPrice, currency), color = BrandGold, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("هامش الربح", style = MaterialTheme.typography.labelSmall, color = TextSecondaryDark)
                    Text(
                        text = "+${String.format("%.1f", product.profitMargin)}%",
                        color = BrandEmeraldPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Quick Actions Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onRestock,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = BrandEmeraldPrimary),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = androidx.compose.ui.graphics.SolidColor(BrandEmeraldPrimary.copy(alpha = 0.5f))
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.AddBox, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("إضافة كمية", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = BrandGold)
                }

                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "حذف", tint = ExpenseRose)
                }
            }
        }
    }
}

@Composable
fun ProductFormDialog(
    initialProduct: ProductEntity,
    currency: String,
    onDismiss: () -> Unit,
    onSave: (ProductEntity) -> Unit
) {
    var name by remember { mutableStateOf(initialProduct.name) }
    var barcode by remember { mutableStateOf(initialProduct.barcode) }
    var category by remember { mutableStateOf(initialProduct.category) }
    var unit by remember { mutableStateOf(initialProduct.unit) }
    var purchasePriceText by remember { mutableStateOf(if (initialProduct.purchasePrice > 0) initialProduct.purchasePrice.toString() else "") }
    var sellingPriceText by remember { mutableStateOf(if (initialProduct.sellingPrice > 0) initialProduct.sellingPrice.toString() else "") }
    var stockText by remember { mutableStateOf(initialProduct.stockQuantity.toString()) }
    var minAlertText by remember { mutableStateOf(initialProduct.minStockAlert.toString()) }

    val purchasePrice = purchasePriceText.toDoubleOrNull() ?: 0.0
    val sellingPrice = sellingPriceText.toDoubleOrNull() ?: 0.0
    val margin = if (purchasePrice > 0) ((sellingPrice - purchasePrice) / purchasePrice) * 100 else 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("product_form_dialog"),
        containerColor = Color(0xFF0F261D),
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                text = if (initialProduct.id == 0L) "إضافة منتج جديد للمخزون" else "تعديل بيانات المنتج",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("اسم المنتج *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = barcode,
                            onValueChange = { barcode = it },
                            label = { Text("الباركود / الكود") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = category,
                            onValueChange = { category = it },
                            label = { Text("التصنيف") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = purchasePriceText,
                            onValueChange = { purchasePriceText = it },
                            label = { Text("سعر الشراء ($currency)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = sellingPriceText,
                            onValueChange = { sellingPriceText = it },
                            label = { Text("سعر البيع ($currency)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }
                item {
                    if (purchasePrice > 0 && sellingPrice > 0) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = BrandSurfaceCardDark),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("هامش الربح المتوقع:", color = TextSecondaryDark, fontSize = 12.sp)
                                Text(
                                    text = "+${String.format("%.1f", margin)}% (${formatCurrency(sellingPrice - purchasePrice, currency)})",
                                    color = BrandEmeraldPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = stockText,
                            onValueChange = { stockText = it },
                            label = { Text("الكمية الحالية") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = minAlertText,
                            onValueChange = { minAlertText = it },
                            label = { Text("حد التنبيه للنفاد") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }
                item {
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("وحدة القياس (قطعة، كلغ، قارورة...)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val product = initialProduct.copy(
                            name = name.trim(),
                            barcode = barcode.trim(),
                            category = category.trim().ifBlank { "عام" },
                            unit = unit.trim().ifBlank { "قطعة" },
                            purchasePrice = purchasePrice,
                            sellingPrice = sellingPrice,
                            stockQuantity = stockText.toIntOrNull() ?: 0,
                            minStockAlert = minAlertText.toIntOrNull() ?: 5,
                            updatedAt = System.currentTimeMillis()
                        )
                        onSave(product)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = BrandEmeraldPrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("حفظ المنتج", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء", color = TextSecondaryDark)
            }
        }
    )
}

@Composable
fun RestockDialog(
    product: ProductEntity,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var quantityText by remember { mutableStateOf("10") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F261D),
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.AddBox, contentDescription = null, tint = BrandEmeraldPrimary)
                Text("إضافة كمية للمخزون", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "المنتج: ${product.name}",
                    color = BrandGoldLight,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "الكمية المتوفرة حالياً: ${product.stockQuantity} ${product.unit}",
                    color = TextSecondaryDark,
                    fontSize = 13.sp
                )
                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { quantityText = it },
                    label = { Text("الكمية المضافة") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val qty = quantityText.toIntOrNull() ?: 0
                    if (qty > 0) onConfirm(qty)
                },
                colors = ButtonDefaults.buttonColors(containerColor = BrandEmeraldPrimary)
            ) {
                Text("تأكيد الإضافة", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء", color = TextSecondaryDark)
            }
        }
    )
}
