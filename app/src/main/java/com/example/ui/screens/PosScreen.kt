package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.CartItem
import com.example.data.model.CustomerEntity
import com.example.data.model.ProductEntity
import com.example.data.model.SaleEntity
import com.example.data.model.SaleItemEntity
import com.example.ui.BarcodeScanResult
import com.example.ui.MainViewModel
import com.example.ui.components.ArabicSearchBar
import com.example.ui.components.CostCalculatorDialog
import com.example.ui.components.RealCameraBarcodeScannerDialog
import com.example.ui.components.ReceiptDialog
import com.example.ui.components.formatCurrency
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val products by viewModel.products.collectAsStateWithLifecycle()
    val cartItems by viewModel.cartItems.collectAsStateWithLifecycle()
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    val selectedCustomer by viewModel.selectedCustomerForSale.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("الكل") }

    var showScannerDialog by remember { mutableStateOf(false) }
    var showCostCalculatorDialog by remember { mutableStateOf(false) }
    var showCartDetailSheet by remember { mutableStateOf(false) }
    var showCheckoutDialog by remember { mutableStateOf(false) }
    var completedSaleForReceipt by remember { mutableStateOf<SaleEntity?>(null) }
    var completedSaleItems by remember { mutableStateOf<List<SaleItemEntity>>(emptyList()) }
    val scope = rememberCoroutineScope()

    // Categories derived from products
    val categories = remember(products) {
        listOf("الكل") + products.map { it.category }.distinct().filter { it.isNotBlank() }
    }

    val filteredProducts = remember(products, searchQuery, selectedCategory) {
        products.filter { product ->
            val matchesCategory = selectedCategory == "الكل" || product.category == selectedCategory
            val matchesSearch = searchQuery.isBlank() ||
                    product.name.contains(searchQuery, ignoreCase = true) ||
                    product.barcode.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesSearch
        }
    }

    val cartTotal = remember(cartItems) { cartItems.sumOf { it.subtotal } }
    val cartCount = remember(cartItems) { cartItems.sumOf { it.quantity } }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("pos_screen_container")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = if (cartItems.isNotEmpty()) 105.dp else 16.dp)
        ) {
            // Header with Search Bar and Barcode Scanner Action
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BrandEmeraldDeep)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Title and Quick Stats
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "نقطة البيع (POS)",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Text(
                            text = "مسح واختيار السلع وحساب المجموع الفوري",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondaryDark
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Cost Calculator Button
                        OutlinedButton(
                            onClick = { showCostCalculatorDialog = true },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = BrandGold
                            ),
                            border = BorderStroke(1.dp, BrandGold),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("open_cost_calculator_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Calculate,
                                contentDescription = "حاسبة التكلفة",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("حاسبة التكلفة", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        // Scanner Button
                        FilledTonalButton(
                            onClick = { showScannerDialog = true },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = BrandEmeraldPrimary,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("open_barcode_scanner_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = "مسح باركود",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("مسح باركود", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }

                // Search Bar
                ArabicSearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    placeholder = "ابحث عن منتج بالاسم أو الباركود...",
                    trailingActionIcon = Icons.Default.QrCodeScanner,
                    onTrailingActionClick = { showScannerDialog = true }
                )

                // Category Filter Pills
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(categories) { cat ->
                        val isSelected = cat == selectedCategory
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat, fontSize = 13.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BrandEmeraldPrimary,
                                selectedLabelColor = Color.Black,
                                containerColor = BrandSurfaceCardDark,
                                labelColor = TextPrimaryDark
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = if (isSelected) BrandEmeraldPrimary else BrandSurfaceBorderDark
                            )
                        )
                    }
                }
            }

            // Products List / Grid
            if (filteredProducts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(70.dp)
                                .clip(CircleShape)
                                .background(BrandSurfaceCardDark),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SearchOff,
                                contentDescription = null,
                                tint = TextSecondaryDark,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Text(
                            text = "لا توجد منتجات مطابقة للبحث",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimaryDark
                        )
                        Text(
                            text = "تأكد من كتابة الاسم أو الباركود بشكل صحيح، أو امسح الباركود مباشرة",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondaryDark,
                            textAlign = TextAlign.Center
                        )
                        if (searchQuery.isNotBlank() || selectedCategory != "الكل") {
                            OutlinedButton(
                                onClick = {
                                    searchQuery = ""
                                    selectedCategory = "الكل"
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("إعادة ضبط الفلاتر", color = BrandEmeraldPrimary)
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("pos_products_list")
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredProducts, key = { it.id }) { product ->
                        val inCartQuantity = cartItems.find { it.product.id == product.id }?.quantity ?: 0
                        ProductPosCard(
                            product = product,
                            currency = viewModel.currency,
                            inCartQuantity = inCartQuantity,
                            onAddToCart = { viewModel.addToCart(product) },
                            onUpdateQuantity = { qty -> viewModel.updateCartQuantity(product.id, qty) }
                        )
                    }
                }
            }
        }

        // Floating Cart Summary & Calculation Bar
        AnimatedVisibility(
            visible = cartItems.isNotEmpty(),
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showCartDetailSheet = true }
                    .testTag("pos_cart_summary_bar"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF132F23)),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.horizontalGradient(
                        listOf(BrandEmeraldPrimary, BrandGold)
                    )
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(BrandGold),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$cartCount",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.Black
                            )
                        }
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "السلة ($cartCount عناصر)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFCBD5E1)
                                )
                                Text(
                                    text = "• اضغط للتفاصيل",
                                    fontSize = 11.sp,
                                    color = BrandGoldLight
                                )
                            }
                            Text(
                                text = formatCurrency(cartTotal, viewModel.currency),
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = BrandEmeraldPrimary
                                )
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Cost Calculator Quick Button
                        IconButton(
                            onClick = { showCostCalculatorDialog = true },
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(BrandSurfaceCardDark)
                                .testTag("bottom_bar_cost_calc_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Calculate,
                                contentDescription = "حاسبة التكلفة",
                                tint = BrandGold
                            )
                        }

                        IconButton(
                            onClick = { viewModel.clearCart() },
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(ExpenseRose.copy(alpha = 0.2f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "إفراغ السلة",
                                tint = ExpenseRose
                            )
                        }

                        Button(
                            onClick = { showCheckoutDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = BrandEmeraldPrimary),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.testTag("checkout_button")
                        ) {
                            Text("دفع وحساب", color = Color.Black, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.ArrowBack, // RTL arrow points forward to checkout
                                contentDescription = null,
                                tint = Color.Black
                            )
                        }
                    }
                }
            }
        }
    }

    // Real Camera Barcode Scanner Dialog
    if (showScannerDialog) {
        RealCameraBarcodeScannerDialog(
            products = products,
            cartItems = cartItems,
            onUpdateQuantity = { productId, qty ->
                viewModel.updateCartQuantity(productId, qty)
            },
            onBarcodeScanned = { barcode ->
                viewModel.scanBarcode(barcode)
            },
            onDismiss = { showScannerDialog = false },
            cartTotal = cartTotal,
            cartCount = cartCount,
            currency = viewModel.currency,
            onOpenCostCalculator = {
                showScannerDialog = false
                showCostCalculatorDialog = true
            }
        )
    }

    // Cost & Margin Calculator Dialog for Scanned Products
    if (showCostCalculatorDialog) {
        CostCalculatorDialog(
            cartItems = cartItems,
            currency = viewModel.currency,
            onUpdateQuantity = { productId, qty -> viewModel.updateCartQuantity(productId, qty) },
            onRemoveItem = { productId -> viewModel.removeFromCart(productId) },
            onOpenScanner = {
                showCostCalculatorDialog = false
                showScannerDialog = true
            },
            onClearAll = { viewModel.clearCart() },
            onDismiss = { showCostCalculatorDialog = false }
        )
    }

    // Cart Details & Interactive Total Calculation Sheet
    if (showCartDetailSheet) {
        CartCalculationSheet(
            cartItems = cartItems,
            cartTotal = cartTotal,
            currency = viewModel.currency,
            onUpdateQuantity = { productId, qty -> viewModel.updateCartQuantity(productId, qty) },
            onRemoveItem = { productId -> viewModel.removeFromCart(productId) },
            onClearCart = {
                viewModel.clearCart()
                showCartDetailSheet = false
            },
            onProceedToCheckout = {
                showCartDetailSheet = false
                showCheckoutDialog = true
            },
            onDismiss = { showCartDetailSheet = false }
        )
    }

    // Checkout Dialog
    if (showCheckoutDialog) {
        CheckoutDialog(
            cartItems = cartItems,
            cartTotal = cartTotal,
            customers = customers,
            selectedCustomer = selectedCustomer,
            currency = viewModel.currency,
            onSelectCustomer = { viewModel.selectCustomerForSale(it) },
            onDismiss = { showCheckoutDialog = false },
            onConfirm = { discount, paidAmount, paymentType, notes ->
                viewModel.completeSale(
                    discount = discount,
                    paidAmount = paidAmount,
                    paymentType = paymentType,
                    notes = notes,
                    onSuccess = { sale ->
                        showCheckoutDialog = false
                        scope.launch {
                            val items = viewModel.getSaleItems(sale.id)
                            completedSaleItems = items
                            completedSaleForReceipt = sale
                        }
                    }
                )
            }
        )
    }

    // Receipt Dialog after completion
    completedSaleForReceipt?.let { sale ->
        ReceiptDialog(
            sale = sale,
            items = completedSaleItems,
            currency = viewModel.currency,
            onDismiss = { completedSaleForReceipt = null }
        )
    }
}

/**
 * Product Card in the POS list with direct quantity adjustment buttons.
 */
@Composable
fun ProductPosCard(
    product: ProductEntity,
    currency: String,
    inCartQuantity: Int,
    onAddToCart: () -> Unit,
    onUpdateQuantity: (Int) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onAddToCart() }
            .testTag("product_pos_card_${product.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (inCartQuantity > 0) Color(0xFF122E22) else BrandSurfaceCardDark
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = if (inCartQuantity > 0) {
                Brush.linearGradient(listOf(BrandEmeraldPrimary, BrandGold))
            } else {
                Brush.linearGradient(listOf(BrandSurfaceBorderDark, BrandEmeraldPrimary.copy(alpha = 0.2f)))
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimaryDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = formatCurrency(product.sellingPrice, currency),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = BrandGold
                        )
                    )

                    if (product.barcode.isNotBlank()) {
                        Text(
                            text = "#${product.barcode}",
                            fontSize = 11.sp,
                            color = TextSecondaryDark
                        )
                    }

                    // Stock indicator pill
                    val (stockBg, stockText) = when {
                        product.isOutOfStock -> Color(0xFF450A0A) to "نفذ المخزون"
                        product.isLowStock -> Color(0xFF451A03) to "متبقي: ${product.stockQuantity} ${product.unit}"
                        else -> Color(0xFF064E3B) to "متوفر: ${product.stockQuantity} ${product.unit}"
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(stockBg)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = stockText,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                    }
                }
            }

            // In-Cart Stepper or Add Button
            if (inCartQuantity > 0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(BrandEmeraldDeep)
                        .padding(4.dp)
                ) {
                    IconButton(
                        onClick = { onUpdateQuantity(inCartQuantity - 1) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = "إنقاص",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Text(
                        text = "$inCartQuantity",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = BrandEmeraldPrimary,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    IconButton(
                        onClick = { onUpdateQuantity(inCartQuantity + 1) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "زيادة",
                            tint = BrandEmeraldPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            } else {
                FilledIconButton(
                    onClick = onAddToCart,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = BrandEmeraldPrimary.copy(alpha = 0.2f),
                        contentColor = BrandEmeraldPrimary
                    ),
                    modifier = Modifier.size(42.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AddShoppingCart,
                        contentDescription = "إضافة للسلة"
                    )
                }
            }
        }
    }
}

/**
 * Barcode Scanner Dialog with simulated camera viewfinder, manual input, and sample barcodes.
 */
@Composable
fun PosBarcodeScannerDialog(
    products: List<ProductEntity>,
    onBarcodeScanned: (String) -> Boolean,
    onDismiss: () -> Unit
) {
    RealCameraBarcodeScannerDialog(
        products = products,
        onBarcodeScanned = { barcode ->
            val p = products.find { it.barcode.equals(barcode, ignoreCase = true) }
            val found = onBarcodeScanned(barcode)
            if (p != null && found) {
                BarcodeScanResult.AddedNew(p)
            } else {
                BarcodeScanResult.NotFound(barcode)
            }
        },
        onDismiss = onDismiss
    )
}

@Composable
private fun DeprecatedOldScannerDialog(
    products: List<ProductEntity>,
    onBarcodeScanned: (String) -> Boolean,
    onDismiss: () -> Unit
) {
    var manualBarcode by remember { mutableStateOf("") }
    var scanFeedbackMessage by remember { mutableStateOf<String?>(null) }
    var scanSuccess by remember { mutableStateOf(false) }

    // Laser scanning animation
    val infiniteTransition = rememberInfiniteTransition(label = "laser")
    val laserOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_pos"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .testTag("pos_barcode_scanner_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF091913)),
            border = BorderStroke(1.dp, BrandEmeraldPrimary)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            tint = BrandEmeraldPrimary
                        )
                        Text(
                            text = "قارئ الباركود (Barcode Scanner)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "إغلاق",
                            tint = TextSecondaryDark
                        )
                    }
                }

                // Simulated Viewfinder Frame
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF040A07))
                        .border(2.dp, BrandEmeraldPrimary.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    // Corner targeting guides
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .border(2.dp, BrandGold.copy(alpha = 0.8f), RoundedCornerShape(10.dp))
                    )

                    // Laser line moving vertically
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .height(3.dp)
                            .align(Alignment.TopCenter)
                            .offset(y = (18.dp + (140.dp * laserOffset)))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color.Transparent, ExpenseRose, Color.Red, ExpenseRose, Color.Transparent)
                                )
                            )
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = TextSecondaryDark.copy(alpha = 0.6f),
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            text = "وجّه الباركود نحو المستطيل أو اكتبه أدناه",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondaryDark
                        )
                    }
                }

                // Scan Feedback Banner
                AnimatedVisibility(
                    visible = scanFeedbackMessage != null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (scanSuccess) Color(0xFF064E3B) else Color(0xFF450A0A)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (scanSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                                contentDescription = null,
                                tint = if (scanSuccess) BrandEmeraldPrimary else ExpenseRose
                            )
                            Text(
                                text = scanFeedbackMessage ?: "",
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }

                // Manual Barcode Input
                OutlinedTextField(
                    value = manualBarcode,
                    onValueChange = { manualBarcode = it },
                    label = { Text("أدخل رمز الباركود يدوياً أو عبر القارئ") },
                    placeholder = { Text("مثال: 6130001") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = {
                        if (manualBarcode.isNotBlank()) {
                            val ok = onBarcodeScanned(manualBarcode)
                            scanSuccess = ok
                            scanFeedbackMessage = if (ok) "تم العثور على المنتج وإضافته للسلة!" else "لم يتم العثور على منتج بهذا الباركود"
                            manualBarcode = ""
                        }
                    }),
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                if (manualBarcode.isNotBlank()) {
                                    val ok = onBarcodeScanned(manualBarcode)
                                    scanSuccess = ok
                                    scanFeedbackMessage = if (ok) "تم العثور على المنتج وإضافته للسلة!" else "لم يتم العثور على منتج بهذا الباركود"
                                    manualBarcode = ""
                                }
                            }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "إضافة", tint = BrandEmeraldPrimary)
                        }
                    },
                    shape = RoundedCornerShape(14.dp)
                )

                // Quick Barcode Demo Samples from Database
                val sampleBarcodes = remember(products) {
                    products.filter { it.barcode.isNotBlank() }.take(6)
                }
                if (sampleBarcodes.isNotEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "أو اضغط للمسح السريع من سلع المخزن:",
                            fontSize = 12.sp,
                            color = TextSecondaryDark
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(sampleBarcodes) { prod ->
                                AssistChip(
                                    onClick = {
                                        val ok = onBarcodeScanned(prod.barcode)
                                        scanSuccess = ok
                                        scanFeedbackMessage = "تم مسح: ${prod.name} وإضافته للسلة بنجاح!"
                                    },
                                    label = {
                                        Text("${prod.name} (#${prod.barcode})", fontSize = 11.sp)
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.QrCode,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = BrandGold
                                        )
                                    },
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = BrandSurfaceCardDark,
                                        labelColor = TextPrimaryDark
                                    )
                                )
                            }
                        }
                    }
                }

                // Close Button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandEmeraldPrimary),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("تم والعودة للمبيعات", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Interactive Cart & Total Purchase Amount Calculation Sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartCalculationSheet(
    cartItems: List<CartItem>,
    cartTotal: Double,
    currency: String,
    onUpdateQuantity: (productId: Long, qty: Int) -> Unit,
    onRemoveItem: (productId: Long) -> Unit,
    onClearCart: () -> Unit,
    onProceedToCheckout: () -> Unit,
    onDismiss: () -> Unit
) {
    var discountPercent by remember { mutableStateOf(0) }
    var taxPercent by remember { mutableStateOf(0) }
    var cashReceivedText by remember { mutableStateOf("") }

    val discountAmount = (cartTotal * discountPercent) / 100.0
    val taxAmount = ((cartTotal - discountAmount) * taxPercent) / 100.0
    val grandTotal = (cartTotal - discountAmount + taxAmount).coerceAtLeast(0.0)

    val cashReceived = cashReceivedText.toDoubleOrNull() ?: 0.0
    val changeReturn = (cashReceived - grandTotal).coerceAtLeast(0.0)
    val remainingUnpaid = (grandTotal - cashReceived).coerceAtLeast(0.0)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF091C14),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = null,
                        tint = BrandGold
                    )
                    Text(
                        text = "تفاصيل السلة وحساب المجموع",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }

                TextButton(onClick = onClearCart) {
                    Text("إفراغ الكل", color = ExpenseRose)
                }
            }

            // Items List
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 240.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(cartItems, key = { it.product.id }) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = BrandSurfaceCardDark),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.product.name,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = TextPrimaryDark,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${formatCurrency(item.product.sellingPrice, currency)} × ${item.quantity}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondaryDark
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                IconButton(
                                    onClick = { onUpdateQuantity(item.product.id, item.quantity - 1) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.RemoveCircleOutline,
                                        contentDescription = "نقص",
                                        tint = ExpenseRose,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Text(
                                    text = "${item.quantity}",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 14.sp
                                )

                                IconButton(
                                    onClick = { onUpdateQuantity(item.product.id, item.quantity + 1) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AddCircleOutline,
                                        contentDescription = "زيادة",
                                        tint = BrandEmeraldPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(4.dp))

                                Text(
                                    text = formatCurrency(item.subtotal, currency),
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = BrandGold
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Calculations Breakdown Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF10271E)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, BrandSurfaceBorderDark)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Subtotal
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("المجموع الفرعي:", color = TextSecondaryDark, fontSize = 13.sp)
                        Text(formatCurrency(cartTotal, currency), color = TextPrimaryDark, fontWeight = FontWeight.Bold)
                    }

                    // Discount Presets Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("الخصم الترويجي:", color = TextSecondaryDark, fontSize = 13.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf(0, 5, 10, 15).forEach { pct ->
                                val isSel = discountPercent == pct
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isSel) BrandEmeraldPrimary else BrandSurfaceCardDark)
                                        .clickable { discountPercent = pct }
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "$pct%",
                                        fontSize = 11.sp,
                                        color = if (isSel) Color.Black else TextSecondaryDark,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    if (discountAmount > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("قيمة الخصم:", color = ExpenseRose, fontSize = 12.sp)
                            Text("- ${formatCurrency(discountAmount, currency)}", color = ExpenseRose, fontSize = 12.sp)
                        }
                    }

                    Divider(color = BrandSurfaceBorderDark, thickness = 1.dp)

                    // Grand Total
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "المجموع الإجمالي المستحق:",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Text(
                            text = formatCurrency(grandTotal, currency),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = BrandGold
                            )
                        )
                    }
                }
            }

            // Customer Cash & Change Return Calculator
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F221A)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "حاسبة الفكة والصرف للزبون:",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFFCBD5E1)
                    )

                    OutlinedTextField(
                        value = cashReceivedText,
                        onValueChange = { cashReceivedText = it },
                        label = { Text("المبلغ المقبوض من الزبون ($currency)") },
                        placeholder = { Text("اكتب المبلغ المستلم") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Quick Cash Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AssistChip(
                            onClick = { cashReceivedText = grandTotal.toInt().toString() },
                            label = { Text("المبلغ بالضبط", fontSize = 10.sp) }
                        )
                        listOf(500, 1000, 2000, 5000).forEach { amount ->
                            AssistChip(
                                onClick = { cashReceivedText = amount.toString() },
                                label = { Text("$amount", fontSize = 10.sp) }
                            )
                        }
                    }

                    if (cashReceived > 0) {
                        if (changeReturn > 0) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("الباقي للزبون (الفكة):", color = BrandEmeraldPrimary, fontWeight = FontWeight.Bold)
                                Text(
                                    formatCurrency(changeReturn, currency),
                                    color = BrandEmeraldPrimary,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 16.sp
                                )
                            }
                        } else if (remainingUnpaid > 0) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("المتبقي غير مدفوع (دين):", color = ExpenseRose, fontWeight = FontWeight.Bold)
                                Text(
                                    formatCurrency(remainingUnpaid, currency),
                                    color = ExpenseRose,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Proceed Button
            Button(
                onClick = onProceedToCheckout,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandEmeraldPrimary),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = "متابعة الدفع وحفظ الفاتورة",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null, tint = Color.Black)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutDialog(
    cartItems: List<CartItem>,
    cartTotal: Double,
    customers: List<CustomerEntity>,
    selectedCustomer: CustomerEntity?,
    currency: String,
    onSelectCustomer: (CustomerEntity?) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (discount: Double, paidAmount: Double, paymentType: String, notes: String) -> Unit
) {
    var discountText by remember { mutableStateOf("") }
    var paidAmountText by remember { mutableStateOf(cartTotal.toString()) }
    var paymentType by remember { mutableStateOf("CASH") } // CASH, DEBT, CARD
    var notesText by remember { mutableStateOf("") }
    var showCustomerPicker by remember { mutableStateOf(false) }

    val discount = discountText.toDoubleOrNull() ?: 0.0
    val netTotal = (cartTotal - discount).coerceAtLeast(0.0)
    val paidAmount = paidAmountText.toDoubleOrNull() ?: 0.0
    val remainingDebt = (netTotal - paidAmount).coerceAtLeast(0.0)

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("checkout_confirmation_dialog"),
        containerColor = Color(0xFF0D251B),
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Payment, contentDescription = null, tint = BrandGold)
                Text(
                    text = "إتمام عملية البيع والدفع",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Customer selector
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("العميل / الزبون:", color = TextSecondaryDark, fontSize = 13.sp)
                        OutlinedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showCustomerPicker = true },
                            colors = CardDefaults.outlinedCardColors(containerColor = BrandSurfaceCardDark),
                            border = BorderStroke(1.dp, BrandSurfaceBorderDark)
                        ) {
                            Row(
                                modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = if (selectedCustomer != null) CustomerCyan else TextSecondaryDark
                                    )
                                    Text(
                                        text = selectedCustomer?.name ?: "زبون عادي نقدي",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = TextPrimaryDark
                                    )
                                }
                                Text("تغيير", color = BrandGold, fontSize = 13.sp)
                            }
                        }
                    }
                }

                // Payment Type Tabs
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("طريقة الدفع:", color = TextSecondaryDark, fontSize = 13.sp)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                Triple("CASH", "نقداً", Icons.Default.Money),
                                Triple("DEBT", "بالدين (كريدي)", Icons.Default.CreditScore),
                                Triple("CARD", "بطاقة", Icons.Default.CreditCard)
                            ).forEach { (type, label, icon) ->
                                val isSelected = paymentType == type
                                OutlinedButton(
                                    onClick = {
                                        paymentType = type
                                        if (type == "DEBT") {
                                            paidAmountText = "0"
                                        } else if (type == "CASH" || type == "CARD") {
                                            paidAmountText = netTotal.toString()
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (isSelected) BrandEmeraldPrimary else Color.Transparent,
                                        contentColor = if (isSelected) Color.Black else TextPrimaryDark
                                    ),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) BrandEmeraldPrimary else BrandSurfaceBorderDark
                                    ),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                // Amounts
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = discountText,
                            onValueChange = { discountText = it },
                            label = { Text("الخصم ($currency)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = paidAmountText,
                            onValueChange = { paidAmountText = it },
                            label = { Text("المدفوع ($currency)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }

                // Balance Calculation Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = BrandSurfaceCardDark),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("المجموع قبل الخصم:", color = TextSecondaryDark, fontSize = 13.sp)
                                Text(formatCurrency(cartTotal, currency), color = TextPrimaryDark)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("المجموع الصافي المستحق:", color = BrandGoldLight, fontWeight = FontWeight.Bold)
                                Text(formatCurrency(netTotal, currency), color = BrandGoldLight, fontWeight = FontWeight.Bold)
                            }
                            if (remainingDebt > 0) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("المتبقي بالدين (كريدي):", color = ExpenseRose, fontWeight = FontWeight.Bold)
                                    Text(
                                        formatCurrency(remainingDebt, currency),
                                        color = ExpenseRose,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // Notes input
                item {
                    OutlinedTextField(
                        value = notesText,
                        onValueChange = { notesText = it },
                        label = { Text("ملاحظات (اختياري)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(discount, paidAmount, paymentType, notesText)
                },
                colors = ButtonDefaults.buttonColors(containerColor = BrandEmeraldPrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("confirm_checkout_button")
            ) {
                Text("تأكيد وحفظ الفاتورة", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء", color = TextSecondaryDark)
            }
        }
    )

    // Customer Selection Modal
    if (showCustomerPicker) {
        AlertDialog(
            onDismissRequest = { showCustomerPicker = false },
            containerColor = Color(0xFF0F261D),
            shape = RoundedCornerShape(20.dp),
            title = { Text("اختر العميل", color = TextPrimaryDark) },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelectCustomer(null)
                                    showCustomerPicker = false
                                },
                            colors = CardDefaults.cardColors(containerColor = BrandSurfaceCardDark)
                        ) {
                            Text(
                                text = "زبون عادي نقدي (غير مسجل)",
                                modifier = Modifier.padding(14.dp),
                                color = TextPrimaryDark,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    items(customers) { customer ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelectCustomer(customer)
                                    showCustomerPicker = false
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedCustomer?.id == customer.id)
                                    BrandEmeraldPrimary.copy(alpha = 0.2f)
                                else BrandSurfaceCardDark
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(customer.name, color = TextPrimaryDark, fontWeight = FontWeight.Bold)
                                    if (customer.phone.isNotBlank()) {
                                        Text(customer.phone, color = TextSecondaryDark, fontSize = 12.sp)
                                    }
                                }
                                if (customer.totalDebt > 0) {
                                    Text(
                                        "عليه: ${formatCurrency(customer.totalDebt, currency)}",
                                        color = ExpenseRose,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCustomerPicker = false }) {
                    Text("إغلاق", color = BrandEmeraldPrimary)
                }
            }
        )
    }
}
