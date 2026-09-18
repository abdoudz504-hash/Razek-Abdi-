package com.example

import com.example.ui.util.LicenseManager
import org.junit.Assert.*
import org.junit.Test

/**
 * اختبارات وحدة التحقق من منطق التفعيل وأكواد المالك ورسائل SMS
 */
class ExampleUnitTest {
    private val testDeviceId = "RZ-A1B2-C3D4"

    @Test
    fun testRequestCodeAndQuickActivation() {
        val reqCode = LicenseManager.getRequestCode(testDeviceId)
        assertEquals(4, reqCode.length)
        assertTrue(reqCode.all { it.isDigit() })

        val quickKey = LicenseManager.getQuickActivationKey(testDeviceId)
        assertEquals("$reqCode-0665", quickKey)

        // التحقق من أن كود التفعيل السريع صالح للجهاز
        val verification = LicenseManager.verifyCodeDetails(testDeviceId, quickKey)
        assertTrue("يجب أن يكون الكود صالحاً", verification.isValid)
    }

    @Test
    fun testRejectIncompleteRequestCode() {
        val reqCode = LicenseManager.getRequestCode(testDeviceId)
        // إذا قام الزبون بإدخال رمز الطلب فقط بدون كود المالك
        val verification = LicenseManager.verifyCodeDetails(testDeviceId, reqCode)
        assertFalse("يجب رفض رمز الطلب إذا لم يحتوي على كود المالك", verification.isValid)
        assertTrue(verification.message.contains("رمز الطلب فقط"))
    }

    @Test
    fun testOfficialCryptographicKey() {
        val officialKey = LicenseManager.generateActivationKey(testDeviceId)
        assertTrue(officialKey.startsWith("RZK-"))

        val verification = LicenseManager.verifyCodeDetails(testDeviceId, officialKey)
        assertTrue("يجب قبول المفتاح المشفر الرسمي", verification.isValid)
    }

    @Test
    fun testMasterDeveloperKey() {
        val masterVerif = LicenseManager.verifyCodeDetails(testDeviceId, "0665233528")
        assertTrue("يجب قبول كود هاتف المالك المباشر", masterVerif.isValid)

        val devPinVerif = LicenseManager.checkDeveloperPin("0665")
        assertTrue("رمز PIN المطور يجب أن يكون 0665", devPinVerif)
    }

    @Test
    fun testSmsExtraction() {
        val reqCode = LicenseManager.getRequestCode(testDeviceId)
        val sms1 = "السلام عليكم، تم استلام المبلغ بنجاح. كود التفعيل الخاص بك هو: $reqCode-0665 شكراً لتعاملك معنا."
        val extracted1 = LicenseManager.extractActivationCodeFromSms(sms1)
        assertEquals("$reqCode-0665", extracted1)

        val officialKey = LicenseManager.generateActivationKey(testDeviceId)
        val sms2 = "مرحباً، مفتاح التفعيل الرسمي: $officialKey"
        val extracted2 = LicenseManager.extractActivationCodeFromSms(sms2)
        assertEquals(officialKey, extracted2)
    }

    @Test
    fun testSmsMessageGeneration() {
        val reqCode = LicenseManager.getRequestCode(testDeviceId)
        val smsRequest = LicenseManager.createSmsRequestMessage(testDeviceId, reqCode, "ميني ماركت الأمانة")
        assertTrue(smsRequest.contains(testDeviceId))
        assertTrue(smsRequest.contains(reqCode))
        assertTrue(smsRequest.contains("الأمانة"))

        val smsReply = LicenseManager.createSmsApprovalReply(testDeviceId)
        assertTrue(smsReply.contains("$reqCode-0665"))
    }
}
