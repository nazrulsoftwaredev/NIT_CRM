package com.nit.crm.features

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.nit.crm.data.*

class MainViewModelFactory(
    private val userRepository: UserRepository,
    private val customerRepository: CustomerRepository,
    private val productRepository: ProductRepository,
    private val invoiceRepository: InvoiceRepository,
    private val paymentRepository: PaymentRepository,
    private val serviceRepository: ServiceRecordRepository,
    private val syncRepository: SyncRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(
                userRepository,
                customerRepository,
                productRepository,
                invoiceRepository,
                paymentRepository,
                serviceRepository,
                syncRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
