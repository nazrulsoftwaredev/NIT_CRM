package com.nit.crm

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.nit.crm.core.database.AppDatabase
import com.nit.crm.core.database.FirebaseSyncHelper
import com.nit.crm.data.*
import com.nit.crm.features.*
import com.nit.crm.features.screens.*
import com.nit.crm.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.text.font.FontWeight
import android.widget.Toast

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Enable Edge-to-Edge
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        FirebaseSyncHelper.initialize(applicationContext)
        val db = AppDatabase.getDatabase(applicationContext)

        lifecycleScope.launch {
            AppDatabase.prepopulateProducts(db)
        }

        val userRepo = UserRepository(db.userDao())
        val customerRepo = CustomerRepository(db.customerDao(), db.syncQueueDao(), db.activityLogDao())
        val productRepo = ProductRepository(db.productDao())
        val invoiceRepo = InvoiceRepository(db.invoiceDao(), db.invoiceItemDao(), db.syncQueueDao(), db.customerDao(), db.activityLogDao())
        val paymentRepo = PaymentRepository(db.paymentDao(), db.customerDao(), db.syncQueueDao(), db.invoiceDao(), db.invoiceItemDao(), db.activityLogDao())
        val serviceRepo = ServiceRecordRepository(db.serviceRecordDao(), db.installationRecordDao(), db.syncQueueDao(), db.activityLogDao())
        val syncRepo = SyncRepository(db.syncQueueDao(), db.customerDao(), db.invoiceDao(), db.paymentDao(), db.serviceRecordDao(), db.installationRecordDao(), db.invoiceItemDao())

        val factory = MainViewModelFactory(userRepo, customerRepo, productRepo, invoiceRepo, paymentRepo, serviceRepo, syncRepo)
        viewModel = ViewModelProvider(this, factory)[MainViewModel::class.java]

        val loginViewModel = LoginViewModel(userRepo)
        val dashboardViewModel = DashboardViewModel(userRepo, syncRepo, invoiceRepo, paymentRepo, customerRepo)
        val customersViewModel = CustomersViewModel(customerRepo, invoiceRepo, paymentRepo, serviceRepo)
        val productsViewModel = ProductsViewModel(productRepo)
        val invoicesViewModel = InvoicesViewModel(invoiceRepo)
        val paymentAndServiceViewModel = PaymentAndServiceViewModel(paymentRepo, serviceRepo, customerRepo, invoiceRepo)
        val settingsViewModel = SettingsViewModel(application)

        setContent {
            MyApplicationTheme {
                AppNavigationContainer(
                    viewModel = viewModel,
                    loginViewModel = loginViewModel,
                    dashboardViewModel = dashboardViewModel,
                    customersViewModel = customersViewModel,
                    productsViewModel = productsViewModel,
                    invoicesViewModel = invoicesViewModel,
                    paymentAndServiceViewModel = paymentAndServiceViewModel,
                    settingsViewModel = settingsViewModel
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigationContainer(
    viewModel: MainViewModel,
    loginViewModel: LoginViewModel,
    dashboardViewModel: DashboardViewModel,
    customersViewModel: CustomersViewModel,
    productsViewModel: ProductsViewModel,
    invoicesViewModel: InvoicesViewModel,
    paymentAndServiceViewModel: PaymentAndServiceViewModel,
    settingsViewModel: SettingsViewModel
) {
    val activeScreen = viewModel.activeScreen
    val isMainTab = activeScreen in listOf("dashboard", "customer_list", "products", "invoice_list", "quotation_list", "service_tickets", "pipeline")

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    BackHandler(enabled = activeScreen != "dashboard" && activeScreen != "login" && activeScreen != "splash") {
        viewModel.goBack()
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = isMainTab || activeScreen == "settings",
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.background,
                modifier = Modifier.width(300.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                // Drawer Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val logoBitmap = remember(settingsViewModel.companyLogoBase64) {
                        if (settingsViewModel.companyLogoBase64.isBlank()) null
                        else try {
                            val decodedBytes = android.util.Base64.decode(settingsViewModel.companyLogoBase64, android.util.Base64.DEFAULT)
                            android.graphics.BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                        } catch (e: Exception) { null }
                    }

                    if (logoBitmap != null) {
                        androidx.compose.foundation.Image(
                            bitmap = logoBitmap.asImageBitmap(),
                            contentDescription = "Logo",
                            modifier = Modifier
                                .size(48.dp)
                                .clip(androidx.compose.foundation.shape.CircleShape)
                                .border(1.dp, MaterialTheme.colorScheme.primary, androidx.compose.foundation.shape.CircleShape)
                        )
                    }

                    Column {
                        Text(
                            text = settingsViewModel.companyName.ifBlank { "NIT CRM" },
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Operations Menu",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(8.dp))
                
                // Drawer Items
                val drawerItems = listOf(
                    Triple("dashboard", "Dashboard", Icons.Default.Dashboard),
                    Triple("customer_list", "Clients", Icons.Default.People),
                    Triple("pipeline", "Sales Pipeline", Icons.AutoMirrored.Filled.TrendingUp),
                    Triple("service_tickets", "Service Tickets", Icons.Default.ConfirmationNumber),
                    Triple("invoice_list", "Invoices", Icons.Default.Description),
                    Triple("quotation_list", "Quotations", Icons.Default.RequestPage),
                    Triple("products", "Inventory Stock", Icons.Default.Inventory),
                    Triple("settings", "System Settings", Icons.Default.Settings)
                )
                
                drawerItems.forEach { (route, label, icon) ->
                    val selected = activeScreen == route
                    NavigationDrawerItem(
                        icon = { Icon(icon, contentDescription = null) },
                        label = { Text(label, style = MaterialTheme.typography.titleMedium) },
                        selected = selected,
                        onClick = {
                            scope.launch { drawerState.close() }
                            if (activeScreen != route) {
                                viewModel.navigateTo(route, clearStack = true)
                            }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
                
                Spacer(modifier = Modifier.weight(1f))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(8.dp))
                
                // Logout Button in Drawer
                NavigationDrawerItem(
                    icon = { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                    label = { Text("Logout", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        dashboardViewModel.logout {
                            viewModel.navigateTo("login", clearStack = true)
                        }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    ) {
        Scaffold(
            bottomBar = {
                if (isMainTab) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.background,
                        tonalElevation = 0.dp,
                        windowInsets = WindowInsets(0) // Remove navigation bar padding
                    ) {
                        val tabs = listOf(
                            Triple("dashboard", "Home", Icons.Outlined.Dashboard to Icons.Filled.Dashboard),
                            Triple("customer_list", "Clients", Icons.Outlined.People to Icons.Filled.People),
                            Triple("pipeline", "Pipeline", Icons.AutoMirrored.Outlined.TrendingUp to Icons.AutoMirrored.Filled.TrendingUp),
                            Triple("products", "Inventory", Icons.Outlined.Inventory to Icons.Filled.Inventory),
                            Triple("invoice_list", "Invoices", Icons.AutoMirrored.Outlined.ReceiptLong to Icons.AutoMirrored.Filled.ReceiptLong)
                        )
                        tabs.forEach { (route, label, icons) ->
                            val selected = activeScreen == route || (route == "invoice_list" && activeScreen == "quotation_list")
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    if (activeScreen != route) {
                                        viewModel.navigateTo(route, clearStack = true)
                                    }
                                },
                                icon = { 
                                    Icon(
                                        imageVector = if (selected) icons.second else icons.first,
                                        contentDescription = label, 
                                        modifier = Modifier.size(24.dp)
                                    ) 
                                },
                                label = { Text(label, style = MaterialTheme.typography.labelLarge) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    unselectedIconColor = MaterialTheme.colorScheme.secondary,
                                    unselectedTextColor = MaterialTheme.colorScheme.secondary,
                                    indicatorColor = Color.Transparent
                                ),
                                modifier = Modifier.testTag("nav_tab_$route")
                            )
                        }
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = if (isMainTab) innerPadding.calculateBottomPadding() else 0.dp)
            ) {
                val context = androidx.compose.ui.platform.LocalContext.current
                Crossfade(targetState = activeScreen, label = "ScreenTransition") { screen ->
                    when (screen) {
                        "splash" -> SplashScreen()
                        "login" -> LoginScreen(loginViewModel = loginViewModel) {
                            viewModel.navigateTo("dashboard", clearStack = true)
                        }
                        "dashboard" -> DashboardScreen(
                            dashboardViewModel = dashboardViewModel,
                            companyName = settingsViewModel.companyName,
                            companyLogoBase64 = settingsViewModel.companyLogoBase64,
                            onNavigateTo = { route -> 
                                if (route == "add_customer") {
                                    viewModel.selectedCustomer = null
                                }
                                viewModel.navigateTo(route) 
                            },
                            onStartNewInvoice = {
                                viewModel.clearInvoice()
                                viewModel.navigateTo("create_invoice")
                            },
                            onStartNewQuotation = {
                                viewModel.clearInvoice()
                                viewModel.navigateTo("create_quotation")
                            },
                            onViewProductStock = {
                                viewModel.isProductSelectionMode = false
                                viewModel.navigateTo("products")
                            },
                            onMenuClick = {
                                scope.launch { drawerState.open() }
                            }
                        )
                        "customer_list" -> CustomerListScreen(
                            customersViewModel = customersViewModel,
                            isForInvoiceSelection = false,
                            onCustomerSelected = { customer ->
                                viewModel.selectedCustomer = customer
                            },
                            onAddClient = {
                                viewModel.selectedCustomer = null
                                viewModel.navigateTo("add_customer")
                            },
                            onNavigateTo = { route -> viewModel.navigateTo(route) },
                            onBack = { scope.launch { drawerState.open() } }
                        )
                        "pipeline" -> PipelineScreen(
                            customersViewModel = customersViewModel,
                            onDealClick = { customer ->
                                viewModel.selectedCustomer = customer
                                viewModel.navigateTo("customer_details")
                            },
                            onBack = { scope.launch { drawerState.open() } }
                        )
                        "customer_list_invoice" -> CustomerListScreen(
                            customersViewModel = customersViewModel,
                            isForInvoiceSelection = true,
                            onCustomerSelected = { customer ->
                                viewModel.selectedCustomer = customer
                            },
                            onAddClient = {
                                viewModel.selectedCustomer = null
                                viewModel.navigateTo("add_customer")
                            },
                            onNavigateTo = { route -> viewModel.navigateTo(route) },
                            onBack = { viewModel.goBack() }
                        )
                        "customer_details" -> {
                            val cust = viewModel.selectedCustomer
                            if (cust != null) {
                                CustomerDetailsScreen(
                                    customerId = cust.id,
                                    customersViewModel = customersViewModel,
                                    onStartNewInvoice = {
                                        viewModel.clearInvoice()
                                        viewModel.selectedCustomer = cust
                                        viewModel.navigateTo("create_invoice")
                                    },
                                    onStartNewQuotation = {
                                        viewModel.clearInvoice()
                                        viewModel.selectedCustomer = cust
                                        viewModel.navigateTo("create_quotation")
                                    },
                                    onNavigateTo = { route -> viewModel.navigateTo(route) },
                                    onBack = {
                                        viewModel.selectedCustomer = null
                                        viewModel.goBack()
                                    }
                                )
                            } else {
                                viewModel.navigateTo("customer_list", clearStack = true)
                            }
                        }
                        "products" -> ProductsCatalogScreen(
                            productsViewModel = productsViewModel,
                            onNavigateToAddProduct = { product ->
                                productsViewModel.clearDrafts()
                                viewModel.selectedProduct = product
                                viewModel.navigateTo("add_product")
                            },
                            onNavigateToScanner = {
                                viewModel.isContinuousScanMode = false
                                viewModel.onBarcodeScannedAction = { barcode ->
                                    productsViewModel.setProductSearchQuery(barcode)
                                }
                                viewModel.navigateTo("scanner")
                            },
                            onBack = {
                                scope.launch { drawerState.open() }
                            }
                        )
                        "create_invoice" -> InvoiceCreatorScreen(viewModel, isQuotation = false)
                        "create_quotation" -> InvoiceCreatorScreen(viewModel, isQuotation = true)
                        "receive_payment" -> ReceivePaymentScreen(viewModel)
                        "log_service" -> LogServiceScreen(viewModel)
                        "log_installation" -> LogInstallationScreen(viewModel)
                        "select_products" -> SelectProductsScreen(
                            mainViewModel = viewModel,
                            productsViewModel = productsViewModel,
                            onNavigateToScanner = {
                                viewModel.isContinuousScanMode = true
                                viewModel.onBarcodeScannedAction = { barcode ->
                                    val matching = productsViewModel.products.value.find { it.sku.equals(barcode, ignoreCase = true) }
                                    if (matching != null) {
                                        viewModel.addProductToInvoice(matching)
                                        Toast.makeText(context, "${matching.name} Added", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "SKU: $barcode not found", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                viewModel.navigateTo("scanner")
                            },
                            onBack = { viewModel.goBack() }
                        )
                        "service_tickets" -> ServiceTicketsScreen(
                            serviceViewModel = paymentAndServiceViewModel,
                            onNavigateToCreate = {
                                viewModel.navigateTo("add_service_ticket")
                            },
                            onNavigateToDetails = { ticket ->
                                viewModel.selectedServiceTicket = ticket
                                viewModel.navigateTo("service_ticket_details")
                            },
                            onBack = { scope.launch { drawerState.open() } }
                        )
                        "add_service_ticket" -> AddServiceTicketScreen(
                            serviceViewModel = paymentAndServiceViewModel,
                            customersViewModel = customersViewModel,
                            onBack = { viewModel.goBack() }
                        )
                        "service_ticket_details" -> {
                            val activeTicket = viewModel.selectedServiceTicket
                            if (activeTicket != null) {
                                ServiceTicketDetailsScreen(
                                    serviceViewModel = paymentAndServiceViewModel,
                                    ticket = activeTicket,
                                    mainViewModel = viewModel,
                                    onNavigateTo = { route -> viewModel.navigateTo(route) },
                                    onBack = { viewModel.goBack() }
                                )
                            }
                        }
                        "settings" -> SettingsScreen(
                            dashboardViewModel = dashboardViewModel,
                            settingsViewModel = settingsViewModel,
                            onNavigateTo = { route -> viewModel.navigateTo(route) },
                            mainViewModel = viewModel,
                            onMenuClick = {
                                scope.launch { drawerState.open() }
                            }
                        )
                        "invoice_list" -> InvoiceListScreen(
                            invoicesViewModel = invoicesViewModel,
                            onInvoiceClick = { invoice ->
                                viewModel.selectedInvoice = invoice
                                viewModel.navigateTo("invoice_details")
                            },
                            onShareClick = { invoice ->
                                val mainActivityScope = (context as? androidx.lifecycle.LifecycleOwner)?.lifecycleScope
                                mainActivityScope?.launch {
                                    val file = viewModel.getOrGenerateInvoicePdf(context, invoice)
                                    if (file != null && file.exists()) {
                                        sharePdfFile(context, file)
                                    } else {
                                        Toast.makeText(context, "Failed to generate PDF", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            onBack = { scope.launch { drawerState.open() } }
                        )
                        "quotation_list" -> QuotationListScreen(
                            invoicesViewModel = invoicesViewModel,
                            onInvoiceClick = { invoice ->
                                viewModel.selectedInvoice = invoice
                                viewModel.navigateTo("invoice_details")
                            },
                            onShareClick = { invoice ->
                                val mainActivityScope = (context as? androidx.lifecycle.LifecycleOwner)?.lifecycleScope
                                mainActivityScope?.launch {
                                    val file = viewModel.getOrGenerateInvoicePdf(context, invoice)
                                    if (file != null && file.exists()) {
                                        sharePdfFile(context, file)
                                    } else {
                                        Toast.makeText(context, "Failed to generate PDF", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            onBack = { scope.launch { drawerState.open() } }
                        )
                    "invoice_details" -> {
                        val invoice = viewModel.selectedInvoice
                        if (invoice != null) {
                            InvoiceDetailsScreen(
                                invoice = invoice,
                                viewModel = viewModel,
                                invoicesViewModel = invoicesViewModel,
                                onGetPdfFile = {
                                    viewModel.getOrGenerateInvoicePdf(context, invoice)
                                },
                                onSharePdf = {
                                    val mainActivityScope = (context as? androidx.lifecycle.LifecycleOwner)?.lifecycleScope
                                    mainActivityScope?.launch {
                                        val file = viewModel.getOrGenerateInvoicePdf(context, invoice)
                                        if (file != null && file.exists()) {
                                            sharePdfFile(context, file)
                                        } else {
                                            Toast.makeText(context, "Failed to generate or locate PDF", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                onBack = { viewModel.goBack() }
                            )
                        } else {
                            viewModel.goBack()
                        }
                    }
                    "add_product" -> AddProductScreen(
                        productsViewModel = productsViewModel,
                        selectedProduct = viewModel.selectedProduct,
                        onNavigateToScanner = {
                            viewModel.onBarcodeScannedAction = { barcode ->
                                productsViewModel.draftSku = barcode
                            }
                            viewModel.navigateTo("scanner")
                        },
                        onBack = { viewModel.goBack() }
                    )
                    "scanner" -> ScannerScreen(
                        continuousMode = viewModel.isContinuousScanMode,
                        onBarcodeDetected = { barcode ->
                            viewModel.onBarcodeScannedAction?.invoke(barcode)
                            if (!viewModel.isContinuousScanMode) {
                                viewModel.goBack()
                            }
                        },
                        onBack = { viewModel.goBack() }
                    )
                    "invoice_success" -> InvoiceSuccessScreen(viewModel)
                    "add_customer" -> AddCustomerScreen(
                        customersViewModel = customersViewModel,
                        selectedCustomer = viewModel.selectedCustomer,
                        onBack = { 
                            viewModel.selectedCustomer = null
                            viewModel.goBack() 
                        }
                    )
                    "signature_capture" -> SignatureCaptureScreen(
                        viewModel = viewModel,
                        onBack = { viewModel.goBack() }
                    )
                    "recent_activity" -> RecentActivityScreen(
                        dashboardViewModel = dashboardViewModel,
                        onNavigateTo = { route -> 
                            if (route == "add_customer") {
                                viewModel.selectedCustomer = null
                            }
                            viewModel.navigateTo(route) 
                        },
                        onBack = { viewModel.goBack() }
                    )
                    "payment_list" -> PaymentHistoryScreen(
                        dashboardViewModel = dashboardViewModel,
                        onBack = { viewModel.goBack() }
                    )
                    else -> DashboardScreen(
                        dashboardViewModel = dashboardViewModel,
                        companyName = settingsViewModel.companyName,
                        companyLogoBase64 = settingsViewModel.companyLogoBase64,
                        onNavigateTo = { route -> viewModel.navigateTo(route) },
                        onStartNewInvoice = {
                            viewModel.clearInvoice()
                            viewModel.navigateTo("create_invoice")
                        },
                        onStartNewQuotation = {
                            viewModel.clearInvoice()
                            viewModel.navigateTo("create_quotation")
                        },
                        onViewProductStock = {
                            viewModel.isProductSelectionMode = false
                            viewModel.navigateTo("products")
                        },
                        onMenuClick = {
                            scope.launch { drawerState.open() }
                        }
                    )
                }
            }
        }
    }
}
}
