package com.nit.crm.features

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nit.crm.core.database.*
import com.nit.crm.data.*
import com.nit.crm.features.screens.createBitmapFromPoints
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class MainViewModel(
    private val userRepository: UserRepository,
    private val customerRepository: CustomerRepository,
    private val productRepository: ProductRepository,
    private val invoiceRepository: InvoiceRepository,
    private val paymentRepository: PaymentRepository,
    private val serviceRepository: ServiceRecordRepository,
    private val syncRepository: SyncRepository
) : ViewModel() {

    companion object {
        fun getInvoicesDir(context: Context): File {
            var baseDir = context.getExternalFilesDir(null)
            if (baseDir == null) {
                baseDir = context.filesDir
            }
            var invoicesDir = File(baseDir, "Invoices")
            if (!invoicesDir.exists()) {
                val created = invoicesDir.mkdirs()
                if (!created && !invoicesDir.exists()) {
                    // Fallback to internal storage
                    invoicesDir = File(context.filesDir, "Invoices")
                    if (!invoicesDir.exists()) {
                        invoicesDir.mkdirs()
                    }
                }
            }
            return invoicesDir
        }
    }

    // Navigation State
    var activeScreen by mutableStateOf("splash")
    private val navigationStack = mutableStateListOf<String>()

    fun navigateTo(screen: String, clearStack: Boolean = false) {
        if (clearStack) {
            navigationStack.clear()
        } else {
            if (activeScreen != "splash" && activeScreen != "login") {
                navigationStack.add(activeScreen)
            }
        }
        if (screen == "log_service") {
            serviceProblem = ""
            serviceSolution = ""
            servicePartsUsed = ""
            serviceLaborCharge = 0.0
            draftSignaturePoints.clear()
        } else if (screen == "settings" || screen == "service_ticket_details") {
            draftSignaturePoints.clear()
        }
        activeScreen = screen
    }

    fun goBack(): Boolean {
        if (navigationStack.isNotEmpty()) {
            activeScreen = navigationStack.removeAt(navigationStack.size - 1)
            return true
        }
        return false
    }

    // Auth States
    val currentUser = userRepository.currentUser.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    // Customer Queries & States
    private val _customerSearchQuery = MutableStateFlow("")
    val customerSearchQuery = _customerSearchQuery.asStateFlow()

    val customers: StateFlow<List<Customer>> = combine(
        customerRepository.allCustomers,
        _customerSearchQuery
    ) { list, query ->
        if (query.isBlank()) {
            list
        } else {
            list.filter {
                it.name.contains(query, ignoreCase = true) ||
                it.phone.contains(query, ignoreCase = true) ||
                it.email.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    var selectedCustomer by mutableStateOf<Customer?>(null)
    var selectedProduct by mutableStateOf<Product?>(null)
    var isProductSelectionMode by mutableStateOf(false)
    var pdfError by mutableStateOf<String?>(null)
    var generatedInvoice by mutableStateOf<Invoice?>(null)
    var onBarcodeScannedAction by mutableStateOf<((String) -> Unit)?>(null)
    var selectedInvoice by mutableStateOf<Invoice?>(null)
    var onSignatureCapturedAction by mutableStateOf<((List<androidx.compose.ui.geometry.Offset>) -> Unit)?>(null)
    var initialSignaturePoints by mutableStateOf<List<androidx.compose.ui.geometry.Offset>>(emptyList())

    // Product Queries & States
    private val _productSearchQuery = MutableStateFlow("")
    val productSearchQuery = _productSearchQuery.asStateFlow()

    val products: StateFlow<List<Product>> = combine(
        productRepository.allProducts,
        _productSearchQuery
    ) { list, query ->
        if (query.isBlank()) {
            list
        } else {
            list.filter {
                it.name.contains(query, ignoreCase = true) ||
                it.sku.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Invoice active states
    val currentInvoiceItems = mutableStateListOf<InvoiceItemDraft>()
    var invoiceDiscount by mutableStateOf(0.0)
    var invoiceServiceCharge by mutableStateOf(0.0)
    var draftCustomerName by mutableStateOf("")
    var draftCustomerPhone by mutableStateOf("")
    var draftCustomerEmail by mutableStateOf("")
    var draftCustomerAddress by mutableStateOf("")
    var draftServiceCharge by mutableStateOf("")
    var draftDiscount by mutableStateOf("")
    var draftAmountPaid by mutableStateOf("")
    val draftSignaturePoints = mutableStateListOf<androidx.compose.ui.geometry.Offset>()
    var selectedServiceTicket by mutableStateOf<com.nit.crm.core.database.ServiceRecord?>(null)
    var isContinuousScanMode by mutableStateOf(false)

    class InvoiceItemDraft(
        val product: Product,
        initialQuantity: Int
    ) {
        var quantity by mutableStateOf(initialQuantity)
        val serialNumbers: MutableList<String> = mutableStateListOf<String>().apply { 
            repeat(initialQuantity) { add("") } 
        }
    }

    // Invoice Queries & States
    private val _invoiceSearchQuery = MutableStateFlow("")
    val invoiceSearchQuery = _invoiceSearchQuery.asStateFlow()

    val searchedInvoices: StateFlow<List<Invoice>> = combine(
        invoiceRepository.allInvoices,
        _invoiceSearchQuery
    ) { list, query ->
        if (query.isBlank()) {
            list
        } else {
            list.filter {
                it.customerName.contains(query, ignoreCase = true) ||
                it.id.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allInvoices = invoiceRepository.allInvoices.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Customer detail histories
    fun getInvoicesForCustomer(customerId: String): StateFlow<List<Invoice>> {
        return invoiceRepository.getInvoicesForCustomer(customerId).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    fun getPaymentsForCustomer(customerId: String): StateFlow<List<Payment>> {
        return paymentRepository.getPaymentsForCustomer(customerId).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    fun getServiceRecordsForCustomer(customerId: String): StateFlow<List<ServiceRecord>> {
        return serviceRepository.getServiceRecordsForCustomer(customerId).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    fun getInstallationRecordsForCustomer(customerId: String): StateFlow<List<InstallationRecord>> {
        return serviceRepository.getInstallationRecordsForCustomer(customerId).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    // Payments lists
    val allPayments = paymentRepository.allPayments.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Sync Queue
    val pendingSyncItems = syncRepository.pendingSyncItems.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    var isSyncing by mutableStateOf(false)

    // Company Settings (defaults)
    var companyName by mutableStateOf("NIT CRM")
    var companyAddress by mutableStateOf("Officer's housing, Pollibidut, Ashulia, Savar, Dhaka")
    var companyPhone by mutableStateOf("+880 1799-800322")
    var companyTaxId by mutableStateOf("CCTV & Computer Solution")

    // Active Service fields
    var serviceProblem by mutableStateOf("")
    var serviceSolution by mutableStateOf("")
    var servicePartsUsed by mutableStateOf("")
    var serviceLaborCharge by mutableStateOf(0.0)

    // Active Installation fields
    var installProductsSummary by mutableStateOf("")
    var installNotes by mutableStateOf("")

    init {
        // Trigger splash timing
        viewModelScope.launch {
            delay(1500)
            navigateTo("login", clearStack = true)
        }
    }

    fun setCustomerSearchQuery(query: String) {
        _customerSearchQuery.value = query
    }

    fun setProductSearchQuery(query: String) {
        _productSearchQuery.value = query
    }

    fun setInvoiceSearchQuery(query: String) {
        _invoiceSearchQuery.value = query
    }

    // Login Method
    fun login(name: String, email: String, role: String) {
        viewModelScope.launch {
            userRepository.login(name, email, role)
            navigateTo("dashboard", clearStack = true)
        }
    }

    // Logout Method
    fun logout() {
        viewModelScope.launch {
            userRepository.logout()
            navigateTo("login", clearStack = true)
        }
    }

    // Customer Management
    fun addCustomer(name: String, phone: String, email: String, address: String) {
        viewModelScope.launch {
            val customer = Customer(
                id = "cust_" + UUID.randomUUID().toString().take(6),
                name = name,
                phone = phone,
                email = email,
                address = address,
                dueAmount = 0.0
            )
            customerRepository.insertCustomer(customer)
        }
    }

    fun saveCustomer(id: String?, name: String, phone: String, email: String, address: String) {
        viewModelScope.launch {
            if (id != null) {
                val existing = customerRepository.getCustomerByIdSync(id)
                if (existing != null) {
                    val updated = existing.copy(name = name, phone = phone, email = email, address = address)
                    customerRepository.updateCustomer(updated)
                }
            } else {
                val customer = Customer(
                    id = "cust_" + UUID.randomUUID().toString().take(6).uppercase(),
                    name = name,
                    phone = phone,
                    email = email,
                    address = address,
                    dueAmount = 0.0
                )
                customerRepository.insertCustomer(customer)
            }
        }
    }

    fun deleteCustomer(id: String) {
        viewModelScope.launch {
            customerRepository.deleteCustomerById(id)
        }
    }

    fun addProduct(name: String, sku: String, price: Double, category: String, stock: Int) {
        viewModelScope.launch {
            val product = Product(
                id = UUID.randomUUID().toString().take(8),
                sku = sku,
                name = name,
                price = price,
                category = category,
                stockQuantity = stock
            )
            productRepository.insertProduct(product)
        }
    }

    fun saveProduct(id: String?, name: String, sku: String, price: Double, category: String, stock: Int) {
        viewModelScope.launch {
            val product = Product(
                id = id ?: UUID.randomUUID().toString().take(8),
                sku = sku,
                name = name,
                price = price,
                category = category,
                stockQuantity = stock
            )
            productRepository.insertProduct(product)
        }
    }

    fun deleteProduct(id: String) {
        viewModelScope.launch {
            productRepository.deleteProductById(id)
        }
    }

    fun selectOrCreateCustomerForJob(clientName: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            // Check if customer already exists (mock behavior for now)
            val customer = Customer(
                id = "cust_" + UUID.randomUUID().toString().take(6),
                name = clientName,
                phone = "+880 1XXXX-XXXXXX",
                email = "contact@${clientName.lowercase().replace(" ", "")}.com",
                address = "Service Site, Bangladesh",
                dueAmount = 0.0
            )
            customerRepository.insertCustomer(customer)
            selectedCustomer = customer
            onComplete()
        }
    }

    var lastInteractedItemIndex by mutableStateOf(-1)

    // Invoice item modification
    fun addProductToInvoice(product: Product) {
        val existingIndex = currentInvoiceItems.indexOfFirst { it.product.id == product.id }
        if (existingIndex >= 0) {
            val current = currentInvoiceItems[existingIndex]
            if (current.quantity < product.stockQuantity) {
                current.quantity += 1
                current.serialNumbers.add("")
                currentInvoiceItems[existingIndex] = current // Force list update notification
            }
        } else {
            if (product.stockQuantity > 0) {
                currentInvoiceItems.add(InvoiceItemDraft(product, 1))
            }
        }
    }

    fun removeProductFromInvoice(product: Product) {
        val existingIndex = currentInvoiceItems.indexOfFirst { it.product.id == product.id }
        if (existingIndex >= 0) {
            val current = currentInvoiceItems[existingIndex]
            if (current.quantity > 1) {
                current.quantity -= 1
                if (current.serialNumbers.isNotEmpty()) {
                    current.serialNumbers.removeAt(current.serialNumbers.size - 1)
                }
                currentInvoiceItems[existingIndex] = current // Force list update notification
            } else {
                currentInvoiceItems.removeAt(existingIndex)
            }
        }
    }

    fun updateProductSerialNumber(productId: String, index: Int, serial: String) {
        val itemIndex = currentInvoiceItems.indexOfFirst { it.product.id == productId }
        if (itemIndex >= 0) {
            val item = currentInvoiceItems[itemIndex]
            if (index < item.serialNumbers.size) {
                item.serialNumbers[index] = serial
            }
        }
    }



    fun clearInvoice() {
        currentInvoiceItems.clear()
        invoiceDiscount = 0.0
        invoiceServiceCharge = 0.0
        draftCustomerName = ""
        draftCustomerPhone = ""
        draftCustomerEmail = ""
        draftCustomerAddress = ""
        selectedCustomer = null
        initialSignaturePoints = emptyList()
        draftServiceCharge = ""
        draftDiscount = ""
        draftAmountPaid = ""
        draftSignaturePoints.clear()
    }

    fun getInvoiceSubtotal(): Double {
        return currentInvoiceItems.sumOf { it.product.price * it.quantity }
    }

    fun getInvoiceTotal(): Double {
        return (getInvoiceSubtotal() + invoiceServiceCharge - invoiceDiscount).coerceAtLeast(0.0)
    }

    // Create & Save Invoice Record
    suspend fun saveInvoice(
        context: Context,
        customerNameInput: String,
        customerPhoneInput: String,
        customerEmailInput: String,
        customerAddressInput: String,
        amountPaid: Double,
        isQuote: Boolean = false,
        signatureBitmap: Bitmap?,
        isDraft: Boolean = false
    ): Invoice? = withContext(Dispatchers.IO) {
        pdfError = null
        if (currentInvoiceItems.isEmpty()) return@withContext null

        val nameStr = customerNameInput.trim()
        val phoneStr = customerPhoneInput.trim()
        val emailStr = customerEmailInput.trim()
        val addressStr = customerAddressInput.trim()

        if (nameStr.isBlank()) return@withContext null

        // 1. Resolve or Create Customer
        var resolvedCustomerId = ""
        var resolvedCustomerName = nameStr
        
        val existingCustomer = customerRepository.findCustomerByPhoneOrEmail(phoneStr, emailStr)
        if (existingCustomer != null) {
            resolvedCustomerId = existingCustomer.id
            resolvedCustomerName = existingCustomer.name
            
            // update empty fields on existing customer
            var updated = false
            var tempCustomer = existingCustomer
            if (tempCustomer.phone.isBlank() && phoneStr.isNotBlank()) {
                tempCustomer = tempCustomer.copy(phone = phoneStr)
                updated = true
            }
            if (tempCustomer.email.isBlank() && emailStr.isNotBlank()) {
                tempCustomer = tempCustomer.copy(email = emailStr)
                updated = true
            }
            if (tempCustomer.address.isBlank() && addressStr.isNotBlank()) {
                tempCustomer = tempCustomer.copy(address = addressStr)
                updated = true
            }
            if (updated) {
                customerRepository.updateCustomer(tempCustomer)
            }
        } else {
            val newId = "cust_" + UUID.randomUUID().toString().take(6)
            val newCustomer = Customer(
                id = newId,
                name = nameStr,
                phone = if (phoneStr.isNotBlank()) phoneStr else "+880 1XXXX-XXXXXX",
                email = if (emailStr.isNotBlank()) emailStr else "contact@${nameStr.lowercase().replace(" ", "")}.com",
                address = if (addressStr.isNotBlank()) addressStr else "Service Site, Bangladesh",
                dueAmount = 0.0
            )
            customerRepository.insertCustomer(newCustomer)
            resolvedCustomerId = newId
            resolvedCustomerName = nameStr
        }

        val prefix = if (isQuote) "QT-" else "INV-"
        val invoiceId = prefix + SimpleDateFormat("yyMMdd", Locale.getDefault()).format(Date()) + "-" + UUID.randomUUID().toString().take(4).uppercase()
        val total = getInvoiceTotal()
        
        // Save Signature to Cache
        var signaturePath: String? = null
        if (signatureBitmap != null) {
            try {
                val cacheFile = File(context.cacheDir, "sig_$invoiceId.png")
                val out = FileOutputStream(cacheFile)
                signatureBitmap.compress(Bitmap.CompressFormat.PNG, 90, out)
                out.flush()
                out.close()
                signaturePath = cacheFile.absolutePath
            } catch (e: Exception) {
                Log.e("MainViewModel", "Failed to save signature: ${e.message}")
            }
        }

        // Determine initial status
        val initialStatus = if (isQuote || isDraft) {
            "Draft"
        } else {
            when {
                amountPaid >= total -> "Paid"
                amountPaid > 0.0 -> "Partially Paid"
                else -> "Unpaid"
            }
        }

        // Prepare Database Invoice object
        val invoice = Invoice(
            id = invoiceId,
            customerId = resolvedCustomerId,
            customerName = resolvedCustomerName,
            totalAmount = total,
            discount = invoiceDiscount,
            serviceCharge = invoiceServiceCharge,
            status = initialStatus,
            signaturePath = signaturePath,
            pdfPath = null,
            isSynced = false,
            isQuotation = isQuote
        )

        val items = currentInvoiceItems.map { draft ->
            InvoiceItem(
                id = UUID.randomUUID().toString(),
                invoiceId = invoiceId,
                productId = draft.product.id,
                productName = draft.product.name,
                quantity = draft.quantity,
                unitPrice = draft.product.price,
                totalPrice = draft.product.price * draft.quantity,
                serialNumber = draft.serialNumbers.filter { it.isNotBlank() }.joinToString(", ")
            )
        }

        // Insert to DB (and sync queue)
        invoiceRepository.createInvoice(invoice, items)

        // If a payment was made upfront on invoice (not quotation)
        if (!isQuote && amountPaid > 0.0) {
            val payment = Payment(
                id = UUID.randomUUID().toString(),
                customerId = resolvedCustomerId,
                invoiceId = invoiceId,
                amount = amountPaid.coerceAtMost(total),
                date = System.currentTimeMillis(),
                paymentMethod = "Cash",
                notes = "Upfront payment during invoice creation",
                isSynced = false
            )
            paymentRepository.recordPayment(payment)
        }
        
        val prefs = context.getSharedPreferences("nitcrm_settings", Context.MODE_PRIVATE)
        val currentCompanyName = prefs.getString("company_name", "NIT CRM") ?: "NIT CRM"
        val currentCompanyAddress = prefs.getString("company_address", "Officer's housing, Pollibidut, Ashulia, Savar, Dhaka") ?: "Officer's housing, Pollibidut, Ashulia, Savar, Dhaka"
        val currentCompanyPhone = prefs.getString("company_phone", "+880 1799-800322") ?: "+880 1799-800322"
        val currentCompanyTaxId = prefs.getString("company_tax_id", "CCTV & Computer Solution") ?: "CCTV & Computer Solution"

        val customer = Customer(
            id = resolvedCustomerId,
            name = resolvedCustomerName,
            phone = phoneStr,
            email = emailStr,
            address = addressStr,
            dueAmount = 0.0
        )

        // Return invoice reference
        invoice
    }

    suspend fun getOrGenerateInvoicePdf(context: Context, invoice: Invoice): File? = withContext(Dispatchers.IO) {
        val existingPath = invoice.pdfPath
        if (!existingPath.isNullOrBlank()) {
            val file = File(existingPath)
            if (file.exists()) {
                file.delete()
            }
        }
        val items = invoiceRepository.getItemsForInvoiceSync(invoice.id)
        val signatureBitmap = if (!invoice.signaturePath.isNullOrBlank()) {
            try {
                BitmapFactory.decodeFile(invoice.signaturePath)
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
        val payments = paymentRepository.getPaymentsForInvoiceSync(invoice.id)
        val totalPaid = payments.sumOf { it.amount }

        val customer = customerRepository.getCustomerByIdSync(invoice.customerId)

        val prefs = context.getSharedPreferences("nitcrm_settings", Context.MODE_PRIVATE)
        val currentCompanyName = prefs.getString("company_name", "NIT CRM") ?: "NIT CRM"
        val currentCompanyAddress = prefs.getString("company_address", "Officer's housing, Pollibidut, Ashulia, Savar, Dhaka") ?: "Officer's housing, Pollibidut, Ashulia, Savar, Dhaka"
        val currentCompanyPhone = prefs.getString("company_phone", "+880 1799-800322") ?: "+880 1799-800322"
        val currentCompanyTaxId = prefs.getString("company_tax_id", "CCTV & Computer Solution") ?: "CCTV & Computer Solution"

        val pdfFile = InvoicePdfGenerator.generateInvoicePdf(
            context = context,
            invoice = invoice,
            items = items,
            signatureBitmap = signatureBitmap,
            companyName = currentCompanyName,
            companyAddress = currentCompanyAddress,
            companyPhone = currentCompanyPhone,
            companyTaxId = currentCompanyTaxId,
            amountPaid = totalPaid,
            customer = customer
        )
        if (pdfFile != null) {
            invoiceRepository.updateInvoicePdfPath(invoice.id, pdfFile.absolutePath, invoice.signaturePath)
        }
        pdfFile
    }

    suspend fun finalizeDraftInvoice(
        context: Context,
        invoice: Invoice,
        signatureBitmap: Bitmap?
    ): Invoice? = withContext(Dispatchers.IO) {
        if (signatureBitmap == null) return@withContext null
        
        // Save Signature to Cache
        var signaturePath: String? = null
        try {
            val cacheFile = File(context.cacheDir, "sig_${invoice.id}.png")
            val out = FileOutputStream(cacheFile)
            signatureBitmap.compress(Bitmap.CompressFormat.PNG, 90, out)
            out.flush()
            out.close()
            signaturePath = cacheFile.absolutePath
        } catch (e: Exception) {
            Log.e("MainViewModel", "Failed to save signature: ${e.message}")
        }

        if (signaturePath == null) return@withContext null

        val finalStatus = "Unpaid"
        invoiceRepository.finalizeInvoice(invoice.id, signaturePath, finalStatus)
        
        // Retrieve and return updated invoice
        invoiceRepository.getInvoiceByIdSync(invoice.id)
    }

    fun finalizeInvoiceAsync(
        context: Context,
        invoice: Invoice,
        signaturePoints: List<androidx.compose.ui.geometry.Offset>,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val sigBitmap = createBitmapFromPoints(signaturePoints)
                val finalized = finalizeDraftInvoice(context, invoice, sigBitmap)
                if (finalized != null) {
                    val file = getOrGenerateInvoicePdf(context, finalized)
                    if (file != null && file.exists()) {
                        onSuccess()
                    } else {
                        onError("Failed to generate official PDF")
                    }
                } else {
                    onError("Failed to finalize invoice")
                }
            } catch (e: Exception) {
                onError(e.message ?: "Unknown error")
            }
        }
    }

    // Payment collection
    fun recordPayment(customerId: String, invoiceId: String, amount: Double, method: String, notes: String) {
        viewModelScope.launch {
            val payment = Payment(
                id = "PAY-" + SimpleDateFormat("yyMMdd", Locale.getDefault()).format(Date()) + "-" + UUID.randomUUID().toString().take(4).uppercase(),
                invoiceId = invoiceId,
                customerId = customerId,
                amount = amount,
                paymentMethod = method,
                notes = notes,
                date = System.currentTimeMillis()
            )
            paymentRepository.recordPayment(payment)

            // Update associated invoice status to "Paid" if amount covers it
            val invoice = invoiceRepository.getInvoiceByIdSync(invoiceId)
            if (invoice != null) {
                val newStatus = if (amount >= invoice.totalAmount) "Paid" else "Partially Paid"
                invoiceRepository.updateInvoice(invoice.copy(status = newStatus))
            }
        }
    }

    fun getPaymentsForInvoice(invoiceId: String): kotlinx.coroutines.flow.Flow<List<Payment>> {
        return paymentRepository.getPaymentsForInvoice(invoiceId)
    }

    fun saveServiceRecord(customerId: String, signatureBitmap: Bitmap?, context: Context, ticketId: String? = null) {
        viewModelScope.launch {
            val recordId = ticketId ?: ("SRV-" + UUID.randomUUID().toString().take(6).uppercase())

            // Save technician signature
            var signaturePath: String? = null
            if (signatureBitmap != null) {
                try {
                    val cacheFile = File(context.cacheDir, "srv_sig_$recordId.png")
                    val out = FileOutputStream(cacheFile)
                    signatureBitmap.compress(Bitmap.CompressFormat.PNG, 90, out)
                    out.flush()
                    out.close()
                    signaturePath = cacheFile.absolutePath
                } catch (e: Exception) {
                    Log.e("MainViewModel", "Failed to save technician signature: ${e.message}")
                }
            }

            var priority = "Medium"
            var existingRecord: ServiceRecord? = null
            if (ticketId != null) {
                existingRecord = serviceRepository.getServiceRecordByIdSync(ticketId)
                if (existingRecord != null) {
                    priority = existingRecord.priority
                }
            }

            val record = ServiceRecord(
                id = recordId,
                customerId = customerId,
                customerName = selectedCustomer?.name ?: "Unknown Customer",
                problem = serviceProblem,
                solution = serviceSolution,
                partsUsed = servicePartsUsed,
                laborCharge = serviceLaborCharge,
                technicianSignaturePath = signaturePath ?: existingRecord?.technicianSignaturePath,
                status = "Completed",
                date = System.currentTimeMillis(),
                priority = priority
            )

            serviceRepository.insertServiceRecord(record)

            // Auto bill labor charge as a due amount if positive and transitioning to Completed
            val transitionedToCompleted = existingRecord == null || !existingRecord.status.equals("Completed", ignoreCase = true)
            if (transitionedToCompleted && serviceLaborCharge > 0.0) {
                val customer = customerRepository.getCustomerByIdSync(customerId)
                if (customer != null) {
                    val newDue = customer.dueAmount + serviceLaborCharge
                    customerRepository.updateCustomer(customer.copy(dueAmount = newDue))
                }
            }

            // Reset fields
            serviceProblem = ""
            serviceSolution = ""
            servicePartsUsed = ""
            serviceLaborCharge = 0.0
        }
    }

    // Installation Record logic
    fun saveInstallationRecord(customerId: String, signatureBitmap: Bitmap?, context: Context) {
        viewModelScope.launch {
            val recordId = "INST-" + UUID.randomUUID().toString().take(6).uppercase()

            // Save customer signature
            var signaturePath: String? = null
            if (signatureBitmap != null) {
                try {
                    val cacheFile = File(context.cacheDir, "inst_sig_$recordId.png")
                    val out = FileOutputStream(cacheFile)
                    signatureBitmap.compress(Bitmap.CompressFormat.PNG, 90, out)
                    out.flush()
                    out.close()
                    signaturePath = cacheFile.absolutePath
                } catch (e: Exception) {
                    Log.e("MainViewModel", "Failed to save customer signature: ${e.message}")
                }
            }

            val record = InstallationRecord(
                id = recordId,
                customerId = customerId,
                customerName = selectedCustomer?.name ?: "Unknown Customer",
                installedProducts = installProductsSummary,
                notes = installNotes,
                customerSignaturePath = signaturePath,
                date = System.currentTimeMillis()
            )

            serviceRepository.insertInstallationRecord(record)

            // Reset fields
            installProductsSummary = ""
            installNotes = ""
        }
    }

    // Background Manual REST Synchronization Simulation
    fun triggerManualSync() {
        viewModelScope.launch {
            if (isSyncing) return@launch
            isSyncing = true
            // Simulate net latency
            delay(2000)
            syncRepository.syncAllPending()
            isSyncing = false
        }
    }
}
