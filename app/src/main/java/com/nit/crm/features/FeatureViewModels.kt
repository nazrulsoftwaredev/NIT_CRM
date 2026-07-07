package com.nit.crm.features

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nit.crm.core.database.*
import com.nit.crm.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

// 1. LoginViewModel
class LoginViewModel(
    private val userRepository: UserRepository
) : ViewModel() {
    fun login(name: String, email: String, role: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            userRepository.login(name, email, role)
            onComplete()
        }
    }
}

// 2. DashboardViewModel
class DashboardViewModel(
    private val userRepository: UserRepository,
    private val syncRepository: SyncRepository,
    private val invoiceRepository: InvoiceRepository,
    private val paymentRepository: PaymentRepository,
    private val customerRepository: CustomerRepository
) : ViewModel() {
    val currentUser = userRepository.currentUser.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val pendingSyncItems = syncRepository.pendingSyncItems.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allInvoices = invoiceRepository.allInvoices.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allPayments = paymentRepository.allPayments.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val customers = customerRepository.allCustomers.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    var isSyncing by mutableStateOf(false)
        private set

    fun triggerManualSync() {
        viewModelScope.launch {
            if (isSyncing) return@launch
            isSyncing = true
            delay(2000)
            syncRepository.syncAllPending()
            isSyncing = false
        }
    }

    fun logout(onComplete: () -> Unit) {
        viewModelScope.launch {
            userRepository.logout()
            onComplete()
        }
    }
}

// 3. CustomersViewModel
class CustomersViewModel(
    private val customerRepository: CustomerRepository,
    private val invoiceRepository: InvoiceRepository,
    private val paymentRepository: PaymentRepository,
    private val serviceRepository: ServiceRecordRepository
) : ViewModel() {
    var activeStageFilter by mutableStateOf("Lead")

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

    fun setCustomerSearchQuery(query: String) {
        _customerSearchQuery.value = query
    }

    fun getCustomerById(customerId: String): StateFlow<Customer?> {
        return customerRepository.getCustomerById(customerId).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )
    }

    fun saveCustomer(
        id: String?,
        name: String,
        phone: String,
        email: String,
        address: String,
        leadStatus: String,
        dealValue: Double,
        expectedCloseDate: Long = 0L,
        priority: String = "Medium",
        lostReason: String = "",
        dealNotes: String = ""
    ) {
        viewModelScope.launch {
            if (id != null) {
                val existing = customerRepository.getCustomerByIdSync(id)
                if (existing != null) {
                    val updated = existing.copy(
                        name = name,
                        phone = phone,
                        email = email,
                        address = address,
                        leadStatus = leadStatus,
                        dealValue = dealValue,
                        expectedCloseDate = expectedCloseDate,
                        priority = priority,
                        lostReason = lostReason,
                        dealNotes = dealNotes
                    )
                    customerRepository.updateCustomer(updated)
                }
            } else {
                val customer = Customer(
                    id = "cust_" + UUID.randomUUID().toString().take(6).uppercase(),
                    name = name,
                    phone = phone,
                    email = email,
                    address = address,
                    dueAmount = 0.0,
                    leadStatus = leadStatus,
                    dealValue = dealValue,
                    expectedCloseDate = expectedCloseDate,
                    priority = priority,
                    lostReason = lostReason,
                    dealNotes = dealNotes
                )
                customerRepository.insertCustomer(customer)
            }
        }
    }

    fun updateCustomerLeadStatus(customerId: String, newStatus: String) {
        viewModelScope.launch {
            val existing = customerRepository.getCustomerByIdSync(customerId)
            if (existing != null) {
                val updated = existing.copy(leadStatus = newStatus)
                customerRepository.updateCustomer(updated)
            }
        }
    }

    fun updateCustomerDealPipeline(
        customerId: String,
        leadStatus: String,
        dealValue: Double,
        expectedCloseDate: Long,
        priority: String,
        lostReason: String,
        dealNotes: String
    ) {
        viewModelScope.launch {
            val existing = customerRepository.getCustomerByIdSync(customerId)
            if (existing != null) {
                val updated = existing.copy(
                    leadStatus = leadStatus,
                    dealValue = dealValue,
                    expectedCloseDate = expectedCloseDate,
                    priority = priority,
                    lostReason = lostReason,
                    dealNotes = dealNotes
                )
                customerRepository.updateCustomer(updated)
            }
        }
    }

    fun deleteCustomer(id: String) {
        viewModelScope.launch {
            customerRepository.deleteCustomerById(id)
        }
    }

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

    fun getActivityLogsForCustomer(customerId: String): StateFlow<List<ActivityLog>> {
        return customerRepository.getActivityLogsForCustomer(customerId).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }
}

// 4. ProductsViewModel
class ProductsViewModel(
    private val productRepository: ProductRepository
) : ViewModel() {
    var draftName by mutableStateOf("")
    var draftSku by mutableStateOf("")
    var draftPrice by mutableStateOf("")
    var draftCategory by mutableStateOf("")
    var draftStock by mutableStateOf("")
    var draftPurchasePrice by mutableStateOf("")
    var draftWarranty by mutableStateOf("")
    var selectedCategoryTab by mutableStateOf("All")

    fun clearDrafts() {
        draftName = ""
        draftSku = ""
        draftPrice = ""
        draftCategory = ""
        draftStock = ""
        draftPurchasePrice = ""
        draftWarranty = ""
    }

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

    fun setProductSearchQuery(query: String) {
        _productSearchQuery.value = query
    }

    fun saveProduct(id: String?, name: String, sku: String, price: Double, category: String, stock: Int, purchasePrice: Double, warranty: String) {
        viewModelScope.launch {
            val product = Product(
                id = id ?: UUID.randomUUID().toString().take(8),
                sku = sku,
                name = name,
                price = price,
                category = category,
                stockQuantity = stock,
                purchasePrice = purchasePrice,
                warranty = warranty
            )
            productRepository.insertProduct(product)
        }
    }

    fun deleteProduct(id: String) {
        viewModelScope.launch {
            productRepository.deleteProductById(id)
        }
    }
}

// 5. InvoicesViewModel
class InvoicesViewModel(
    private val invoiceRepository: InvoiceRepository
) : ViewModel() {
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

    fun setInvoiceSearchQuery(query: String) {
        _invoiceSearchQuery.value = query
    }

    fun getItemsForInvoice(invoiceId: String): StateFlow<List<InvoiceItem>> {
        return invoiceRepository.getItemsForInvoice(invoiceId).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    fun deleteInvoice(invoice: Invoice) {
        viewModelScope.launch {
            invoiceRepository.deleteInvoice(invoice)
        }
    }
}

// 6. PaymentAndServiceViewModel
class PaymentAndServiceViewModel(
    private val paymentRepository: PaymentRepository,
    private val serviceRepository: ServiceRecordRepository,
    private val customerRepository: CustomerRepository,
    private val invoiceRepository: InvoiceRepository
) : ViewModel() {
    
    // Active Service fields
    var serviceProblem by mutableStateOf("")
    var serviceSolution by mutableStateOf("")
    var servicePartsUsed by mutableStateOf("")
    var serviceLaborCharge by mutableStateOf(0.0)

    val allServiceRecords = serviceRepository.allServiceRecords.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun saveServiceTicket(record: ServiceRecord) {
        viewModelScope.launch {
            val previous = serviceRepository.getServiceRecordByIdSync(record.id)
            val transitionedToCompleted = record.status.equals("Completed", ignoreCase = true) &&
                    (previous == null || !previous.status.equals("Completed", ignoreCase = true))

            serviceRepository.insertServiceRecord(record)

            if (transitionedToCompleted && record.laborCharge > 0.0) {
                val customer = customerRepository.getCustomerByIdSync(record.customerId)
                if (customer != null) {
                    val newDue = customer.dueAmount + record.laborCharge
                    customerRepository.updateCustomer(customer.copy(dueAmount = newDue))
                }
            }
        }
    }

    // Active Installation fields
    var installProductsSummary by mutableStateOf("")
    var installNotes by mutableStateOf("")

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

            val invoice = invoiceRepository.getInvoiceByIdSync(invoiceId)
            if (invoice != null) {
                val newStatus = if (amount >= invoice.totalAmount) "Paid" else "Partially Paid"
                invoiceRepository.updateInvoice(invoice.copy(status = newStatus))
            }
        }
    }

    fun saveServiceRecord(customerId: String, customerName: String, signatureBitmap: Bitmap?, context: Context) {
        viewModelScope.launch {
            val recordId = "SRV-" + UUID.randomUUID().toString().take(6).uppercase()

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
                    android.util.Log.e("PaymentAndServiceVM", "Failed to save signature: ${e.message}")
                }
            }

            val record = ServiceRecord(
                id = recordId,
                customerId = customerId,
                customerName = customerName,
                problem = serviceProblem,
                solution = serviceSolution,
                partsUsed = servicePartsUsed,
                laborCharge = serviceLaborCharge,
                technicianSignaturePath = signaturePath,
                status = "Completed",
                date = System.currentTimeMillis()
            )

            serviceRepository.insertServiceRecord(record)

            if (serviceLaborCharge > 0.0) {
                val customer = customerRepository.getCustomerByIdSync(customerId)
                if (customer != null) {
                    val newDue = customer.dueAmount + serviceLaborCharge
                    customerRepository.updateCustomer(customer.copy(dueAmount = newDue))
                }
            }

            serviceProblem = ""
            serviceSolution = ""
            servicePartsUsed = ""
            serviceLaborCharge = 0.0
        }
    }

    fun saveInstallationRecord(customerId: String, customerName: String, signatureBitmap: Bitmap?, context: Context) {
        viewModelScope.launch {
            val recordId = "INST-" + UUID.randomUUID().toString().take(6).uppercase()

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
                    android.util.Log.e("PaymentAndServiceVM", "Failed to save signature: ${e.message}")
                }
            }

            val record = InstallationRecord(
                id = recordId,
                customerId = customerId,
                customerName = customerName,
                installedProducts = installProductsSummary,
                notes = installNotes,
                customerSignaturePath = signaturePath,
                date = System.currentTimeMillis()
            )

            serviceRepository.insertInstallationRecord(record)

            installProductsSummary = ""
            installNotes = ""
        }
    }
}

// 7. SettingsViewModel
class SettingsViewModel(application: android.app.Application) : androidx.lifecycle.AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("nitcrm_settings", android.content.Context.MODE_PRIVATE)

    var companyName by mutableStateOf(prefs.getString("company_name", "NIT CRM") ?: "NIT CRM")
    var companyAddress by mutableStateOf(prefs.getString("company_address", "Officer's housing, Pollibidut, Ashulia, Savar, Dhaka") ?: "Officer's housing, Pollibidut, Ashulia, Savar, Dhaka")
    var companyPhone by mutableStateOf(prefs.getString("company_phone", "+880 1799-800322") ?: "+880 1799-800322")
    var companyTaxId by mutableStateOf(prefs.getString("company_tax_id", "CCTV & Computer Solution") ?: "CCTV & Computer Solution")
    var companyLogoBase64 by mutableStateOf(prefs.getString("company_logo_base64", "") ?: "")
    var companySignatureBase64 by mutableStateOf(prefs.getString("company_signature_base64", "") ?: "")

    fun saveSettings(
        name: String,
        address: String,
        phone: String,
        taxId: String,
        logoBase64: String = companyLogoBase64,
        signatureBase64: String = companySignatureBase64
    ) {
        companyName = name
        companyAddress = address
        companyPhone = phone
        companyTaxId = taxId
        companyLogoBase64 = logoBase64
        companySignatureBase64 = signatureBase64
        prefs.edit()
            .putString("company_name", name)
            .putString("company_address", address)
            .putString("company_phone", phone)
            .putString("company_tax_id", taxId)
            .putString("company_logo_base64", logoBase64)
            .putString("company_signature_base64", signatureBase64)
            .apply()
    }
}
