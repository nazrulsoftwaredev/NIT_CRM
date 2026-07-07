package com.nit.crm.features.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nit.crm.core.database.Invoice
import com.nit.crm.core.database.Payment
import com.nit.crm.features.DashboardViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    dashboardViewModel: DashboardViewModel,
    companyName: String,
    companyLogoBase64: String,
    onNavigateTo: (String) -> Unit,
    onStartNewInvoice: () -> Unit,
    onStartNewQuotation: () -> Unit,
    onViewProductStock: () -> Unit,
    onMenuClick: () -> Unit
) {
    val invoices by dashboardViewModel.allInvoices.collectAsState()
    val payments by dashboardViewModel.allPayments.collectAsState()
    val customers by dashboardViewModel.customers.collectAsState()
    val pendingSync by dashboardViewModel.pendingSyncItems.collectAsState()

    val totalRevenue = invoices.sumOf { it.totalAmount }
    val totalCollected = payments.sumOf { it.amount }
    val totalDue = customers.sumOf { it.dueAmount }

    val recentActivities = remember(invoices, payments) {
        (invoices.map { "INV" to it } + payments.map { "PAY" to it }).sortedByDescending { pair ->
                when (val obj = pair.second) {
                    is Invoice -> obj.date
                    is Payment -> obj.date
                    else -> 0L
                }
            }.take(5)
    }

    Scaffold(
        topBar = {
            TopAppBar(title = {
                Column {
                    Text(
                        text = "Dashboard",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = companyName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }, navigationIcon = {
                IconButton(onClick = onMenuClick) {
                    Icon(Icons.Default.Menu, contentDescription = "Menu")
                }
            }, actions = {
                val logoBitmap = remember(companyLogoBase64) {
                    if (companyLogoBase64.isBlank()) null
                    else try {
                        val decodedBytes = Base64.decode(companyLogoBase64, Base64.DEFAULT)
                        BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                    } catch (e: Exception) {
                        null
                    }
                }

                if (logoBitmap != null) {
                    androidx.compose.foundation.Image(
                        bitmap = logoBitmap.asImageBitmap(),
                        contentDescription = "Logo",
                        modifier = Modifier.size(32.dp).clip(CircleShape)
                            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }

                IconButton(onClick = { dashboardViewModel.triggerManualSync() }) {
                    Icon(
                        imageVector = if (pendingSync.isNotEmpty()) Icons.Default.CloudSync else Icons.Default.Sync,
                        contentDescription = "Sync",
                        tint = if (pendingSync.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                    )
                }
            })
        }, containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 1. Overview Grid (At the very top)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricCard(
                        label = "Revenue",
                        value = "৳${String.format(Locale.US, "%.1fk", totalRevenue / 1000)}",
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        label = "Collected",
                        value = "৳${String.format(Locale.US, "%.1fk", totalCollected / 1000)}",
                        color = Color(0xFF10B981),
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        label = "Due",
                        value = "৳${String.format(Locale.US, "%.1fk", totalDue / 1000)}",
                        color = Color(0xFFEF4444),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 2. Performance Analytics
            item {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        "Performance Analytics",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Revenue Bar Chart (7 Days)
                        Card(
                            modifier = Modifier.weight(1.5f).height(220.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "Weekly Revenue Trend",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                RevenueBarChart(invoices = invoices)
                            }
                        }

                        // Status Donut Chart
                        Card(
                            modifier = Modifier.weight(1f).height(220.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    "Invoice Status",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                InvoiceStatusPieChart(invoices = invoices)
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            // 3. Quick Actions
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Quick Actions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 4.dp)
                    ) {
                        item {
                            ActionChip(
                                label = "Invoice",
                                icon = Icons.AutoMirrored.Outlined.ReceiptLong,
                                onClick = onStartNewInvoice
                            )
                        }
                        item {
                            ActionChip(
                                label = "Quotation",
                                icon = Icons.Outlined.RequestPage,
                                onClick = onStartNewQuotation
                            )
                        }
                        item {
                            ActionChip(
                                label = "Client",
                                icon = Icons.Outlined.PersonAdd,
                                onClick = { onNavigateTo("add_customer") })
                        }
                        item {
                            ActionChip(
                                label = "Payments",
                                icon = Icons.Outlined.Payments,
                                onClick = { onNavigateTo("receive_payment") })
                        }
                        item {
                            ActionChip(
                                label = "Inventory",
                                icon = Icons.Outlined.Inventory,
                                onClick = onViewProductStock
                            )
                        }
                    }
                }
            }

            // 4. Recent Transactions
            item {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Recent Activity",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(onClick = { onNavigateTo("recent_activity") }) {
                            Text("View All")
                        }
                    }

                    if (recentActivities.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(100.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No recent activity recorded",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    } else {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 8.dp)) {
                                recentActivities.forEachIndexed { index, activity ->
                                    RecentActivityItem(
                                        activity = activity,
                                        onNavigateTo = onNavigateTo
                                    )
                                    if (index < recentActivities.size - 1) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(horizontal = 16.dp),
                                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
fun MetricCard(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = color
            )
        }
    }
}

@Composable
fun RevenueBarChart(invoices: List<Invoice>) {
    val weeklyData = remember(invoices) {
        val calendar = Calendar.getInstance()
        val data = mutableListOf<Float>()
        for (i in 6 downTo 0) {
            calendar.time = Date()
            calendar.add(Calendar.DAY_OF_YEAR, -i)
            val dateKey = SimpleDateFormat("yyyyMMdd", Locale.US).format(calendar.time)
            val dayTotal = invoices.filter {
                SimpleDateFormat("yyyyMMdd", Locale.US).format(Date(it.date)) == dateKey
            }.sumOf { it.totalAmount }.toFloat()
            data.add(dayTotal)
        }
        data
    }

    val maxVal = weeklyData.maxOrNull()?.coerceAtLeast(1f) ?: 1f

    Canvas(modifier = Modifier.fillMaxSize()) {
        val barWidth = size.width / 14f
        val space = size.width / 14f

        weeklyData.forEachIndexed { index, value ->
            val barHeight = (value / maxVal) * size.height
            val x = index * (barWidth + space) + space / 2

            drawRoundRect(
                color = Color(0xFF0EA5E9),
                topLeft = androidx.compose.ui.geometry.Offset(x, size.height - barHeight),
                size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
            )
        }
    }
}

@Composable
fun InvoiceStatusPieChart(invoices: List<Invoice>) {
    val stats = remember(invoices) {
        val paid = invoices.count { it.status == "Paid" }.toFloat()
        val unpaid =
            invoices.count { it.status == "Unpaid" || it.status == "Partially Paid" }.toFloat()
        listOf(paid, unpaid)
    }

    val total = stats.sum().coerceAtLeast(1f)
    val colors = listOf(Color(0xFF10B981), Color(0xFFEF4444))

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(110.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            var startAngle = -90f
            stats.forEachIndexed { index, value ->
                val sweepAngle = (value / total) * 360f
                if (sweepAngle > 0) {
                    drawArc(
                        color = colors[index % colors.size],
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = true,
                        style = Fill
                    )
                }
                startAngle += sweepAngle
            }

            // Draw a small inner circle to make it look like a donut/pie hybrid
            drawCircle(
                color = Color.White.copy(alpha = 0.2f),
                radius = 10.dp.toPx(),
                style = Stroke(width = 2.dp.toPx())
            )
        }

        // Simple Legend
        Column(
            modifier = Modifier.align(Alignment.BottomCenter).offset(y = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(colors[0]))
                Text("Paid", fontSize = 8.sp, color = MaterialTheme.colorScheme.secondary)
                Spacer(modifier = Modifier.width(8.dp))
                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(colors[1]))
                Text("Unpaid", fontSize = 8.sp, color = MaterialTheme.colorScheme.secondary)
            }
        }
    }
}

@Composable
fun ActionChip(label: String, icon: ImageVector, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        }
    }
}
