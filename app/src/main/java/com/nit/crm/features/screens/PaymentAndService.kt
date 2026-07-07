package com.nit.crm.features.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas as ComposeCanvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nit.crm.features.MainViewModel
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceivePaymentScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var activeCustomer by remember { mutableStateOf<com.nit.crm.core.database.Customer?>(viewModel.selectedCustomer) }
    
    // Reset activeCustomer if viewModel.selectedCustomer changes
    LaunchedEffect(viewModel.selectedCustomer) {
        activeCustomer = viewModel.selectedCustomer
    }

    var searchQuery by remember { mutableStateOf("") }
    val allCustomers by viewModel.customers.collectAsState()
    
    val filteredCustomers = remember(searchQuery, allCustomers) {
        if (searchQuery.isBlank()) {
            allCustomers
        } else {
            allCustomers.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.phone.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (activeCustomer == null) "Select Client for Payment" else "Record Payment", style = MaterialTheme.typography.headlineMedium) },
                navigationIcon = {
                    IconButton(onClick = { 
                        if (activeCustomer != null && viewModel.selectedCustomer == null) {
                            activeCustomer = null // Go back to customer selection
                        } else {
                            viewModel.goBack() 
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        if (activeCustomer == null) {
            // Customer Search UI
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search client by name or phone...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )

                if (filteredCustomers.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No clients found", color = MaterialTheme.colorScheme.secondary)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredCustomers.size) { index ->
                            val cust = filteredCustomers[index]
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { activeCustomer = cust },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(cust.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        Text(cust.phone, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Due", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                                        Text(
                                            "৳${String.format(Locale.US, "%.0f", cust.dueAmount)}",
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = if (cust.dueAmount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            val customer = activeCustomer!!
            val invoicesFlow = remember(customer.id) { viewModel.getInvoicesForCustomer(customer.id) }
            val invoices by invoicesFlow.collectAsState(initial = emptyList())
            
            val unpaidInvoices = remember(invoices) {
                invoices.filter { !it.isQuotation && it.status != "Paid" && it.status != "Draft" }
            }

            var selectedInvoiceId by remember { mutableStateOf("MANUAL") }
            val selectedInvoice = remember(selectedInvoiceId, unpaidInvoices) {
                unpaidInvoices.find { it.id == selectedInvoiceId }
            }

            // Gather payments for the selected invoice if a specific one is selected
            val invoicePaymentsFlow = remember(selectedInvoiceId) {
                viewModel.getPaymentsForInvoice(selectedInvoiceId)
            }
            val invoicePayments by invoicePaymentsFlow.collectAsState(initial = emptyList())

            val remainingDue = remember(selectedInvoice, invoicePayments, customer.dueAmount) {
                if (selectedInvoice != null) {
                    val paid = invoicePayments.sumOf { it.amount }
                    (selectedInvoice.totalAmount - paid).coerceAtLeast(0.0)
                } else {
                    customer.dueAmount
                }
            }

            var amountText by remember { mutableStateOf("") }
            // Default amountText when customer or invoice changes
            LaunchedEffect(selectedInvoiceId, remainingDue) {
                amountText = String.format(Locale.US, "%.2f", remainingDue)
            }

            var selectedMethod by remember { mutableStateOf("Cash") }
            var notes by remember { mutableStateOf("") }
            val methods = listOf("Cash", "Card", "Transfer", "Other")
            
            var isInvoiceDropdownExpanded by remember { mutableStateOf(false) }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                item { Spacer(modifier = Modifier.height(4.dp)) }

                // Customer Header Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("CUSTOMER", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                                Text(customer.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                Text(customer.phone, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
                            }
                            if (viewModel.selectedCustomer == null) {
                                TextButton(onClick = { activeCustomer = null }) {
                                    Text("Change")
                                }
                            }
                        }
                    }
                }

                // Invoice Selector
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("SELECT TARGET INVOICE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                        
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { isInvoiceDropdownExpanded = true },
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                val displayTitle = if (selectedInvoiceId == "MANUAL") {
                                    "General Account Credit (৳${String.format(Locale.US, "%.2f", customer.dueAmount)} due)"
                                } else {
                                    "Invoice #${selectedInvoiceId.substringAfterLast("-", selectedInvoiceId)} (Remaining: ৳${String.format(Locale.US, "%.2f", remainingDue)})"
                                }
                                Text(displayTitle, style = MaterialTheme.typography.bodyMedium)
                                Spacer(modifier = Modifier.weight(1f))
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }

                            DropdownMenu(
                                expanded = isInvoiceDropdownExpanded,
                                onDismissRequest = { isInvoiceDropdownExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("General Account Credit") },
                                    onClick = {
                                        selectedInvoiceId = "MANUAL"
                                        isInvoiceDropdownExpanded = false
                                    }
                                )
                                unpaidInvoices.forEach { inv ->
                                    val displayNo = inv.id.substringAfterLast("-", inv.id)
                                    val dateStr = java.text.SimpleDateFormat("dd/MM/yy", java.util.Locale.US).format(java.util.Date(inv.date))
                                    DropdownMenuItem(
                                        text = { Text("Invoice #$displayNo - $dateStr (Total: ৳${String.format(Locale.US, "%.0f", inv.totalAmount)})") },
                                        onClick = {
                                            selectedInvoiceId = inv.id
                                            isInvoiceDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Payment Inputs
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("PAYMENT DETAILS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                        
                        OutlinedTextField(
                            value = amountText,
                            onValueChange = { amountText = it },
                            label = { Text("Amount Received") },
                            prefix = { Text("৳") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            methods.forEach { method ->
                                val isSelected = selectedMethod == method
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable { selectedMethod = method }
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        method,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = { Text("Notes (Optional)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                // Confirm Action Button
                item {
                    Button(
                        onClick = {
                            val amt = amountText.toDoubleOrNull() ?: 0.0
                            if (amt <= 0) {
                                Toast.makeText(context, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            coroutineScope.launch {
                                viewModel.recordPayment(customer.id, selectedInvoiceId, amt, selectedMethod, notes)
                                Toast.makeText(context, "Payment of ৳$amt recorded successfully!", Toast.LENGTH_LONG).show()
                                viewModel.goBack()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Confirm Payment", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogServiceScreen(viewModel: MainViewModel) {
    val customer = viewModel.selectedCustomer ?: return
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var chargeText by remember { mutableStateOf("") }
    
    val customerTickets by viewModel.getServiceRecordsForCustomer(customer.id).collectAsState()
    val openTickets = remember(customerTickets) {
        customerTickets.filter { !it.status.equals("Completed", ignoreCase = true) }
    }

    var selectedTicket by remember { mutableStateOf<com.nit.crm.core.database.ServiceRecord?>(null) }
    var ticketDropdownExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Service Report", style = MaterialTheme.typography.headlineMedium) },
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
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            item {
                Column {
                    Text("CLIENT & LOCATION", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
                    Text(customer.name, style = MaterialTheme.typography.titleLarge)
                    Text(customer.address, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
                }
            }

            // Ticket Picker Dropdown (Only if open tickets exist)
            if (openTickets.isNotEmpty()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "LINK TO SUPPORT TICKET",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Bold
                        )

                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { ticketDropdownExpanded = true },
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                val btnText = if (selectedTicket != null) {
                                    "Ticket: ${selectedTicket!!.id.substringAfterLast("-", selectedTicket!!.id)} - ${selectedTicket!!.problem.take(30)}..."
                                } else {
                                    "General Service (No Ticket)"
                                }
                                Text(
                                    text = btnText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }

                            DropdownMenu(
                                expanded = ticketDropdownExpanded,
                                onDismissRequest = { ticketDropdownExpanded = false },
                                modifier = Modifier.fillMaxWidth(0.9f)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("General Service (No Ticket)", fontWeight = FontWeight.SemiBold) },
                                    onClick = {
                                        selectedTicket = null
                                        viewModel.serviceProblem = ""
                                        ticketDropdownExpanded = false
                                    }
                                )
                                openTickets.forEach { t ->
                                    val dispId = t.id.substringAfterLast("-", t.id)
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text("Ticket $dispId - ${t.priority}", fontWeight = FontWeight.SemiBold)
                                                Text(t.problem, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary, maxLines = 1)
                                            }
                                        },
                                        onClick = {
                                            selectedTicket = t
                                            viewModel.serviceProblem = t.problem
                                            ticketDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("REPAIR DETAILS", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
                    
                    OutlinedTextField(
                        value = viewModel.serviceProblem,
                        onValueChange = { viewModel.serviceProblem = it },
                        label = { Text("Problem Description") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        enabled = selectedTicket == null,
                        readOnly = selectedTicket != null
                    )

                    OutlinedTextField(
                        value = viewModel.serviceSolution,
                        onValueChange = { viewModel.serviceSolution = it },
                        label = { Text("Resolution") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        value = chargeText,
                        onValueChange = { 
                            chargeText = it
                            viewModel.serviceLaborCharge = it.toDoubleOrNull() ?: 0.0
                        },
                        label = { Text("Service Charge") },
                        prefix = { Text("৳") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }

            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("TECHNICIAN SIGNATURE", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
                        Row {
                            Text(
                                text = "Full Screen",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clickable { 
                                    viewModel.initialSignaturePoints = viewModel.draftSignaturePoints.toList()
                                    viewModel.onSignatureCapturedAction = { points ->
                                        viewModel.draftSignaturePoints.clear()
                                        viewModel.draftSignaturePoints.addAll(points)
                                    }
                                    viewModel.navigateTo("signature_capture")
                                },
                                fontWeight = FontWeight.Bold
                            )
                            if (viewModel.draftSignaturePoints.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Clear",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.clickable { viewModel.draftSignaturePoints.clear() },
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    SignaturePad(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        points = viewModel.draftSignaturePoints
                    )
                }
            }

            item {
                Button(
                    onClick = {
                        if (viewModel.serviceProblem.isBlank() || viewModel.draftSignaturePoints.isEmpty()) return@Button
                        coroutineScope.launch {
                            val sigBitmap = com.nit.crm.features.screens.createBitmapFromPoints(viewModel.draftSignaturePoints.toList())
                            viewModel.saveServiceRecord(customer.id, sigBitmap, context, selectedTicket?.id)
                            viewModel.goBack()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save Report", style = MaterialTheme.typography.titleMedium)
                }
            }
            
            item { Spacer(modifier = Modifier.height(20.dp)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogInstallationScreen(viewModel: MainViewModel) {
    val customer = viewModel.selectedCustomer ?: return
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val customerSigPoints = remember { mutableStateListOf<Offset>() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Installation Log", style = MaterialTheme.typography.headlineMedium) },
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
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            item {
                Column {
                    Text("CLIENT", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
                    Text(customer.name, style = MaterialTheme.typography.titleLarge)
                    Text(customer.address, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("INSTALLATION SUMMARY", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
                    
                    OutlinedTextField(
                        value = viewModel.installProductsSummary,
                        onValueChange = { viewModel.installProductsSummary = it },
                        label = { Text("Deployed Products") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        value = viewModel.installNotes,
                        onValueChange = { viewModel.installNotes = it },
                        label = { Text("Technical Notes") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }

            item {
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("CLIENT ACCEPTANCE", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
                        TextButton(onClick = { customerSigPoints.clear() }) {
                            Text("Clear", color = MaterialTheme.colorScheme.error)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    SignaturePad(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        points = customerSigPoints
                    )
                }
            }

            item {
                Button(
                    onClick = {
                        if (viewModel.installProductsSummary.isBlank() || customerSigPoints.isEmpty()) return@Button
                        coroutineScope.launch {
                            val sigBitmap = com.nit.crm.features.screens.createBitmapFromPoints(customerSigPoints.toList())
                            viewModel.saveInstallationRecord(customer.id, sigBitmap, context)
                            viewModel.goBack()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Complete Installation", style = MaterialTheme.typography.titleMedium)
                }
            }
            
            item { Spacer(modifier = Modifier.height(20.dp)) }
        }
    }
}
