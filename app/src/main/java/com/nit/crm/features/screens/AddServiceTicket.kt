package com.nit.crm.features.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nit.crm.core.database.Customer
import com.nit.crm.core.database.ServiceRecord
import com.nit.crm.features.CustomersViewModel
import com.nit.crm.features.PaymentAndServiceViewModel
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddServiceTicketScreen(
    serviceViewModel: PaymentAndServiceViewModel,
    customersViewModel: CustomersViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val customers by customersViewModel.customers.collectAsState()

    var selectedCustomer by remember { mutableStateOf<Customer?>(null) }
    var problemInput by remember { mutableStateOf("") }
    var selectedPriority by remember { mutableStateOf("Medium") }
    
    var customerDropdownExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Service Ticket", style = MaterialTheme.typography.headlineMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
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
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // 1. Client Picker Section
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "CLIENT / CUSTOMER PROFILE",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold
                    )

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { customerDropdownExpanded = true },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = if (selectedCustomer != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.secondary
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Text(
                                text = selectedCustomer?.name ?: "Tap to choose a client profile...",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }

                        DropdownMenu(
                            expanded = customerDropdownExpanded,
                            onDismissRequest = { customerDropdownExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            customers.forEach { cust ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(cust.name, fontWeight = FontWeight.SemiBold)
                                            Text(cust.phone, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                                        }
                                    },
                                    onClick = {
                                        selectedCustomer = cust
                                        customerDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Display details of selected customer
                    selectedCustomer?.let { customer ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(customer.name, fontWeight = FontWeight.Bold)
                                Text("Phone: ${customer.phone}", style = MaterialTheme.typography.bodyMedium)
                                Text("Email: ${customer.email}", style = MaterialTheme.typography.bodyMedium)
                                Text("Address: ${customer.address}", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }

            // 2. Problem Description Section
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "ISSUE / TICKET SUMMARY",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = problemInput,
                        onValueChange = { problemInput = it },
                        placeholder = { Text("What is the issue or support request?") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 4,
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                }
            }

            // 3. Priority Selection Section
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "TICKET URGENCY / PRIORITY",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Low", "Medium", "High").forEach { prio ->
                            val selected = selectedPriority == prio
                            val color = when (prio) {
                                "High" -> Color(0xFFEF4444)
                                "Medium" -> Color(0xFFF59E0B)
                                else -> Color(0xFF3B82F6)
                            }
                            Surface(
                                selected = selected,
                                onClick = { selectedPriority = prio },
                                shape = RoundedCornerShape(20.dp),
                                color = if (selected) color else MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(
                                    1.dp,
                                    if (selected) color else MaterialTheme.colorScheme.outline
                                ),
                                modifier = Modifier.weight(1f).height(40.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Text(
                                        text = prio,
                                        color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 4. Save Button
            item {
                Button(
                    onClick = {
                        val cust = selectedCustomer
                        if (cust != null && problemInput.isNotBlank()) {
                            val newTicket = ServiceRecord(
                                id = "SRV-" + UUID.randomUUID().toString().take(6).uppercase(),
                                customerId = cust.id,
                                customerName = cust.name,
                                problem = problemInput,
                                solution = "",
                                partsUsed = "",
                                laborCharge = 0.0,
                                status = "Open",
                                priority = selectedPriority,
                                date = System.currentTimeMillis()
                            )
                            serviceViewModel.saveServiceTicket(newTicket)
                            Toast.makeText(context, "Support Ticket created successfully!", Toast.LENGTH_SHORT).show()
                            onBack()
                        } else {
                            Toast.makeText(context, "Please select client and write problem description", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp).padding(vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save and Launch Ticket", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}
