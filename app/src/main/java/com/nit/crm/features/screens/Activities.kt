package com.nit.crm.features.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nit.crm.core.database.Invoice
import com.nit.crm.core.database.Payment
import com.nit.crm.features.DashboardViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentActivityScreen(
    dashboardViewModel: DashboardViewModel,
    onNavigateTo: (String) -> Unit,
    onBack: () -> Unit
) {
    val invoices by dashboardViewModel.allInvoices.collectAsState()
    val payments by dashboardViewModel.allPayments.collectAsState()

    val recentActivities = remember(invoices, payments) {
        (invoices.map { "INV" to it } + payments.map { "PAY" to it })
            .sortedByDescending { pair ->
                when (val obj = pair.second) {
                    is Invoice -> obj.date
                    is Payment -> obj.date
                    else -> 0L
                }
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recent Activity", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        if (recentActivities.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Text("No activity found", color = MaterialTheme.colorScheme.secondary)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(recentActivities) { activity ->
                    RecentActivityItem(activity = activity, onNavigateTo = onNavigateTo)
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentHistoryScreen(
    dashboardViewModel: DashboardViewModel,
    onBack: () -> Unit
) {
    val payments by dashboardViewModel.allPayments.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Payment History", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        if (payments.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Text("No payments found", color = MaterialTheme.colorScheme.secondary)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(payments.sortedByDescending { it.date }) { payment ->
                    PaymentListItem(payment = payment)
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                    )
                }
            }
        }
    }
}

@Composable
fun PaymentListItem(payment: Payment) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp) )
                .background(Color(0xFF10B981).copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Payments, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(24.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text("Collection", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            Text("via ${payment.paymentMethod}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "+৳${String.format(Locale.US, "%.0f", payment.amount)}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF10B981)
            )
            Text(
                text = SimpleDateFormat("MMM dd, yyyy", Locale.US).format(Date(payment.date)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

@Composable
fun RecentActivityItem(activity: Pair<String, Any>, onNavigateTo: (String) -> Unit) {
    val type = activity.first
    val data = activity.second
    
    val title = if (type == "INV") (data as Invoice).customerName else "Payment Received"
    val subtitle = if (type == "INV") {
        val inv = data as Invoice
        "${if (inv.isQuotation) "Quotation" else "Invoice"} • ${inv.id.substringAfterLast("-")}"
    } else {
        val pay = data as Payment
        "via ${pay.paymentMethod}"
    }
    val amount = if (type == "INV") (data as Invoice).totalAmount else (data as Payment).amount
    val date = if (type == "INV") (data as Invoice).date else (data as Payment).date
    
    val icon = if (type == "INV") Icons.Default.Description else Icons.Default.Payments
    val iconColor = if (type == "INV") MaterialTheme.colorScheme.primary else Color(0xFF10B981)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { 
                if (type == "INV") onNavigateTo("invoice_list") 
                else onNavigateTo("payment_list") 
            }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(24.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
        }
        
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "${if (type == "PAY") "+" else ""}৳${String.format(Locale.US, "%.0f", amount)}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = if (type == "PAY") Color(0xFF10B981) else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = SimpleDateFormat("MMM dd", Locale.US).format(Date(date)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}
