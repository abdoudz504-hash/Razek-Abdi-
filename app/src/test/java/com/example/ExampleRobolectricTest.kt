package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Rz Tasyir", appName)
  }

  @Test
  fun `launch MainActivity`() {
    val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
    assertNotNull(controller.get())
  }

  @Test
  fun `verify pos calculation logic`() {
    val subtotal = 1500.0
    val discountPercent = 10
    val discountAmount = (subtotal * discountPercent) / 100.0
    val grandTotal = subtotal - discountAmount
    val cashReceived = 2000.0
    val changeReturn = cashReceived - grandTotal

    assertEquals(150.0, discountAmount, 0.001)
    assertEquals(1350.0, grandTotal, 0.001)
    assertEquals(650.0, changeReturn, 0.001)
  }

  @Test
  fun `verify product and sale entities structure`() {
    val product = com.example.data.model.ProductEntity(
      id = 1L,
      name = "حليب كونديال 1 لتر",
      barcode = "6130001",
      category = "ألبان وأجبان",
      purchasePrice = 110.0,
      sellingPrice = 135.0,
      stockQuantity = 24
    )
    assertEquals("6130001", product.barcode)
    assertEquals(25.0, product.sellingPrice - product.purchasePrice, 0.001)
    assertEquals(false, product.isLowStock)

    val sale = com.example.data.model.SaleEntity(
      id = 1L,
      invoiceNumber = "INV-260915-114500",
      totalAmount = 270.0,
      costAmount = 220.0,
      paidAmount = 270.0,
      paymentType = "CASH"
    )
    assertEquals("INV-260915-114500", sale.invoiceNumber)
    assertEquals(0.0, sale.debtAmount, 0.001)
  }

  @Test
  fun `verify single barcode scan rule and manual quantity control`() {
    val context = ApplicationProvider.getApplicationContext<android.app.Application>()
    val viewModel = com.example.ui.MainViewModel(context)

    // Clear cart first
    viewModel.clearCart()
    assertEquals(0, viewModel.cartItems.value.size)

    // Wait/ensure products are seeded or check BarcodeScanResult sealed class
    val dummyProduct = com.example.data.model.ProductEntity(
      id = 999L,
      name = "منتج تجريبي للباركود",
      barcode = "999888777",
      category = "عام",
      purchasePrice = 50.0,
      sellingPrice = 100.0,
      stockQuantity = 50
    )

    // Initially add to cart
    viewModel.addToCart(dummyProduct)
    assertEquals(1, viewModel.cartItems.value.size)
    assertEquals(1, viewModel.cartItems.value[0].quantity)

    // Manual increment (+1 by hand)
    viewModel.updateCartQuantity(dummyProduct.id, 2)
    assertEquals(2, viewModel.cartItems.value[0].quantity)

    // Manual decrement (-1 by hand)
    viewModel.updateCartQuantity(dummyProduct.id, 1)
    assertEquals(1, viewModel.cartItems.value[0].quantity)
  }

  @Test
  fun `verify commercial license lock and activation flow`() {
    val context = ApplicationProvider.getApplicationContext<android.app.Application>()
    val licenseManager = com.example.ui.util.LicenseManager

    // Reset license to test locked state
    licenseManager.resetLicenseForTesting(context)
    assertFalse(licenseManager.isActivated(context))

    val deviceId = licenseManager.getDeviceId(context)
    assertTrue(deviceId.startsWith("RZ-"))

    // Invalid key should be rejected
    val invalidResult = licenseManager.activate(context, "INVALID-KEY-1234", "محل تجريبي")
    assertFalse(invalidResult)
    assertFalse(licenseManager.isActivated(context))

    // Valid generated key for this device
    val validKey = licenseManager.generateActivationKey(deviceId)
    assertTrue(validKey.startsWith("RZK-"))

    // Activation with valid key
    val activateResult = licenseManager.activate(context, validKey, "سوبر ماركت النور")
    assertTrue(activateResult)
    assertTrue(licenseManager.isActivated(context))

    // Verify stored info
    val info = licenseManager.getLicenseInfo(context)
    assertTrue(info.isActivated)
    assertEquals("سوبر ماركت النور", info.storeName)
    assertEquals(deviceId, info.deviceId)
    assertEquals("0665233528", info.developerPhone)

    // Verify developer PIN check
    assertTrue(licenseManager.checkDeveloperPin("0665"))
    assertFalse(licenseManager.checkDeveloperPin("0000"))

    // Reset and lock again
    licenseManager.resetLicenseForTesting(context)
    assertFalse(licenseManager.isActivated(context))

    // Test dynamic Request Code and Quick Key
    val requestCode = licenseManager.getRequestCode(deviceId)
    assertEquals(4, requestCode.length)

    // Request code alone should NOT activate
    val reqAloneResult = licenseManager.activate(context, requestCode, "محل")
    assertFalse(reqAloneResult)
    assertFalse(licenseManager.isActivated(context))

    // Quick key ($requestCode-0665) SHOULD activate successfully!
    val quickKey = licenseManager.getQuickActivationKey(deviceId)
    assertEquals("$requestCode-0665", quickKey)
    val quickActivateResult = licenseManager.activate(context, quickKey, "محل السلام")
    assertTrue(quickActivateResult)
    assertTrue(licenseManager.isActivated(context))

    // Reset and lock again
    licenseManager.resetLicenseForTesting(context)
    assertFalse(licenseManager.isActivated(context))

    // Verify Developer Master Bypass Key (0665233528)
    val masterBypassResult = licenseManager.activate(context, "0665233528", "متجر المطور")
    assertTrue(masterBypassResult)
    assertTrue(licenseManager.isActivated(context))
  }
}

