package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ExpenseEntity
import com.example.data.model.SaleEntity
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max

data class DailyFinancialPoint(
    val dayIndex: Int,
    val dateMillis: Long,
    val dayName: String,
    val shortDate: String,
    val fullDate: String,
    val salesAmount: Double,
    val costAmount: Double,
    val grossProfit: Double,
    val expensesAmount: Double,
    val netProfit: Double,
    val salesCount: Int
)

enum class ChartType {
    BARS,   // أعمدة بيانية
    LINE    // منحنى تفاعلي
}

enum class ChartMetricFilter {
    ALL,        // الكل (مبيعات + أرباح + مصاريف)
    PROFIT_ONLY // الأرباح والمصاريف
}

/**
 * دالة مساعدة لتجميع المبيعات والمصاريف على مدى عدد من الأيام السابقة (7، 14، أو 30 يوماً)
 */
fun aggregateDailyFinancials(
    sales: List<SaleEntity>,
    expenses: List<ExpenseEntity>,
    daysCount: Int = 7
): List<DailyFinancialPoint> {
    val result = mutableListOf<DailyFinancialPoint>()
    val cal = Calendar.getInstance()

    // تنسيقات التاريخ باللغة العربية
    val dayNameFormat = SimpleDateFormat("EEEE", Locale("ar"))
    val shortDateFormat = SimpleDateFormat("d MMM", Locale("ar"))
    val fullDateFormat = SimpleDateFormat("EEEE d MMMM yyyy", Locale("ar"))

    for (i in (daysCount - 1) downTo 0) {
        val targetCal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -i)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfDay = targetCal.timeInMillis
        val endOfDay = startOfDay + 24 * 60 * 60 * 1000 - 1

        val daySales = sales.filter { it.timestamp in startOfDay..endOfDay }
        val dayExpenses = expenses.filter { it.timestamp in startOfDay..endOfDay }

        val salesAmount = daySales.sumOf { it.totalAmount }
        val costAmount = daySales.sumOf { it.costAmount }
        val grossProfit = salesAmount - costAmount
        val expensesAmount = dayExpenses.sumOf { it.amount }
        val netProfit = grossProfit - expensesAmount

        val rawDayName = when (i) {
            0 -> "اليوم"
            1 -> "أمس"
            else -> dayNameFormat.format(Date(startOfDay))
        }

        result.add(
            DailyFinancialPoint(
                dayIndex = daysCount - 1 - i,
                dateMillis = startOfDay,
                dayName = rawDayName,
                shortDate = shortDateFormat.format(Date(startOfDay)),
                fullDate = fullDateFormat.format(Date(startOfDay)),
                salesAmount = salesAmount,
                costAmount = costAmount,
                grossProfit = grossProfit,
                expensesAmount = expensesAmount,
                netProfit = netProfit,
                salesCount = daySales.size
            )
        )
    }

    return result
}

@Composable
fun DailyFinancialChartCard(
    sales: List<SaleEntity>,
    expenses: List<ExpenseEntity>,
    currency: String = "د.ج",
    modifier: Modifier = Modifier,
    initialDaysCount: Int = 7,
    title: String = "مخطط المبيعات والأرباح اليومية"
) {
    var daysCount by remember { mutableIntStateOf(initialDaysCount) }
    var chartType by remember { mutableStateOf(ChartType.BARS) }
    var metricFilter by remember { mutableStateOf(ChartMetricFilter.ALL) }

    val dailyData = remember(sales, expenses, daysCount) {
        aggregateDailyFinancials(sales, expenses, daysCount)
    }

    var selectedPointIndex by remember(dailyData) {
        mutableStateOf<Int?>(if (dailyData.isNotEmpty()) dailyData.lastIndex else null)
    }

    val selectedPoint = selectedPointIndex?.let { idx ->
        if (idx in dailyData.indices) dailyData[idx] else null
    }

    // حساب إجماليات الفترة المعروضة
    val totalPeriodSales = remember(dailyData) { dailyData.sumOf { it.salesAmount } }
    val totalPeriodProfit = remember(dailyData) { dailyData.sumOf { it.netProfit } }
    val totalPeriodExpenses = remember(dailyData) { dailyData.sumOf { it.expensesAmount } }
    val bestSalesDay = remember(dailyData) { dailyData.maxByOrNull { it.salesAmount } }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("daily_financial_chart_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = BrandSurfaceCardDark),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.verticalGradient(
                listOf(BrandEmeraldPrimary.copy(alpha = 0.4f), BrandSurfaceBorderDark)
            )
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header: Title, Period selector, and Chart type toggles
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
                            .background(BrandEmeraldPrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = null,
                            tint = BrandEmeraldPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Text(
                            text = "متابعة تطور الأرباح والمصاريف يوماً بيوم",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondaryDark
                        )
                    }
                }

                // Chart Style Toggle (Bars vs Line)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(BrandEmeraldDeep)
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    IconButton(
                        onClick = { chartType = ChartType.BARS },
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (chartType == ChartType.BARS) BrandEmeraldPrimary else Color.Transparent)
                    ) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = "أعمدة",
                            tint = if (chartType == ChartType.BARS) Color.Black else TextSecondaryDark,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = { chartType = ChartType.LINE },
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (chartType == ChartType.LINE) BrandEmeraldPrimary else Color.Transparent)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShowChart,
                            contentDescription = "منحنى بياني",
                            tint = if (chartType == ChartType.LINE) Color.Black else TextSecondaryDark,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Period Duration Selector (7 days, 14 days, 30 days)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(BrandEmeraldDeep)
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(7 to "آخر 7 أيام", 14 to "آخر 14 يوماً", 30 to "آخر 30 يوماً").forEach { (count, label) ->
                    val isSelected = daysCount == count
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(9.dp))
                            .background(if (isSelected) BrandEmeraldPrimary else Color.Transparent)
                            .clickable {
                                daysCount = count
                                selectedPointIndex = null
                            }
                            .padding(vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) Color.Black else TextSecondaryDark,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Quick KPI Row for Selected Period
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Total Sales
                KpiMiniCard(
                    title = "إجمالي المبيعات",
                    value = formatCurrency(totalPeriodSales, currency),
                    color = BrandGold,
                    modifier = Modifier.weight(1f)
                )

                // Net Profit
                KpiMiniCard(
                    title = "صافي الربح",
                    value = formatCurrency(totalPeriodProfit, currency),
                    color = if (totalPeriodProfit >= 0) BrandEmeraldPrimary else ExpenseRose,
                    modifier = Modifier.weight(1f)
                )

                // Total Expenses
                KpiMiniCard(
                    title = "المصاريف",
                    value = formatCurrency(totalPeriodExpenses, currency),
                    color = ExpenseRose,
                    modifier = Modifier.weight(1f)
                )
            }

            // Chart Legend (مفتاح الرسم)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendItem(color = BrandGold, label = "المبيعات")
                Spacer(modifier = Modifier.width(14.dp))
                LegendItem(color = BrandEmeraldPrimary, label = "صافي الأرباح")
                Spacer(modifier = Modifier.width(14.dp))
                LegendItem(color = ExpenseRose, label = "المصاريف")
            }

            // Canvas Area: The interactive Graphic Chart
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(BrandEmeraldDeep.copy(alpha = 0.6f))
                    .padding(horizontal = 8.dp, vertical = 12.dp)
            ) {
                if (chartType == ChartType.BARS) {
                    BarChartCanvas(
                        dailyData = dailyData,
                        selectedPointIndex = selectedPointIndex,
                        onSelectPoint = { selectedPointIndex = it }
                    )
                } else {
                    LineChartCanvas(
                        dailyData = dailyData,
                        selectedPointIndex = selectedPointIndex,
                        onSelectPoint = { selectedPointIndex = it }
                    )
                }
            }

            // Interactive Day Details Card (عند الضغط على أي يوم)
            AnimatedVisibility(
                visible = selectedPoint != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                selectedPoint?.let { pt ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("selected_day_details_card"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F261D)),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = Brush.horizontalGradient(
                                listOf(BrandEmeraldPrimary.copy(alpha = 0.5f), BrandGold.copy(alpha = 0.5f))
                            )
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarToday,
                                        contentDescription = null,
                                        tint = BrandGold,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "${pt.dayName} (${pt.fullDate})",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White
                                    )
                                }

                                Text(
                                    text = "${pt.salesCount} عمليات بيع",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondaryDark
                                )
                            }

                            HorizontalDivider(color = BrandSurfaceBorderDark)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("المبيعات اليومية:", color = TextSecondaryDark, fontSize = 11.sp)
                                    Text(
                                        formatCurrency(pt.salesAmount, currency),
                                        color = BrandGold,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }

                                Column {
                                    Text("تكلفة البضاعة:", color = TextSecondaryDark, fontSize = 11.sp)
                                    Text(
                                        formatCurrency(pt.costAmount, currency),
                                        color = Color(0xFFFCA5A5),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }

                                Column {
                                    Text("المصاريف:", color = TextSecondaryDark, fontSize = 11.sp)
                                    Text(
                                        formatCurrency(pt.expensesAmount, currency),
                                        color = ExpenseRose,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text("صافي الربح:", color = TextSecondaryDark, fontSize = 11.sp)
                                    Text(
                                        formatCurrency(pt.netProfit, currency),
                                        color = if (pt.netProfit >= 0) BrandEmeraldPrimary else ExpenseRose,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Best Sales Day Highlight Note
            if (bestSalesDay != null && bestSalesDay.salesAmount > 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(BrandEmeraldDeep)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Stars,
                        contentDescription = null,
                        tint = BrandGold,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "أعلى يوم مبيعات خلال الفترة: ${bestSalesDay.dayName} (${bestSalesDay.shortDate}) بمجموع ${formatCurrency(bestSalesDay.salesAmount, currency)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun KpiMiniCard(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(BrandEmeraldDeep)
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
            .padding(10.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondaryDark,
                fontSize = 11.sp
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = color,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
fun LegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondaryDark,
            fontSize = 11.sp
        )
    }
}

/**
 * رسم بياني للأعمدة ثلاثية المتغيرات (مبيعات، أرباح، مصاريف) لكل يوم
 */
@Composable
fun BarChartCanvas(
    dailyData: List<DailyFinancialPoint>,
    selectedPointIndex: Int?,
    onSelectPoint: (Int) -> Unit
) {
    if (dailyData.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("لا توجد بيانات متاحة في هذه الفترة", color = TextSecondaryDark, fontSize = 12.sp)
        }
        return
    }

    val maxVal = remember(dailyData) {
        val peak = dailyData.maxOfOrNull {
            max(it.salesAmount, max(max(it.netProfit, 0.0), it.expensesAmount))
        } ?: 1.0
        if (peak <= 0.0) 1000.0 else peak * 1.15
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(dailyData) {
                detectTapGestures { offset ->
                    val slotWidth = size.width / dailyData.size
                    val clickedIndex = (offset.x / slotWidth).toInt().coerceIn(0, dailyData.lastIndex)
                    onSelectPoint(clickedIndex)
                }
            }
    ) {
        val w = size.width
        val h = size.height
        val bottomMargin = 26.dp.toPx()
        val chartHeight = h - bottomMargin
        val count = dailyData.size
        val slotWidth = w / count

        // Draw horizontal dashed grid lines
        val gridSteps = 3
        for (step in 1..gridSteps) {
            val yPos = chartHeight - (chartHeight * (step.toFloat() / gridSteps))
            drawLine(
                color = Color.White.copy(alpha = 0.08f),
                start = Offset(0f, yPos),
                end = Offset(w, yPos),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
            )
        }

        // Draw Zero Baseline
        drawLine(
            color = Color.White.copy(alpha = 0.2f),
            start = Offset(0f, chartHeight),
            end = Offset(w, chartHeight),
            strokeWidth = 1.5.dp.toPx()
        )

        // Draw bars for each day
        dailyData.forEachIndexed { index, pt ->
            val slotStart = index * slotWidth
            val isSelected = selectedPointIndex == index

            // Selection glow highlight
            if (isSelected) {
                drawRoundRect(
                    color = BrandEmeraldPrimary.copy(alpha = 0.12f),
                    topLeft = Offset(slotStart, 0f),
                    size = Size(slotWidth, chartHeight + bottomMargin),
                    cornerRadius = CornerRadius(8.dp.toPx())
                )
            }

            // Width of each individual bar within slot
            val barSpacing = 2.dp.toPx()
            val totalBarGroupWidth = (slotWidth * 0.75f).coerceAtMost(36.dp.toPx())
            val individualBarWidth = (totalBarGroupWidth - 2 * barSpacing) / 3f
            val groupStartX = slotStart + (slotWidth - totalBarGroupWidth) / 2f

            // 1. Sales Bar (Gold)
            val salesH = ((pt.salesAmount / maxVal) * chartHeight).toFloat().coerceAtLeast(3f)
            drawRoundRect(
                brush = Brush.verticalGradient(listOf(BrandGold, BrandGoldDark)),
                topLeft = Offset(groupStartX, chartHeight - salesH),
                size = Size(individualBarWidth, salesH),
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
            )

            // 2. Net Profit Bar (Emerald)
            val profitVal = pt.netProfit.coerceAtLeast(0.0)
            val profitH = ((profitVal / maxVal) * chartHeight).toFloat().coerceAtLeast(3f)
            drawRoundRect(
                brush = Brush.verticalGradient(listOf(BrandEmeraldPrimary, BrandEmeraldDarker)),
                topLeft = Offset(groupStartX + individualBarWidth + barSpacing, chartHeight - profitH),
                size = Size(individualBarWidth, profitH),
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
            )

            // 3. Expenses Bar (Rose)
            val expenseH = ((pt.expensesAmount / maxVal) * chartHeight).toFloat().coerceAtLeast(3f)
            drawRoundRect(
                brush = Brush.verticalGradient(listOf(ExpenseRose, Color(0xFFB91C1C))),
                topLeft = Offset(groupStartX + (individualBarWidth + barSpacing) * 2, chartHeight - expenseH),
                size = Size(individualBarWidth, expenseH),
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
            )

            // Bottom day indicator dot / marker
            val indicatorRadius = if (isSelected) 3.5.dp.toPx() else 2.dp.toPx()
            val indicatorColor = if (isSelected) BrandGold else TextSecondaryDark.copy(alpha = 0.6f)
            drawCircle(
                color = indicatorColor,
                radius = indicatorRadius,
                center = Offset(slotStart + slotWidth / 2f, chartHeight + 12.dp.toPx())
            )
        }
    }
}

/**
 * رسم بياني للمنحنى الانسيابي (Cubic Bézier Smooth Line / Area Chart)
 */
@Composable
fun LineChartCanvas(
    dailyData: List<DailyFinancialPoint>,
    selectedPointIndex: Int?,
    onSelectPoint: (Int) -> Unit
) {
    if (dailyData.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("لا توجد بيانات متاحة في هذه الفترة", color = TextSecondaryDark, fontSize = 12.sp)
        }
        return
    }

    val maxVal = remember(dailyData) {
        val peak = dailyData.maxOfOrNull {
            max(it.salesAmount, max(max(it.netProfit, 0.0), it.expensesAmount))
        } ?: 1.0
        if (peak <= 0.0) 1000.0 else peak * 1.15
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(dailyData) {
                detectTapGestures { offset ->
                    val count = dailyData.size
                    if (count <= 1) {
                        onSelectPoint(0)
                        return@detectTapGestures
                    }
                    val stepX = size.width / (count - 1)
                    val clickedIndex = ((offset.x + stepX / 2f) / stepX).toInt().coerceIn(0, dailyData.lastIndex)
                    onSelectPoint(clickedIndex)
                }
            }
    ) {
        val w = size.width
        val h = size.height
        val bottomMargin = 24.dp.toPx()
        val chartHeight = h - bottomMargin
        val count = dailyData.size

        if (count < 2) return@Canvas

        val stepX = w / (count - 1)

        // Draw horizontal grid lines
        val gridSteps = 3
        for (step in 1..gridSteps) {
            val yPos = chartHeight - (chartHeight * (step.toFloat() / gridSteps))
            drawLine(
                color = Color.White.copy(alpha = 0.08f),
                start = Offset(0f, yPos),
                end = Offset(w, yPos),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
            )
        }

        // Draw baseline
        drawLine(
            color = Color.White.copy(alpha = 0.2f),
            start = Offset(0f, chartHeight),
            end = Offset(w, chartHeight),
            strokeWidth = 1.5.dp.toPx()
        )

        // Generate points for Sales, Profit, Expenses
        val salesPoints = dailyData.mapIndexed { idx, pt ->
            val y = chartHeight - ((pt.salesAmount / maxVal) * chartHeight).toFloat()
            Offset(idx * stepX, y)
        }

        val profitPoints = dailyData.mapIndexed { idx, pt ->
            val pVal = pt.netProfit.coerceAtLeast(0.0)
            val y = chartHeight - ((pVal / maxVal) * chartHeight).toFloat()
            Offset(idx * stepX, y)
        }

        val expensePoints = dailyData.mapIndexed { idx, pt ->
            val y = chartHeight - ((pt.expensesAmount / maxVal) * chartHeight).toFloat()
            Offset(idx * stepX, y)
        }

        // Draw Profit Area Fill below profit line
        val profitAreaPath = Path().apply {
            moveTo(profitPoints.first().x, chartHeight)
            lineTo(profitPoints.first().x, profitPoints.first().y)
            for (i in 0 until profitPoints.size - 1) {
                val p0 = profitPoints[i]
                val p1 = profitPoints[i + 1]
                val cx = (p0.x + p1.x) / 2f
                cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
            }
            lineTo(profitPoints.last().x, chartHeight)
            close()
        }

        drawPath(
            path = profitAreaPath,
            brush = Brush.verticalGradient(
                listOf(BrandEmeraldPrimary.copy(alpha = 0.25f), Color.Transparent),
                startY = 0f,
                endY = chartHeight
            )
        )

        // Draw Curved Lines for Sales, Profit, and Expenses
        drawCurvedLine(this, salesPoints, BrandGold, 3.dp.toPx())
        drawCurvedLine(this, profitPoints, BrandEmeraldPrimary, 3.dp.toPx())
        drawCurvedLine(this, expensePoints, ExpenseRose, 2.dp.toPx())

        // Draw data points & selected day vertical guideline
        selectedPointIndex?.let { selIdx ->
            if (selIdx in dailyData.indices) {
                val selX = selIdx * stepX
                drawLine(
                    color = BrandGold.copy(alpha = 0.4f),
                    start = Offset(selX, 0f),
                    end = Offset(selX, chartHeight),
                    strokeWidth = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                )
            }
        }

        // Glowing Circles on points
        dailyData.forEachIndexed { idx, _ ->
            val pX = idx * stepX
            val isSelected = selectedPointIndex == idx

            val pProfit = profitPoints[idx]
            val pSales = salesPoints[idx]

            // Sales dot
            drawCircle(
                color = BrandGold,
                radius = if (isSelected) 5.dp.toPx() else 3.dp.toPx(),
                center = pSales
            )

            // Profit dot
            drawCircle(
                color = BrandEmeraldPrimary,
                radius = if (isSelected) 5.dp.toPx() else 3.dp.toPx(),
                center = pProfit
            )
            drawCircle(
                color = Color.White,
                radius = if (isSelected) 2.5.dp.toPx() else 1.5.dp.toPx(),
                center = pProfit
            )
        }
    }
}

private fun drawCurvedLine(
    drawScope: DrawScope,
    points: List<Offset>,
    color: Color,
    strokeWidth: Float
) {
    if (points.size < 2) return

    val path = Path().apply {
        moveTo(points.first().x, points.first().y)
        for (i in 0 until points.size - 1) {
            val p0 = points[i]
            val p1 = points[i + 1]
            val cx = (p0.x + p1.x) / 2f
            cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
        }
    }

    drawScope.drawPath(
        path = path,
        color = color,
        style = Stroke(
            width = strokeWidth,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )
}
