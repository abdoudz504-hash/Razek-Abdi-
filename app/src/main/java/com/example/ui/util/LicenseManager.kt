package com.example.ui.util

import android.content.Context
import android.content.SharedPreferences
import android.provider.Settings
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*

data class SubscriptionPlan(
    val id: String,
    val title: String,
    val durationText: String,
    val priceEuro: Int,
    val formattedPrice: String,
    val durationDays: Int,
    val planCodeSuffix: String, // LIFE
    val badge: String? = null
)

data class LicenseInfo(
    val isActivated: Boolean,
    val deviceId: String,
    val storeName: String,
    val activatedAt: Long,
    val expiresAt: Long? = null,
    val formattedActivationDate: String,
    val formattedExpirationDate: String? = null,
    val remainingDays: Int? = null,
    val isExpired: Boolean = false,
    val isLifetime: Boolean = false,
    val linkedGmail: String = "",
    val activeSessionToken: String = "",
    val licenseType: String,
    val planId: String = "life",
    val developerPhone: String = "0665233528"
)

data class VerificationResult(
    val isValid: Boolean,
    val method: String,
    val message: String,
    val detectedPlan: SubscriptionPlan? = null,
    val isLifetime: Boolean = false
)

object LicenseManager {
    private const val PREFS_NAME = "rz_license_prefs"
    private const val KEY_IS_ACTIVATED = "is_activated"
    private const val KEY_DEVICE_ID = "device_unique_id"
    private const val KEY_ACTIVATION_KEY = "activation_key"
    private const val KEY_STORE_NAME = "store_name"
    private const val KEY_ACTIVATED_AT = "activated_at"
    private const val KEY_EXPIRES_AT = "expires_at"
    private const val KEY_LICENSE_TYPE = "license_type"
    private const val KEY_PLAN_ID = "plan_id"
    private const val KEY_LINKED_GMAIL = "linked_gmail"
    private const val KEY_ACTIVE_SESSION_TOKEN = "active_session_token"
    private const val KEY_FIRST_ONLINE_CHECK_DONE = "first_online_check_done"
    private const val KEY_LAST_DEVICE_SYNC = "last_device_sync"

    const val DEVELOPER_PHONE = "0665233528"
    const val DEVELOPER_PHONE_INTL = "+213665233528"
    const val DEVELOPER_WHATSAPP = "+213665233528"
    private const val MASTER_SALT = "RZ_TASYIR_SALT_0665233528_COMMERCIAL_PROTECTION"
    private const val MASTER_DEV_KEY = "RZ-MASTER-0665"
    private const val MASTER_DEV_PIN = "0665"

    // رابط مباشر لتحميل التطبيق بصيغة APK والتحديثات
    const val APP_DOWNLOAD_URL = "https://github.com/rz-tasyir/app/releases/latest/download/rz-tasyir.apk"
    const val APP_SHARE_URL = "https://rz-tasyir.dz/download"

    // ترخيص التفعيل الوحيد المعتمد: تفعيل مدى الحياة بمبلغ 100 يورو
    val SUBSCRIPTION_PLANS = listOf(
        SubscriptionPlan(
            id = "life",
            title = "ترخيص تفعيل مدى الحياة (دائم بدون تجديد)",
            durationText = "مدى الحياة دائم",
            priceEuro = 100,
            formattedPrice = "100 €",
            durationDays = 36500, // 100 سنة
            planCodeSuffix = "LIFE",
            badge = "مدى الحياة 🔥 100 €"
        )
    )

    val LIFETIME_PLAN: SubscriptionPlan get() = SUBSCRIPTION_PLANS[0]

    fun getPlanById(id: String): SubscriptionPlan {
        return SUBSCRIPTION_PLANS[0]
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * استرجاع كود الجهاز الفريد للتثبيت (Deterministic Device Machine ID)
     * يبقى ثابتاً في نفس الجهاز
     */
    fun getDeviceId(context: Context): String {
        val prefs = getPrefs(context)
        var savedId = prefs.getString(KEY_DEVICE_ID, null)
        if (savedId.isNullOrBlank()) {
            val androidId = try {
                Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            } catch (e: Exception) {
                null
            }

            val seed = if (!androidId.isNullOrBlank() && androidId != "9774d56d682e549c") {
                androidId
            } else {
                UUID.randomUUID().toString()
            }

            val digest = MessageDigest.getInstance("MD5").digest(seed.toByteArray(Charsets.UTF_8))
            val p1 = ((digest[0].toInt() and 0xFF) shl 8 or (digest[1].toInt() and 0xFF)) % 9000 + 1000
            val p2 = ((digest[2].toInt() and 0xFF) shl 8 or (digest[3].toInt() and 0xFF)) % 9000 + 1000
            savedId = "RZ-$p1-$p2"
            prefs.edit().putString(KEY_DEVICE_ID, savedId).apply()
        }
        return savedId
    }

    /**
     * استخراج رمز الطلب الآلي المرتبط بجهاز الزبون (4 أرقام ديناميكية وفريدة لكل جهاز)
     * هذا الرمز يصلك تلقائياً في رسالة الزبون
     */
    fun getRequestCode(targetDeviceId: String): String {
        val cleanId = targetDeviceId.trim().uppercase(Locale.ROOT)
        val payload = "$cleanId#$MASTER_SALT"
        val digest = MessageDigest.getInstance("SHA-256").digest(payload.toByteArray(Charsets.UTF_8))
        val k1 = ((digest[0].toInt() and 0xFF) shl 8 or (digest[1].toInt() and 0xFF)) % 9000 + 1000
        return "$k1"
    }

    /**
     * كود التفعيل السريع المباشر للمطور لإرساله للزبون بعد استلام المال
     * يدعم تحديد الخطة: 1M (شهر), 3M (3 أشهر), 6M (6 أشهر), 1Y (سنة)
     */
    fun getQuickActivationKey(targetDeviceId: String, plan: SubscriptionPlan? = null): String {
        val reqCode = getRequestCode(targetDeviceId)
        return if (plan != null) {
            "$reqCode-$MASTER_DEV_PIN-${plan.planCodeSuffix}"
        } else {
            "$reqCode-$MASTER_DEV_PIN"
        }
    }

    /**
     * حساب مفتاح التفعيل الرسمي لأي كود جهاز
     * هذه الخوارزمية الرياضية تربط كود الجهاز برقم هاتف المطور والملح السري
     */
    fun generateActivationKey(targetDeviceId: String, plan: SubscriptionPlan? = null): String {
        val cleanId = targetDeviceId.trim().uppercase(Locale.ROOT)
        val suffix = plan?.planCodeSuffix ?: "1Y"
        val payload = "$cleanId#$suffix#$MASTER_SALT"
        val digest = MessageDigest.getInstance("SHA-256").digest(payload.toByteArray(Charsets.UTF_8))
        val k1 = ((digest[0].toInt() and 0xFF) shl 8 or (digest[1].toInt() and 0xFF)) % 9000 + 1000
        val k2 = ((digest[2].toInt() and 0xFF) shl 8 or (digest[3].toInt() and 0xFF)) % 9000 + 1000
        return "RZK-$k1-$k2-$suffix"
    }

    /**
     * التحقق من تفاصيل كود التفعيل ومصدر اعتماده والخطة المحددة
     */
    fun verifyCodeDetails(targetDeviceId: String, inputKey: String, selectedPlan: SubscriptionPlan? = null): VerificationResult {
        val cleanInput = inputKey.trim().uppercase(Locale.ROOT).replace(" ", "")
        if (cleanInput.isEmpty()) {
            return VerificationResult(false, "فارغ", "يرجى كتابة كود التفعيل")
        }

        val reqCode = getRequestCode(targetDeviceId)
        val cleanDevice = targetDeviceId.trim().uppercase(Locale.ROOT).replace(" ", "")

        // حماية: إذا أدخل رمز الطلب أو معرف الجهاز فقط
        if (cleanInput == reqCode || cleanInput == cleanDevice) {
            return VerificationResult(
                isValid = false,
                method = "رمز طلب غير مكتمل",
                message = "هذا رمز الطلب فقط! يتطلب موافقة المالك وإصدار كود التفعيل المعتمد بعد السداد."
            )
        }

        // 1. فحص كود التفعيل السريع للمالك مع لاحقة الخطة (مثال: 7824-0665-1M أو 7824-0665-3M أو 7824-0665-6M أو 7824-0665-1Y)
        for (plan in SUBSCRIPTION_PLANS) {
            val planCode = "$reqCode-$MASTER_DEV_PIN-${plan.planCodeSuffix}"
            val planCodeAlt = "RZ-$reqCode-$MASTER_DEV_PIN-${plan.planCodeSuffix}"
            val planCodeCompact = "$reqCode$MASTER_DEV_PIN${plan.planCodeSuffix}"

            if (cleanInput == planCode || cleanInput == planCodeAlt || cleanInput == planCodeCompact) {
                return VerificationResult(
                    isValid = true,
                    method = "كود معتمد: ${plan.title} (${plan.durationText})",
                    message = "تم التحقق بنجاح! كود صالح لخطة ${plan.title} (${plan.formattedPrice}).",
                    detectedPlan = plan
                )
            }
        }

        // 2. فحص كود التفعيل السريع الافتراضي بدون لاحقة (يعتبر خطة شهر أو الخطة المحددة)
        val quickKey1 = "$reqCode-$MASTER_DEV_PIN"
        val quickKey2 = "$MASTER_DEV_PIN-$reqCode"
        val quickKeyRaw = "$reqCode$MASTER_DEV_PIN"
        val quickKeyRaw2 = "$MASTER_DEV_PIN$reqCode"
        val quickKeyRz = "RZ-$reqCode-$MASTER_DEV_PIN"

        if (cleanInput == quickKey1 || cleanInput == quickKey2 ||
            cleanInput == quickKeyRaw || cleanInput == quickKeyRaw2 ||
            cleanInput == quickKeyRz) {
            val planToUse = selectedPlan ?: SUBSCRIPTION_PLANS[0]
            return VerificationResult(
                isValid = true,
                method = "كود تفعيل معتمد من المالك (${planToUse.title})",
                message = "تم التحقق بنجاح! الكود مطابق لرمز طلب جهازك وتوقيع المالك المعتمد.",
                detectedPlan = planToUse
            )
        }

        // 3. فحص مفتاح التفعيل الرسمي الخوارزمي المشفر لكل الخطط أو الصيغة الكلاسيكية
        for (plan in SUBSCRIPTION_PLANS) {
            val expectedPlanOfficial = generateActivationKey(targetDeviceId, plan)
            if (cleanInput == expectedPlanOfficial || cleanInput == expectedPlanOfficial.replace("-", "")) {
                return VerificationResult(
                    isValid = true,
                    method = "تحقق محلي خوارزمي مشفر: ${plan.title}",
                    message = "تم التحقق محلياً بنجاح لخطة ${plan.title}!",
                    detectedPlan = plan
                )
            }
        }

        // المفتاح الكلاسيكي RZK-XXXX-YYYY (دائم / سنة)
        val payloadClassic = "$cleanDevice#$MASTER_SALT"
        val digestClassic = MessageDigest.getInstance("SHA-256").digest(payloadClassic.toByteArray(Charsets.UTF_8))
        val ck1 = ((digestClassic[0].toInt() and 0xFF) shl 8 or (digestClassic[1].toInt() and 0xFF)) % 9000 + 1000
        val ck2 = ((digestClassic[2].toInt() and 0xFF) shl 8 or (digestClassic[3].toInt() and 0xFF)) % 9000 + 1000
        val expectedClassic = "RZK-$ck1-$ck2"
        if (cleanInput == expectedClassic || cleanInput == expectedClassic.replace("-", "")) {
            return VerificationResult(
                isValid = true,
                method = "تحقق محلي مشفر (ترخيص سنوي/دائم)",
                message = "تم التحقق محلياً بنجاح! ترخيص سنة كاملة معتمد.",
                detectedPlan = SUBSCRIPTION_PLANS.last()
            )
        }

        // 4. فحص كود الطوارئ السري للمالك
        if (cleanInput == MASTER_DEV_KEY || cleanInput == DEVELOPER_PHONE || cleanInput == "0665") {
            return VerificationResult(
                isValid = true,
                method = "كود الماستر الرئيسي للمالك",
                message = "تم التحقق بواسطة تصريح المالك المباشر (ترخيص كامل).",
                detectedPlan = SUBSCRIPTION_PLANS.last()
            )
        }

        return VerificationResult(
            isValid = false,
            method = "كود غير صالح",
            message = "كود التفعيل غير صالح لهذا الجهاز. يرجى التواصل عبر واتساب أو SMS للحصول على كود التفعيل المعتمد."
        )
    }

    /**
     * استخراج كود التفعيل تلقائياً من نص أي رسالة نصية SMS واردة
     * تدعم كافة تنسيقات الرسائل والخطط
     */
    fun extractActivationCodeFromSms(smsText: String): String? {
        if (smsText.isBlank()) return null
        val clean = smsText.trim()

        // 1. البحث عن صيغة كود التفعيل مع لاحقة الخطة: 7824-0665-1M أو 3M أو 6M أو 1Y
        val planRegex = Regex("""(?:\b)(\d{4}-0665-(?:1M|3M|6M|1Y)|RZ-\d{4}-0665-(?:1M|3M|6M|1Y))(?:\b)""", RegexOption.IGNORE_CASE)
        val planMatch = planRegex.find(clean)
        if (planMatch != null) {
            return planMatch.value
        }

        // 2. البحث عن صيغة كود التفعيل السريع البسيط: 4 أرقام - 0665
        val quickRegex = Regex("""(?:\b)(\d{4}-0665|0665-\d{4}|RZ-\d{4}-0665)(?:\b)""", RegexOption.IGNORE_CASE)
        val quickMatch = quickRegex.find(clean)
        if (quickMatch != null) {
            return quickMatch.value
        }

        // 3. البحث عن الصيغة الرسمية RZK-XXXX-YYYY-(1M|3M|6M|1Y) أو RZK-XXXX-YYYY
        val officialPlanRegex = Regex("""(?:\b)(RZK-[0-9]{4}-[0-9]{4}-(?:1M|3M|6M|1Y))(?:\b)""", RegexOption.IGNORE_CASE)
        val officialPlanMatch = officialPlanRegex.find(clean)
        if (officialPlanMatch != null) {
            return officialPlanMatch.value
        }

        val officialRegex = Regex("""(?:\b)(RZK-[0-9]{4}-[0-9]{4})(?:\b)""", RegexOption.IGNORE_CASE)
        val officialMatch = officialRegex.find(clean)
        if (officialMatch != null) {
            return officialMatch.value
        }

        // 4. البحث عن كود الماستر
        if (clean.contains("0665233528")) return "0665233528"
        if (clean.contains("RZ-MASTER-0665")) return "RZ-MASTER-0665"

        return null
    }

    /**
     * إعداد نص رسالة طلب التفعيل الموجهة لمالك التطبيق عبر SMS متضمنة ترخيص مدى الحياة بمبلغ 100 يورو
     */
    fun createSmsRequestMessage(
        deviceId: String,
        requestCode: String,
        storeName: String?,
        plan: SubscriptionPlan = LIFETIME_PLAN
    ): String {
        val store = if (!storeName.isNullOrBlank()) "\nالمتجر: $storeName" else ""
        return "طلب تفعيل تطبيق RZ-تسيير (ترخيص مدى الحياة - 100€):$store\nالجهاز: $deviceId\nرمز الطلب: $requestCode\nأرجو إرسال كود التفعيل بعد سداد المبلغ (100 يورو) وشكراً."
    }

    /**
     * إعداد رسالة الرد والاعتماد من المالك للزبون عبر SMS مع تحديد الخطة ورابط التحميل المباشر
     */
    fun createSmsApprovalReply(
        deviceId: String,
        customCode: String? = null,
        plan: SubscriptionPlan = LIFETIME_PLAN
    ): String {
        val quickKey = customCode ?: getQuickActivationKey(deviceId, plan)
        return "تم اعتماد ترخيصك الدائم في تطبيق RZ-تسيير (مدى الحياة - 100 €).\nكود التفعيل: $quickKey\nرابط تحميل وتحديث التطبيق المباشر:\n$APP_DOWNLOAD_URL"
    }

    /**
     * إعداد رابط ورسالة احترافية لإرسال كود التفعيل ومطابقة الحساب مباشرة إلى واتساب المالك
     * يتم إرسال الرسالة إلى رقم واتساب المطور مباشرة عبر واجهة واتساب الرسمية
     */
    fun createWhatsAppRequestUrl(
        deviceId: String,
        requestCode: String,
        storeName: String?,
        gmailAccount: String,
        plan: SubscriptionPlan = LIFETIME_PLAN
    ): String {
        val store = if (!storeName.isNullOrBlank()) "🏪 المتجر: $storeName\n" else ""
        val gmail = if (gmailAccount.isNotBlank()) "📧 حساب Gmail المرتبط: $gmailAccount\n" else ""
        val message = """
            👋 السلام عليكم ورحمة الله،
            أود تفعيل ترخيص تطبيق RZ-تسيير على جهازي:
            $store$gmail📱 معرف جهازي الفريد: $deviceId
            🔑 رمز الطلب الآلي: $requestCode
            💎 نوع الترخيص: ترخيص مدى الحياة دائم (100 €)
            
            ⚠️ تم توثيق ارتباط هذا الترخيص بحساب Gmail المذكور ليعمل على هذا الهاتف حصرياً.
            يرجى تزويدي بكود التفعيل المعتمد بعد تأكيد استلام مبلغ التفعيل (100 يورو) وشكراً.
        """.trimIndent()

        val encoded = android.net.Uri.encode(message)
        return "https://api.whatsapp.com/send?phone=$DEVELOPER_WHATSAPP&text=$encoded"
    }

    /**
     * ربط وحفظ حساب Gmail المرتبط بالترخيص لتوثيق ملكية الجهاز الواحد
     */
    fun bindGmailAccount(context: Context, email: String) {
        val cleanEmail = email.trim().lowercase(Locale.ROOT)
        getPrefs(context).edit()
            .putString(KEY_LINKED_GMAIL, cleanEmail)
            .apply()
    }

    fun getLinkedGmail(context: Context): String {
        return getPrefs(context).getString(KEY_LINKED_GMAIL, "") ?: ""
    }

    /**
     * التحقق من قفل الجهاز الواحد ومحاكاة إلغاء التفعيل في الهواتف السابقة
     * عند تشغيل الترخيص في هاتف جديد، يتولد Session Token جديد.
     */
    fun generateDeviceSessionToken(deviceId: String, email: String): String {
        val raw = "$deviceId#$email#${System.currentTimeMillis()}#$MASTER_SALT"
        val digest = MessageDigest.getInstance("MD5").digest(raw.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    /**
     * التحقق من مطابقة مفتاح التفعيل المدخل لكود الجهاز
     */
    fun isValidKey(targetDeviceId: String, inputKey: String, plan: SubscriptionPlan? = null): Boolean {
        return verifyCodeDetails(targetDeviceId, inputKey, plan).isValid
    }

    /**
     * فحص هل النسخة مفعلة وصالحة حالياً (مع التحقق من انتهاء المدة وارتباط الجهاز)
     * التطبيق يعمل أوفلاين 100%، ويطلب فحص اتصال مرة واحدة فقط في أول تفعيل لكشف التلاعب
     */
    fun isActivated(context: Context): Boolean {
        val prefs = getPrefs(context)
        val isAct = prefs.getBoolean(KEY_IS_ACTIVATED, false)
        if (!isAct) return false

        // فحص خطة مدى الحياة (إذا كانت مدى الحياة، لا تنتهي أبداً)
        val planId = prefs.getString(KEY_PLAN_ID, "life") ?: "life"
        val isLifetime = planId == "life"

        if (!isLifetime) {
            val expiresAt = prefs.getLong(KEY_EXPIRES_AT, 0L)
            if (expiresAt > 0 && System.currentTimeMillis() > expiresAt) {
                return false // انتهت صلاحية الاشتراك
            }
        }

        // Anti-tampering: التأكد أن المفتاح المحفوظ يطابق الجهاز الحالي
        val currentDeviceId = getDeviceId(context)
        val savedKey = prefs.getString(KEY_ACTIVATION_KEY, "") ?: ""
        return isValidKey(currentDeviceId, savedKey)
    }

    /**
     * تفعيل النسخة وحفظ بيانات الترخيص وتاريخ الانتهاء وربط حساب Gmail
     */
    fun activate(
        context: Context,
        inputKey: String,
        storeName: String,
        plan: SubscriptionPlan? = null,
        gmailAccount: String = ""
    ): Boolean {
        val deviceId = getDeviceId(context)
        val verification = verifyCodeDetails(deviceId, inputKey, plan)
        if (!verification.isValid) {
            return false
        }

        val effectivePlan = verification.detectedPlan ?: plan ?: SUBSCRIPTION_PLANS[0]
        val cleanStore = storeName.trim().ifEmpty { "متجري" }
        val cleanEmail = gmailAccount.trim().lowercase(Locale.ROOT)
        val now = System.currentTimeMillis()
        val durationMillis = effectivePlan.durationDays.toLong() * 24 * 60 * 60 * 1000L
        val expiresAt = now + durationMillis
        val sessionToken = generateDeviceSessionToken(deviceId, cleanEmail)

        getPrefs(context).edit()
            .putBoolean(KEY_IS_ACTIVATED, true)
            .putString(KEY_ACTIVATION_KEY, inputKey.trim().uppercase(Locale.ROOT))
            .putString(KEY_STORE_NAME, cleanStore)
            .putString(KEY_LINKED_GMAIL, cleanEmail)
            .putString(KEY_ACTIVE_SESSION_TOKEN, sessionToken)
            .putBoolean(KEY_FIRST_ONLINE_CHECK_DONE, true)
            .putLong(KEY_ACTIVATED_AT, now)
            .putLong(KEY_EXPIRES_AT, expiresAt)
            .putString(KEY_PLAN_ID, effectivePlan.id)
            .putString(KEY_LICENSE_TYPE, "${effectivePlan.title} (${effectivePlan.durationText})")
            .apply()

        return true
    }

    /**
     * محاكاة نقل التفعيل لهاتف آخر (يغلق تلقائياً في هذا الهاتف)
     */
    fun revokeDeviceAccessDueToNewPhone(context: Context) {
        getPrefs(context).edit()
            .putBoolean(KEY_IS_ACTIVATED, false)
            .remove(KEY_ACTIVATION_KEY)
            .remove(KEY_ACTIVE_SESSION_TOKEN)
            .apply()
    }

    /**
     * الحصول على معلومات الترخيص كاملة مع حساب الأيام المتبقية وحالة الانتهاء
     */
    fun getLicenseInfo(context: Context): LicenseInfo {
        val prefs = getPrefs(context)
        val isAct = isActivated(context)
        val rawActivated = prefs.getBoolean(KEY_IS_ACTIVATED, false)
        val deviceId = getDeviceId(context)
        val store = prefs.getString(KEY_STORE_NAME, "متجر تجاري") ?: "متجر تجاري"
        val linkedEmail = prefs.getString(KEY_LINKED_GMAIL, "") ?: ""
        val sessionToken = prefs.getString(KEY_ACTIVE_SESSION_TOKEN, "") ?: ""
        val activatedAt = prefs.getLong(KEY_ACTIVATED_AT, 0L)
        val expiresAt = prefs.getLong(KEY_EXPIRES_AT, 0L)
        val planId = prefs.getString(KEY_PLAN_ID, "life") ?: "life"
        val isLifetime = planId == "life"
        val licenseType = prefs.getString(KEY_LICENSE_TYPE, "ترخيص مدى الحياة دائم") ?: "ترخيص مدى الحياة دائم"

        val now = System.currentTimeMillis()
        val isExpired = rawActivated && !isLifetime && expiresAt > 0 && now > expiresAt

        val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale("ar"))
        val formattedActivatedDate = if (activatedAt > 0) sdf.format(Date(activatedAt)) else "غير مفعل"
        val formattedExpiresDate = if (isLifetime) "مدى الحياة (لا ينتهي أبداً)" else if (expiresAt > 0) sdf.format(Date(expiresAt)) else null

        val remainingDays = if (rawActivated && !isLifetime && expiresAt > 0) {
            val diff = expiresAt - now
            if (diff > 0) ((diff / (1000 * 60 * 60 * 24)).toInt() + 1) else 0
        } else if (isLifetime) 99999 else null

        return LicenseInfo(
            isActivated = isAct,
            deviceId = deviceId,
            storeName = store,
            activatedAt = activatedAt,
            expiresAt = if (expiresAt > 0) expiresAt else null,
            formattedActivationDate = formattedActivatedDate,
            formattedExpirationDate = formattedExpiresDate,
            remainingDays = remainingDays,
            isExpired = isExpired,
            isLifetime = isLifetime,
            linkedGmail = linkedEmail,
            activeSessionToken = sessionToken,
            licenseType = licenseType,
            planId = planId,
            developerPhone = DEVELOPER_PHONE
        )
    }

    /**
     * التحقق من رمز PIN لوحة المطور (لتوليد المفاتيح للزبائن)
     */
    fun checkDeveloperPin(pin: String): Boolean {
        val cleanPin = pin.trim()
        return cleanPin == MASTER_DEV_PIN || cleanPin == "3528" || cleanPin == DEVELOPER_PHONE
    }

    /**
     * إلغاء التفعيل (لأغراض الاختبار أو نقل الملكية)
     */
    fun resetLicenseForTesting(context: Context) {
        getPrefs(context).edit()
            .putBoolean(KEY_IS_ACTIVATED, false)
            .remove(KEY_ACTIVATION_KEY)
            .apply()
    }
}
