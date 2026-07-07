package com.nit.crm.data

import com.nit.crm.core.database.*
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private val repositoryScope = CoroutineScope(Dispatchers.IO)

class UserRepository(private val userDao: UserDao) {
    val currentUser: Flow<User?> = userDao.getCurrentUser()

    suspend fun login(name: String, email: String, role: String) {
        userDao.clearAll()
        userDao.insertUser(
            User(
                id = UUID.randomUUID().toString(),
                name = name,
                email = email,
                role = role,
                isLoggedIn = true
            )
        )
    }

    suspend fun logout() {
        userDao.logoutAll()
    }
}

class CustomerRepository(
    private val customerDao: CustomerDao,
    private val syncQueueDao: SyncQueueDao,
    private val activityLogDao: ActivityLogDao
) {
    val allCustomers: Flow<List<Customer>> = customerDao.getAllCustomers()

    fun getCustomerById(id: String): Flow<Customer?> = customerDao.getCustomerById(id)

    suspend fun getCustomerByIdSync(id: String): Customer? = customerDao.getCustomerByIdSync(id)

    suspend fun findCustomerByPhoneOrEmail(phone: String, email: String): Customer? =
        customerDao.findCustomerByPhoneOrEmail(phone, email)

    fun searchCustomers(query: String): Flow<List<Customer>> {
        return if (query.isBlank()) {
            customerDao.getAllCustomers()
        } else {
            customerDao.searchCustomers(query)
        }
    }

    suspend fun insertCustomer(customer: Customer) {
        customerDao.insertCustomer(customer)
        syncQueueDao.insertSyncItem(
            SyncQueue(
                entityType = "Customer",
                entityId = customer.id,
                actionType = "INSERT"
            )
        )
        logActivity(
            customer.id,
            "CustomerCreated",
            "Client profile created (Stage: ${customer.leadStatus}, Priority: ${customer.priority})"
        )
        repositoryScope.launch {
            try {
                FirebaseSyncHelper.syncCustomer(customer)
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    suspend fun updateCustomer(customer: Customer) {
        val old = customerDao.getCustomerByIdSync(customer.id)
        customerDao.updateCustomer(customer)
        syncQueueDao.insertSyncItem(
            SyncQueue(
                entityType = "Customer",
                entityId = customer.id,
                actionType = "UPDATE"
            )
        )
        if (old != null && old.leadStatus != customer.leadStatus) {
            logActivity(
                customer.id,
                "PipelineUpdate",
                "Deal stage updated from ${old.leadStatus} to ${customer.leadStatus} (Value: ৳${String.format(java.util.Locale.US, "%.0f", customer.dealValue)})"
            )
        } else {
            logActivity(customer.id, "CustomerUpdated", "Profile information updated")
        }
        repositoryScope.launch {
            try {
                FirebaseSyncHelper.syncCustomer(customer)
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    suspend fun deleteCustomerById(id: String) {
        customerDao.deleteCustomerById(id)
        activityLogDao.deleteActivityLogsForCustomer(id)
    }

    fun getActivityLogsForCustomer(customerId: String): Flow<List<ActivityLog>> =
        activityLogDao.getActivityLogsForCustomer(customerId)

    suspend fun logActivity(customerId: String, type: String, details: String) {
        activityLogDao.insertActivityLog(
            ActivityLog(
                customerId = customerId,
                type = type,
                details = details
            )
        )
    }
}

class ProductRepository(private val productDao: ProductDao) {
    val allProducts: Flow<List<Product>> = productDao.getAllProducts()

    suspend fun getProductById(id: String): Product? = productDao.getProductById(id)

    suspend fun getProductBySku(sku: String): Product? = productDao.getProductBySku(sku)

    fun searchProducts(query: String): Flow<List<Product>> {
        return if (query.isBlank()) {
            productDao.getAllProducts()
        } else {
            productDao.searchProducts(query)
        }
    }

    suspend fun insertProduct(product: Product) {
        productDao.insertProduct(product)
    }

    suspend fun deleteProductById(id: String) {
        productDao.deleteProductById(id)
    }
}

class InvoiceRepository(
    private val invoiceDao: InvoiceDao,
    private val invoiceItemDao: InvoiceItemDao,
    private val syncQueueDao: SyncQueueDao,
    private val customerDao: CustomerDao,
    private val activityLogDao: ActivityLogDao
) {
    val allInvoices: Flow<List<Invoice>> = invoiceDao.getAllInvoices()

    fun getInvoiceById(id: String): Flow<Invoice?> = invoiceDao.getInvoiceById(id)

    suspend fun getInvoiceByIdSync(id: String): Invoice? = invoiceDao.getInvoiceByIdSync(id)

    suspend fun updateInvoice(invoice: Invoice) = invoiceDao.updateInvoice(invoice)

    fun getItemsForInvoice(invoiceId: String): Flow<List<InvoiceItem>> =
        invoiceItemDao.getItemsForInvoice(invoiceId)

    suspend fun getItemsForInvoiceSync(invoiceId: String): List<InvoiceItem> =
        invoiceItemDao.getItemsForInvoiceSync(invoiceId)

    fun getInvoicesForCustomer(customerId: String): Flow<List<Invoice>> =
        invoiceDao.getInvoicesForCustomer(customerId)

    fun searchInvoices(query: String): Flow<List<Invoice>> {
        return if (query.isBlank()) {
            invoiceDao.getAllInvoices()
        } else {
            invoiceDao.searchInvoices(query)
        }
    }

    suspend fun createInvoice(invoice: Invoice, items: List<InvoiceItem>) {
        // 1. Insert invoice and its items
        invoiceDao.insertInvoice(invoice)
        invoiceItemDao.insertInvoiceItems(items)

        // 2. Add item to synchronization queue
        syncQueueDao.insertSyncItem(
            SyncQueue(
                entityType = "Invoice",
                entityId = invoice.id,
                actionType = "INSERT"
            )
        )

        // 3. Log activity
        val label = if (invoice.isQuotation) "Quotation" else "Invoice"
        val logType = if (invoice.isQuotation) "Quotation" else "Invoice"
        val displayNo = invoice.id.substringAfterLast("-", invoice.id)
        activityLogDao.insertActivityLog(
            ActivityLog(
                customerId = invoice.customerId,
                type = logType,
                details = "$label created: #$displayNo (Total: ৳${String.format(java.util.Locale.US, "%.2f", invoice.totalAmount)})"
            )
        )

        // 4. Update customer's due balance (add invoice total if not a quote and not a draft)
        if (!invoice.isQuotation && invoice.status != "Draft") {
            val customer = customerDao.getCustomerByIdSync(invoice.customerId)
            if (customer != null) {
                val newDue = customer.dueAmount + invoice.totalAmount
                customerDao.updateCustomer(customer.copy(dueAmount = newDue))
            }
        }

        // Try immediate Firebase sync in background
        repositoryScope.launch {
            try {
                val success = FirebaseSyncHelper.syncInvoice(invoice, items)
                if (success) {
                    invoiceDao.updateInvoice(invoice.copy(isSynced = true))
                }
            } catch (e: Exception) {
                // ignore fallback
            }
        }
    }

    suspend fun deleteInvoice(invoice: Invoice) {
        // 1. Delete invoice items
        invoiceItemDao.deleteItemsForInvoice(invoice.id)

        // 2. Delete invoice
        invoiceDao.deleteInvoice(invoice)

        // 3. Log activity
        val label = if (invoice.isQuotation) "Quotation" else "Invoice"
        val displayNo = invoice.id.substringAfterLast("-", invoice.id)
        activityLogDao.insertActivityLog(
            ActivityLog(
                customerId = invoice.customerId,
                type = "CustomerUpdated",
                details = "$label deleted: #$displayNo"
            )
        )

        // 4. Subtract invoice total from customer's due balance (if not quote and not draft)
        if (!invoice.isQuotation && invoice.status != "Draft") {
            val customer = customerDao.getCustomerByIdSync(invoice.customerId)
            if (customer != null) {
                val newDue = (customer.dueAmount - invoice.totalAmount).coerceAtLeast(0.0)
                customerDao.updateCustomer(customer.copy(dueAmount = newDue))
            }
        }

        // 5. Add delete action to sync queue
        syncQueueDao.insertSyncItem(
            SyncQueue(
                entityType = "Invoice",
                entityId = invoice.id,
                actionType = "DELETE"
            )
        )

        // Try immediate Firebase delete in background
        repositoryScope.launch {
            try {
                FirebaseSyncHelper.deleteInvoice(invoice.id)
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    suspend fun updateInvoicePdfPath(invoiceId: String, pdfPath: String, signaturePath: String?) {
        val invoice = invoiceDao.getInvoiceByIdSync(invoiceId)
        if (invoice != null) {
            val updated = invoice.copy(
                pdfPath = pdfPath,
                signaturePath = signaturePath ?: invoice.signaturePath
            )
            invoiceDao.updateInvoice(updated)
            
            syncQueueDao.insertSyncItem(
                SyncQueue(
                    entityType = "Invoice",
                    entityId = invoiceId,
                    actionType = "UPDATE"
                )
            )

            // Try immediate Firebase sync in background
            repositoryScope.launch {
                try {
                    val items = invoiceItemDao.getItemsForInvoiceSync(invoiceId)
                    val success = FirebaseSyncHelper.syncInvoice(updated, items)
                    if (success) {
                        invoiceDao.updateInvoice(updated.copy(isSynced = true))
                    }
                } catch (e: Exception) {
                    // ignore
                }
            }
        }
    }

    suspend fun finalizeInvoice(invoiceId: String, signaturePath: String, status: String) {
        val invoice = invoiceDao.getInvoiceByIdSync(invoiceId) ?: return
        val updated = invoice.copy(
            status = status,
            signaturePath = signaturePath,
            pdfPath = null,
            isSynced = false
        )
        invoiceDao.updateInvoice(updated)

        // Update customer's due balance now that it is finalized
        val customer = customerDao.getCustomerByIdSync(invoice.customerId)
        if (customer != null) {
            val newDue = customer.dueAmount + invoice.totalAmount
            customerDao.updateCustomer(customer.copy(dueAmount = newDue))
        }

        // Insert UPDATE to synchronization queue
        syncQueueDao.insertSyncItem(
            SyncQueue(
                entityType = "Invoice",
                entityId = invoiceId,
                actionType = "UPDATE"
            )
        )

        // Try immediate Firebase sync in background
        val items = invoiceItemDao.getItemsForInvoiceSync(invoiceId)
        repositoryScope.launch {
            try {
                val success = FirebaseSyncHelper.syncInvoice(updated, items)
                if (success) {
                    invoiceDao.updateInvoice(updated.copy(isSynced = true))
                }
            } catch (e: Exception) {
                // ignore
            }
        }
    }
}

class PaymentRepository(
    private val paymentDao: PaymentDao,
    private val customerDao: CustomerDao,
    private val syncQueueDao: SyncQueueDao,
    private val invoiceDao: InvoiceDao,
    private val invoiceItemDao: InvoiceItemDao,
    private val activityLogDao: ActivityLogDao
) {
    val allPayments: Flow<List<Payment>> = paymentDao.getAllPayments()

    fun getPaymentsForCustomer(customerId: String): Flow<List<Payment>> =
        paymentDao.getPaymentsForCustomer(customerId)

    fun getPaymentsForInvoice(invoiceId: String): Flow<List<Payment>> =
        paymentDao.getPaymentsForInvoice(invoiceId)

    suspend fun getPaymentsForInvoiceSync(invoiceId: String): List<Payment> =
        paymentDao.getPaymentsForInvoiceSync(invoiceId)

    suspend fun recordPayment(payment: Payment) {
        // 1. Insert payment record
        paymentDao.insertPayment(payment)

        // 2. Add to synchronization queue
        syncQueueDao.insertSyncItem(
            SyncQueue(
                entityType = "Payment",
                entityId = payment.id,
                actionType = "INSERT"
            )
        )

        // 3. Log activity
        val displayInvoice = if (payment.invoiceId.isNullOrBlank() || payment.invoiceId == "MANUAL") {
            "Account Credit"
        } else {
            "Invoice #${payment.invoiceId.substringAfterLast("-", payment.invoiceId)}"
        }
        activityLogDao.insertActivityLog(
            ActivityLog(
                customerId = payment.customerId,
                type = "Payment",
                details = "Payment recorded via ${payment.paymentMethod}: ৳${String.format(java.util.Locale.US, "%.2f", payment.amount)} ($displayInvoice)"
            )
        )

        // 4. Subtract payment amount from customer's due balance
        val customer = customerDao.getCustomerByIdSync(payment.customerId)
        if (customer != null) {
            val newDue = (customer.dueAmount - payment.amount).coerceAtLeast(0.0)
            customerDao.updateCustomer(customer.copy(dueAmount = newDue))
        }

        // 4. Update corresponding Invoice status if available
        if (!payment.invoiceId.isNullOrBlank()) {
            val invoice = invoiceDao.getInvoiceByIdSync(payment.invoiceId)
            if (invoice != null) {
                val allPaymentsForInvoice = paymentDao.getPaymentsForInvoiceSync(invoice.id)
                val totalPaid = allPaymentsForInvoice.sumOf { it.amount }
                val newStatus = when {
                    totalPaid >= invoice.totalAmount -> "Paid"
                    totalPaid > 0.0 -> "Partially Paid"
                    else -> "Unpaid"
                }
                val updatedInvoice = invoice.copy(status = newStatus)
                invoiceDao.updateInvoice(updatedInvoice)
                
                // Sync the updated invoice state
                repositoryScope.launch {
                    try {
                        val items = invoiceItemDao.getItemsForInvoiceSync(invoice.id)
                        FirebaseSyncHelper.syncInvoice(updatedInvoice, items)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }

        // Try immediate Firebase sync in background
        repositoryScope.launch {
            try {
                val success = FirebaseSyncHelper.syncPayment(payment)
                if (success) {
                    paymentDao.insertPayment(payment.copy(isSynced = true))
                }
            } catch (e: Exception) {
                // ignore
            }
        }
    }
}

class ServiceRecordRepository(
    private val serviceRecordDao: ServiceRecordDao,
    private val installationRecordDao: InstallationRecordDao,
    private val syncQueueDao: SyncQueueDao,
    private val activityLogDao: ActivityLogDao
) {
    val allServiceRecords: Flow<List<ServiceRecord>> = serviceRecordDao.getAllServiceRecords()
    val allInstallationRecords: Flow<List<InstallationRecord>> = installationRecordDao.getAllInstallationRecords()

    fun getServiceRecordsForCustomer(customerId: String): Flow<List<ServiceRecord>> =
        serviceRecordDao.getServiceRecordsForCustomer(customerId)

    fun getInstallationRecordsForCustomer(customerId: String): Flow<List<InstallationRecord>> =
        installationRecordDao.getInstallationRecordsForCustomer(customerId)

    suspend fun getServiceRecordByIdSync(id: String): ServiceRecord? =
        serviceRecordDao.getServiceRecordByIdSync(id)

    suspend fun insertServiceRecord(record: ServiceRecord) {
        serviceRecordDao.insertServiceRecord(record)
        syncQueueDao.insertSyncItem(
            SyncQueue(
                entityType = "Service",
                entityId = record.id,
                actionType = "INSERT"
            )
        )

        // Log activity
        val displayId = record.id.substringAfterLast("-", record.id)
        activityLogDao.insertActivityLog(
            ActivityLog(
                customerId = record.customerId,
                type = "ServiceTicket",
                details = "Service Ticket completed: #$displayId (Labor: ৳${String.format(java.util.Locale.US, "%.2f", record.laborCharge)}). Problem: ${record.problem}"
            )
        )

        // Try immediate Firebase sync in background
        repositoryScope.launch {
            try {
                val success = FirebaseSyncHelper.syncServiceRecord(record)
                if (success) {
                    serviceRecordDao.insertServiceRecord(record.copy(isSynced = true))
                }
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    suspend fun insertInstallationRecord(record: InstallationRecord) {
        installationRecordDao.insertInstallationRecord(record)
        syncQueueDao.insertSyncItem(
            SyncQueue(
                entityType = "Installation",
                entityId = record.id,
                actionType = "INSERT"
            )
        )

        // Log activity
        val displayId = record.id.substringAfterLast("-", record.id)
        activityLogDao.insertActivityLog(
            ActivityLog(
                customerId = record.customerId,
                type = "ServiceTicket",
                details = "Installation completed: #$displayId. Products: ${record.installedProducts}"
            )
        )
        // Try immediate Firebase sync in background
        repositoryScope.launch {
            try {
                val success = FirebaseSyncHelper.syncInstallationRecord(record)
                if (success) {
                    installationRecordDao.insertInstallationRecord(record.copy(isSynced = true))
                }
            } catch (e: Exception) {
                // ignore
            }
        }
    }
}

class SyncRepository(
    private val syncQueueDao: SyncQueueDao,
    private val customerDao: CustomerDao,
    private val invoiceDao: InvoiceDao,
    private val paymentDao: PaymentDao,
    private val serviceRecordDao: ServiceRecordDao,
    private val installationRecordDao: InstallationRecordDao,
    private val invoiceItemDao: InvoiceItemDao
) {
    val pendingSyncItems: Flow<List<SyncQueue>> = syncQueueDao.getPendingSyncItems()

    suspend fun syncAllPending() {
        val pendingItems = syncQueueDao.getPendingSyncItemsSync()
        for (item in pendingItems) {
            var success = false
            when (item.entityType) {
                "Customer" -> {
                    val customer = customerDao.getCustomerByIdSync(item.entityId)
                    if (customer != null) {
                        success = FirebaseSyncHelper.syncCustomer(customer)
                    } else {
                        success = true // Entity doesn't exist locally, skip to avoid blocking queue
                    }
                }
                "Invoice" -> {
                    if (item.actionType == "DELETE") {
                        success = FirebaseSyncHelper.deleteInvoice(item.entityId)
                    } else {
                        val invoice = invoiceDao.getInvoiceByIdSync(item.entityId)
                        if (invoice != null) {
                            val items = invoiceItemDao.getItemsForInvoiceSync(item.entityId)
                            success = FirebaseSyncHelper.syncInvoice(invoice, items)
                            if (success) {
                                invoiceDao.updateInvoice(invoice.copy(isSynced = true))
                            }
                        } else {
                            success = true
                        }
                    }
                }
                "Payment" -> {
                    val payment = paymentDao.getPaymentByIdSync(item.entityId)
                    if (payment != null) {
                        success = FirebaseSyncHelper.syncPayment(payment)
                        if (success) {
                            paymentDao.insertPayment(payment.copy(isSynced = true))
                        }
                    } else {
                        success = true
                    }
                }
                "Service" -> {
                    val record = serviceRecordDao.getServiceRecordByIdSync(item.entityId)
                    if (record != null) {
                        success = FirebaseSyncHelper.syncServiceRecord(record)
                        if (success) {
                            serviceRecordDao.insertServiceRecord(record.copy(isSynced = true))
                        }
                    } else {
                        success = true
                    }
                }
                "Installation" -> {
                    val record = installationRecordDao.getInstallationRecordByIdSync(item.entityId)
                    if (record != null) {
                        success = FirebaseSyncHelper.syncInstallationRecord(record)
                        if (success) {
                            installationRecordDao.insertInstallationRecord(record.copy(isSynced = true))
                        }
                    } else {
                        success = true
                    }
                }
            }
            if (success) {
                // Mark as done in sync queue
                syncQueueDao.markAsSynced(item.id)
            }
        }
    }
}
