package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.ui.theme.*
import com.example.ui.util.LanguageManager

sealed class Screen(
    val route: String,
    val titleAr: String,
    val icon: ImageVector,
    val accentColor: Color
) {
    val localizedTitle: String
        get() = LanguageManager.tr(route)

    data object Home : Screen("home", "الرئيسية", Icons.Default.Dashboard, BrandEmeraldPrimary)
    data object Pos : Screen("pos", "المبيعات", Icons.Default.ShoppingCart, SalesOrange)
    data object Inventory : Screen("inventory", "المخزون", Icons.Default.Inventory2, StockPurple)
    data object Customers : Screen("customers", "العملاء", Icons.Default.People, CustomerCyan)
    data object Expenses : Screen("expenses", "المصاريف", Icons.Default.ReceiptLong, ExpenseRose)
    data object Reports : Screen("reports", "التقارير", Icons.Default.TrendingUp, ReportGreen)

    companion object {
        val bottomNavItems: List<Screen>
            get() = listOf(Home, Pos, Inventory, Customers, Expenses, Reports)
    }
}
