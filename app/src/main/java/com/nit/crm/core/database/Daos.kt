package com.nit.crm.core.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users LIMIT 1")
    fun getCurrentUser(): Flow<User?>

    @Query("SELECT * FROM users LIMIT 1")
    suspend fun getCurrentUserSync(): User?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    @Query("UPDATE users SET isLoggedIn = 0")
    suspend fun logoutAll()

    @Query("DELETE FROM users")
    suspend fun clearAll()
}

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers ORDER BY name ASC")
    fun getAllCustomers(): Flow<List<Customer>>

    @Query("SELECT * FROM customers WHERE id = :id")
    fun getCustomerById(id: String): Flow<Customer?>

    @Query("SELECT * FROM customers WHERE id = :id")
    suspend fun getCustomerByIdSync(id: String): Customer?

    @Query("SELECT * FROM customers WHERE name LIKE '%' || :query || '%' OR phone LIKE '%' || :query || '%' OR email LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchCustomers(query: String): Flow<List<Customer>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: Customer)

    @Update
    suspend fun updateCustomer(customer: Customer)

    @Query("DELETE FROM customers WHERE id = :id")
    suspend fun deleteCustomerById(id: String)

    @Query("SELECT * FROM customers WHERE (phone = :phone AND :phone != '') OR (email = :email AND :email != '') LIMIT 1")
    suspend fun findCustomerByPhoneOrEmail(phone: String, email: String): Customer?
}

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY name ASC")
    fun getAllProducts(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getProductById(id: String): Product?

    @Query("SELECT * FROM products WHERE sku = :sku LIMIT 1")
    suspend fun getProductBySku(sku: String): Product?

    @Query("SELECT * FROM products WHERE name LIKE '%' || :query || '%' OR sku LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchProducts(query: String): Flow<List<Product>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<Product>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: Product)

    @Query("DELETE FROM products WHERE id = :id")
    suspend fun deleteProductById(id: String)
}

@Dao
interface InvoiceDao {
    @Query("SELECT * FROM invoices ORDER BY date DESC")
    fun getAllInvoices(): Flow<List<Invoice>>

    @Query("SELECT * FROM invoices WHERE id = :id")
    fun getInvoiceById(id: String): Flow<Invoice?>

    @Query("SELECT * FROM invoices WHERE id = :id")
    suspend fun getInvoiceByIdSync(id: String): Invoice?

    @Query("SELECT * FROM invoices WHERE customerId = :customerId ORDER BY date DESC")
    fun getInvoicesForCustomer(customerId: String): Flow<List<Invoice>>

    @Query("SELECT * FROM invoices WHERE customerName LIKE '%' || :query || '%' OR id LIKE '%' || :query || '%' ORDER BY date DESC")
    fun searchInvoices(query: String): Flow<List<Invoice>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoice(invoice: Invoice)

    @Update
    suspend fun updateInvoice(invoice: Invoice)

    @Delete
    suspend fun deleteInvoice(invoice: Invoice)
}

@Dao
interface InvoiceItemDao {
    @Query("SELECT * FROM invoice_items WHERE invoiceId = :invoiceId")
    fun getItemsForInvoice(invoiceId: String): Flow<List<InvoiceItem>>

    @Query("SELECT * FROM invoice_items WHERE invoiceId = :invoiceId")
    suspend fun getItemsForInvoiceSync(invoiceId: String): List<InvoiceItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoiceItems(items: List<InvoiceItem>)

    @Query("DELETE FROM invoice_items WHERE invoiceId = :invoiceId")
    suspend fun deleteItemsForInvoice(invoiceId: String)
}

@Dao
interface PaymentDao {
    @Query("SELECT * FROM payments ORDER BY date DESC")
    fun getAllPayments(): Flow<List<Payment>>

    @Query("SELECT * FROM payments WHERE customerId = :customerId ORDER BY date DESC")
    fun getPaymentsForCustomer(customerId: String): Flow<List<Payment>>

    @Query("SELECT * FROM payments WHERE invoiceId = :invoiceId ORDER BY date DESC")
    fun getPaymentsForInvoice(invoiceId: String): Flow<List<Payment>>

    @Query("SELECT * FROM payments WHERE invoiceId = :invoiceId")
    suspend fun getPaymentsForInvoiceSync(invoiceId: String): List<Payment>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: Payment)

    @Query("SELECT * FROM payments WHERE id = :id LIMIT 1")
    suspend fun getPaymentByIdSync(id: String): Payment?
}

@Dao
interface ServiceRecordDao {
    @Query("SELECT * FROM service_records ORDER BY date DESC")
    fun getAllServiceRecords(): Flow<List<ServiceRecord>>

    @Query("SELECT * FROM service_records WHERE customerId = :customerId ORDER BY date DESC")
    fun getServiceRecordsForCustomer(customerId: String): Flow<List<ServiceRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServiceRecord(record: ServiceRecord)

    @Query("SELECT * FROM service_records WHERE id = :id LIMIT 1")
    suspend fun getServiceRecordByIdSync(id: String): ServiceRecord?
}

@Dao
interface InstallationRecordDao {
    @Query("SELECT * FROM installation_records ORDER BY date DESC")
    fun getAllInstallationRecords(): Flow<List<InstallationRecord>>

    @Query("SELECT * FROM installation_records WHERE customerId = :customerId ORDER BY date DESC")
    fun getInstallationRecordsForCustomer(customerId: String): Flow<List<InstallationRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInstallationRecord(record: InstallationRecord)

    @Query("SELECT * FROM installation_records WHERE id = :id LIMIT 1")
    suspend fun getInstallationRecordByIdSync(id: String): InstallationRecord?
}

@Dao
interface SyncQueueDao {
    @Query("SELECT * FROM sync_queue WHERE isPending = 1 ORDER BY timestamp ASC")
    fun getPendingSyncItems(): Flow<List<SyncQueue>>

    @Query("SELECT * FROM sync_queue WHERE isPending = 1 ORDER BY timestamp ASC")
    suspend fun getPendingSyncItemsSync(): List<SyncQueue>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSyncItem(item: SyncQueue)

    @Query("UPDATE sync_queue SET isPending = 0 WHERE id = :id")
    suspend fun markAsSynced(id: Int)
}

@Dao
interface ActivityLogDao {
    @Query("SELECT * FROM activity_logs WHERE customerId = :customerId ORDER BY timestamp DESC")
    fun getActivityLogsForCustomer(customerId: String): Flow<List<ActivityLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivityLog(log: ActivityLog)

    @Query("DELETE FROM activity_logs WHERE customerId = :customerId")
    suspend fun deleteActivityLogsForCustomer(customerId: String)
}

