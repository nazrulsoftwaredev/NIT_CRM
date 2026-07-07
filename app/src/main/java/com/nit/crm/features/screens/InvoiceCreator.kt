package com.nit.crm.features.screens

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.widget.Toast
import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.compose.foundation.Canvas as ComposeCanvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import android.content.ClipData
import com.nit.crm.core.database.Product
import com.nit.crm.features.MainViewModel
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceCreatorScreen(viewModel: MainViewModel, isQuotation: Boolean = false) {
    val items = viewModel.currentInvoiceItems
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        if (viewModel.lastInteractedItemIndex >= 0) {
            listState.animateScrollToItem(viewModel.lastInteractedItemIndex + 2) // +2 for spacing/header
            viewModel.lastInteractedItemIndex = -1
        }
    }

    var serviceChargeText by remember { mutableStateOf(viewModel.draftServiceCharge) }
    var discountText by remember { mutableStateOf(viewModel.draftDiscount) }

    LaunchedEffect(serviceChargeText) {
        viewModel.draftServiceCharge = serviceChargeText
        viewModel.invoiceServiceCharge = serviceChargeText.toDoubleOrNull() ?: 0.0
    }
    LaunchedEffect(discountText) {
        viewModel.draftDiscount = discountText
        viewModel.invoiceDiscount = discountText.toDoubleOrNull() ?: 0.0
    }

    val signaturePoints = viewModel.draftSignaturePoints
    var isSaving by remember { mutableStateOf(false) }

    var clientName by viewModel::draftCustomerName
    var clientPhone by viewModel::draftCustomerPhone
    var clientEmail by viewModel::draftCustomerEmail
    var clientAddress by viewModel::draftCustomerAddress

    LaunchedEffect(viewModel.selectedCustomer) {
        val c = viewModel.selectedCustomer
        if (c != null) {
            viewModel.draftCustomerName = c.name
            viewModel.draftCustomerPhone = c.phone
            viewModel.draftCustomerEmail = c.email
            viewModel.draftCustomerAddress = c.address
        }
    }

    val allCustomers by viewModel.customers.collectAsState()
    val suggestions = remember(clientName, allCustomers) {
        if (clientName.length >= 2 && !allCustomers.any { it.name.equals(clientName, ignoreCase = true) }) {
            allCustomers.filter {
                it.name.contains(clientName, ignoreCase = true) ||
                it.phone.contains(clientName, ignoreCase = true)
            }.take(4)
        } else {
            emptyList()
        }
    }

    val isLocked = viewModel.selectedCustomer != null
    var payAmountText by remember { mutableStateOf(viewModel.draftAmountPaid) }
    LaunchedEffect(payAmountText) {
        viewModel.draftAmountPaid = payAmountText
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isQuotation) "Generate Quotation" else "Generate Invoice", style = MaterialTheme.typography.headlineMedium) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.goBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            // 1. Recipient Info
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("RECIPIENT INFORMATION", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
                    
                    OutlinedTextField(
                        value = clientName,
                        onValueChange = { 
                            clientName = it
                            if (viewModel.selectedCustomer?.name != it) {
                                viewModel.selectedCustomer = null
                            }
                        },
                        label = { Text("Client Name") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true,
                        readOnly = isLocked,
                        trailingIcon = {
                            if (isLocked) {
                                IconButton(onClick = {
                                    viewModel.selectedCustomer = null
                                    clientName = ""
                                    clientPhone = ""
                                    clientEmail = ""
                                    clientAddress = ""
                                }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear Client", tint = MaterialTheme.colorScheme.error)
                                }
                            } else {
                                IconButton(onClick = {
                                    viewModel.navigateTo("customer_list_invoice")
                                }) {
                                    Icon(Icons.Default.Contacts, contentDescription = "Browse Clients", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    )

                    // Autocomplete suggestions list
                    if (!isLocked && suggestions.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column {
                                suggestions.forEach { sugg ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                viewModel.selectedCustomer = sugg
                                                clientName = sugg.name
                                                clientPhone = sugg.phone
                                                clientEmail = sugg.email
                                                clientAddress = sugg.address
                                            }
                                            .padding(horizontal = 16.dp, vertical = 12.dp)
                                    ) {
                                        Column {
                                            Text(sugg.name, style = MaterialTheme.typography.bodyLarge)
                                            Text(sugg.phone, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = clientPhone,
                        onValueChange = { clientPhone = it },
                        label = { Text("Phone Number") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true,
                        readOnly = isLocked
                    )

                    OutlinedTextField(
                        value = clientEmail,
                        onValueChange = { clientEmail = it },
                        label = { Text("Email Address") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true,
                        readOnly = isLocked
                    )

                    OutlinedTextField(
                        value = clientAddress,
                        onValueChange = { clientAddress = it },
                        label = { Text("Billing Address") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true,
                        readOnly = isLocked
                    )
                }
            }

            // 2. Items Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("ITEMS", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
                    TextButton(onClick = {
                        viewModel.navigateTo("select_products")
                    }) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Item")
                    }
                }
            }

            if (items.isEmpty()) {
                item {
                    Text(
                        "No items added yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                items(items.toList()) { draft ->
                    val index = items.indexOf(draft)
                    InvoiceItemRow(draft = draft, viewModel = viewModel, itemIndex = index, isQuotation = isQuotation)
                }
            }

            // 3. Adjustments
            item {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("ADJUSTMENTS", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = serviceChargeText,
                            onValueChange = { serviceChargeText = it },
                            label = { Text("Service Fee") },
                            prefix = { Text("৳") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            trailingIcon = {
                                if (serviceChargeText.isNotEmpty()) {
                                    IconButton(onClick = { serviceChargeText = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                                    }
                                }
                            }
                        )
                        OutlinedTextField(
                            value = discountText,
                            onValueChange = { discountText = it },
                            label = { Text("Discount") },
                            prefix = { Text("৳") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            trailingIcon = {
                                if (discountText.isNotEmpty()) {
                                    IconButton(onClick = { discountText = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                                    }
                                }
                            }
                        )
                    }
                }
            }

            // 4. Totals
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PriceRow("Subtotal", String.format(Locale.US, "৳%.2f", viewModel.getInvoiceSubtotal()))
                    PriceRow("Service Fee", String.format(Locale.US, "৳%.2f", viewModel.invoiceServiceCharge))
                    PriceRow("Discount", String.format(Locale.US, "-৳%.2f", viewModel.invoiceDiscount))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total Due", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(
                            String.format(Locale.US, "৳%.2f", viewModel.getInvoiceTotal()),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.tertiary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // 4.5 Payment Received (invoices only)
            if (!isQuotation) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("PAYMENT RECEIVED", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
                        
                        OutlinedTextField(
                            value = payAmountText,
                            onValueChange = { payAmountText = it },
                            label = { Text("Amount Paid Upfront") },
                            prefix = { Text("৳") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true,
                            trailingIcon = {
                                Row(
                                    modifier = Modifier.padding(end = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextButton(
                                        onClick = { payAmountText = "0" },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Text("0", style = MaterialTheme.typography.labelMedium)
                                    }
                                    TextButton(
                                        onClick = { payAmountText = String.format(Locale.US, "%.2f", viewModel.getInvoiceTotal()) },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Text("Max", style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                        )
                    }
                }
            }

            // 5. Signature
            if (!isQuotation) {
                item {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text("CLIENT SIGNATURE", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
                            Row {
                                TextButton(onClick = { 
                                    viewModel.initialSignaturePoints = signaturePoints.toList()
                                    viewModel.onSignatureCapturedAction = { points ->
                                        signaturePoints.clear()
                                        signaturePoints.addAll(points)
                                    }
                                    viewModel.navigateTo("signature_capture")
                                }) {
                                    Icon(Icons.Default.Fullscreen, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Full Screen", style = MaterialTheme.typography.labelLarge)
                                }
                                TextButton(onClick = { signaturePoints.clear() }) {
                                    Text("Clear", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        SignaturePad(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp),
                            points = signaturePoints
                        )
                    }
                }
            }

            // 6. Action
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = {
                            if (isSaving) return@Button
                            if (clientName.trim().isBlank()) {
                                Toast.makeText(context, "Please enter client name", Toast.LENGTH_LONG).show()
                                return@Button
                            }
                            if (items.isEmpty()) {
                                Toast.makeText(context, "Please add at least one product item", Toast.LENGTH_LONG).show()
                                return@Button
                            }
                            if (!isQuotation && signaturePoints.isEmpty()) {
                                Toast.makeText(context, "Please draw a client signature", Toast.LENGTH_LONG).show()
                                return@Button
                            }
                            isSaving = true
                            coroutineScope.launch {
                                try {
                                    val sigBitmap = createBitmapFromPoints(signaturePoints.toList())
                                    val amountPaid = if (isQuotation) 0.0 else (payAmountText.toDoubleOrNull() ?: 0.0)
                                    val invoice = viewModel.saveInvoice(
                                        context = context,
                                        customerNameInput = clientName,
                                        customerPhoneInput = clientPhone,
                                        customerEmailInput = clientEmail,
                                        customerAddressInput = clientAddress,
                                        amountPaid = amountPaid,
                                        isQuote = isQuotation,
                                        signatureBitmap = sigBitmap,
                                        isDraft = false
                                    )
                                    if (invoice != null) {
                                        val pdfFile = viewModel.getOrGenerateInvoicePdf(context, invoice)
                                        if (pdfFile != null && pdfFile.exists()) {
                                            viewModel.generatedInvoice = invoice.copy(pdfPath = pdfFile.absolutePath)
                                            viewModel.navigateTo("invoice_success")
                                        } else {
                                            Toast.makeText(context, "Failed to generate PDF file locally", Toast.LENGTH_LONG).show()
                                        }
                                    } else {
                                        Toast.makeText(context, "Failed to save invoice record", Toast.LENGTH_LONG).show()
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                                } finally {
                                    isSaving = false
                                }
                            }
                        },
                        enabled = !isSaving,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("finalize_invoice_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            Icon(Icons.Outlined.Share, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isQuotation) "Finalize & Share Quotation" else "Finalize & Share PDF", style = MaterialTheme.typography.titleMedium)
                        }
                    }

                    if (!isQuotation) {
                        OutlinedButton(
                            onClick = {
                                if (isSaving) return@OutlinedButton
                                if (clientName.trim().isBlank()) {
                                    Toast.makeText(context, "Please enter client name", Toast.LENGTH_LONG).show()
                                    return@OutlinedButton
                                }
                                if (items.isEmpty()) {
                                    Toast.makeText(context, "Please add at least one product item", Toast.LENGTH_LONG).show()
                                    return@OutlinedButton
                                }
                                isSaving = true
                                coroutineScope.launch {
                                    try {
                                        val invoice = viewModel.saveInvoice(
                                            context = context,
                                            customerNameInput = clientName,
                                            customerPhoneInput = clientPhone,
                                            customerEmailInput = clientEmail,
                                            customerAddressInput = clientAddress,
                                            amountPaid = 0.0,
                                            isQuote = false,
                                            signatureBitmap = null,
                                            isDraft = true
                                        )
                                        if (invoice != null) {
                                            val pdfFile = viewModel.getOrGenerateInvoicePdf(context, invoice)
                                            if (pdfFile != null && pdfFile.exists()) {
                                                viewModel.generatedInvoice = invoice.copy(pdfPath = pdfFile.absolutePath)
                                                viewModel.navigateTo("invoice_success")
                                            } else {
                                                Toast.makeText(context, "Failed to generate Draft PDF", Toast.LENGTH_LONG).show()
                                            }
                                        } else {
                                            Toast.makeText(context, "Failed to save draft invoice", Toast.LENGTH_LONG).show()
                                        }
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                                    } finally {
                                        isSaving = false
                                    }
                                }
                            },
                            enabled = !isSaving,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .testTag("save_draft_invoice_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Save Invoice as Draft", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
            
            item { Spacer(modifier = Modifier.height(20.dp)) }
        }
    }


    val pdfError = viewModel.pdfError
    if (pdfError != null) {
        AlertDialog(
            onDismissRequest = { viewModel.pdfError = null },
            title = { Text("PDF Generation Failed") },
            text = { Text("We encountered an error generating the invoice PDF:\n\n$pdfError") },
            confirmButton = {
                TextButton(onClick = { viewModel.pdfError = null }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
fun InvoiceItemRow(draft: com.nit.crm.features.MainViewModel.InvoiceItemDraft, viewModel: MainViewModel, itemIndex: Int, isQuotation: Boolean = false) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(draft.product.name, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                Text("৳${draft.product.price} × ${draft.quantity}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { viewModel.removeProductFromInvoice(draft.product) }) {
                    Icon(Icons.Outlined.RemoveCircleOutline, contentDescription = null, modifier = Modifier.size(20.dp))
                }
                Text(draft.quantity.toString(), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(horizontal = 8.dp))
                IconButton(onClick = { 
                    if (draft.quantity < draft.product.stockQuantity) {
                        viewModel.addProductToInvoice(draft.product)
                    } else {
                        // Optional: Toast message for overstocking
                    }
                }) {
                    Icon(
                        imageVector = Icons.Outlined.AddCircleOutline, 
                        contentDescription = null, 
                        modifier = Modifier.size(20.dp),
                        tint = if (draft.quantity < draft.product.stockQuantity) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
        
        if (!isQuotation) {
            Spacer(modifier = Modifier.height(8.dp))

            draft.serialNumbers.forEachIndexed { sIndex, serial ->
                OutlinedTextField(
                    value = serial,
                    onValueChange = { viewModel.updateProductSerialNumber(draft.product.id, sIndex, it) },
                    label = { Text("Serial Number ${if (draft.quantity > 1) sIndex + 1 else ""}", style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodySmall,
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                viewModel.lastInteractedItemIndex = itemIndex
                                viewModel.onBarcodeScannedAction = { barcode ->
                                    viewModel.updateProductSerialNumber(draft.product.id, sIndex, barcode)
                                }
                                viewModel.navigateTo("scanner")
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.QrCodeScanner,
                                contentDescription = "Scan Serial Number"
                            )
                        }
                    }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
    }
}

@Composable
fun PriceRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}


fun sharePdfFile(context: Context, file: File) {
    try {
        val authority = "${context.packageName}.fileprovider"
        val uri = FileProvider.getUriForFile(context, authority, file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newRawUri("Invoice PDF", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, "Share Invoice").apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    } catch (e: Exception) {
        Toast.makeText(context, "Sharing failed: ${e.message}", Toast.LENGTH_LONG).show()
        android.util.Log.e("InvoiceCreator", "Failed to share PDF: ${e.message}", e)
    }
}
