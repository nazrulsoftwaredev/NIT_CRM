package com.nit.crm.core.database

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

object FirebaseSyncHelper {
    private const val TAG = "FirebaseSyncHelper"
    private var firestoreInstance: FirebaseFirestore? = null
    var isInitialized = false
        private set

    // Simple reactive status state for UI feedback
    var statusMessage = "Offline Local Mode (Active)"
        private set

    fun initialize(context: Context) {
        try {
            if (FirebaseApp.getApps(context).isNotEmpty()) {
                firestoreInstance = FirebaseFirestore.getInstance()
                isInitialized = true
                statusMessage = "Firebase Connected"
                Log.d(TAG, "Firebase auto-initialized.")
                return
            }

            // Build programmatically to ensure robust runtime operations if google-services.json is absent
            val options = FirebaseOptions.Builder()
                .setApplicationId("1:679684694105:android:2213175b3f4e4567")
                .setApiKey("AIzaSyA1B2C3D4E5F6G7H8I9J0K1L2M3N4O5P")
                .setProjectId("nit-crm-solutions")
                .build()

            FirebaseApp.initializeApp(context.applicationContext, options)
            firestoreInstance = FirebaseFirestore.getInstance()
            
            // Check if mock/dummy API key is used
            val isMockApiKey = options.apiKey == "AIzaSyA1B2C3D4E5F6G7H8I9J0K1L2M3N4O5P"
            if (isMockApiKey) {
                firestoreInstance?.disableNetwork()
                statusMessage = "Offline Local Mode (Active)"
                Log.i(TAG, "Firestore initialized in local-only offline mode due to placeholder credentials.")
            } else {
                statusMessage = "Firebase Live Sync Active"
            }
            
            isInitialized = true
            Log.d(TAG, "Firebase programmatically initialized.")
        } catch (e: Exception) {
            Log.e(TAG, "Firebase initialization failed: ${e.message}")
            isInitialized = false
            statusMessage = "Local-Only Mode (Active)"
        }
    }

    private fun getDb(): FirebaseFirestore? {
        return firestoreInstance
    }

    fun isMockMode(): Boolean {
        return firestoreInstance == null || statusMessage == "Offline Local Mode (Active)"
    }

    suspend fun deleteInvoice(invoiceId: String): Boolean {
        if (isMockMode()) return true
        val db = getDb() ?: return false
        return try {
            withTimeoutOrNull(5000L) {
                db.collection("invoices").document(invoiceId).delete().await()
                true
            } ?: false
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting invoice: ${e.message}")
            false
        }
    }

    suspend fun syncCustomer(customer: Customer): Boolean {
        if (isMockMode()) return true
        val db = getDb() ?: return false
        return try {
            val data = mapOf(
                "id" to customer.id,
                "name" to customer.name,
                "phone" to customer.phone,
                "email" to customer.email,
                "address" to customer.address,
                "dueAmount" to customer.dueAmount,
                "createdAt" to customer.createdAt,
                "lastUpdated" to System.currentTimeMillis()
            )
            val success = withTimeoutOrNull(5000L) {
                db.collection("customers").document(customer.id)
                    .set(data, SetOptions.merge())
                    .await()
                true
            } ?: false
            if (success) {
                Log.d(TAG, "Synced Customer ${customer.id} to Firestore.")
            }
            success
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing customer: ${e.message}")
            false
        }
    }

    suspend fun syncInvoice(invoice: Invoice, items: List<InvoiceItem>): Boolean {
        if (isMockMode()) return true
        val db = getDb() ?: return false
        return try {
            val invoiceData = mapOf(
                "id" to invoice.id,
                "customerId" to invoice.customerId,
                "customerName" to invoice.customerName,
                "date" to invoice.date,
                "totalAmount" to invoice.totalAmount,
                "discount" to invoice.discount,
                "serviceCharge" to invoice.serviceCharge,
                "status" to invoice.status,
                "pdfPath" to invoice.pdfPath,
                "signaturePath" to invoice.signaturePath,
                "isQuotation" to invoice.isQuotation,
                "lastUpdated" to System.currentTimeMillis()
            )
            val success = withTimeoutOrNull(10000L) {
                db.collection("invoices").document(invoice.id)
                    .set(invoiceData, SetOptions.merge())
                    .await()

                // Sync items as a subcollection or separately
                for (item in items) {
                    val itemData = mapOf(
                        "id" to item.id,
                        "invoiceId" to item.invoiceId,
                        "productId" to item.productId,
                        "productName" to item.productName,
                        "quantity" to item.quantity,
                        "unitPrice" to item.unitPrice,
                        "totalPrice" to item.totalPrice,
                        "serialNumber" to item.serialNumber
                    )
                    db.collection("invoices").document(invoice.id)
                        .collection("items").document(item.id)
                        .set(itemData, SetOptions.merge())
                        .await()
                }
                true
            } ?: false
            if (success) {
                Log.d(TAG, "Synced Invoice ${invoice.id} with items to Firestore.")
            }
            success
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing invoice: ${e.message}")
            false
        }
    }

    suspend fun syncPayment(payment: Payment): Boolean {
        if (isMockMode()) return true
        val db = getDb() ?: return false
        return try {
            val data = mapOf(
                "id" to payment.id,
                "invoiceId" to payment.invoiceId,
                "customerId" to payment.customerId,
                "amount" to payment.amount,
                "paymentMethod" to payment.paymentMethod,
                "date" to payment.date,
                "notes" to payment.notes,
                "lastUpdated" to System.currentTimeMillis()
            )
            val success = withTimeoutOrNull(5000L) {
                db.collection("payments").document(payment.id)
                    .set(data, SetOptions.merge())
                    .await()
                true
            } ?: false
            if (success) {
                Log.d(TAG, "Synced Payment ${payment.id} to Firestore.")
            }
            success
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing payment: ${e.message}")
            false
        }
    }

    suspend fun syncServiceRecord(record: ServiceRecord): Boolean {
        if (isMockMode()) return true
        val db = getDb() ?: return false
        return try {
            val data = mapOf(
                "id" to record.id,
                "customerId" to record.customerId,
                "customerName" to record.customerName,
                "problem" to record.problem,
                "solution" to record.solution,
                "partsUsed" to record.partsUsed,
                "laborCharge" to record.laborCharge,
                "technicianSignaturePath" to record.technicianSignaturePath,
                "status" to record.status,
                "date" to record.date,
                "lastUpdated" to System.currentTimeMillis()
            )
            val success = withTimeoutOrNull(5000L) {
                db.collection("service_records").document(record.id)
                    .set(data, SetOptions.merge())
                    .await()
                true
            } ?: false
            if (success) {
                Log.d(TAG, "Synced ServiceRecord ${record.id} to Firestore.")
            }
            success
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing service record: ${e.message}")
            false
        }
    }

    suspend fun syncInstallationRecord(record: InstallationRecord): Boolean {
        if (isMockMode()) return true
        val db = getDb() ?: return false
        return try {
            val data = mapOf(
                "id" to record.id,
                "customerId" to record.customerId,
                "customerName" to record.customerName,
                "installedProducts" to record.installedProducts,
                "notes" to record.notes,
                "customerSignaturePath" to record.customerSignaturePath,
                "date" to record.date,
                "lastUpdated" to System.currentTimeMillis()
            )
            val success = withTimeoutOrNull(5000L) {
                db.collection("installation_records").document(record.id)
                    .set(data, SetOptions.merge())
                    .await()
                true
            } ?: false
            if (success) {
                Log.d(TAG, "Synced InstallationRecord ${record.id} to Firestore.")
            }
            success
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing installation record: ${e.message}")
            false
        }
    }
}
