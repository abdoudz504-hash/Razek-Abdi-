package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.components.TopStatusBar
import com.example.ui.theme.*
import com.example.ui.util.LanguageManager
import com.example.ui.util.LicenseInfo
import com.example.ui.util.LicenseManager
import com.example.ui.util.SoundHelper
import com.example.ui.util.SubscriptionPlan
import com.example.ui.util.SystemStatusManager
import com.example.ui.util.VerificationResult

@Composable
fun LicenseActivationScreen(
    onActivatedSuccessfully: () -> Unit
) {
    val context = LocalContext.current
    val deviceId = remember { LicenseManager.getDeviceId(context) }
    val requestCode = remember(deviceId) { LicenseManager.getRequestCode(deviceId) }
    var selectedPlan by remember { mutableStateOf(LicenseManager.SUBSCRIPTION_PLANS[0]) } // Default to Lifetime
    var gmailInput by remember { mutableStateOf(LicenseManager.getLinkedGmail(context).ifEmpty { "user@gmail.com" }) }
    var isGmailVerified by remember { mutableStateOf(false) }
    var isSimulatingOnlineCheck by remember { mutableStateOf(false) }
    var onlineCheckPassed by remember { mutableStateOf(false) }
    var activationKeyInput by remember { mutableStateOf("") }
    var storeNameInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showDevDialog by remember { mutableStateOf(false) }
    var showSmsImportDialog by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    // Helper functions for communication
    fun copyToClipboard(text: String, label: String = "كود الجهاز") {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "تم نسخ $label بنجاح!", Toast.LENGTH_SHORT).show()
    }

    fun pasteFromClipboard() {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = clipboard.primaryClip
        if (clip != null && clip.itemCount > 0) {
            val text = clip.getItemAt(0).text?.toString() ?: ""
            // فحص إذا كان النص يحتوي على كود داخل رسالة SMS
            val extracted = LicenseManager.extractActivationCodeFromSms(text) ?: text.trim()
            activationKeyInput = extracted
            errorMessage = null
            Toast.makeText(context, "تم إدراج الكود: $extracted", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "الحافظة فارغة", Toast.LENGTH_SHORT).show()
        }
    }

    fun openWhatsAppDirect() {
        val cleanEmail = gmailInput.trim()
        if (cleanEmail.isBlank() || !cleanEmail.contains("@")) {
            Toast.makeText(context, "يرجى كتابة حساب Gmail المرتبط أولاً للربط بالجهاز", Toast.LENGTH_LONG).show()
            return
        }
        val url = LicenseManager.createWhatsAppRequestUrl(
            deviceId = deviceId,
            requestCode = requestCode,
            storeName = storeNameInput.ifBlank { null },
            gmailAccount = cleanEmail,
            plan = selectedPlan
        )
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "يرجى التأكد من تثبيت تطبيق واتساب على جهازك", Toast.LENGTH_LONG).show()
        }
    }

    fun openDirectDownloadLink() {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(LicenseManager.APP_DOWNLOAD_URL))
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "رابط التحميل المباشر: ${LicenseManager.APP_DOWNLOAD_URL}", Toast.LENGTH_LONG).show()
        }
    }

    fun sendSms(message: String) {
        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:${LicenseManager.DEVELOPER_PHONE}")
                putExtra("sms_body", message)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "تعذر فتح تطبيق الرسائل", Toast.LENGTH_LONG).show()
        }
    }

    fun makePhoneCall() {
        try {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:${LicenseManager.DEVELOPER_PHONE}")
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "تعذر فتح تطبيق الاتصال", Toast.LENGTH_LONG).show()
        }
    }

    fun attemptActivation() {
        errorMessage = null
        val cleanInput = activationKeyInput.trim()
        val cleanGmail = gmailInput.trim()

        if (cleanGmail.isBlank() || !cleanGmail.contains("@")) {
            errorMessage = "يرجى إدخال حساب Gmail الخاص بك لربط الترخيص بهذا الهاتف حصرياً"
            SoundHelper.playWarningTone()
            return
        }

        if (cleanInput.isBlank()) {
            errorMessage = "يرجى كتابة كود التفعيل المستلم من المالك عبر واتساب أو SMS"
            SoundHelper.playWarningTone()
            return
        }

        // فحص التحقق من الكود المدخل محلياً وخوارزمياً مع الخطة
        val verification = LicenseManager.verifyCodeDetails(deviceId, cleanInput, selectedPlan)
        if (!verification.isValid) {
            SoundHelper.playWarningTone()
            errorMessage = verification.message
            return
        }

        val success = LicenseManager.activate(
            context = context,
            inputKey = cleanInput,
            storeName = storeNameInput,
            plan = verification.detectedPlan ?: selectedPlan,
            gmailAccount = cleanGmail
        )

        if (success) {
            SoundHelper.playCashRegister()
            val planTitle = verification.detectedPlan?.title ?: selectedPlan.title
            Toast.makeText(
                context,
                "✓ تم توثيق الترخيص بحساب $cleanGmail بنجاح!\nالخطة: $planTitle\nيعمل الآن بدون إنترنت وبحماية الهاتف الواحد.",
                Toast.LENGTH_LONG
            ).show()
            onActivatedSuccessfully()
        } else {
            SoundHelper.playWarningTone()
            errorMessage = "فشل تفعيل الترخيص! يرجى إعادة المحاولة أو مراسلة المالك مباشرة عبر واتساب."
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .testTag("license_activation_screen")
    ) {
        // Top Real-time Status Bar (Clock, Date, Live Weather, All World Languages)
        TopStatusBar()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // App Icon & Lock Badge
            Box(contentAlignment = Alignment.BottomEnd) {
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(BrandEmeraldDeep, BrandEmeraldDarker)
                            )
                        )
                        .border(2.dp, BrandGold, RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = null,
                        tint = BrandGold,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .offset(x = 4.dp, y = 4.dp)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(ExpenseRose)
                        .border(2.dp, DarkBackground, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "محمي",
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "تطبيق RZ-تسيير التجاري",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp
                ),
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "نظام التفعيل المعتمد • ترخيص مدى الحياة • حماية الجهاز الواحد",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = BrandGold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Security & Offline Highlights Banner
            Surface(
                color = Color(0xFF071C14),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, BrandEmeraldPrimary.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.CloudOff, contentDescription = null, tint = BrandEmeraldPrimary, modifier = Modifier.size(20.dp))
                        Text(
                            text = "يعمل 100% بدون إنترنت (أوفلاين)",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "يحتاج اتصال إنترنت خفيف للمرة الأولى فقط عند إدخال الكود لمنع التلاعب وتوثيق الهاتف، ثم يعمل بشكل كامل بدون نت للأبد.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondaryDark,
                        lineHeight = 18.sp
                    )

                    HorizontalDivider(color = Color(0xFF133827), modifier = Modifier.padding(vertical = 8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.PhonelinkLock, contentDescription = null, tint = BrandGold, modifier = Modifier.size(20.dp))
                        Text(
                            text = "حماية الجهاز الواحد (Single Device Protection)",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = BrandGold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "الكود مرتبط بحساب Gmail ومعرف هاتفك حصرياً. لا يمكن تشغيله في هاتفين معاً. إذا فتحت التطبيق بهاتف جديد سيغلق تلقائياً في القديم لضمان حقك وحق المالك.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondaryDark,
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ==========================================
            // خطوة A: ربط حساب Gmail (حساب جوجل الموثق)
            // ==========================================
            Card(
                colors = CardDefaults.cardColors(containerColor = BrandSurfaceCardDark),
                border = BorderStroke(1.dp, Color(0xFFEA4335).copy(alpha = 0.7f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = null,
                            tint = Color(0xFFEA4335),
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "الخطوة 1: ربط حساب Gmail الشخصي للتوثيق",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "سيكون كود التفعيل مقترناً بحساب الجيميل الخاص بك دائماً لحفظ ترخيصك ومنع فتحه في عدة هواتف في نفس الوقت:",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondaryDark,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = gmailInput,
                        onValueChange = { gmailInput = it },
                        label = { Text("بريد Gmail الخاص بك") },
                        placeholder = { Text("yourname@gmail.com") },
                        singleLine = true,
                        leadingIcon = {
                            Icon(Icons.Default.Mail, contentDescription = null, tint = Color(0xFFEA4335))
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFEA4335),
                            unfocusedBorderColor = Color(0xFF475569),
                            focusedLabelColor = Color(0xFFEA4335),
                            unfocusedLabelColor = TextSecondaryDark,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("gmail_input")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Surface(
                        color = Color(0xFF261010),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFFEA4335).copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFFEA4335), modifier = Modifier.size(14.dp))
                            Text(
                                text = "حساب Gmail يضمن استعادة ترخيصك بأمان في حال استبدال هاتفك.",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFFF8A80)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ==========================================
            // خطوة B: ترخيص مدى الحياة (100 يورو)
            // ==========================================
            Card(
                colors = CardDefaults.cardColors(containerColor = BrandSurfaceCardDark),
                border = BorderStroke(1.5.dp, BrandGold),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.WorkspacePremium,
                            contentDescription = null,
                            tint = BrandGold,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "الخطوة 2: ترخيص التفعيل الرسمي الدائم",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Single Lifetime Plan Card (100 Euro)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF142B1E),
                        border = BorderStroke(2.dp, BrandGold),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("plan_item_life")
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = BrandGold,
                                    modifier = Modifier.size(22.dp)
                                )
                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "ترخيص مدى الحياة",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = Color.White
                                        )
                                        Surface(
                                            color = BrandGold,
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = "دائم 🔥",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                                color = Color.Black,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                    Text(
                                        text = "تفعيل دائم على هاتفك بدون اشتراكات شهرية أو سنوية",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondaryDark
                                    )
                                }
                            }

                            Text(
                                text = "100 €",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = BrandGold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ==========================================
            // كود الجهاز ورمز الطلب
            // ==========================================
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1E19)),
                border = BorderStroke(1.5.dp, BrandEmeraldPrimary),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "معرف هاتفك الفريد للتفعيل (Machine ID):",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondaryDark
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF06140F))
                            .border(1.dp, BrandGold.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(vertical = 10.dp, horizontal = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = deviceId,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 2.sp
                                ),
                                color = BrandGold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "رمز الطلب: $requestCode",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = BrandEmeraldPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = { copyToClipboard("معرف الجهاز: $deviceId - رمز الطلب: $requestCode - Gmail: ${gmailInput.trim()}", "كود الجهاز") },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = BorderStroke(1.dp, BrandEmeraldPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("copy_device_id_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = null,
                            tint = BrandEmeraldPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("نسخ كود الجهاز ومعلومات الطلب", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ==========================================
            // الخطوة 3: إرسال الكود للواتساب مباشرة للمالك
            // ==========================================
            Card(
                colors = CardDefaults.cardColors(containerColor = BrandSurfaceCardDark),
                border = BorderStroke(1.5.dp, Color(0xFF25D366)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Chat,
                            contentDescription = null,
                            tint = Color(0xFF25D366),
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "الخطوة 3: إرسال الطلب عبر واتساب للمالك المباشر",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "اضغط الزر أدناه ليفتح واتساب مباشرة مع رسالة جاهزة تتضمن كود جهازك وحساب Gmail لترخيص مدى الحياة (100 €) لإرسال كود التفعيل المعتمد لك فور سداد المبلغ:",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondaryDark,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Direct WhatsApp Button (Hero) - الرقم مخفي ويوجه مباشرة لمحادثة واتساب
                    Button(
                        onClick = { openWhatsAppDirect() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("send_whatsapp_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Chat,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "إرسال كود التفعيل إلى واتساب مباشرة",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Secondary SMS and Import options
                    val messageToSend = LicenseManager.createSmsRequestMessage(deviceId, requestCode, storeNameInput.ifBlank { null }, selectedPlan)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { sendSms(messageToSend) },
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .testTag("send_sms_button"),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF38BDF8)),
                            border = BorderStroke(1.dp, Color(0xFF0284C7)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Sms, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("إرسال عبر SMS", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = { showSmsImportDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .testTag("open_sms_import_button"),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = BrandEmeraldPrimary),
                            border = BorderStroke(1.dp, BrandEmeraldPrimary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.DownloadDone, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("استخراج الكود", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ==========================================
            // رابط مباشر لتحميل التطبيق بصيغة APK
            // ==========================================
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0A1926)),
                border = BorderStroke(1.dp, Color(0xFF0284C7)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(28.dp)
                            )
                            Column {
                                Text(
                                    text = "رابط مباشر لتحميل وتحديث التطبيق (APK)",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                                Text(
                                    text = LicenseManager.APP_DOWNLOAD_URL,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF94A3B8),
                                    maxLines = 1
                                )
                            }
                        }

                        Button(
                            onClick = { openDirectDownloadLink() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("تحميل APK", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                copyToClipboard(LicenseManager.APP_DOWNLOAD_URL, "رابط تحميل التطبيق APK")
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF38BDF8)),
                            border = BorderStroke(1.dp, Color(0xFF0284C7)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("نسخ الرابط المباشر", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }

                        OutlinedButton(
                            onClick = {
                                try {
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_SUBJECT, "تحميل تطبيق RZ-تسيير")
                                        putExtra(
                                            Intent.EXTRA_TEXT,
                                            "رابط مباشر لتحميل تطبيق RZ-تسيير للمحلات وإدارة المبيعات (APK):\n${LicenseManager.APP_DOWNLOAD_URL}\nترخيص تفعيل مدى الحياة دائم (100 €)"
                                        )
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "مشاركة رابط التطبيق"))
                                } catch (e: Exception) {
                                    copyToClipboard(LicenseManager.APP_DOWNLOAD_URL, "رابط تحميل APK")
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = BrandEmeraldPrimary),
                            border = BorderStroke(1.dp, BrandEmeraldPrimary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("مشاركة الرابط", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ==========================================
            // الخطوة 4: إدخال كود التفعيل والتحقق المحلي الفوري
            // ==========================================
            Card(
                colors = CardDefaults.cardColors(containerColor = BrandSurfaceCardDark),
                border = BorderStroke(1.dp, BrandEmeraldPrimary.copy(alpha = 0.7f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VpnKey,
                            contentDescription = null,
                            tint = BrandGold,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "الخطوة 4: إدخال كود التفعيل وتثبيت الترخيص",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Store Name (Optional)
                    OutlinedTextField(
                        value = storeNameInput,
                        onValueChange = { storeNameInput = it },
                        label = { Text("اسم المتجر أو النشاط التجاري (اختياري)") },
                        placeholder = { Text("مثال: ميني ماركت الهدى") },
                        singleLine = true,
                        leadingIcon = {
                            Icon(Icons.Default.Storefront, contentDescription = null, tint = BrandGold)
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BrandGold,
                            unfocusedBorderColor = Color(0xFF475569),
                            focusedLabelColor = BrandGold,
                            unfocusedLabelColor = TextSecondaryDark,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("store_name_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Activation Code Input
                    OutlinedTextField(
                        value = activationKeyInput,
                        onValueChange = {
                            activationKeyInput = it
                            errorMessage = null
                        },
                        label = { Text("كود التفعيل المستلم") },
                        placeholder = { Text("مثال: 7824-0665-LIFE أو RZK-XXXX-YYYY-LIFE") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Characters,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { attemptActivation() }),
                        leadingIcon = {
                            Icon(Icons.Default.Key, contentDescription = null, tint = BrandEmeraldPrimary)
                        },
                        trailingIcon = {
                            IconButton(
                                onClick = { pasteFromClipboard() },
                                modifier = Modifier.testTag("paste_code_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentPaste,
                                    contentDescription = "لصق الكود",
                                    tint = BrandGold
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BrandEmeraldPrimary,
                            unfocusedBorderColor = Color(0xFF475569),
                            focusedLabelColor = BrandEmeraldPrimary,
                            unfocusedLabelColor = TextSecondaryDark,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("activation_key_input")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Real-Time Local Verification Feedback
                    val liveVerification = remember(activationKeyInput, deviceId, selectedPlan) {
                        if (activationKeyInput.trim().isNotEmpty()) {
                            LicenseManager.verifyCodeDetails(deviceId, activationKeyInput.trim(), selectedPlan)
                        } else null
                    }

                    AnimatedVisibility(visible = liveVerification != null) {
                        liveVerification?.let { result ->
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (result.isValid) BrandEmeraldDarker.copy(alpha = 0.5f) else ExpenseRose.copy(alpha = 0.15f)
                                ),
                                border = BorderStroke(
                                    1.dp,
                                    if (result.isValid) BrandEmeraldPrimary else ExpenseRose
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = if (result.isValid) Icons.Default.CheckCircle else Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = if (result.isValid) BrandEmeraldPrimary else ExpenseRose,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column {
                                        Text(
                                            text = if (result.isValid) "التحقق المحلي: معتمد بنجاح" else "فحص الكود:",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = if (result.isValid) BrandEmeraldPrimary else ExpenseRose
                                        )
                                        Text(
                                            text = result.message,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (result.isValid) Color.White else ExpenseRose,
                                            lineHeight = 16.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Error message
                    AnimatedVisibility(visible = errorMessage != null && liveVerification == null) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = ExpenseRose.copy(alpha = 0.15f)),
                            border = BorderStroke(1.dp, ExpenseRose),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = ExpenseRose,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = errorMessage ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ExpenseRose,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Primary Activate Button
                    Button(
                        onClick = { attemptActivation() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("confirm_activation_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BrandEmeraldPrimary,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "التحقق وتفعيل ترخيص ${selectedPlan.title}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.Black
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Developer Gate Button
            TextButton(
                onClick = { showDevDialog = true },
                modifier = Modifier.testTag("open_dev_panel_button")
            ) {
                Icon(
                    imageVector = Icons.Default.AdminPanelSettings,
                    contentDescription = null,
                    tint = TextSecondaryDark,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "بوابة المطور (توليد مفاتيح المحلات ومدى الحياة)",
                    color = TextSecondaryDark,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Developer Key Generator Dialog (للمطور فقط)
    if (showDevDialog) {
        DeveloperKeyGeneratorDialog(
            currentDeviceId = deviceId,
            onDismiss = { showDevDialog = false },
            onQuickActivateCurrent = { key ->
                activationKeyInput = key
                showDevDialog = false
                attemptActivation()
            }
        )
    }

    // نافذة استخراج كود التفعيل من رسائل SMS الواردة
    if (showSmsImportDialog) {
        SmsActivationExtractorDialog(
            currentDeviceId = deviceId,
            onDismiss = { showSmsImportDialog = false },
            onCodeExtracted = { extractedCode ->
                activationKeyInput = extractedCode
                showSmsImportDialog = false
                attemptActivation()
            }
        )
    }
}

/**
 * نافذة المطور السرية لتوليد مفاتيح التفعيل للزبائن والمحلات
 * محمية بـ PIN سري للمطور فقط (0665 أو 3528 أو رقم هاتفه)
 */
@Composable
fun DeveloperKeyGeneratorDialog(
    currentDeviceId: String,
    onDismiss: () -> Unit,
    onQuickActivateCurrent: (String) -> Unit
) {
    val context = LocalContext.current
    var pinInput by remember { mutableStateOf("") }
    var isPinUnlocked by remember { mutableStateOf(false) }
    var pinError by remember { mutableStateOf(false) }

    var targetDeviceIdInput by remember { mutableStateOf(currentDeviceId) }
    var calculatedKey by remember { mutableStateOf(LicenseManager.generateActivationKey(currentDeviceId)) }

    fun copyKey() {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("مفتاح التفعيل", calculatedKey))
        Toast.makeText(context, "تم نسخ مفتاح التفعيل: $calculatedKey", Toast.LENGTH_SHORT).show()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 24.dp)
                .testTag("dev_generator_dialog"),
            colors = CardDefaults.cardColors(containerColor = BrandSurfaceCardDark),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.5.dp, BrandGold)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color.White)
                    }
                    Text(
                        text = "🔑 بوابة المطور (توليد المفاتيح)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = BrandGold
                    )
                    Box(modifier = Modifier.size(36.dp))
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (!isPinUnlocked) {
                    // PIN Entry Screen
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = BrandGold,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "أدخل الرمز السري للمطور (0665)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = {
                            pinInput = it
                            pinError = false
                        },
                        label = { Text("رمز PIN") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BrandGold,
                            unfocusedBorderColor = Color(0xFF475569),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (pinError) {
                        Text(
                            text = "رمز PIN غير صحيح! مخصص للمطور فقط",
                            color = ExpenseRose,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (LicenseManager.checkDeveloperPin(pinInput)) {
                                isPinUnlocked = true
                            } else {
                                pinError = true
                                SoundHelper.playWarningTone()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandGold, contentColor = Color.Black),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("فتح لوحة المفاتيح", fontWeight = FontWeight.Bold)
                    }
                } else {
                    // Unlocked Key Generator Panel
                    Text(
                        text = "يمكنك توليد كود التفعيل لأي زبون بإدخال معرف جهازه أو استخدام قاعدة الرد السريع:",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondaryDark,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = targetDeviceIdInput,
                        onValueChange = {
                            targetDeviceIdInput = it
                            calculatedKey = LicenseManager.generateActivationKey(it)
                        },
                        label = { Text("معرف جهاز الزبون (Device ID)") },
                        placeholder = { Text("مثال: RZ-XXXX-YYYY") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BrandEmeraldPrimary,
                            unfocusedBorderColor = Color(0xFF475569),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    val quickReplyKey = remember(targetDeviceIdInput) { LicenseManager.getQuickActivationKey(targetDeviceIdInput) }

                    // Calculated Result Box
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F241C)),
                        border = BorderStroke(1.dp, BrandEmeraldPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "1. كود التفعيل السريع (أرسله للزبون بعد الدفع):",
                                style = MaterialTheme.typography.labelMedium,
                                color = BrandGold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = quickReplyKey,
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 2.sp
                                ),
                                color = BrandGold
                            )

                            HorizontalDivider(color = Color(0xFF1E3A2E), modifier = Modifier.padding(vertical = 8.dp))

                            Text(
                                text = "2. مفتاح التفعيل الرسمي الكامل:",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondaryDark
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = calculatedKey,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = BrandEmeraldPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Tip box
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val sampleReqCode = remember(currentDeviceId) { LicenseManager.getRequestCode(currentDeviceId) }
                        Text(
                            text = "💡 سر المطور: كل رسالة تصلك من زبون تجد فيها 'رمز الطلب' (مثال: $sampleReqCode). كود التفعيل هو ببساطة رمز الطلب متبوعاً بـ 0665 (مثال: $sampleReqCode-0665)!",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF94A3B8),
                            modifier = Modifier.padding(10.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("كود التفعيل", quickReplyKey))
                                Toast.makeText(context, "تم نسخ كود التفعيل: $quickReplyKey", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandGold, contentColor = Color.Black),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("نسخ الكود", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        // Send SMS reply button
                        Button(
                            onClick = {
                                try {
                                    val replyBody = LicenseManager.createSmsApprovalReply(targetDeviceIdInput, quickReplyKey)
                                    val smsIntent = Intent(Intent.ACTION_SENDTO).apply {
                                        data = Uri.parse("smsto:")
                                        putExtra("sms_body", replyBody)
                                    }
                                    context.startActivity(smsIntent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "تعذر فتح تطبيق الرسائل", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("إرسال SMS", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                onQuickActivateCurrent(quickReplyKey)
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandEmeraldPrimary, contentColor = Color.Black),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("تفعيل هنا", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "كود الطوارئ السريع للمطور: 0665",
                        style = MaterialTheme.typography.labelSmall,
                        color = BrandGold.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/**
 * نافذة مخصصة لاستخراج كود التفعيل من نصوص رسائل SMS الواردة من المالك
 */
@Composable
fun SmsActivationExtractorDialog(
    currentDeviceId: String,
    onDismiss: () -> Unit,
    onCodeExtracted: (String) -> Unit
) {
    val context = LocalContext.current
    var smsText by remember { mutableStateOf("") }
    var detectedCode by remember { mutableStateOf<String?>(null) }
    var verificationResult by remember { mutableStateOf<VerificationResult?>(null) }

    fun analyzeText(text: String) {
        smsText = text
        val found = LicenseManager.extractActivationCodeFromSms(text)
        detectedCode = found
        if (found != null) {
            verificationResult = LicenseManager.verifyCodeDetails(currentDeviceId, found)
        } else {
            verificationResult = null
        }
    }

    fun pasteFromClipboard() {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = clipboard.primaryClip
        if (clip != null && clip.itemCount > 0) {
            val content = clip.getItemAt(0).text?.toString() ?: ""
            analyzeText(content)
        } else {
            Toast.makeText(context, "الحافظة فارغة", Toast.LENGTH_SHORT).show()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 24.dp)
                .testTag("sms_extractor_dialog"),
            colors = CardDefaults.cardColors(containerColor = BrandSurfaceCardDark),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.5.dp, Color(0xFF0284C7))
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color.White)
                    }
                    Text(
                        text = "📩 استخراج كود التفعيل من رسالة SMS",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF38BDF8)
                    )
                    Box(modifier = Modifier.size(36.dp))
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "الصق نص الرسالة التي وصلتك من المالك عبر SMS أو واتساب. سيقوم النظام بالتعرف التلقائي على كود التفعيل والتحقق منه محلياً:",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondaryDark,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = smsText,
                    onValueChange = { analyzeText(it) },
                    label = { Text("نص الرسالة المستلمة") },
                    placeholder = { Text("الصق هنا نص رسالة SMS المستلمة...") },
                    minLines = 3,
                    maxLines = 5,
                    trailingIcon = {
                        IconButton(onClick = { pasteFromClipboard() }) {
                            Icon(Icons.Default.ContentPaste, contentDescription = "لصق", tint = Color(0xFF38BDF8))
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF0284C7),
                        unfocusedBorderColor = Color(0xFF475569),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = { pasteFromClipboard() },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF38BDF8)),
                    border = BorderStroke(1.dp, Color(0xFF0284C7)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("لصق الرسالة من الحافظة", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Result Box
                if (detectedCode != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0B291E)),
                        border = BorderStroke(1.dp, BrandEmeraldPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "✓ تم العثور على كود التفعيل بنجاح:",
                                style = MaterialTheme.typography.labelMedium,
                                color = BrandEmeraldPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = detectedCode ?: "",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 2.sp
                                ),
                                color = BrandGold
                            )

                            verificationResult?.let { vRes ->
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (vRes.isValid) "حالة الكود: سليم ومعتمد لجهازك" else vRes.message,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (vRes.isValid) BrandEmeraldPrimary else ExpenseRose
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            detectedCode?.let { onCodeExtracted(it) }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandEmeraldPrimary, contentColor = Color.Black),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("استخدام هذا الكود وتفعيل التطبيق", fontWeight = FontWeight.Bold)
                    }
                } else if (smsText.isNotBlank()) {
                    Text(
                        text = "لم يتم العثور على كود تفعيل واضح داخل النص. تأكد من أن الرسالة تحتوي على كود مثل 7824-0665 أو RZK-XXXX.",
                        style = MaterialTheme.typography.bodySmall,
                        color = ExpenseRose,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
