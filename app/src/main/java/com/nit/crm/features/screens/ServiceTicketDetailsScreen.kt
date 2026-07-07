package com.nit.crm.features.screens

import android.widget.Toast
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nit.crm.core.database.ServiceRecord
import com.nit.crm.features.PaymentAndServiceViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceTicketDetailsScreen(
    serviceViewModel: PaymentAndServiceViewModel,
    ticket: ServiceRecord,
    mainViewModel: com.nit.crm.features.MainViewModel,
    onNavigateTo: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var statusInput by remember { mutableStateOf(ticket.status) }
    var priorityInput by remember { mutableStateOf(ticket.priority) }
    var solutionInput by remember { mutableStateOf(ticket.solution) }
    var partsInput by remember { mutableStateOf(ticket.partsUsed) }
    var chargeInput by remember { mutableStateOf(if (ticket.laborCharge > 0.0) String.format(Locale.US, "%.2f", ticket.laborCharge) else "") }

    var statusExpanded by remember { mutableStateOf(false) }
    var priorityExpanded by remember { mutableStateOf(false) }

    var savedSigBitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(Unit) {
        ticket.technicianSignaturePath?.let { path ->
            try {
                val file = java.io.File(path)
                if (file.exists()) {
                    savedSigBitmap = android.graphics.BitmapFactory.decodeFile(path)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ticket Details", style = MaterialTheme.typography.headlineMedium) },
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // 1. Client Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = ticket.customerName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                text = ticket.id.substringAfterLast("-", ticket.id),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "Date Logged: ${SimpleDateFormat("MMM dd, yyyy  •  hh:mm a", Locale.getDefault()).format(Date(ticket.date))}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 6.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                        Text(
                            text = "Problem Summary:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = ticket.problem,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // 2. Status & Priority Selectors
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Status Selector
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedButton(
                            onClick = { statusExpanded = true },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Status: $statusInput")
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }

                        DropdownMenu(
                            expanded = statusExpanded,
                            onDismissRequest = { statusExpanded = false }
                        ) {
                            listOf("Open", "In Progress", "Pending Parts", "Completed").forEach { stat ->
                                DropdownMenuItem(
                                    text = { Text(stat) },
                                    onClick = {
                                        statusInput = stat
                                        statusExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Priority Selector
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedButton(
                            onClick = { priorityExpanded = true },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Prio: $priorityInput")
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }

                        DropdownMenu(
                            expanded = priorityExpanded,
                            onDismissRequest = { priorityExpanded = false }
                        ) {
                            listOf("Low", "Medium", "High").forEach { prio ->
                                DropdownMenuItem(
                                    text = { Text(prio) },
                                    onClick = {
                                        priorityInput = prio
                                        priorityExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // 3. Solution Details Text
            item {
                OutlinedTextField(
                    value = solutionInput,
                    onValueChange = { solutionInput = it },
                    label = { Text("Resolution / Action Taken") },
                    placeholder = { Text("Detail the steps taken to resolve this support ticket...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    shape = RoundedCornerShape(8.dp)
                )
            }

            // 4. Parts Used
            item {
                OutlinedTextField(
                    value = partsInput,
                    onValueChange = { partsInput = it },
                    label = { Text("Parts Replaced / Used") },
                    placeholder = { Text("e.g. CCTV Camera Sensor, BNC Cable...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )
            }

            // 5. Labor Charge Input
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = chargeInput,
                        onValueChange = { chargeInput = it },
                        label = { Text("Labor Service Charge") },
                        prefix = { Text("৳") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        trailingIcon = {
                            if (chargeInput.isNotEmpty()) {
                                IconButton(onClick = { chargeInput = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        }
                    )

                    // Quick Charges Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(0, 500, 1000, 2000).forEach { amount ->
                            val label = if (amount == 0) "Free" else "৳$amount"
                            OutlinedButton(
                                onClick = { chargeInput = amount.toString() },
                                modifier = Modifier.weight(1f).height(36.dp),
                                shape = RoundedCornerShape(18.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // 6. Signature canvas
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TECHNICIAN SIGN-OFF VERIFICATION",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Bold
                        )
                        Row {
                            Text(
                                text = "Full Screen",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clickable { 
                                    mainViewModel.initialSignaturePoints = mainViewModel.draftSignaturePoints.toList()
                                    mainViewModel.onSignatureCapturedAction = { points ->
                                        mainViewModel.draftSignaturePoints.clear()
                                        mainViewModel.draftSignaturePoints.addAll(points)
                                    }
                                    onNavigateTo("signature_capture")
                                },
                                fontWeight = FontWeight.Bold
                            )
                            if (mainViewModel.draftSignaturePoints.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Reset Signature",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.clickable { mainViewModel.draftSignaturePoints.clear() },
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Drawing canvas
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (mainViewModel.draftSignaturePoints.isEmpty()) {
                            savedSigBitmap?.let { bitmap ->
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = "Saved Signature",
                                    modifier = Modifier.fillMaxSize().padding(10.dp)
                                )
                            } ?: Text(
                                text = "Sign here to close ticket",
                                color = Color.LightGray,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        } else {
                            SignaturePad(
                                modifier = Modifier.fillMaxSize(),
                                points = mainViewModel.draftSignaturePoints
                            )
                        }
                    }
                }
            }

            // 7. Save Changes Button
            item {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            var sigPath = ticket.technicianSignaturePath
                            if (mainViewModel.draftSignaturePoints.isNotEmpty()) {
                                try {
                                    val sigBitmap = createBitmapFromPoints(mainViewModel.draftSignaturePoints.toList())
                                    val cacheFile = java.io.File(context.cacheDir, "srv_sig_${ticket.id}.png")
                                    val out = java.io.FileOutputStream(cacheFile)
                                    sigBitmap.compress(Bitmap.CompressFormat.PNG, 90, out)
                                    out.flush()
                                    out.close()
                                    sigPath = cacheFile.absolutePath
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                            
                            val chargeVal = chargeInput.toDoubleOrNull() ?: 0.0
                            val updatedRecord = ticket.copy(
                                status = statusInput,
                                priority = priorityInput,
                                solution = solutionInput,
                                partsUsed = partsInput,
                                laborCharge = chargeVal,
                                technicianSignaturePath = sigPath
                            )
                            serviceViewModel.saveServiceTicket(updatedRecord)
                            Toast.makeText(context, "Service ticket updated successfully!", Toast.LENGTH_SHORT).show()
                            onBack()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp).padding(vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Apply Updates", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}
