package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.MainViewModel
import com.example.ui.components.DailyFinancialChartCard
import com.example.ui.components.MetricCard
import com.example.ui.components.ModuleBadgeCard
import com.example.ui.components.formatCurrency
import com.example.ui.navigation.Screen
import com.example.ui.theme.*

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onNavigateTo: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    val kpis by viewModel.kpis.collectAsStateWithLifecycle()
    val products by viewModel.products.collectAsStateWithLifecycle()
    val sales by viewModel.sales.collectAsStateWithLifecycle()
    val expenses by viewModel.expenses.collectAsStateWithLifecycle()
    val lowStockProducts = products.filter { it.isLowStock || it.isOutOfStock }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("home_screen_content")
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Card styled after the user's poster
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("home_hero_card"),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(
                    containerColor = BrandEmeraldHeader
                ),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.verticalGradient(
                        listOf(BrandEmeraldPrimary.copy(alpha = 0.7f), BrandGold.copy(alpha = 0.4f))
                    )
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0xFF0F3223),
                                    Color(0xFF091F16)
                                )
                            )
                        )
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Logo Badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(BrandEmeraldDeep)
                            .border(1.5.dp, BrandGold, RoundedCornerShape(16.dp))
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = null,
                            tint = BrandGold,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Rz Tasyir",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = null,
                            tint = BrandEmeraldPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "مَرحَبًا بك في Rz Tasyir 👋",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        ),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "نظّم تجارتك، تابع مبيعاتك، راقب مخزونك، وأدر مصاريفك بكل سهولة.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFCBD5E1),
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // 5 Module Quick Badges (matching the poster exactly)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ModuleBadgeCard(
                            title = "المبيعات",
                            icon = Icons.Default.ShoppingCart,
                            color = SalesOrange,
                            onClick = { onNavigateTo(Screen.Pos) },
                            modifier = Modifier.weight(1f)
                        )
                        ModuleBadgeCard(
                            title = "المخزون",
                            icon = Icons.Default.Inventory2,
                            color = StockPurple,
                            onClick = { onNavigateTo(Screen.Inventory) },
                            modifier = Modifier.weight(1f)
                        )
                        ModuleBadgeCard(
                            title = "العملاء",
                            icon = Icons.Default.People,
                            color = CustomerCyan,
                            onClick = { onNavigateTo(Screen.Customers) },
                            modifier = Modifier.weight(1f)
                        )
                        ModuleBadgeCard(
                            title = "المصاريف",
                            icon = Icons.Default.ReceiptLong,
                            color = ExpenseRose,
                            onClick = { onNavigateTo(Screen.Expenses) },
                            modifier = Modifier.weight(1f)
                        )
                        ModuleBadgeCard(
                            title = "التقارير",
                            icon = Icons.Default.TrendingUp,
                            color = ReportGreen,
                            onClick = { onNavigateTo(Screen.Reports) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Motivational Banner matching image
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF0A2218))
                            .border(1.dp, BrandEmeraldDarker, RoundedCornerShape(12.dp))
                            .padding(vertical = 8.dp, horizontal = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "✨ كل شيء تحت السيطرة... في مكان واحد ✨",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = BrandGoldLight
                        )
                    }
                }
            }
        }

        // Section Title: Key Performance Indicators
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ملخص اليوم المالي",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimaryDark
                )
                TextButton(onClick = { onNavigateTo(Screen.Reports) }) {
                    Text("عرض كامل التقارير", color = BrandEmeraldPrimary)
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = BrandEmeraldPrimary
                    )
                }
            }
        }

        // Metrics Grid (2x2)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    title = "مبيعات اليوم",
                    value = formatCurrency(kpis.todaySales, viewModel.currency),
                    subtitle = "دخل نقاط البيع",
                    icon = Icons.Default.PointOfSale,
                    accentColor = SalesOrange,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateTo(Screen.Pos) }
                )
                MetricCard(
                    title = "صافي ربح اليوم",
                    value = formatCurrency(kpis.todayNetProfit, viewModel.currency),
                    subtitle = "بعد حسم التكلفة والمصاريف",
                    icon = Icons.Default.Savings,
                    accentColor = BrandEmeraldPrimary,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateTo(Screen.Reports) }
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    title = "ديون العملاء (كريدي)",
                    value = formatCurrency(kpis.totalDebts, viewModel.currency),
                    subtitle = "أموال مستحقة في السوق",
                    icon = Icons.Default.AccountBalanceWallet,
                    accentColor = CustomerCyan,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateTo(Screen.Customers) }
                )
                MetricCard(
                    title = "مصاريف اليوم",
                    value = formatCurrency(kpis.todayExpenses, viewModel.currency),
                    subtitle = "فواتير ومصاريف",
                    icon = Icons.Default.Payments,
                    accentColor = ExpenseRose,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateTo(Screen.Expenses) }
                )
            }
        }

        // Daily Financial Graphic Chart (Sales, Profits, Expenses)
        item {
            DailyFinancialChartCard(
                sales = sales,
                expenses = expenses,
                currency = viewModel.currency,
                initialDaysCount = 7,
                title = "مخطط الأداء والمبيعات الأسبوعية"
            )
        }

        // Low stock warning banner if any products are running low
        if (lowStockProducts.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("low_stock_warning_card")
                        .clickable { onNavigateTo(Screen.Inventory) },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2E1818)),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.horizontalGradient(listOf(ExpenseRose, Color(0xFF991B1B)))
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(ExpenseRose.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.WarningAmber,
                                contentDescription = null,
                                tint = ExpenseRose,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "تنبيه المخزون: ${lowStockProducts.size} منتجات قاربت على النفاد!",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimaryDark
                            )
                            Text(
                                text = "اضغط لمعاينة السلع وإعادة تعبئة الكميات وتجنب نفاد السلعة.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFFCA5A5)
                            )
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = ExpenseRose
                        )
                    }
                }
            }
        }

        // Quick Primary Action Bar
        item {
            Button(
                onClick = { onNavigateTo(Screen.Pos) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("home_quick_pos_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandEmeraldPrimary)
            ) {
                Icon(
                    imageVector = Icons.Default.AddShoppingCart,
                    contentDescription = null,
                    tint = Color.Black
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "فتح نقطة البيع وتسجيل فاتورة جديدة",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.Black
                )
            }
        }
    }
}
