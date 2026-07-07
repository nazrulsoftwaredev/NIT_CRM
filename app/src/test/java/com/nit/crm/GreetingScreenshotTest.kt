package com.nit.crm

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.test.junit4.createComposeRule
import kotlinx.coroutines.runBlocking
import androidx.compose.ui.test.onRoot
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.nit.crm.core.database.AppDatabase
import com.nit.crm.data.*
import com.nit.crm.features.MainViewModel
import com.nit.crm.features.screens.LoginScreen
import com.nit.crm.features.screens.SplashScreen
import com.nit.crm.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var db: AppDatabase
  private lateinit var viewModel: MainViewModel

  @Before
  fun setUp() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
      .allowMainThreadQueries()
      .build()

    val userRepo = UserRepository(db.userDao())
    val customerRepo = CustomerRepository(db.customerDao(), db.syncQueueDao(), db.activityLogDao())
    val productRepo = ProductRepository(db.productDao())
    val invoiceRepo = InvoiceRepository(db.invoiceDao(), db.invoiceItemDao(), db.syncQueueDao(), db.customerDao(), db.activityLogDao())
    val paymentRepo = PaymentRepository(db.paymentDao(), db.customerDao(), db.syncQueueDao(), db.invoiceDao(), db.invoiceItemDao(), db.activityLogDao())
    val serviceRepo = ServiceRecordRepository(db.serviceRecordDao(), db.installationRecordDao(), db.syncQueueDao(), db.activityLogDao())
    val syncRepo = SyncRepository(db.syncQueueDao(), db.customerDao(), db.invoiceDao(), db.paymentDao(), db.serviceRecordDao(), db.installationRecordDao(), db.invoiceItemDao())

    viewModel = MainViewModel(userRepo, customerRepo, productRepo, invoiceRepo, paymentRepo, serviceRepo, syncRepo)
  }

  @After
  fun tearDown() {
    db.close()
  }

  @Test
  fun test_splash_screen_screenshot() {
    composeTestRule.setContent {
      MyApplicationTheme {
        SplashScreen()
      }
    }
    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/splash_screen.png")
  }

  @Test
  fun test_login_screen_screenshot() {
    val loginViewModel = com.nit.crm.features.LoginViewModel(UserRepository(db.userDao()))
    composeTestRule.setContent {
      MyApplicationTheme {
        LoginScreen(loginViewModel) {}
      }
    }
    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/login_screen.png")
  }

  @Test
  fun test_invoice_saving_and_pdf_generation() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    
    val product = com.nit.crm.core.database.Product(
        id = "prod-1",
        sku = "SKU-TEST",
        name = "Test Product",
        price = 1200.0,
        category = "CCTV",
        stockQuantity = 10
    )
    
    runBlocking {
        db.productDao().insertProduct(product)
        viewModel.addProductToInvoice(product)
        
        val customer = com.nit.crm.core.database.Customer(
            id = "cust-1",
            name = "John Doe",
            phone = "123456",
            email = "john@example.com",
            address = "Dhaka, Bangladesh"
        )
        db.customerDao().insertCustomer(customer)
        viewModel.selectedCustomer = customer
        
        val invoice = viewModel.saveInvoice(
            context = context,
            customerNameInput = "John Doe",
            customerPhoneInput = "123456",
            customerEmailInput = "john@example.com",
            customerAddressInput = "Dhaka, Bangladesh",
            amountPaid = 0.0,
            isQuote = false,
            signatureBitmap = null
        )
        
        org.junit.Assert.assertNotNull("Invoice should not be null", invoice)
        
        val pdfFile = viewModel.getOrGenerateInvoicePdf(context, invoice!!)
        org.junit.Assert.assertNotNull("PDF file should not be null", pdfFile)
        org.junit.Assert.assertTrue("PDF file should exist", pdfFile!!.exists())
    }
  }

  @Test
  fun test_draft_invoice_creation_and_finalization() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    
    val product = com.nit.crm.core.database.Product(
        id = "prod-2",
        sku = "SKU-DRAFT",
        name = "Draft Test Product",
        price = 1500.0,
        category = "CCTV",
        stockQuantity = 5
    )
    
    runBlocking {
        db.productDao().insertProduct(product)
        viewModel.addProductToInvoice(product)
        
        val customer = com.nit.crm.core.database.Customer(
            id = "cust-2",
            name = "Jane Smith",
            phone = "654321",
            email = "jane@example.com",
            address = "Dhaka, Bangladesh"
        )
        db.customerDao().insertCustomer(customer)
        viewModel.selectedCustomer = customer
        
        // 1. Save as Draft Invoice
        val draftInvoice = viewModel.saveInvoice(
            context = context,
            customerNameInput = "Jane Smith",
            customerPhoneInput = "654321",
            customerEmailInput = "jane@example.com",
            customerAddressInput = "Dhaka, Bangladesh",
            amountPaid = 0.0,
            isQuote = false,
            signatureBitmap = null,
            isDraft = true
        )
        
        org.junit.Assert.assertNotNull("Draft Invoice should not be null", draftInvoice)
        org.junit.Assert.assertEquals("Draft", draftInvoice!!.status)
        
        // Generate draft PDF
        val draftPdfFile = viewModel.getOrGenerateInvoicePdf(context, draftInvoice)
        org.junit.Assert.assertNotNull("Draft PDF file should not be null", draftPdfFile)
        org.junit.Assert.assertTrue("Draft PDF file should exist", draftPdfFile!!.exists())

        // 2. Finalize Invoice
        val sigBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val finalizedInvoice = viewModel.finalizeDraftInvoice(context, draftInvoice, sigBitmap)
        
        org.junit.Assert.assertNotNull("Finalized Invoice should not be null", finalizedInvoice)
        org.junit.Assert.assertEquals("Unpaid", finalizedInvoice!!.status)
        org.junit.Assert.assertNotNull("Signature path should be set", finalizedInvoice.signaturePath)

        // Generate official PDF
        val officialPdfFile = viewModel.getOrGenerateInvoicePdf(context, finalizedInvoice)
        org.junit.Assert.assertNotNull("Official PDF file should not be null", officialPdfFile)
        org.junit.Assert.assertTrue("Official PDF file should exist", officialPdfFile!!.exists())
    }
  }

  @Test
  fun test_quotation_pdf_generation() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    
    val product = com.nit.crm.core.database.Product(
        id = "prod-3",
        sku = "SKU-QUOTE",
        name = "Quote Test Product",
        price = 500.0,
        category = "CCTV",
        stockQuantity = 20
    )
    
    runBlocking {
        db.productDao().insertProduct(product)
        viewModel.addProductToInvoice(product)
        
        val customer = com.nit.crm.core.database.Customer(
            id = "cust-3",
            name = "Quote Client",
            phone = "987654",
            email = "client@example.com",
            address = "Dhaka, Bangladesh"
        )
        db.customerDao().insertCustomer(customer)
        viewModel.selectedCustomer = customer
        
        // Save as Quotation
        val quote = viewModel.saveInvoice(
            context = context,
            customerNameInput = "Quote Client",
            customerPhoneInput = "987654",
            customerEmailInput = "client@example.com",
            customerAddressInput = "Dhaka, Bangladesh",
            amountPaid = 0.0,
            isQuote = true,
            signatureBitmap = null
        )
        
        org.junit.Assert.assertNotNull("Quotation should not be null", quote)
        org.junit.Assert.assertEquals("Draft", quote!!.status)
        org.junit.Assert.assertTrue("Should be isQuotation = true", quote.isQuotation)
        
        val pdfFile = viewModel.getOrGenerateInvoicePdf(context, quote)
        org.junit.Assert.assertNotNull("Quotation PDF file should not be null", pdfFile)
        org.junit.Assert.assertTrue("Quotation PDF file should exist", pdfFile!!.exists())
    }
  }
}
