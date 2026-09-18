package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.CartItem
import com.example.ui.theme.*
import com.example.ui.util.SoundHelper

/**
 * Interface / Dialog to calculate total cost, revenue, net profit, and margins
 * for all scanned products.
 */
@Composable
fun CostCalculatorDialog(
    cartItems: List<CartItem>,
    currency: String,
    onUpdateQuantity: (Long, Int) -> Unit,
    onRemoveItem: (Long) -> Unit,
    onOpenScanner: () -> Unit,
    onClearAll: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var extraExpensesInput by remember { mutableStateOf("") }
    val extraExpenses = extraExpensesInput.toDoubleOrNull() ?: 0.0

    // Item-specific custom cost overrides for "what-if" margin testing
    val customCosts = remember { mutableStateMapOf<Long, Double>() }

    // Aggregate statistics
    val totalPieces = cartItems.sumOf { it.quantity }
    val totalCost = cartItems.sumOf { item ->
        val unitCost = customCosts[item.product.id] ?: item.product.purchasePrice
        unitCost * item.quantity
    } + extraExpenses

    val totalRevenue = cartItems.sumOf { it.subtotal }
    val netProfit = totalRevenue - totalCost
    val profitMarginPercent = if (totalRevenue > 0) (netProfit / totalRevenue) * 100.0 else 0.0

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f)
                .testTag("cost_calculator_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = BrandSurfaceDark),
            border = BorderStroke(1.5.dp, BrandEmeraldPrimary.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Top Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(BrandEmeraldPrimary, BrandGold)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Calculate,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "حاسبة تكلفة وأرباح السلع الممسوحة",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Text(
                                text = "تحليل رأس المال، سعر البيع، وصافي الربح للسلع في السلة",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondaryDark
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(BrandSurfaceCardDark)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "إغلاق",
                            tint = TextSecondaryDark,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Financial Summary Cards (KPIs)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF0C241B))
                        .border(1.dp, BrandEmeraldPrimary.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Row 1: Cost vs Revenue
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Total Cost Card
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF16201D)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        Icons.Default.AccountBalanceWallet,
                                        contentDescription = null,
                                        tint = ExpenseRose,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        "إجمالي التكلفة (رأس المال)",
                                        fontSize = 11.sp,
                                        color = TextSecondaryDark
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = formatCurrency(totalCost, currency),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = ExpenseRose
                                    )
                                )
                            }
                        }

                        // Total Selling Price Card
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF16201D)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Storefront,
                                        contentDescription = null,
                                        tint = BrandEmeraldPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        "إجمالي سعر البيع",
                                        fontSize = 11.sp,
                                        color = TextSecondaryDark
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = formatCurrency(totalRevenue, currency),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = BrandEmeraldPrimary
                                    )
                                )
                            }
                        }
                    }

                    // Row 2: Net Profit and Margin
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Net Profit
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(
                                containerColor = if (netProfit >= 0) Color(0xFF064E3B).copy(alpha = 0.5f)
                                else Color(0xFF450A0A).copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        if (netProfit >= 0) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                                        contentDescription = null,
                                        tint = if (netProfit >= 0) BrandEmeraldPrimary else ExpenseRose,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        "صافي الربح المتوقع",
                                        fontSize = 11.sp,
                                        color = Color.White
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = (if (netProfit > 0) "+" else "") + formatCurrency(netProfit, currency),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (netProfit >= 0) BrandEmeraldPrimary else ExpenseRose
                                    )
                                )
                            }
                        }

                        // Profit Margin % & Total Items
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF16201D)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Percent,
                                        contentDescription = null,
                                        tint = BrandGold,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        "نسبة هامش الربح",
                                        fontSize = 11.sp,
                                        color = TextSecondaryDark
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = String.format("%.1f%%", profitMarginPercent),
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = BrandGold
                                        )
                                    )
                                    Text(
                                        text = "($totalPieces قطعة)",
                                        fontSize = 11.sp,
                                        color = TextSecondaryDark
                                    )
                                }
                            }
                        }
                    }

                    // Optional Extra Overhead / Shipping
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.LocalShipping,
                            contentDescription = null,
                            tint = BrandGoldLight,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "مصاريف شحن/نقل إضافية:",
                            fontSize = 11.sp,
                            color = TextSecondaryDark
                        )
                        OutlinedTextField(
                            value = extraExpensesInput,
                            onValueChange = { extraExpensesInput = it },
                            placeholder = { Text("0 $currency", fontSize = 11.sp) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BrandEmeraldPrimary,
                                unfocusedBorderColor = BrandSurfaceBorderDark
                            )
                        )
                    }
                }

                // Action Bar: Scan More Barcode / Clear
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onOpenScanner,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("cost_calc_scan_more_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandEmeraldPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("مسح سلعة أخرى", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            // Copy report to clipboard
                            val report = buildString {
                                appendLine("=== تقرير تكلفة وأرباح السلع الممسوحة ===")
                                appendLine("التاريخ: ${java.text.SimpleDateFormat("yyyy/MM/dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date())}")
                                appendLine("عدد السلع: ${cartItems.size} أنواع (${totalPieces} قطعة)")
                                appendLine("----------------------------------------")
                                cartItems.forEachIndexed { i, item ->
                                    val unitCost = customCosts[item.product.id] ?: item.product.purchasePrice
                                    val itemCost = unitCost * item.quantity
                                    val itemProfit = item.subtotal - itemCost
                                    appendLine("${i + 1}. ${item.product.name} (كود: ${item.product.barcode})")
                                    appendLine("   الكمية: ${item.quantity} | تكلفة الوحدة: $unitCost $currency | سعر البيع: ${item.customPrice} $currency")
                                    appendLine("   إجمالي التكلفة: $itemCost $currency | إجمالي البيع: ${item.subtotal} $currency | الربح: $itemProfit $currency")
                                }
                                appendLine("----------------------------------------")
                                if (extraExpenses > 0) {
                                    appendLine("مصاريف شحن إضافية: $extraExpenses $currency")
                                }
                                appendLine("إجمالي التكلفة الكلية: $totalCost $currency")
                                appendLine("إجمالي سعر البيع: $totalRevenue $currency")
                                appendLine("صافي الربح المتوقع: $netProfit $currency")
                                appendLine("نسبة هامش الربح: ${String.format("%.1f", profitMarginPercent)}%")
                            }

                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            val clip = ClipData.newPlainText("تقرير التكلفة", report)
                            clipboard?.setPrimaryClip(clip)
                            SoundHelper.playSuccessChime()
                            Toast.makeText(context, "تم نسخ تقرير التكلفة والأرباح إلى الحافظة بنجاح", Toast.LENGTH_LONG).show()
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = BrandGold)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("نسخ التقرير", fontSize = 12.sp)
                    }

                    if (cartItems.isNotEmpty()) {
                        IconButton(
                            onClick = onClearAll,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(ExpenseRose.copy(alpha = 0.2f))
                        ) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "إفراغ", tint = ExpenseRose)
                        }
                    }
                }

                // Scanned Items List with detailed Cost & Margin breakdown
                if (cartItems.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = null,
                                tint = TextSecondaryDark,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = "لم يتم مسح أي منتج بعد",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimaryDark
                            )
                            Text(
                                text = "استخدم زر 'مسح سلعة أخرى' لمسح باركود المنتجات وحساب تكلفتها وأرباحها فوراً.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondaryDark,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .testTag("cost_items_lazy_column"),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(cartItems, key = { it.product.id }) { item ->
                            val unitCost = customCosts[item.product.id] ?: item.product.purchasePrice
                            val itemTotalCost = unitCost * item.quantity
                            val itemTotalRev = item.subtotal
                            val itemProfit = itemTotalRev - itemTotalCost
                            val itemMarginPct = if (itemTotalRev > 0) (itemProfit / itemTotalRev) * 100.0 else 0.0

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = BrandSurfaceCardDark),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, BrandSurfaceBorderDark)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Row 1: Name, Barcode & Delete
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = item.product.name,
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = FontWeight.Bold
                                                ),
                                                color = Color.White
                                            )
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = "باركود: ${item.product.barcode}",
                                                    fontSize = 11.sp,
                                                    color = BrandGoldLight
                                                )
                                                Text(
                                                    text = "• ${item.product.category}",
                                                    fontSize = 11.sp,
                                                    color = TextSecondaryDark
                                                )
                                            }
                                        }

                                        // Stepper Controls
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            IconButton(
                                                onClick = {
                                                    onUpdateQuantity(item.product.id, item.quantity - 1)
                                                },
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .clip(CircleShape)
                                                    .background(BrandSurfaceDark)
                                            ) {
                                                Icon(
                                                    Icons.Default.Remove,
                                                    contentDescription = "إنقاص",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }

                                            Text(
                                                text = "${item.quantity}",
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = FontWeight.Bold
                                                ),
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 4.dp)
                                            )

                                            IconButton(
                                                onClick = {
                                                    onUpdateQuantity(item.product.id, item.quantity + 1)
                                                },
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .clip(CircleShape)
                                                    .background(BrandEmeraldDeep)
                                            ) {
                                                Icon(
                                                    Icons.Default.Add,
                                                    contentDescription = "زيادة",
                                                    tint = BrandEmeraldPrimary,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }

                                            IconButton(
                                                onClick = { onRemoveItem(item.product.id) },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Close,
                                                    contentDescription = "حذف",
                                                    tint = ExpenseRose,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }

                                    Divider(color = BrandSurfaceBorderDark.copy(alpha = 0.5f), thickness = 0.5.dp)

                                    // Row 2: Financial calculations for this item
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Cost
                                        Column {
                                            Text(
                                                text = "سعر الشراء: ${formatCurrency(unitCost, currency)}",
                                                fontSize = 11.sp,
                                                color = TextSecondaryDark
                                            )
                                            Text(
                                                text = "تكلفة الإجمالي: ${formatCurrency(itemTotalCost, currency)}",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = ExpenseRose
                                            )
                                        }

                                        // Selling
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = "سعر البيع: ${formatCurrency(item.customPrice, currency)}",
                                                fontSize = 11.sp,
                                                color = TextSecondaryDark
                                            )
                                            Text(
                                                text = "بيع الإجمالي: ${formatCurrency(itemTotalRev, currency)}",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = BrandEmeraldPrimary
                                            )
                                        }

                                        // Net Margin Badge
                                        Surface(
                                            color = if (itemProfit >= 0) Color(0xFF064E3B) else Color(0xFF450A0A),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                horizontalAlignment = Alignment.End
                                            ) {
                                                Text(
                                                    text = (if (itemProfit >= 0) "+" else "") + formatCurrency(itemProfit, currency),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (itemProfit >= 0) BrandEmeraldPrimary else ExpenseRose
                                                )
                                                Text(
                                                    text = String.format("%.1f%% ربح", itemMarginPct),
                                                    fontSize = 9.sp,
                                                    color = Color.White
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Done Button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("close_cost_calc_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandEmeraldPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("إغلاق الحاسبة", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
