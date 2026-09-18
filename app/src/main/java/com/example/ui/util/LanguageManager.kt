package com.example.ui.util

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.LayoutDirection

enum class AppLanguage(
    val code: String,
    val nativeName: String,
    val englishName: String,
    val flagEmoji: String,
    val isRtl: Boolean = false
) {
    ARABIC("ar", "العربية", "Arabic", "🇩🇿", isRtl = true),
    FRENCH("fr", "Français", "French", "🇫🇷"),
    ENGLISH("en", "English", "English", "🇺🇸"),
    SPANISH("es", "Español", "Spanish", "🇪🇸"),
    TURKISH("tr", "Türkçe", "Turkish", "🇹🇷"),
    GERMAN("de", "Deutsch", "German", "🇩🇪"),
    ITALIAN("it", "Italiano", "Italian", "🇮🇹"),
    RUSSIAN("ru", "Русский", "Russian", "🇷🇺"),
    CHINESE("zh", "中文", "Chinese", "🇨🇳"),
    HINDI("hi", "हिन्दी", "Hindi", "🇮🇳"),
    PORTUGUESE("pt", "Português", "Portuguese", "🇵🇹"),
    INDONESIAN("id", "Bahasa Indonesia", "Indonesian", "🇮🇩"),
    PERSIAN("fa", "فارسی", "Persian", "🇮🇷", isRtl = true),
    URDU("ur", "اردو", "Urdu", "🇵🇰", isRtl = true)
}

object LanguageManager {
    private const val PREFS_NAME = "rz_language_prefs"
    private const val KEY_LANG = "selected_language"

    var currentLanguage by mutableStateOf(AppLanguage.ARABIC)
        private set

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getString(KEY_LANG, AppLanguage.ARABIC.code) ?: AppLanguage.ARABIC.code
        currentLanguage = AppLanguage.values().firstOrNull { it.code == saved } ?: AppLanguage.ARABIC
    }

    fun setLanguage(context: Context, lang: AppLanguage) {
        currentLanguage = lang
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LANG, lang.code).apply()
    }

    // Translations Dictionary for all major UI keys across languages
    private val translations = mapOf(
        "app_name" to mapOf(
            "ar" to "RZ-تسيير",
            "fr" to "RZ-Gestion",
            "en" to "RZ-Management",
            "es" to "RZ-Gestión",
            "tr" to "RZ-Yönetim",
            "de" to "RZ-Verwaltung",
            "it" to "RZ-Gestione",
            "ru" to "RZ-Управление",
            "zh" to "RZ-管理系统",
            "hi" to "RZ-प्रबंधन",
            "pt" to "RZ-Gestão",
            "id" to "RZ-Manajemen",
            "fa" to "RZ-مدیریت",
            "ur" to "RZ-انتظام"
        ),
        "home" to mapOf(
            "ar" to "الرئيسية",
            "fr" to "Accueil",
            "en" to "Home",
            "es" to "Inicio",
            "tr" to "Ana Sayfa",
            "de" to "Startseite",
            "it" to "Home",
            "ru" to "Главная",
            "zh" to "首页",
            "hi" to "मुख्य पृष्ठ",
            "pt" to "Início",
            "id" to "Beranda",
            "fa" to "خانه",
            "ur" to "ہوم"
        ),
        "pos" to mapOf(
            "ar" to "نقطة البيع",
            "fr" to "Point de Vente",
            "en" to "POS",
            "es" to "Punto de Venta",
            "tr" to "Satış Noktası",
            "de" to "Kasse",
            "it" to "Punto Vendita",
            "ru" to "Касса",
            "zh" to "收银台",
            "hi" to "बिक्री केंद्र",
            "pt" to "Ponto de Venda",
            "id" to "Kasir",
            "fa" to "صندوق فروش",
            "ur" to "پوائنٹ آف سیل"
        ),
        "inventory" to mapOf(
            "ar" to "المخزون",
            "fr" to "Stock",
            "en" to "Inventory",
            "es" to "Inventario",
            "tr" to "Stok",
            "de" to "Inventar",
            "it" to "Magazzino",
            "ru" to "Склад",
            "zh" to "库存",
            "hi" to "इन्वेंट्री",
            "pt" to "Estoque",
            "id" to "Inventaris",
            "fa" to "موجودی کالا",
            "ur" to "اسٹاک"
        ),
        "customers" to mapOf(
            "ar" to "العملاء",
            "fr" to "Clients",
            "en" to "Customers",
            "es" to "Clientes",
            "tr" to "Müşteriler",
            "de" to "Kunden",
            "it" to "Clienti",
            "ru" to "Клиенты",
            "zh" to "客户",
            "hi" to "ग्राहक",
            "pt" to "Clientes",
            "id" to "Pelanggan",
            "fa" to "مشتریان",
            "ur" to "گاہک"
        ),
        "expenses" to mapOf(
            "ar" to "المصاريف",
            "fr" to "Dépenses",
            "en" to "Expenses",
            "es" to "Gastos",
            "tr" to "Giderler",
            "de" to "Ausgaben",
            "it" to "Spese",
            "ru" to "Расходы",
            "zh" to "支出",
            "hi" to "खर्चे",
            "pt" to "Despesas",
            "id" to "Pengeluaran",
            "fa" to "هزینه‌ها",
            "ur" to "اخراجات"
        ),
        "reports" to mapOf(
            "ar" to "التقارير",
            "fr" to "Rapports",
            "en" to "Reports",
            "es" to "Informes",
            "tr" to "Raporlar",
            "de" to "Berichte",
            "it" to "Rapporti",
            "ru" to "Отчеты",
            "zh" to "报表",
            "hi" to "रिपोर्ट्स",
            "pt" to "Relatórios",
            "id" to "Laporan",
            "fa" to "گزارش‌ها",
            "ur" to "رپورٹس"
        ),
        "activation_title" to mapOf(
            "ar" to "شاشة تفعيل التطبيق",
            "fr" to "Activation de l'application",
            "en" to "App Activation",
            "es" to "Activación de la aplicación",
            "tr" to "Uygulama Etkinleştirme",
            "de" to "App-Aktivierung",
            "it" to "Attivazione App",
            "ru" to "Активация приложения",
            "zh" to "应用激活",
            "hi" to "ऐप सक्रियण",
            "pt" to "Ativação do Aplicativo",
            "id" to "Aktivasi Aplikasi",
            "fa" to "فعال‌سازی برنامه",
            "ur" to "ایپ ایکٹیویشن"
        ),
        "activation_code" to mapOf(
            "ar" to "كود التفعيل",
            "fr" to "Code d'activation",
            "en" to "Activation Code",
            "es" to "Código de activación",
            "tr" to "Etkinleştirme Kodu",
            "de" to "Aktivierungscode",
            "it" to "Codice di attivazione",
            "ru" to "Код активации",
            "zh" to "激活码",
            "hi" to "सक्रियण कोड",
            "pt" to "Código de Ativação",
            "id" to "Kode Aktivasi",
            "fa" to "کد فعال‌سازی",
            "ur" to "ایکٹیویشن کوڈ"
        ),
        "device_id" to mapOf(
            "ar" to "معرف الجهاز الفريد",
            "fr" to "Identifiant unique de l'appareil",
            "en" to "Unique Device ID",
            "es" to "ID único del dispositivo",
            "tr" to "Benzersiz Cihaz Kimliği",
            "de" to "Eindeutige Geräte-ID",
            "it" to "ID Dispositivo Univoco",
            "ru" to "Уникальный ID устройства",
            "zh" to "唯一设备ID",
            "hi" to "अद्वितीय डिवाइस आईडी",
            "pt" to "ID Único do Dispositivo",
            "id" to "ID Perangkat Unik",
            "fa" to "شناسه یکتای دستگاه",
            "ur" to "منفرد ڈیوائس آئی ڈی"
        ),
        "google_account" to mapOf(
            "ar" to "حساب Gmail المرتبط",
            "fr" to "Compte Gmail associé",
            "en" to "Linked Gmail Account",
            "es" to "Cuenta de Gmail vinculada",
            "tr" to "Bağlı Gmail Hesabı",
            "de" to "Verknüpftes Gmail-Konto",
            "it" to "Account Gmail Collegato",
            "ru" to "Связанный аккаунт Gmail",
            "zh" to "关联的Gmail账号",
            "hi" to "लिंक किया गया जीमेल खाता",
            "pt" to "Conta do Gmail Vinculada",
            "id" to "Akun Gmail Terhubung",
            "fa" to "حساب جیمیل متصل",
            "ur" to "منسلک جی میل اکاؤنٹ"
        ),
        "offline_ready" to mapOf(
            "ar" to "يعمل 100% بدون إنترنت",
            "fr" to "Fonctionne 100% hors-ligne",
            "en" to "Works 100% Offline",
            "es" to "Funciona 100% sin internet",
            "tr" to "%100 Çevrimdışı Çalışır",
            "de" to "Funktioniert 100% offline",
            "it" to "Funziona al 100% offline",
            "ru" to "Работает 100% без интернета",
            "zh" to "100%离线可用",
            "hi" to "100% ऑफ़लाइन कार्य करता है",
            "pt" to "Funciona 100% Offline",
            "id" to "Bekerja 100% Tanpa Internet",
            "fa" to "۱۰۰٪ آفلاین کار می‌کند",
            "ur" to "100% انٹرنیٹ کے بغیر کام کرتا ہے"
        ),
        "single_device_notice" to mapOf(
            "ar" to "حماية الجهاز الواحد: يعمل على هاتف واحد فقط وعند تغيير الهاتف يغلق تلقائياً في القديم",
            "fr" to "Protection mono-appareil : fonctionne sur un seul téléphone à la fois",
            "en" to "Single-device protection: Works on one phone only, auto-revokes on the old device",
            "es" to "Protección de dispositivo único: funciona en un solo teléfono a la vez",
            "tr" to "Tek cihaz koruması: Aynı anda yalnızca tek telefonda çalışır",
            "de" to "Einzelgeräte-Schutz: Funktioniert nur auf einem Gerät gleichzeitig",
            "it" to "Protezione singolo dispositivo: funziona su un solo telefono alla volta",
            "ru" to "Защита одного устройства: работает только на одном телефоне",
            "zh" to "单设备保护：同一时间仅在一台手机上运行",
            "hi" to "एकल डिवाइस सुरक्षा: एक समय में केवल एक फोन पर चलता है",
            "pt" to "Proteção de dispositivo único: funciona em apenas um telefone por vez",
            "id" to "Proteksi perangkat tunggal: Hanya berfungsi di satu HP dalam satu waktu",
            "fa" to "محافظت تک‌دستگاه: تنها روی یک گوشی در لحظه کار می‌کند",
            "ur" to "سنگل ڈیوائس تحفظ: ایک وقت میں صرف ایک فون پر چلتا ہے"
        ),
        "weather" to mapOf(
            "ar" to "الطقس",
            "fr" to "Météo",
            "en" to "Weather",
            "es" to "Clima",
            "tr" to "Hava Durumu",
            "de" to "Wetter",
            "it" to "Meteo",
            "ru" to "Погода",
            "zh" to "天气",
            "hi" to "मौसम",
            "pt" to "Clima",
            "id" to "Cuaca",
            "fa" to "آب و هوا",
            "ur" to "موسم"
        ),
        "languages" to mapOf(
            "ar" to "اللغات",
            "fr" to "Langues",
            "en" to "Languages",
            "es" to "Idiomas",
            "tr" to "Diller",
            "de" to "Sprachen",
            "it" to "Lingue",
            "ru" to "Языки",
            "zh" to "语言",
            "hi" to "भाषाएं",
            "pt" to "Idiomas",
            "id" to "Bahasa",
            "fa" to "زبان‌ها",
            "ur" to "زبانیں"
        )
    )

    fun tr(key: String): String {
        val langCode = currentLanguage.code
        return translations[key]?.get(langCode)
            ?: translations[key]?.get("ar")
            ?: key
    }
}
