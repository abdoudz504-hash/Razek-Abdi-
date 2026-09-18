package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.example.ui.MainViewModel
import com.example.ui.components.TopStatusBar
import com.example.ui.navigation.Screen
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.ui.util.LanguageManager
import com.example.ui.util.LicenseInfo
import com.example.ui.util.LicenseManager
import com.example.ui.util.SoundHelper

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MainViewModel(application) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            enableEdgeToEdge()
        } catch (e: Throwable) {
            android.util.Log.e("MainActivity", "Error enabling edge to edge", e)
        }
        setContent {
            val context = LocalContext.current
            LaunchedEffect(Unit) {
                LanguageManager.init(context)
            }
            val currentLang = LanguageManager.currentLanguage
            val layoutDir = if (currentLang.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr

            MyApplicationTheme(darkTheme = true) {
                // Support dynamic language direction (RTL for Arabic/Persian/Urdu, LTR for others)
                CompositionLocalProvider(LocalLayoutDirection provides layoutDir) {
                    var isActivated by remember { mutableStateOf(LicenseManager.isActivated(context)) }

                    if (!isActivated) {
                        LicenseActivationScreen(
                            onActivatedSuccessfully = {
                                isActivated = true
                            }
                        )
                    } else {
                        RzTasyirApp(
                            viewModel = viewModel,
                            onLockApp = { isActivated = false }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RzTasyirApp(
    viewModel: MainViewModel,
    onLockApp: () -> Unit = {}
) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }
    val cartItems by viewModel.cartItems.collectAsStateWithLifecycle()
    val cartCount = remember(cartItems) { cartItems.sumOf { it.quantity } }
    var showLicenseDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("app_root_scaffold"),
        topBar = {
            Column {
                // Top Live System Bar: Weather, Temperature, Clock, Date, All Languages
                TopStatusBar()

                TopAppBar(
                    title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(BrandEmeraldDarker)
                                .border(1.dp, BrandGold, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingCart,
                                contentDescription = null,
                                tint = BrandGold,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column {
                            Text(
                                text = LanguageManager.tr("app_name"),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                            )
                            Text(
                                text = currentScreen.localizedTitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = currentScreen.accentColor
                            )
                        }
                    }
                },
                actions = {
                    // License Status Button
                    IconButton(
                        onClick = { showLicenseDialog = true },
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .clip(CircleShape)
                            .background(BrandSurfaceCardDark)
                            .testTag("license_info_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = "ترخيص النسخة التجارية",
                            tint = BrandEmeraldPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    if (currentScreen != Screen.Pos) {
                        BadgedBox(
                            badge = {
                                if (cartCount > 0) {
                                    Badge(
                                        containerColor = BrandGold,
                                        contentColor = Color.Black
                                    ) {
                                        Text("$cartCount", fontWeight = FontWeight.Bold)
                                    }
                                }
                            },
                            modifier = Modifier.padding(horizontal = 4.dp)
                        ) {
                            IconButton(
                                onClick = { currentScreen = Screen.Pos },
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(BrandSurfaceCardDark)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ShoppingCart,
                                    contentDescription = "السلة ونقاط البيع",
                                    tint = if (cartCount > 0) BrandGold else TextPrimaryDark
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BrandEmeraldDeep,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = BrandEmeraldDeep,
                contentColor = TextPrimaryDark,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .testTag("app_bottom_nav_bar")
            ) {
                Screen.bottomNavItems.forEach { screen ->
                    val isSelected = currentScreen == screen
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentScreen = screen },
                        icon = {
                            Icon(
                                imageVector = screen.icon,
                                contentDescription = screen.localizedTitle
                            )
                        },
                        label = {
                            Text(
                                text = screen.localizedTitle,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.Black,
                            selectedTextColor = screen.accentColor,
                            indicatorColor = screen.accentColor,
                            unselectedIconColor = TextSecondaryDark,
                            unselectedTextColor = TextSecondaryDark
                        )
                    )
                }
            }
        },
        containerColor = DarkBackground
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                is Screen.Home -> HomeScreen(
                    viewModel = viewModel,
                    onNavigateTo = { currentScreen = it }
                )
                is Screen.Pos -> PosScreen(
                    viewModel = viewModel
                )
                is Screen.Inventory -> InventoryScreen(
                    viewModel = viewModel
                )
                is Screen.Customers -> CustomersScreen(
                    viewModel = viewModel
                )
                is Screen.Expenses -> ExpensesScreen(
                    viewModel = viewModel
                )
                is Screen.Reports -> ReportsScreen(
                    viewModel = viewModel
                )
            }
        }
    }

    if (showLicenseDialog) {
        CommercialLicenseInfoDialog(
            onDismiss = { showLicenseDialog = false },
            onLockApp = {
                showLicenseDialog = false
                onLockApp()
            }
        )
    }
}

/**
 * نافذة عرض معلومات الترخيص التجاري للمحل والدعم الفني
 */
@Composable
fun CommercialLicenseInfoDialog(
    onDismiss: () -> Unit,
    onLockApp: () -> Unit
) {
    val context = LocalContext.current
    val licenseInfo = remember { LicenseManager.getLicenseInfo(context) }
    var showDevLockConfirm by remember { mutableStateOf(false) }
    var devPinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }

    fun copyToClipboard(text: String, label: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "تم نسخ $label بنجاح!", Toast.LENGTH_SHORT).show()
    }

    fun openWhatsApp() {
        try {
            val msg = Uri.encode("السلام عليكم، بخصوص ترخيص تطبيق RZ-تسيير للمحل (${licenseInfo.storeName}) - كود الجهاز: ${licenseInfo.deviceId}")
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?phone=${LicenseManager.DEVELOPER_PHONE_INTL}&text=$msg"))
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "يرجى التأكد من تثبيت تطبيق واتساب للتواصل مع الدعم", Toast.LENGTH_SHORT).show()
        }
    }

    fun makeCall() {
        try {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${LicenseManager.DEVELOPER_PHONE}"))
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "تعذر فتح تطبيق الاتصال", Toast.LENGTH_SHORT).show()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 20.dp)
                .testTag("commercial_license_info_dialog"),
            colors = CardDefaults.cardColors(containerColor = BrandSurfaceCardDark),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.5.dp, BrandEmeraldPrimary)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color.White)
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = null,
                            tint = BrandEmeraldPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "الترخيص التجاري المعتمد",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }

                    Box(modifier = Modifier.size(36.dp))
                }

                Spacer(modifier = Modifier.height(16.dp))

                // License Card Details
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0C1F17)),
                    border = BorderStroke(1.dp, BrandEmeraldDarker),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("اسم المتجر:", style = MaterialTheme.typography.bodySmall, color = TextSecondaryDark)
                            Text(licenseInfo.storeName, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                        }

                        HorizontalDivider(color = Color(0xFF1E3A2E))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("كود الجهاز:", style = MaterialTheme.typography.bodySmall, color = TextSecondaryDark)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    licenseInfo.deviceId,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
                                    color = BrandGold
                                )
                                IconButton(
                                    onClick = { copyToClipboard(licenseInfo.deviceId, "كود الجهاز") },
                                    modifier = Modifier.size(24.dp).padding(start = 4.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "نسخ", tint = BrandGold, modifier = Modifier.size(14.dp))
                                }
                            }
                        }

                        HorizontalDivider(color = Color(0xFF1E3A2E))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("خطة الترخيص:", style = MaterialTheme.typography.bodySmall, color = TextSecondaryDark)
                            Text(
                                text = licenseInfo.licenseType,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = BrandGold
                            )
                        }

                        if (licenseInfo.linkedGmail.isNotEmpty()) {
                            HorizontalDivider(color = Color(0xFF1E3A2E))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("حساب Gmail المرتبط:", style = MaterialTheme.typography.bodySmall, color = TextSecondaryDark)
                                Text(
                                    text = licenseInfo.linkedGmail,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFFFF8A80)
                                )
                            }
                        }

                        HorizontalDivider(color = Color(0xFF1E3A2E))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("حالة النسخة:", style = MaterialTheme.typography.bodySmall, color = TextSecondaryDark)
                            Text("مفعلة وتعمل بدون إنترنت ✅", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = BrandEmeraldPrimary)
                        }

                        HorizontalDivider(color = Color(0xFF1E3A2E))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("حماية الهاتف:", style = MaterialTheme.typography.bodySmall, color = TextSecondaryDark)
                            Text("هاتف واحد حصرياً 📱", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = BrandEmeraldPrimary)
                        }

                        HorizontalDivider(color = Color(0xFF1E3A2E))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("تاريخ التفعيل:", style = MaterialTheme.typography.bodySmall, color = TextSecondaryDark)
                            Text(licenseInfo.formattedActivationDate, style = MaterialTheme.typography.bodySmall, color = Color.White)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Developer & Support Info - الرقم مخفي ويوجه للواتساب مباشرة
                Text(
                    text = "فريق الدعم الفني وخدمة العملاء:",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondaryDark
                )
                Text(
                    text = "متاح عبر واتساب والمراسلة المباشرة",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = BrandGold)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Action buttons to contact developer & APK download
                Button(
                    onClick = { openWhatsApp() },
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Chat, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("مراسلة الدعم الفني عبر واتساب", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                Spacer(modifier = Modifier.height(8.dp))

                // زر تحميل وتحديث APK المباشر داخل نافذة معلومات الترخيص
                Button(
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(LicenseManager.APP_DOWNLOAD_URL))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "رابط APK: ${LicenseManager.APP_DOWNLOAD_URL}", Toast.LENGTH_LONG).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("رابط مباشر لتحميل APK والتحديثات", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Developer Lock/Reset Button (حماية وقفل التطبيق للمطور فقط)
                if (!showDevLockConfirm) {
                    TextButton(onClick = { showDevLockConfirm = true }) {
                        Icon(Icons.Default.LockReset, contentDescription = null, tint = TextSecondaryDark, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("قفل التطبيق مجدداً (للمطور فقط)", color = TextSecondaryDark, fontSize = 11.sp)
                    }
                } else {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = ExpenseRose.copy(alpha = 0.12f)),
                        border = BorderStroke(1.dp, ExpenseRose),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("أدخل رمز PIN للمطور (0665) لتأكيد قفل التطبيق:", color = ExpenseRose, fontSize = 11.sp)
                            Spacer(modifier = Modifier.height(6.dp))

                            OutlinedTextField(
                                value = devPinInput,
                                onValueChange = {
                                    devPinInput = it
                                    pinError = false
                                },
                                placeholder = { Text("رمز PIN") },
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                modifier = Modifier.fillMaxWidth().height(50.dp)
                            )

                            if (pinError) {
                                Text("رمز PIN غير صحيح!", color = ExpenseRose, fontSize = 11.sp)
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { showDevLockConfirm = false },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("إلغاء", fontSize = 11.sp)
                                }

                                Button(
                                    onClick = {
                                        if (LicenseManager.checkDeveloperPin(devPinInput)) {
                                            LicenseManager.resetLicenseForTesting(context)
                                            SoundHelper.playWarningTone()
                                            Toast.makeText(context, "تم قفل التطبيق بنجاح!", Toast.LENGTH_SHORT).show()
                                            onLockApp()
                                        } else {
                                            pinError = true
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = ExpenseRose),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("تأكيد القفل", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
