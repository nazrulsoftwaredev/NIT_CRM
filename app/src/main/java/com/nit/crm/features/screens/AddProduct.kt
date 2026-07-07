package com.nit.crm.features.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.ui.Alignment
import com.nit.crm.features.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProductScreen(
    productsViewModel: com.nit.crm.features.ProductsViewModel,
    selectedProduct: com.nit.crm.core.database.Product?,
    onNavigateToScanner: () -> Unit,
    onBack: () -> Unit
) {
    var name by remember { mutableStateOf(productsViewModel.draftName) }
    var sku by remember { mutableStateOf(productsViewModel.draftSku) }
    var price by remember { mutableStateOf(productsViewModel.draftPrice) }
    var category by remember { mutableStateOf(productsViewModel.draftCategory) }
    var stock by remember { mutableStateOf(productsViewModel.draftStock) }
    var purchasePrice by remember { mutableStateOf(productsViewModel.draftPurchasePrice) }
    var warranty by remember { mutableStateOf(productsViewModel.draftWarranty) }

    LaunchedEffect(name) { productsViewModel.draftName = name }
    LaunchedEffect(sku) { productsViewModel.draftSku = sku }
    LaunchedEffect(price) { productsViewModel.draftPrice = price }
    LaunchedEffect(category) { productsViewModel.draftCategory = category }
    LaunchedEffect(stock) { productsViewModel.draftStock = stock }
    LaunchedEffect(purchasePrice) { productsViewModel.draftPurchasePrice = purchasePrice }
    LaunchedEffect(warranty) { productsViewModel.draftWarranty = warranty }

    val isEditing = selectedProduct != null

    // Initialize states from selected product when in edit mode
    LaunchedEffect(selectedProduct) {
        val product = selectedProduct
        if (product != null) {
            name = product.name
            sku = product.sku
            price = product.price.toString()
            category = product.category
            stock = product.stockQuantity.toString()
            purchasePrice = product.purchasePrice.toString()
            warranty = product.warranty
        } else {
            if (name.isBlank() && productsViewModel.draftName.isNotBlank()) {
                name = productsViewModel.draftName
            }
            if (sku.isBlank() && productsViewModel.draftSku.isNotBlank()) {
                sku = productsViewModel.draftSku
            }
            if (price.isBlank() && productsViewModel.draftPrice.isNotBlank()) {
                price = productsViewModel.draftPrice
            }
            if (category.isBlank() && productsViewModel.draftCategory.isNotBlank()) {
                category = productsViewModel.draftCategory
            }
            if (stock.isBlank() && productsViewModel.draftStock.isNotBlank()) {
                stock = productsViewModel.draftStock
            }
            if (purchasePrice.isBlank() && productsViewModel.draftPurchasePrice.isNotBlank()) {
                purchasePrice = productsViewModel.draftPurchasePrice
            }
            if (warranty.isBlank() && productsViewModel.draftWarranty.isNotBlank()) {
                warranty = productsViewModel.draftWarranty
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "Edit Product" else "Add New Product", style = MaterialTheme.typography.headlineMedium) },
                navigationIcon = {
                    IconButton(onClick = { 
                        onBack() 
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isEditing) {
                        IconButton(onClick = {
                            val product = selectedProduct
                            if (product != null) {
                                productsViewModel.deleteProduct(product.id)
                                productsViewModel.clearDrafts()
                                onBack()
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Product",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }
            
            item {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Product Name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )
            }
            
            item {
                OutlinedTextField(
                    value = sku,
                    onValueChange = { sku = it },
                    label = { Text("SKU") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    enabled = !isEditing, // SKU is unique and typically not editable
                    trailingIcon = {
                        if (!isEditing) {
                            IconButton(onClick = onNavigateToScanner) {
                                Icon(Icons.Outlined.QrCodeScanner, contentDescription = "Scan Barcode")
                            }
                        }
                    }
                )
            }
            
            item {
                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it },
                    label = { Text("Selling Price (Retail)") },
                    prefix = { Text("৳") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )
            }

            item {
                OutlinedTextField(
                    value = purchasePrice,
                    onValueChange = { purchasePrice = it },
                    label = { Text("Purchase Price (Cost)") },
                    prefix = { Text("৳") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )
            }
            
            item {
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )
            }
            
            item {
                OutlinedTextField(
                    value = stock,
                    onValueChange = { stock = it },
                    label = { Text("Stock Quantity") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )
            }

            item {
                OutlinedTextField(
                    value = warranty,
                    onValueChange = { warranty = it },
                    label = { Text("Warranty Duration") },
                    placeholder = { Text("e.g. 1 Year, 6 Months") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )
            }
            
            item {
                Button(
                    onClick = {
                        val p = price.toDoubleOrNull() ?: 0.0
                        val s = stock.toIntOrNull() ?: 0
                        val pp = purchasePrice.toDoubleOrNull() ?: 0.0
                        val w = warranty.ifBlank { "No Warranty" }
                        if (name.isNotBlank() && sku.isNotBlank()) {
                            productsViewModel.saveProduct(selectedProduct?.id, name, sku, p, category, s, pp, w)
                            productsViewModel.clearDrafts()
                            onBack()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (isEditing) "Save Changes" else "Add Product", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}
