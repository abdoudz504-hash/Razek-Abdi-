package com.example.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.model.CartItem
import com.example.data.model.ProductEntity
import com.example.ui.BarcodeScanResult
import com.example.ui.theme.*
import com.example.ui.util.SoundHelper
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

/**
 * Real CameraX Barcode Scanner Composable with ML Kit detection, flashlight control,
 * audio/haptic feedback, continuous cashier mode, and manual fallback.
 */
@androidx.annotation.OptIn(ExperimentalGetImage::class)
@Composable
fun RealCameraBarcodeScannerDialog(
    products: List<ProductEntity>,
    cartItems: List<CartItem> = emptyList(),
    onUpdateQuantity: ((Long, Int) -> Unit)? = null,
    onBarcodeScanned: (String) -> BarcodeScanResult,
    onDismiss: () -> Unit,
    cartTotal: Double = 0.0,
    cartCount: Int = 0,
    currency: String = "د.ج",
    onOpenCostCalculator: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    var manualBarcode by remember { mutableStateOf("") }
    var scanFeedbackMessage by remember { mutableStateOf<String?>(null) }
    var scanSuccess by remember { mutableStateOf(false) }
    var activeProduct by remember { mutableStateOf<ProductEntity?>(null) }
    var lastScannedCode by remember { mutableStateOf("") }
    var lastScanTimestamp by remember { mutableLongStateOf(0L) }
    var isFlashlightOn by remember { mutableStateOf(false) }
    var cameraControl by remember { mutableStateOf<CameraControl?>(null) }

    // Haptic feedback trigger
    fun triggerHapticFeedback() {
        try {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(
                    VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(80)
            }
        } catch (_: Exception) {
            // Ignore on devices without vibrator
        }
    }

    fun handleBarcodeDetection(code: String) {
        val trimmed = code.trim()
        if (trimmed.isEmpty()) return

        val now = System.currentTimeMillis()
        // Debounce same barcode within 1.5 seconds to avoid spamming audio
        if (trimmed == lastScannedCode && (now - lastScanTimestamp) < 1500L) {
            return
        }

        lastScannedCode = trimmed
        lastScanTimestamp = now

        when (val result = onBarcodeScanned(trimmed)) {
            is BarcodeScanResult.AddedNew -> {
                activeProduct = result.product
                SoundHelper.playBarcodeBeep()
                triggerHapticFeedback()
                scanSuccess = true
                scanFeedbackMessage = "تمت قراءة: ${result.product.name} (الكمية: 1) ✅\nاستخدم الأزرار أدناه للزيادة أو النقصان باليد"
            }
            is BarcodeScanResult.AlreadyInCart -> {
                activeProduct = result.product
                SoundHelper.playBarcodeBeep()
                triggerHapticFeedback()
                scanSuccess = true
                scanFeedbackMessage = "المنتج مسجل بالسلة: ${result.product.name} (الكمية: ${result.currentQuantity})\nتم قفل التكرار التلقائي. عدّل الكمية باليد (+ / -)"
            }
            is BarcodeScanResult.NotFound -> {
                activeProduct = null
                SoundHelper.playWarningTone()
                scanSuccess = false
                scanFeedbackMessage = "تم مسح: ${result.barcode} (غير مسجل بالمخزن)"
            }
        }
    }

    // Laser Animation for scanner line
    val infiniteTransition = rememberInfiniteTransition(label = "laser_animation")
    val laserOffset by infiniteTransition.animateFloat(
        initialValue = 0.05f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_movement"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f)
                .testTag("real_camera_scanner_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF07140E)),
            border = BorderStroke(1.5.dp, BrandEmeraldPrimary)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(BrandEmeraldDeep),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = null,
                                tint = BrandEmeraldPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "ماسح الباركود بالكاميرا",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Text(
                                text = "وجّه الكاميرا إلى باركود السلعة للمسح الفوري",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondaryDark
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Flashlight Toggle
                        IconButton(
                            onClick = {
                                isFlashlightOn = !isFlashlightOn
                                cameraControl?.enableTorch(isFlashlightOn)
                            },
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(if (isFlashlightOn) BrandGold else BrandSurfaceCardDark)
                        ) {
                            Icon(
                                imageVector = if (isFlashlightOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                                contentDescription = "فلاش الكاميرا",
                                tint = if (isFlashlightOn) Color.Black else TextSecondaryDark,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Close Dialog
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(BrandSurfaceCardDark)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "إغلاق",
                                tint = TextSecondaryDark,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Camera Viewfinder Frame
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.Black)
                        .border(2.dp, BrandEmeraldPrimary.copy(alpha = 0.4f), RoundedCornerShape(18.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (hasCameraPermission) {
                        // Real CameraX Preview & ML Kit Analyzer
                        AndroidView(
                            factory = { ctx ->
                                val previewView = PreviewView(ctx).apply {
                                    scaleType = PreviewView.ScaleType.FILL_CENTER
                                }

                                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                                val executor = ContextCompat.getMainExecutor(ctx)
                                val analysisExecutor = Executors.newSingleThreadExecutor()

                                val barcodeScanner = BarcodeScanning.getClient(
                                    BarcodeScannerOptions.Builder()
                                        .setBarcodeFormats(
                                            Barcode.FORMAT_ALL_FORMATS
                                        )
                                        .build()
                                )

                                cameraProviderFuture.addListener({
                                    try {
                                        val cameraProvider = cameraProviderFuture.get()

                                        val preview = Preview.Builder().build().also {
                                            it.setSurfaceProvider(previewView.surfaceProvider)
                                        }

                                        val imageAnalysis = ImageAnalysis.Builder()
                                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                            .build()

                                        imageAnalysis.setAnalyzer(analysisExecutor) { imageProxy ->
                                            val mediaImage = imageProxy.image
                                            if (mediaImage != null) {
                                                val inputImage = InputImage.fromMediaImage(
                                                    mediaImage,
                                                    imageProxy.imageInfo.rotationDegrees
                                                )
                                                barcodeScanner.process(inputImage)
                                                    .addOnSuccessListener { barcodes ->
                                                        for (barcode in barcodes) {
                                                            val raw = barcode.rawValue
                                                            if (!raw.isNullOrBlank()) {
                                                                executor.execute {
                                                                    handleBarcodeDetection(raw)
                                                                }
                                                                break
                                                            }
                                                        }
                                                    }
                                                    .addOnCompleteListener {
                                                        imageProxy.close()
                                                    }
                                            } else {
                                                imageProxy.close()
                                            }
                                        }

                                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                                        cameraProvider.unbindAll()
                                        val camera = cameraProvider.bindToLifecycle(
                                            lifecycleOwner,
                                            cameraSelector,
                                            preview,
                                            imageAnalysis
                                        )
                                        cameraControl = camera.cameraControl
                                    } catch (e: Exception) {
                                        Log.e("CameraScanner", "Camera init failed", e)
                                    }
                                }, executor)

                                previewView
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        // Permission Request Placeholder
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(BrandEmeraldPrimary.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = null,
                                    tint = BrandEmeraldPrimary,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Text(
                                text = "يلزم إذن الكاميرا لمسح الباركود",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Text(
                                text = "يرجى منح إذن استخدام الكاميرا لتتمكن من توجيه الهاتف نحو باركود المنتجات وإضافتها تلقائياً.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondaryDark,
                                textAlign = TextAlign.Center
                            )
                            Button(
                                onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                                colors = ButtonDefaults.buttonColors(containerColor = BrandEmeraldPrimary),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.LockOpen, contentDescription = null, tint = Color.Black)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("منح إذن الكاميرا الآن", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Targeting Crosshair & Border
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .height(180.dp)
                            .border(
                                width = 2.dp,
                                brush = Brush.linearGradient(
                                    listOf(BrandEmeraldPrimary, BrandGold)
                                ),
                                shape = RoundedCornerShape(14.dp)
                            )
                    ) {
                        // Animated Scanning Laser Line
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .align(Alignment.TopCenter)
                                .offset(y = (180.dp * laserOffset))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            Color.Transparent,
                                            BrandEmeraldPrimary,
                                            ExpenseRose,
                                            BrandEmeraldPrimary,
                                            Color.Transparent
                                        )
                                    )
                                )
                        )
                    }
                }

                // Scan Result Alert Banner
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
                                imageVector = if (scanSuccess) Icons.Default.CheckCircle else Icons.Default.Warning,
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

                // Manual Quantity Stepper for Scanned Product (تعديل الكمية يدوياً باليد)
                val activeCartItem = activeProduct?.let { p -> cartItems.find { it.product.id == p.id } }
                if (activeProduct != null && activeCartItem != null && onUpdateQuantity != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("manual_quantity_stepper_card"),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF132F23)),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.5.dp, BrandEmeraldPrimary)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = BrandEmeraldPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = activeProduct!!.name,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White,
                                        maxLines = 1
                                    )
                                }
                                Text(
                                    text = "الوحدة: ${formatCurrency(activeProduct!!.sellingPrice, currency)} | المجموع: ${formatCurrency(activeProduct!!.sellingPrice * activeCartItem.quantity, currency)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = BrandGold
                                )
                            }

                            // Manual Stepper (+ / -) for hand manipulation
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(BrandEmeraldDeep)
                                    .padding(horizontal = 4.dp, vertical = 3.dp)
                            ) {
                                IconButton(
                                    onClick = {
                                        triggerHapticFeedback()
                                        onUpdateQuantity(activeProduct!!.id, activeCartItem.quantity - 1)
                                    },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(
                                        imageVector = if (activeCartItem.quantity == 1) Icons.Default.DeleteOutline else Icons.Default.Remove,
                                        contentDescription = "إنقاص باليد",
                                        tint = if (activeCartItem.quantity == 1) ExpenseRose else Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Text(
                                    text = "${activeCartItem.quantity}",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                                    color = BrandEmeraldPrimary,
                                    modifier = Modifier.padding(horizontal = 6.dp)
                                )

                                IconButton(
                                    onClick = {
                                        triggerHapticFeedback()
                                        onUpdateQuantity(activeProduct!!.id, activeCartItem.quantity + 1)
                                    },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "زيادة باليد",
                                        tint = BrandEmeraldPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Live Cart Counter inside Scanner (Continuous scanning helper)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF10271E))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = null,
                            tint = BrandGold,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "السلة الحالية: $cartCount سلع",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = formatCurrency(cartTotal, currency),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = BrandEmeraldPrimary
                            )
                        )

                        if (onOpenCostCalculator != null && cartCount > 0) {
                            FilledTonalButton(
                                onClick = onOpenCostCalculator,
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = BrandGold,
                                    contentColor = Color.Black
                                ),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Icon(Icons.Default.Calculate, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("حساب التكلفة", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Manual Barcode Input Fallback
                OutlinedTextField(
                    value = manualBarcode,
                    onValueChange = { manualBarcode = it },
                    label = { Text("أو أدخل رقم الباركود يدوياً", fontSize = 12.sp) },
                    placeholder = { Text("مثال: 6130001", fontSize = 12.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("manual_barcode_input"),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = {
                        if (manualBarcode.isNotBlank()) {
                            handleBarcodeDetection(manualBarcode)
                            manualBarcode = ""
                        }
                    }),
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                if (manualBarcode.isNotBlank()) {
                                    handleBarcodeDetection(manualBarcode)
                                    manualBarcode = ""
                                }
                            }
                        ) {
                            Icon(
                                Icons.Default.AddShoppingCart,
                                contentDescription = "إضافة",
                                tint = BrandEmeraldPrimary
                            )
                        }
                    },
                    shape = RoundedCornerShape(14.dp)
                )

                // Quick Barcode Chips from Existing Products
                val sampleBarcodes = remember(products) {
                    products.filter { it.barcode.isNotBlank() }.take(6)
                }
                if (sampleBarcodes.isNotEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(sampleBarcodes) { prod ->
                            AssistChip(
                                onClick = {
                                    handleBarcodeDetection(prod.barcode)
                                },
                                label = {
                                    Text("${prod.name} (${prod.barcode})", fontSize = 11.sp)
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.QrCode,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
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

                // Done & Cost Calculator Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (onOpenCostCalculator != null) {
                        OutlinedButton(
                            onClick = onOpenCostCalculator,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("scanner_open_cost_calc_button"),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = BrandGold),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, BrandGold)
                        ) {
                            Icon(Icons.Default.Calculate, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("حساب التكلفة ($cartCount)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(if (onOpenCostCalculator != null) 1.2f else 1f)
                            .testTag("close_camera_scanner_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandEmeraldPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            "الانتهاء والفاتورة",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}
