package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.SaleEntity
import com.example.data.model.SaleItemEntity
import com.example.ui.MainViewModel
import com.example.ui.components.DailyFinancialChartCard
import com.example.ui.components.ReceiptDialog
import com.example.ui.components.formatCurrency
import com.example.ui.components.formatDate
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val sales by viewModel.sales.collectAsStateWithLifecycle()
    val expenses by viewModel.expenses.collectAsStateWithLifecycle()
    val kpis by viewModel.kpis.collectAsStateWithLifecycle()

    var selectedPeriod by remember { mutableStateOf("TODAY") } // TODAY, MONTH, ALL
    var viewingReceiptSale by remember { mutableStateOf<SaleEntity?>(null) }
    var viewingReceiptItems by remember { mutableStateOf<List<SaleItemEntity>>(emptyList()) }
    val scope = rememberCoroutineScope()

    val filteredSales = remember(sales, selectedPeriod) {
        val cal = Calendar.getInstance()
        when (selectedPeriod) {
            "TODAY" -> {
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                val start = cal.timeInMillis
                sales.filter { it.timestamp >= start }
            }
            "MONTH" -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                val start = cal.timeInMillis
                sales.filter { it.timestamp >= start }
            }
            else -> sales
        }
    }

    val filteredExpenses = remember(expenses, selectedPeriod) {
        val cal = Calendar.getInstance()
        when (selectedPeriod) {
            "TODAY" -> {
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                val start = cal.timeInMillis
                expenses.filter { it.timestamp >= start }
            }
            "MONTH" -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                val start = cal.timeInMillis
                expenses.filter { it.timestamp >= start }
            }
            else -> expenses
        }
    }

    val periodRevenue = remember(filteredSales) { filteredSales.sumOf { it.totalAmount } }
    val periodCost = remember(filteredSales) { filteredSales.sumOf { it.costAmount } }
    val periodGrossProfit = remember(periodRevenue, periodCost) { periodRevenue - periodCost }
    val periodExpensesTotal = remember(filteredExpenses) { filteredExpenses.sumOf { it.amount } }
    val periodNetProfit = remember(periodGrossProfit, periodExpensesTotal) { periodGrossProfit - periodExpensesTotal }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("reports_screen_content")
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Period Selector Tabs
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(BrandSurfaceCardDark)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    "TODAY" to "اليوم",
                    "MONTH" to "هذا الشهر",
                    "ALL" to "كل الفترات"
                ).forEach { (key, label) ->
                    val isSelected = selectedPeriod == key
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) BrandEmeraldPrimary else Color.Transparent)
                            .clickable { selectedPeriod = key }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) Color.Black else TextPrimaryDark,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // Interactive Daily Financial Chart (Sales, Profit, Expenses)
        item {
            DailyFinancialChartCard(
                sales = sales,
                expenses = expenses,
                currency = viewModel.currency,
                initialDaysCount = 7,
                title = "تحليل المبيعات والأرباح اليومية"
            )
        }

        // Profit & Loss Statement Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F261D)),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.verticalGradient(
                        listOf(BrandEmeraldPrimary.copy(alpha = 0.5f), BrandGold.copy(alpha = 0.3f))
                    )
                )
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "تقرير الأرباح والخسائر",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (periodNetProfit >= 0) BrandEmeraldPrimary.copy(alpha = 0.2f) else ExpenseRose.copy(alpha = 0.2f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (periodNetProfit >= 0) "أرباح إيجابية ✓" else "خسارة تشغيلية ⚠",
                                color = if (periodNetProfit >= 0) BrandEmeraldPrimary else ExpenseRose,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }

                    HorizontalDivider(color = BrandSurfaceBorderDark)

                    // Line items
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("إجمالي المبيعات (الإيرادات):", color = TextSecondaryDark)
                        Text(formatCurrency(periodRevenue, viewModel.currency), color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("تكلفة شراء السلع المباعة:", color = TextSecondaryDark)
                        Text("- ${formatCurrency(periodCost, viewModel.currency)}", color = Color(0xFFFCA5A5))
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("مجمل الربح التجاري:", color = BrandGoldLight)
                        Text(formatCurrency(periodGrossProfit, viewModel.currency), color = BrandGoldLight, fontWeight = FontWeight.Bold)
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("المصاريف التشغيلية (فواتير، كراء...):", color = TextSecondaryDark)
                        Text("- ${formatCurrency(periodExpensesTotal, viewModel.currency)}", color = ExpenseRose)
                    }

                    HorizontalDivider(color = BrandSurfaceBorderDark)

                    // Net Profit Highlight
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "صافي الربح الفعلي:",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Text(
                            text = formatCurrency(periodNetProfit, viewModel.currency),
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = if (periodNetProfit >= 0) BrandEmeraldPrimary else ExpenseRose
                            )
                        )
                    }
                }
            }
        }

        // Invoices History Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "سجل فواتير المبيعات (${filteredSales.size})",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimaryDark
                )
            }
        }

        if (filteredSales.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = BrandSurfaceCardDark)
                ) {
                    Box(modifier = Modifier.padding(24.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("لا توجد فواتير مبيعات مسجلة في هذه الفترة", color = TextSecondaryDark)
                    }
                }
            }
        } else {
            items(filteredSales, key = { it.id }) { sale ->
                InvoiceHistoryCard(
                    sale = sale,
                    currency = viewModel.currency,
                    onViewReceipt = {
                        scope.launch {
                            val items = viewModel.getSaleItems(sale.id)
                            viewingReceiptItems = items
                            viewingReceiptSale = sale
                        }
                    }
                )
            }
        }
    }

    // Receipt viewer
    viewingReceiptSale?.let { sale ->
        ReceiptDialog(
            sale = sale,
            items = viewingReceiptItems,
            currency = viewModel.currency,
            onDismiss = { viewingReceiptSale = null }
        )
    }
}

@Composable
fun InvoiceHistoryCard(
    sale: SaleEntity,
    currency: String,
    onViewReceipt: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onViewReceipt() }
            .testTag("invoice_card_${sale.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = BrandSurfaceCardDark),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(BrandSurfaceBorderDark)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(BrandEmeraldPrimary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Receipt, contentDescription = null, tint = BrandEmeraldPrimary)
                }

                Column {
                    Text(
                        text = sale.invoiceNumber,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimaryDark
                    )
                    Text(
                        text = "${sale.customerName} • ${formatDate(sale.timestamp)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondaryDark
                    )
                    if (sale.debtAmount > 0) {
                        Text(
                            text = "باقي بالدين: ${formatCurrency(sale.debtAmount, currency)}",
                            color = ExpenseRose,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatCurrency(sale.totalAmount, currency),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = BrandGold
                    )
                )
                Text(
                    text = when (sale.paymentType) {
                        "CASH" -> "نقداً"
                        "DEBT" -> "بالدين"
                        "CARD" -> "بطاقة"
                        else -> sale.paymentType
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = BrandEmeraldPrimary
                )
            }
        }
    }
}
