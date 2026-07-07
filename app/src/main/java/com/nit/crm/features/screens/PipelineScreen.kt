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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nit.crm.core.database.Customer
import com.nit.crm.features.CustomersViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PipelineScreen(
    customersViewModel: CustomersViewModel,
    onDealClick: (Customer) -> Unit,
    onBack: () -> Unit
) {
    val customers by customersViewModel.customers.collectAsState()
    val searchVal by customersViewModel.customerSearchQuery.collectAsState()
    val context = LocalContext.current

    var activeStageFilter by customersViewModel::activeStageFilter

    val pipelineStages = listOf("Lead", "Contacted", "Proposal", "Won", "Lost")

    // Stats
    val activeStages = listOf("Lead", "Contacted", "Proposal")
    val pipelineValue = remember(customers) { customers.filter { it.leadStatus in activeStages }.sumOf { it.dealValue } }
    val wonValue = remember(customers) { customers.filter { it.leadStatus == "Won" }.sumOf { it.dealValue } }
    val openDealsCount = remember(customers) { customers.count { it.leadStatus in activeStages } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sales Pipeline", style = MaterialTheme.typography.headlineMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchVal,
                onValueChange = { customersViewModel.setCustomerSearchQuery(it) },
                placeholder = { Text("Search deals by client...", style = MaterialTheme.typography.bodyMedium) },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
                modifier = Modifier.fillMaxWidth().testTag("search_pipeline_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.Transparent
                ),
                shape = RoundedCornerShape(8.dp),
                singleLine = true
            )

            // KPI Stats Cards Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("PIPELINE VALUE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                        Text("৳%,.0f".format(Locale.US, pipelineValue), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text("$openDealsCount Deals Active", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("CLOSED WON", style = MaterialTheme.typography.labelSmall, color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                        Text("৳%,.0f".format(Locale.US, wonValue), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                        val wonCount = customers.count { it.leadStatus == "Won" }
                        Text("$wonCount Won Deals", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                    }
                }
            }

            // Horizontal stages chips row
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(pipelineStages) { stage ->
                    val selected = activeStageFilter == stage
                    val count = customers.count { it.leadStatus.equals(stage, ignoreCase = true) }
                    Surface(
                        selected = selected,
                        onClick = { activeStageFilter = stage },
                        shape = RoundedCornerShape(20.dp),
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(
                            1.dp,
                            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        ),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 14.dp)
                        ) {
                            Text(
                                text = "$stage ($count)",
                                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            val pipelineList = remember(customers, activeStageFilter, searchVal) {
                customers.filter {
                    it.leadStatus.equals(activeStageFilter, ignoreCase = true) &&
                    (searchVal.isBlank() || it.name.contains(searchVal, ignoreCase = true) || it.phone.contains(searchVal, ignoreCase = true))
                }
            }

            if (pipelineList.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("No deals in $activeStageFilter stage", color = MaterialTheme.colorScheme.secondary)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(pipelineList) { customer ->
                        ProfessionalDealItemCard(
                            customer = customer,
                            onMoveLeft = {
                                val currentIdx = pipelineStages.indexOf(customer.leadStatus)
                                if (currentIdx > 0) {
                                    customersViewModel.updateCustomerLeadStatus(customer.id, pipelineStages[currentIdx - 1])
                                }
                            },
                            onMoveRight = {
                                val currentIdx = pipelineStages.indexOf(customer.leadStatus)
                                if (currentIdx < pipelineStages.size - 1) {
                                    customersViewModel.updateCustomerLeadStatus(customer.id, pipelineStages[currentIdx + 1])
                                }
                            },
                            onClick = { onDealClick(customer) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProfessionalDealItemCard(
    customer: Customer,
    onMoveLeft: () -> Unit,
    onMoveRight: () -> Unit,
    onClick: () -> Unit
) {
    val priorityColor = when (customer.priority.lowercase()) {
        "high" -> Color(0xFFEF4444)
        "medium" -> Color(0xFFF59E0B)
        else -> Color(0xFF3B82F6)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Priority Tag
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(priorityColor.copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${customer.priority} Priority",
                        style = MaterialTheme.typography.labelSmall,
                        color = priorityColor,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Estimated Close Date if present
                if (customer.expectedCloseDate > 0L) {
                    val dateStr = SimpleDateFormat("dd MMM, yyyy", Locale.US).format(Date(customer.expectedCloseDate))
                    Text(
                        text = "Exp: $dateStr",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                        fontSize = 11.sp
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = customer.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = customer.phone,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "৳%,.0f".format(Locale.US, customer.dealValue),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onMoveLeft, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Move Left", modifier = Modifier.size(16.dp))
                        }
                        IconButton(onClick = onMoveRight, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Move Right", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfessionalDealDetailDialog(
    customer: Customer,
    onDismiss: () -> Unit,
    onSave: (String, Double, Long, String, String, String) -> Unit
) {
    val context = LocalContext.current
    var dealValueInput by remember { mutableStateOf(if (customer.dealValue > 0.0) String.format(Locale.US, "%.2f", customer.dealValue) else "") }
    var selectedStatus by remember { mutableStateOf(customer.leadStatus) }
    var selectedPriority by remember { mutableStateOf(customer.priority) }
    var lostReasonInput by remember { mutableStateOf(customer.lostReason) }
    var notesInput by remember { mutableStateOf(customer.dealNotes) }

    val pipelineStages = listOf("Lead", "Contacted", "Proposal", "Won", "Lost")

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    val value = dealValueInput.toDoubleOrNull() ?: 0.0
                    onSave(selectedStatus, value, customer.expectedCloseDate, selectedPriority, lostReasonInput, notesInput)
                }
            ) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        title = {
            Text("Deal Workspace", fontWeight = FontWeight.ExtraBold)
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Client Contacts Quick Actions
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(customer.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                            Text("Address: ${customer.address}", style = MaterialTheme.typography.bodySmall)

                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Call
                                IconButton(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${customer.phone}"))
                                        context.startActivity(intent)
                                    },
                                    modifier = Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape).size(36.dp)
                                ) {
                                    Icon(Icons.Default.Phone, contentDescription = "Call", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                }

                                // SMS
                                IconButton(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${customer.phone}"))
                                        context.startActivity(intent)
                                    },
                                    modifier = Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape).size(36.dp)
                                ) {
                                    Icon(Icons.Default.Sms, contentDescription = "SMS", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                }

                                // Email
                                IconButton(
                                    onClick = {
                                        if (customer.email.isNotBlank()) {
                                            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${customer.email}"))
                                            context.startActivity(intent)
                                        } else {
                                            Toast.makeText(context, "No email profile registered", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape).size(36.dp)
                                ) {
                                    Icon(Icons.Default.Email, contentDescription = "Email", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }

                // Interactive Progress Chevron Flow
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("PIPELINE STAGE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(pipelineStages) { stage ->
                                val active = selectedStatus == stage
                                val color = when (stage) {
                                    "Won" -> Color(0xFF10B981)
                                    "Lost" -> Color(0xFFEF4444)
                                    else -> MaterialTheme.colorScheme.primary
                                }
                                Surface(
                                    selected = active,
                                    onClick = { selectedStatus = stage },
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (active) color else MaterialTheme.colorScheme.surfaceVariant,
                                    border = BorderStroke(1.dp, if (active) color else MaterialTheme.colorScheme.outline),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Box(modifier = Modifier.padding(horizontal = 10.dp), contentAlignment = Alignment.Center) {
                                        Text(stage, style = MaterialTheme.typography.labelMedium, color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                // Lost Reason field if Lost selected
                if (selectedStatus == "Lost") {
                    item {
                        var expandedLostDropdown by remember { mutableStateOf(false) }
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("REASON FOR CLOSING LOST", style = MaterialTheme.typography.labelSmall, color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(
                                    onClick = { expandedLostDropdown = true },
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(if (lostReasonInput.isNotBlank()) lostReasonInput else "Select Lost Reason...")
                                    Spacer(modifier = Modifier.weight(1f))
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }

                                DropdownMenu(
                                    expanded = expandedLostDropdown,
                                    onDismissRequest = { expandedLostDropdown = false }
                                ) {
                                    listOf("Price too high", "Competitor selected", "Not interested", "No budget", "Other").forEach { reason ->
                                        DropdownMenuItem(
                                            text = { Text(reason) },
                                            onClick = {
                                                lostReasonInput = reason
                                                expandedLostDropdown = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Deal Value field
                item {
                    OutlinedTextField(
                        value = dealValueInput,
                        onValueChange = { dealValueInput = it },
                        label = { Text("Deal Estimated Value") },
                        prefix = { Text("৳") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                }

                // Priority Selection
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("DEAL PRIORITY", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("Low", "Medium", "High").forEach { prio ->
                                val active = selectedPriority == prio
                                val color = when (prio) {
                                    "High" -> Color(0xFFEF4444)
                                    "Medium" -> Color(0xFFF59E0B)
                                    else -> Color(0xFF3B82F6)
                                }
                                Surface(
                                    selected = active,
                                    onClick = { selectedPriority = prio },
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (active) color else MaterialTheme.colorScheme.surfaceVariant,
                                    border = BorderStroke(1.dp, if (active) color else MaterialTheme.colorScheme.outline),
                                    modifier = Modifier.weight(1f).height(34.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(prio, color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                // Deal Notes field
                item {
                    OutlinedTextField(
                        value = notesInput,
                        onValueChange = { notesInput = it },
                        label = { Text("Follow-up / Deal Notes") },
                        placeholder = { Text("Add call log, meeting updates, or deal tracking notes here...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }
        }
    )
}
