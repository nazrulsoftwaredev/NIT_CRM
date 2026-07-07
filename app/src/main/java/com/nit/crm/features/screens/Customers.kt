package com.nit.crm.features.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nit.crm.core.database.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerListScreen(
    customersViewModel: com.nit.crm.features.CustomersViewModel,
    isForInvoiceSelection: Boolean = false,
    onCustomerSelected: (Customer) -> Unit,
    onAddClient: () -> Unit,
    onNavigateTo: (String) -> Unit,
    onBack: () -> Unit
) {
    val searchVal by customersViewModel.customerSearchQuery.collectAsState()
    val customers by customersViewModel.customers.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isForInvoiceSelection) "Select Client" else "Clients",
                        style = MaterialTheme.typography.headlineMedium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onBack() }) {
                        if (isForInvoiceSelection) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        } else {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            if (!isForInvoiceSelection) {
                FloatingActionButton(
                    onClick = onAddClient,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("add_customer_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Client")
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Search Bar (Minimalist)
            OutlinedTextField(
                value = searchVal,
                onValueChange = { customersViewModel.setCustomerSearchQuery(it) },
                placeholder = { Text("Search customers...", style = MaterialTheme.typography.bodyMedium) },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_customer_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.Transparent
                ),
                shape = RoundedCornerShape(8.dp),
                singleLine = true
            )

            if (customers.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("No records found", color = MaterialTheme.colorScheme.secondary)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(customers) { customer ->
                        CustomerItem(
                            customer = customer,
                            showActions = !isForInvoiceSelection,
                            onClick = {
                                onCustomerSelected(customer)
                                if (isForInvoiceSelection) {
                                    onNavigateTo("create_invoice")
                                } else {
                                    onNavigateTo("customer_details")
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CustomerItem(customer: Customer, showActions: Boolean = true, onClick: () -> Unit) {
    val initials = customer.name.split(" ").mapNotNull { it.firstOrNull() }.take(2).joinToString("").uppercase()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 4.dp)
            .testTag("customer_item_${customer.id}"),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Smaller Initials Circle
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initials,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = customer.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = customer.phone,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
            
            // Compact Status Badge
            if (showActions) {
                val color = when (customer.leadStatus) {
                    "Won" -> Color(0xFF10B981)
                    "Lost" -> Color(0xFFEF4444)
                    "Proposal" -> Color(0xFF3B82F6)
                    else -> MaterialTheme.colorScheme.secondary
                }
                Surface(
                    color = color.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(4.dp),
                    border = BorderStroke(0.5.dp, color.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = customer.leadStatus,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = color,
                        fontSize = 10.sp
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDetailsScreen(
    customerId: String,
    customersViewModel: com.nit.crm.features.CustomersViewModel,
    onStartNewInvoice: () -> Unit,
    onStartNewQuotation: () -> Unit,
    onNavigateTo: (String) -> Unit,
    onBack: () -> Unit
) {
    val customerFlow = remember(customerId) { customersViewModel.getCustomerById(customerId) }
    val customerState by customerFlow.collectAsState()
    val customer = customerState ?: return

    val activityLogsFlow = remember(customerId) { customersViewModel.getActivityLogsForCustomer(customerId) }
    val activityLogs by activityLogsFlow.collectAsState()

    var showDealEditor by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Account Details", style = MaterialTheme.typography.headlineMedium) },
                navigationIcon = {
                    IconButton(onClick = { onBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { onNavigateTo("add_customer") }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Details")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Profile Info & Avatar Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    val initials = customer.name.split(" ").mapNotNull { it.firstOrNull() }.joinToString("").take(2)
                    Text(
                        text = initials,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column {
                    Text(
                        text = customer.name,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = customer.phone,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    if (customer.address.isNotBlank()) {
                        Text(
                            text = customer.address,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }

            // Cards Grid for Balance and Deal Value
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "BALANCE",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = String.format(Locale.US, "৳%.0f", customer.dueAmount),
                            style = MaterialTheme.typography.titleLarge,
                            color = if (customer.dueAmount > 0) MaterialTheme.colorScheme.error else Color(0xFF10B981),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "DEAL VALUE",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = String.format(Locale.US, "৳%.0f", customer.dealValue),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Deal Info Row (Status & Priority)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDealEditor = true },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Current Stage", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                        Text(customer.leadStatus, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Priority", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                        val priorityColor = when (customer.priority.lowercase()) {
                            "high" -> Color(0xFFEF4444)
                            "medium" -> Color(0xFFF59E0B)
                            else -> Color(0xFF3B82F6)
                        }
                        Text(customer.priority, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = priorityColor)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Deal",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            if (customer.dealNotes.isNotBlank()) {
                Column {
                    Text("Deal Notes", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(customer.dealNotes, style = MaterialTheme.typography.bodyMedium)
                }
            }

            // Quick Actions Cards Grid (4-Button Layout)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ActionBtn("Invoice", Icons.Outlined.Receipt, Modifier.weight(1f)) {
                        onStartNewInvoice()
                    }
                    ActionBtn("Quotation", Icons.Outlined.RequestPage, Modifier.weight(1f)) {
                        onStartNewQuotation()
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ActionBtn("Payment", Icons.Outlined.Payments, Modifier.weight(1f)) {
                        onNavigateTo("receive_payment")
                    }
                    ActionBtn("Service Ticket", Icons.Outlined.Build, Modifier.weight(1f)) {
                        onNavigateTo("log_service")
                    }
                }
            }

            // Timeline Header
            Text("Activity History", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)

            val timelineEvents = remember(activityLogs) {
                activityLogs.map { log ->
                    val icon = when (log.type) {
                        "CustomerCreated" -> Icons.Outlined.Person
                        "PipelineUpdate" -> Icons.AutoMirrored.Filled.TrendingUp
                        "Invoice" -> Icons.Outlined.Receipt
                        "Quotation" -> Icons.Outlined.RequestPage
                        "Payment" -> Icons.Outlined.Payments
                        "ServiceTicket" -> Icons.Outlined.Build
                        else -> Icons.Outlined.Info
                    }
                    val title = when (log.type) {
                        "CustomerCreated" -> "Account Created"
                        "PipelineUpdate" -> "Pipeline Updated"
                        "Invoice" -> "Invoice Generated"
                        "Quotation" -> "Quotation Generated"
                        "Payment" -> "Payment Recorded"
                        "ServiceTicket" -> "Service Ticket"
                        else -> "Activity Log"
                    }
                    TimelineEvent(
                        title = title,
                        timestamp = log.timestamp,
                        details = log.details,
                        icon = icon
                    )
                }
            }

            if (timelineEvents.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No activities recorded", color = MaterialTheme.colorScheme.secondary)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 20.dp)
                ) {
                    items(timelineEvents.size) { index ->
                        TimelineItem(
                            event = timelineEvents[index],
                            isFirst = index == 0,
                            isLast = index == timelineEvents.size - 1
                        )
                    }
                }
            }
        }
    }

    if (showDealEditor) {
        DealEditorDialog(
            customer = customer,
            onDismiss = { showDealEditor = false },
            onSave = { updatedStatus, updatedValue, updatedPriority, updatedNotes ->
                customersViewModel.updateCustomerDealPipeline(
                    customerId = customer.id,
                    leadStatus = updatedStatus,
                    dealValue = updatedValue,
                    expectedCloseDate = customer.expectedCloseDate,
                    priority = updatedPriority,
                    lostReason = customer.lostReason,
                    dealNotes = updatedNotes
                )
                showDealEditor = false
                Toast.makeText(context, "Deal updated", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DealEditorDialog(
    customer: Customer,
    onDismiss: () -> Unit,
    onSave: (String, Double, String, String) -> Unit
) {
    var dealValueInput by remember { mutableStateOf(customer.dealValue.toString()) }
    var selectedStatus by remember { mutableStateOf(customer.leadStatus) }
    var selectedPriority by remember { mutableStateOf(customer.priority) }
    var notesInput by remember { mutableStateOf(customer.dealNotes) }

    val pipelineStages = listOf("Lead", "Contacted", "Proposal", "Won", "Lost")
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header
            Column {
                Text(
                    text = "Deal Workspace",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = customer.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            // Deal Value Input
            OutlinedTextField(
                value = dealValueInput,
                onValueChange = { dealValueInput = it },
                label = { Text("Deal Value") },
                modifier = Modifier.fillMaxWidth(),
                prefix = { Text("৳ ") },
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            // Pipeline Stage selection row (segmented custom pills)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Pipeline Stage",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    pipelineStages.forEach { stage ->
                        val isSelected = selectedStatus == stage
                        val stageColor = when (stage) {
                            "Won" -> Color(0xFFE6F4EA) to Color(0xFF137333)     // Green
                            "Lost" -> Color(0xFFFCE8E6) to Color(0xFFC5221F)    // Red
                            else -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (isSelected) stageColor.first else Color.Transparent
                                )
                                .clickable { selectedStatus = stage },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stage,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) stageColor.second else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // Priority selection row (segmented custom pills)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Priority",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf("Low", "Medium", "High").forEach { prio ->
                        val isSelected = selectedPriority == prio
                        val prioColor = when (prio) {
                            "High" -> Color(0xFFFCE8E6) to Color(0xFFC5221F)    // Red
                            "Medium" -> Color(0xFFFEF7E0) to Color(0xFFB06000)  // Orange/Amber
                            else -> Color(0xFFE8F0FE) to Color(0xFF1A73E8)     // Blue
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (isSelected) prioColor.first else Color.Transparent
                                )
                                .clickable { selectedPriority = prio },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = prio,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) prioColor.second else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }

            // Deal Notes Input
            OutlinedTextField(
                value = notesInput,
                onValueChange = { notesInput = it },
                label = { Text("Deal Notes") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancel")
                }

                Button(
                    onClick = {
                        onSave(
                            selectedStatus,
                            dealValueInput.toDoubleOrNull() ?: 0.0,
                            selectedPriority,
                            notesInput
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Update Deal")
                }
            }
        }
    }
}

@Composable
fun ActionBtn(label: String, icon: ImageVector, modifier: Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier
            .height(72.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

data class TimelineEvent(
    val title: String,
    val timestamp: Long,
    val details: String,
    val icon: ImageVector
)

@Composable
fun TimelineItem(event: TimelineEvent, isFirst: Boolean, isLast: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        // Timeline vertical lines linking components
        Box(
            modifier = Modifier
                .width(32.dp)
                .fillMaxHeight(),
            contentAlignment = Alignment.TopCenter
        ) {
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.outline)
                )
            }
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = event.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Content Block
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(event.timestamp)),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = event.details,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}
