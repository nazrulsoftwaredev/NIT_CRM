package com.nit.crm.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey val id: String,
    val name: String,
    val email: String,
    val role: String, // "Technician", "Sales", "Owner"
    val isLoggedIn: Boolean = false
)

@Entity(tableName = "customers")
data class Customer(
    @PrimaryKey val id: String,
    val name: String,
    val phone: String,
    val email: String,
    val address: String,
    val dueAmount: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis(),
    val leadStatus: String = "Lead", // "Lead", "Contacted", "Proposal", "Won", "Lost"
    val dealValue: Double = 0.0,
    val expectedCloseDate: Long = 0L,
    val priority: String = "Medium", // "Low", "Medium", "High"
    val lostReason: String = "",
    val dealNotes: String = ""
)

@Entity(tableName = "products")
data class Product(
    @PrimaryKey val id: String,
    val sku: String,
    val name: String,
    val price: Double,
    val category: String,
    val stockQuantity: Int,
    val purchasePrice: Double = 0.0,
    val warranty: String = "No Warranty"
)

@Entity(tableName = "invoices")
data class Invoice(
    @PrimaryKey val id: String,
    val customerId: String,
    val customerName: String,
    val date: Long = System.currentTimeMillis(),
    val totalAmount: Double,
    val discount: Double = 0.0,
    val serviceCharge: Double = 0.0,
    val status: String, // "Paid", "Partially Paid", "Unpaid"
    val signaturePath: String? = null,
    val pdfPath: String? = null,
    val isSynced: Boolean = false,
    val isQuotation: Boolean = false
)

@Entity(tableName = "invoice_items")
data class InvoiceItem(
    @PrimaryKey val id: String,
    val invoiceId: String,
    val productId: String,
    val productName: String,
    val quantity: Int,
    val unitPrice: Double,
    val totalPrice: Double,
    val serialNumber: String? = null
)

@Entity(tableName = "payments")
data class Payment(
    @PrimaryKey val id: String,
    val invoiceId: String,
    val customerId: String,
    val amount: Double,
    val paymentMethod: String, // "Cash", "Card", "Bank Transfer", "Mobile Pay"
    val date: Long = System.currentTimeMillis(),
    val notes: String = "",
    val isSynced: Boolean = false
)

@Entity(tableName = "service_records")
data class ServiceRecord(
    @PrimaryKey val id: String,
    val customerId: String,
    val customerName: String,
    val problem: String,
    val solution: String,
    val partsUsed: String, // Comma separated list of parts
    val laborCharge: Double,
    val technicianSignaturePath: String? = null,
    val status: String, // "Completed", "Pending Parts", "In Progress"
    val date: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false,
    val priority: String = "Medium"
)

@Entity(tableName = "installation_records")
data class InstallationRecord(
    @PrimaryKey val id: String,
    val customerId: String,
    val customerName: String,
    val installedProducts: String, // Detailed notes or sku quantities
    val notes: String,
    val customerSignaturePath: String? = null,
    val date: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
)

@Entity(tableName = "sync_queue")
data class SyncQueue(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val entityType: String, // "Customer", "Invoice", "Payment", "Service", "Installation"
    val entityId: String,
    val actionType: String, // "INSERT", "UPDATE"
    val timestamp: Long = System.currentTimeMillis(),
    val isPending: Boolean = true
)

@Entity(tableName = "activity_logs")
data class ActivityLog(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val customerId: String,
    val type: String, // "CustomerCreated", "PipelineUpdate", "Invoice", "Quotation", "Payment", "ServiceTicket"
    val timestamp: Long = System.currentTimeMillis(),
    val details: String
)

