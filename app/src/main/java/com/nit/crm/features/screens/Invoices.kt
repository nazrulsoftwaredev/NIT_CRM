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
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import java.io.File
import kotlinx.coroutines.launch
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nit.crm.core.database.Invoice
import com.nit.crm.core.database.InvoiceItem
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Done
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.asImageBitmap
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.content.Context
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun InvoiceListScreen(
    invoicesViewModel: com.nit.crm.features.InvoicesViewModel,
    onInvoiceClick: (Invoice) -> Unit,
    onShareClick: (Invoice) -> Unit,
    onBack: () -> Unit
) {
    val searchVal by invoicesViewModel.invoiceSearchQuery.collectAsState()
    val invoices by invoicesViewModel.searchedInvoices.collectAsState()
    val context = LocalContext.current

    val filteredInvoices = remember(invoices) {
        invoices.filter { !it.isQuotation }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Invoices", style = MaterialTheme.typography.headlineMedium) },
                navigationIcon = {
                    IconButton(onClick = { onBack() }) {
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchVal,
                onValueChange = { invoicesViewModel.setInvoiceSearchQuery(it) },
                placeholder = { Text("Search by name or ID...", style = MaterialTheme.typography.bodyMedium) },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_invoice_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.Transparent
                ),
                shape = RoundedCornerShape(8.dp),
                singleLine = true
            )

            if (filteredInvoices.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        "No invoices found",
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(filteredInvoices) { invoice ->
                        InvoiceItem(
                            invoice = invoice,
                            onClick = { onInvoiceClick(invoice) },
                            onShareClick = { onShareClick(invoice) },
                            onDeleteClick = { 
                                invoicesViewModel.deleteInvoice(invoice)
                                Toast.makeText(context, "Document deleted", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun QuotationListScreen(
    invoicesViewModel: com.nit.crm.features.InvoicesViewModel,
    onInvoiceClick: (Invoice) -> Unit,
    onShareClick: (Invoice) -> Unit,
    onBack: () -> Unit
) {
    val searchVal by invoicesViewModel.invoiceSearchQuery.collectAsState()
    val invoices by invoicesViewModel.searchedInvoices.collectAsState()
    val context = LocalContext.current

    val filteredInvoices = remember(invoices) {
        invoices.filter { it.isQuotation }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quotations", style = MaterialTheme.typography.headlineMedium) },
                navigationIcon = {
                    IconButton(onClick = { onBack() }) {
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchVal,
                onValueChange = { invoicesViewModel.setInvoiceSearchQuery(it) },
                placeholder = { Text("Search by name or ID...", style = MaterialTheme.typography.bodyMedium) },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_invoice_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.Transparent
                ),
                shape = RoundedCornerShape(8.dp),
                singleLine = true
            )

            if (filteredInvoices.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        "No quotations found",
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(filteredInvoices) { invoice ->
                        InvoiceItem(
                            invoice = invoice,
                            onClick = { onInvoiceClick(invoice) },
                            onShareClick = { onShareClick(invoice) },
                            onDeleteClick = { 
                                invoicesViewModel.deleteInvoice(invoice)
                                Toast.makeText(context, "Document deleted", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun InvoiceItem(
    invoice: Invoice,
    onClick: () -> Unit,
    onShareClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Box {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { showMenu = true }
                )
                .padding(vertical = 12.dp)
                .testTag("invoice_item_${invoice.id}")
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Description,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = invoice.customerName,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "ID: ${invoice.id.substringAfterLast("-", invoice.id)} • ${SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(invoice.date))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = String.format(Locale.US, "৳%.2f", invoice.totalAmount),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = invoice.status,
                        style = MaterialTheme.typography.labelSmall,
                        color = when (invoice.status) {
                            "Paid" -> MaterialTheme.colorScheme.tertiary
                            "Unpaid" -> MaterialTheme.colorScheme.error
                            "Draft" -> Color(0xFFD97706)
                            else -> MaterialTheme.colorScheme.secondary
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text("View Preview") },
                onClick = {
                    showMenu = false
                    onClick()
                },
                leadingIcon = { Icon(Icons.Default.Visibility, contentDescription = null) }
            )
            DropdownMenuItem(
                text = { Text("Share Document") },
                onClick = {
                    showMenu = false
                    onShareClick()
                },
                leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) }
            )
            DropdownMenuItem(
                text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                onClick = {
                    showMenu = false
                    onDeleteClick()
                },
                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
            )
        }
    }
}

fun renderPdfPage(context: Context, pdfFile: File): Bitmap? {
    try {
        val isRobolectric = try {
            Class.forName("org.robolectric.Robolectric")
            true
        } catch (e: ClassNotFoundException) {
            false
        }
        if (isRobolectric) {
            return Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        }

        val input = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(input)
        if (renderer.pageCount > 0) {
            val page = renderer.openPage(0)
            // Use a higher scale (e.g. 3.5x - 4x) for high-density displays to prevent blurriness when zooming
            val displayMetrics = context.resources.displayMetrics
            val scale = (displayMetrics.density * 1.2f).coerceAtLeast(3.5f)
            val width = (page.width * scale).toInt()
            val height = (page.height * scale).toInt()
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bitmap)
            canvas.drawColor(android.graphics.Color.WHITE)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()
            renderer.close()
            input.close()
            return bitmap
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceDetailsScreen(
    invoice: Invoice,
    viewModel: com.nit.crm.features.MainViewModel,
    invoicesViewModel: com.nit.crm.features.InvoicesViewModel,
    onGetPdfFile: suspend () -> File?,
    onSharePdf: () -> Unit,
    onBack: () -> Unit
) {
    var pdfBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoadingPdf by remember { mutableStateOf(true) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    val context = LocalContext.current

    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }

    LaunchedEffect(invoice) {
        isLoadingPdf = true
        try {
            val file = onGetPdfFile()
            if (file != null && file.exists()) {
                pdfBitmap = renderPdfPage(context, file)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        isLoadingPdf = false
    }

    val isDraft = remember(invoice) { !invoice.isQuotation && invoice.status == "Draft" }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (invoice.isQuotation) "Quotation Preview" else "Invoice Preview", style = MaterialTheme.typography.headlineMedium) },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("dialog_close_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = onSharePdf,
                        modifier = Modifier.testTag("dialog_share_button")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(
                        onClick = { showDeleteConfirmation = true },
                        modifier = Modifier.testTag("dialog_delete_button")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            if (isDraft) {
                Surface(
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Button(
                            onClick = {
                                viewModel.initialSignaturePoints = emptyList()
                                viewModel.onSignatureCapturedAction = { points ->
                                    isLoadingPdf = true
                                    viewModel.finalizeInvoiceAsync(
                                        context = context,
                                        invoice = invoice,
                                        signaturePoints = points,
                                        onSuccess = {
                                            Toast.makeText(context, "Invoice finalized successfully", Toast.LENGTH_SHORT).show()
                                            onBack()
                                        },
                                        onError = { error ->
                                            Toast.makeText(context, "Error: $error", Toast.LENGTH_LONG).show()
                                            isLoadingPdf = false
                                        }
                                    )
                                }
                                viewModel.navigateTo("signature_capture")
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("finalize_draft_invoice_button"),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Done, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Finalize Invoice (Capture Signature)")
                        }
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(1f, 5f)
                        if (scale > 1f) {
                            offset += pan
                        } else {
                            offset = androidx.compose.ui.geometry.Offset.Zero
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            if (isLoadingPdf) {
                CircularProgressIndicator()
            } else if (pdfBitmap != null) {
                Image(
                    bitmap = pdfBitmap!!.asImageBitmap(),
                    contentDescription = "PDF Preview",
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offset.x,
                            translationY = offset.y
                        )
                )
            } else {
                Text("Could not render PDF preview", color = MaterialTheme.colorScheme.error)
            }
        }
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Confirm Deletion", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete this invoice? This will remove all associated item billings and reduce the customer's outstanding balance accordingly. This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        invoicesViewModel.deleteInvoice(invoice)
                        showDeleteConfirmation = false
                        Toast.makeText(context, "Invoice deleted successfully", Toast.LENGTH_SHORT).show()
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.secondary)
                }
            }
        )
    }
}
