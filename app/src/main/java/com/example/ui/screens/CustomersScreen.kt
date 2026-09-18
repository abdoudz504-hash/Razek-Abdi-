package com.example.ui.screens

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.CustomerEntity
import com.example.ui.MainViewModel
import com.example.ui.components.ArabicSearchBar
import com.example.ui.components.formatCurrency
import com.example.ui.components.formatDate
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomersScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    val activeCustomer by viewModel.activeCustomer.collectAsStateWithLifecycle()
    val activeCustomerPayments by viewModel.activeCustomerPayments.collectAsStateWithLifecycle()
    val activeCustomerSales by viewModel.activeCustomerSales.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var filterOnlyDebtors by remember { mutableStateOf(false) }

    var isAddingCustomer by remember { mutableStateOf(false) }
    var customerToEdit by remember { mutableStateOf<CustomerEntity?>(null) }
    var customerForPayment by remember { mutableStateOf<CustomerEntity?>(null) }
    var customerToDelete by remember { mutableStateOf<CustomerEntity?>(null) }
    var showCustomerLedgerSheet by remember { mutableStateOf(false) }

    val totalDebt = remember(customers) { customers.sumOf { it.totalDebt } }
    val debtorsCount = remember(customers) { customers.count { it.totalDebt > 0 } }

    val filteredCustomers = remember(customers, searchQuery, filterOnlyDebtors) {
        customers.filter { customer ->
            val matchesSearch = searchQuery.isBlank() ||
                    customer.name.contains(searchQuery, ignoreCase = true) ||
                    customer.phone.contains(searchQuery, ignoreCase = true)
            val matchesDebt = !filterOnlyDebtors || customer.totalDebt > 0
            matchesSearch && matchesDebt
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { isAddingCustomer = true },
                containerColor = CustomerCyan,
                contentColor = Color.Black,
                icon = { Icon(Icons.Default.PersonAdd, contentDescription = null) },
                text = { Text("إضافة عميل", fontWeight = FontWeight.Bold) },
                modifier = Modifier
                    .testTag("add_customer_fab")
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
            // Header: Total Debts in Market
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
                            listOf(CustomerCyan.copy(alpha = 0.5f), ExpenseRose.copy(alpha = 0.4f))
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
                            Text(
                                text = "إجمالي ديون العملاء (كريدي):",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondaryDark
                            )
                            Text(
                                text = formatCurrency(totalDebt, viewModel.currency),
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = ExpenseRose
                                )
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF1E293B))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "$debtorsCount زبائن عليهم ديون",
                                color = CustomerCyan,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                ArabicSearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    placeholder = "بحث بالاسم أو رقم الهاتف..."
                )

                FilterChip(
                    selected = filterOnlyDebtors,
                    onClick = { filterOnlyDebtors = !filterOnlyDebtors },
                    label = { Text("عرض من عليهم ديون فقط ($debtorsCount)", fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ExpenseRose,
                        selectedLabelColor = Color.White,
                        containerColor = BrandSurfaceCardDark,
                        labelColor = TextPrimaryDark
                    )
                )
            }

            // Customer List
            if (filteredCustomers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "لا يوجد عملاء مطابقين للبحث",
                        color = TextSecondaryDark,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("customers_list")
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredCustomers, key = { it.id }) { customer ->
                        CustomerCard(
                            customer = customer,
                            currency = viewModel.currency,
                            onRecordPayment = { customerForPayment = customer },
                            onViewLedger = {
                                viewModel.setActiveCustomer(customer)
                                showCustomerLedgerSheet = true
                            },
                            onEdit = { customerToEdit = customer },
                            onDelete = { customerToDelete = customer }
                        )
                    }
                }
            }
        }
    }

    // Add / Edit Customer Dialog
    if (isAddingCustomer || customerToEdit != null) {
        val initial = customerToEdit ?: CustomerEntity(name = "")
        CustomerFormDialog(
            initialCustomer = initial,
            currency = viewModel.currency,
            onDismiss = {
                isAddingCustomer = false
                customerToEdit = null
            },
            onSave = { customer ->
                viewModel.saveCustomer(customer) {
                    isAddingCustomer = false
                    customerToEdit = null
                }
            }
        )
    }

    // Payment Registration Dialog
    customerForPayment?.let { customer ->
        PaymentRegistrationDialog(
            customer = customer,
            currency = viewModel.currency,
            onDismiss = { customerForPayment = null },
            onConfirm = { amount, notes ->
                viewModel.recordPayment(customer.id, amount, notes) {
                    customerForPayment = null
                }
            }
        )
    }

    // Delete Confirmation Dialog
    customerToDelete?.let { customer ->
        AlertDialog(
            onDismissRequest = { customerToDelete = null },
            containerColor = Color(0xFF1E1010),
            shape = RoundedCornerShape(20.dp),
            title = { Text("تأكيد حذف العميل", color = TextPrimaryDark, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "هل أنت متأكد من حذف العميل: ${customer.name}؟ إذا كان عليه ديون فستفقد سجل المستحقات.",
                    color = Color(0xFFFCA5A5)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteCustomer(customer)
                        customerToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ExpenseRose)
                ) {
                    Text("حذف", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { customerToDelete = null }) {
                    Text("إلغاء", color = TextSecondaryDark)
                }
            }
        )
    }

    // Customer Ledger Sheet
    if (showCustomerLedgerSheet && activeCustomer != null) {
        val customer = activeCustomer!!
        AlertDialog(
            onDismissRequest = { showCustomerLedgerSheet = false },
            modifier = Modifier.testTag("customer_ledger_dialog"),
            containerColor = Color(0xFF0F261D),
            shape = RoundedCornerShape(24.dp),
            title = {
                Column {
                    Text(
                        text = "كشف حساب: ${customer.name}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                    if (customer.phone.isNotBlank()) {
                        Text(
                            text = "الهاتف: ${customer.phone}",
                            color = CustomerCyan,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Debt Badge
                    Card(
                        colors = CardDefaults.cardColors(containerColor = BrandSurfaceCardDark),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("الرصيد المتبقي بذمته:", color = TextSecondaryDark)
                            Text(
                                text = formatCurrency(customer.totalDebt, viewModel.currency),
                                color = if (customer.totalDebt > 0) ExpenseRose else BrandEmeraldPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }
                    }

                    // Share debt reminder button
                    if (customer.totalDebt > 0) {
                        Button(
                            onClick = {
                                val message = "السلام عليكم ورحمة الله، أخي الكريم ${customer.name}، نود تذكيركم بمستحقات متجر Rz Tasyir وقدرها ${formatCurrency(customer.totalDebt, viewModel.currency)}. شكراً لتعاملكم الراقي معنا."
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, message)
                                    type = "text/plain"
                                }
                                val shareIntent = Intent.createChooser(sendIntent, "مشاركة تذكير الدين")
                                context.startActivity(shareIntent)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A8A)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("إرسال تذكير بالمستحقات", fontSize = 12.sp)
                        }
                    }

                    Text("سجل الدفعات والمشتريات:", style = MaterialTheme.typography.labelMedium, color = TextSecondaryDark)

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (activeCustomerPayments.isEmpty() && activeCustomerSales.isEmpty()) {
                            item {
                                Text("لا توجد حركات مسجلة حالياً.", color = TextSecondaryDark, fontSize = 13.sp)
                            }
                        }

                        items(activeCustomerPayments) { payment ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF0D3323)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("تسديد دفعة", color = BrandEmeraldPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text(formatDate(payment.timestamp), color = TextSecondaryDark, fontSize = 11.sp)
                                        if (payment.notes.isNotBlank()) {
                                            Text(payment.notes, color = Color(0xFF94A3B8), fontSize = 11.sp)
                                        }
                                    }
                                    Text(
                                        "- ${formatCurrency(payment.amount, viewModel.currency)}",
                                        color = BrandEmeraldPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        items(activeCustomerSales.filter { it.debtAmount > 0 }) { sale ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF331818)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("فاتورة بيع بالدين (${sale.invoiceNumber})", color = ExpenseRose, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text(formatDate(sale.timestamp), color = TextSecondaryDark, fontSize = 11.sp)
                                    }
                                    Text(
                                        "+ ${formatCurrency(sale.debtAmount, viewModel.currency)}",
                                        color = ExpenseRose,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCustomerLedgerSheet = false }) {
                    Text("إغلاق", color = BrandEmeraldPrimary)
                }
            }
        )
    }
}

@Composable
fun CustomerCard(
    customer: CustomerEntity,
    currency: String,
    onRecordPayment: () -> Unit,
    onViewLedger: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onViewLedger() }
            .testTag("customer_card_${customer.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BrandSurfaceCardDark),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.linearGradient(
                listOf(
                    if (customer.totalDebt > 0) ExpenseRose.copy(alpha = 0.4f) else BrandSurfaceBorderDark,
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
                            .background(CustomerCyan.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = CustomerCyan
                        )
                    }

                    Column {
                        Text(
                            text = customer.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimaryDark
                        )
                        if (customer.phone.isNotBlank()) {
                            Text(
                                text = customer.phone,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondaryDark
                            )
                        }
                    }
                }

                // Total Debt Badge
                if (customer.totalDebt > 0) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text("المستحق بالدين", style = MaterialTheme.typography.labelSmall, color = TextSecondaryDark)
                        Text(
                            text = formatCurrency(customer.totalDebt, currency),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = ExpenseRose
                            )
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF064E3B))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "خالص (0 د.ج)",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFFA7F3D0)
                        )
                    }
                }
            }

            if (customer.notes.isNotBlank()) {
                Text(
                    text = "ملاحظة: ${customer.notes}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF94A3B8)
                )
            }

            HorizontalDivider(color = BrandSurfaceBorderDark)

            // Actions Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onViewLedger,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("دفتر الحساب", fontSize = 12.sp, color = CustomerCyan)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (customer.totalDebt > 0) {
                        Button(
                            onClick = onRecordPayment,
                            colors = ButtonDefaults.buttonColors(containerColor = BrandEmeraldPrimary),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.PriceCheck, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("تسديد دفعة", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

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
}

@Composable
fun CustomerFormDialog(
    initialCustomer: CustomerEntity,
    currency: String,
    onDismiss: () -> Unit,
    onSave: (CustomerEntity) -> Unit
) {
    var name by remember { mutableStateOf(initialCustomer.name) }
    var phone by remember { mutableStateOf(initialCustomer.phone) }
    var initialDebtText by remember { mutableStateOf(if (initialCustomer.totalDebt > 0) initialCustomer.totalDebt.toString() else "") }
    var notes by remember { mutableStateOf(initialCustomer.notes) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("customer_form_dialog"),
        containerColor = Color(0xFF0F261D),
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                text = if (initialCustomer.id == 0L) "إضافة عميل جديد" else "تعديل بيانات العميل",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم العميل *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("رقم الهاتف (للتواصل والتذكير)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                if (initialCustomer.id == 0L) {
                    OutlinedTextField(
                        value = initialDebtText,
                        onValueChange = { initialDebtText = it },
                        label = { Text("رصيد دين سابق إن وجد ($currency)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات (عنوان أو تفاصيل إضافية)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val initialDebt = initialDebtText.toDoubleOrNull() ?: 0.0
                        val customer = initialCustomer.copy(
                            name = name.trim(),
                            phone = phone.trim(),
                            totalDebt = if (initialCustomer.id == 0L) initialDebt else initialCustomer.totalDebt,
                            notes = notes.trim()
                        )
                        onSave(customer)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = BrandEmeraldPrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("حفظ العميل", color = Color.Black, fontWeight = FontWeight.Bold)
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
fun PaymentRegistrationDialog(
    customer: CustomerEntity,
    currency: String,
    onDismiss: () -> Unit,
    onConfirm: (amount: Double, notes: String) -> Unit
) {
    var amountText by remember { mutableStateOf(customer.totalDebt.toString()) }
    var notesText by remember { mutableStateOf("تسديد دفعة نقدية") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F261D),
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.PriceCheck, contentDescription = null, tint = BrandEmeraldPrimary)
                Text("تسجيل تسديد دين", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("العميل: ${customer.name}", color = BrandGoldLight, fontWeight = FontWeight.Bold)
                Text(
                    "المبلغ المستحق حالياً: ${formatCurrency(customer.totalDebt, currency)}",
                    color = ExpenseRose,
                    fontSize = 13.sp
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("المبلغ المدفوع ($currency)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = { Text("ملاحظات / وصف الدفعة") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: 0.0
                    if (amount > 0) {
                        onConfirm(amount, notesText)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = BrandEmeraldPrimary)
            ) {
                Text("تأكيد التسديد", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء", color = TextSecondaryDark)
            }
        }
    )
}
