package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ExpenseEntity
import com.example.ui.MainViewModel
import com.example.ui.components.formatCurrency
import com.example.ui.components.formatDate
import com.example.ui.theme.*
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensesScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val expenses by viewModel.expenses.collectAsStateWithLifecycle()
    val kpis by viewModel.kpis.collectAsStateWithLifecycle()

    var selectedCategory by remember { mutableStateOf("الكل") }
    var isAddingExpense by remember { mutableStateOf(false) }
    var expenseToDelete by remember { mutableStateOf<ExpenseEntity?>(null) }

    val categories = listOf("الكل", "كراء المحل", "كهرباء وماء", "رواتب عمال", "نقل وسلعة", "مستلزمات", "صيانة", "أخرى")

    val totalMonthExpenses = remember(expenses) {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        val startOfMonth = cal.timeInMillis
        expenses.filter { it.timestamp >= startOfMonth }.sumOf { it.amount }
    }

    val filteredExpenses = remember(expenses, selectedCategory) {
        if (selectedCategory == "الكل") expenses
        else expenses.filter { it.category == selectedCategory }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { isAddingExpense = true },
                containerColor = ExpenseRose,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("تسجيل مصروف", fontWeight = FontWeight.Bold) },
                modifier = Modifier
                    .testTag("add_expense_fab")
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
            // Header Stats
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
                        brush = Brush.horizontalGradient(
                            listOf(ExpenseRose.copy(alpha = 0.5f), Color(0xFFB91C1C).copy(alpha = 0.3f))
                        )
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("مصاريف اليوم:", style = MaterialTheme.typography.bodySmall, color = TextSecondaryDark)
                            Text(
                                formatCurrency(kpis.todayExpenses, viewModel.currency),
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = ExpenseRose
                                )
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text("مصاريف الشهر الحالي:", style = MaterialTheme.typography.bodySmall, color = TextSecondaryDark)
                            Text(
                                formatCurrency(totalMonthExpenses, viewModel.currency),
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                        }
                    }
                }

                // Category Filter Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(categories) { cat ->
                        val isSelected = cat == selectedCategory
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ExpenseRose,
                                selectedLabelColor = Color.White,
                                containerColor = BrandSurfaceCardDark,
                                labelColor = TextPrimaryDark
                            )
                        )
                    }
                }
            }

            // Expenses List
            if (filteredExpenses.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "لا توجد مصاريف مسجلة في هذا التصنيف",
                        color = TextSecondaryDark,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("expenses_list")
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredExpenses, key = { it.id }) { expense ->
                        ExpenseItemCard(
                            expense = expense,
                            currency = viewModel.currency,
                            onDelete = { expenseToDelete = expense }
                        )
                    }
                }
            }
        }
    }

    // Add Expense Dialog
    if (isAddingExpense) {
        ExpenseFormDialog(
            currency = viewModel.currency,
            categories = categories.filter { it != "الكل" },
            onDismiss = { isAddingExpense = false },
            onSave = { expense ->
                viewModel.saveExpense(expense) {
                    isAddingExpense = false
                }
            }
        )
    }

    // Delete Confirmation Dialog
    expenseToDelete?.let { expense ->
        AlertDialog(
            onDismissRequest = { expenseToDelete = null },
            containerColor = Color(0xFF1E1010),
            shape = RoundedCornerShape(20.dp),
            title = { Text("تأكيد حذف المصروف", color = TextPrimaryDark) },
            text = { Text("هل تريد حذف هذا المصروف: ${expense.title} بقيمة ${formatCurrency(expense.amount, viewModel.currency)}؟", color = Color(0xFFFCA5A5)) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteExpense(expense)
                        expenseToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ExpenseRose)
                ) {
                    Text("حذف", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { expenseToDelete = null }) {
                    Text("إلغاء", color = TextSecondaryDark)
                }
            }
        )
    }
}

@Composable
fun ExpenseItemCard(
    expense: ExpenseEntity,
    currency: String,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("expense_card_${expense.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BrandSurfaceCardDark),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.linearGradient(
                listOf(BrandSurfaceBorderDark, ExpenseRose.copy(alpha = 0.2f))
            )
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
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(ExpenseRose.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ReceiptLong,
                        contentDescription = null,
                        tint = ExpenseRose
                    )
                }

                Column {
                    Text(
                        text = expense.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimaryDark
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(expense.category, color = ExpenseRose, fontSize = 12.sp)
                        Text("•", color = TextSecondaryDark)
                        Text(formatDate(expense.timestamp), color = TextSecondaryDark, fontSize = 11.sp)
                    }
                    if (expense.notes.isNotBlank()) {
                        Text(expense.notes, color = Color(0xFF94A3B8), fontSize = 12.sp)
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = formatCurrency(expense.amount, currency),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = ExpenseRose
                    )
                )
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "حذف", tint = Color(0xFF94A3B8))
                }
            }
        }
    }
}

@Composable
fun ExpenseFormDialog(
    currency: String,
    categories: List<String>,
    onDismiss: () -> Unit,
    onSave: (ExpenseEntity) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(categories.firstOrNull() ?: "أخرى") }
    var amountText by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("expense_form_dialog"),
        containerColor = Color(0xFF0F261D),
        shape = RoundedCornerShape(24.dp),
        title = {
            Text("تسجيل مصروف جديد", color = Color.White, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("بيان المصروف (مثال: فاتورة كهرباء، كراء، أكياس)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Text("التصنيف:", color = TextSecondaryDark, fontSize = 13.sp)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(categories) { cat ->
                        val isSelected = cat == selectedCategory
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ExpenseRose,
                                selectedLabelColor = Color.White,
                                containerColor = BrandSurfaceCardDark,
                                labelColor = TextPrimaryDark
                            )
                        )
                    }
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("المبلغ ($currency) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات إضافية") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: 0.0
                    if (title.isNotBlank() && amount > 0) {
                        onSave(
                            ExpenseEntity(
                                title = title.trim(),
                                category = selectedCategory,
                                amount = amount,
                                timestamp = System.currentTimeMillis(),
                                notes = notes.trim()
                            )
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = ExpenseRose),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("حفظ المصروف", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء", color = TextSecondaryDark)
            }
        }
    )
}
