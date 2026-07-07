package com.nit.crm.features.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.graphics.asImageBitmap
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.util.Base64
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nit.crm.features.DashboardViewModel
import com.nit.crm.features.SettingsViewModel
import java.io.ByteArrayOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    dashboardViewModel: DashboardViewModel,
    settingsViewModel: SettingsViewModel,
    onNavigateTo: (String) -> Unit,
    mainViewModel: com.nit.crm.features.MainViewModel,
    onMenuClick: () -> Unit
) {
    val pendingSync by dashboardViewModel.pendingSyncItems.collectAsState()
    val context = LocalContext.current

    var companyNameInput by remember { mutableStateOf(settingsViewModel.companyName) }
    var companyAddressInput by remember { mutableStateOf(settingsViewModel.companyAddress) }
    var companyPhoneInput by remember { mutableStateOf(settingsViewModel.companyPhone) }
    var companyTaxIdInput by remember { mutableStateOf(settingsViewModel.companyTaxId) }
    var companyLogoBase64 by remember { mutableStateOf(settingsViewModel.companyLogoBase64) }
    var companySignatureBase64 by remember { mutableStateOf(settingsViewModel.companySignatureBase64) }

    var monogramText by remember { mutableStateOf("") }
    var genNameText by remember { mutableStateOf("") }


    val logoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            uri?.let {
                try {
                    val inputStream = context.contentResolver.openInputStream(it)
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    if (bitmap != null) {
                        // Resize if too large
                        val scaledBitmap = if (bitmap.width > 500 || bitmap.height > 500) {
                            val ratio = bitmap.width.toFloat() / bitmap.height.toFloat()
                            if (ratio > 1) {
                                Bitmap.createScaledBitmap(bitmap, 500, (500 / ratio).toInt(), true)
                            } else {
                                Bitmap.createScaledBitmap(bitmap, (500 * ratio).toInt(), 500, true)
                            }
                        } else bitmap
                        
                        val outputStream = ByteArrayOutputStream()
                        scaledBitmap.compress(Bitmap.CompressFormat.PNG, 90, outputStream)
                        companyLogoBase64 = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Failed to load image", Toast.LENGTH_SHORT).show()
                }
            }
        }
    )

    var isSaving by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
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

            // 1. Profile Preview Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Display logo bitmap if present, otherwise initial letter
                        val logoBitmap = remember(companyLogoBase64) {
                            decodeBase64ToBitmap(companyLogoBase64)
                        }

                        if (logoBitmap != null) {
                            Image(
                                bitmap = logoBitmap.asImageBitmap(),
                                contentDescription = "Company Logo",
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .border(1.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                    .clickable {
                                        logoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    }
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = companyNameInput.firstOrNull()?.uppercase() ?: "N",
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }

                        Column {
                            Text(
                                text = companyNameInput.ifBlank { "Your Company" },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = companyTaxIdInput.ifBlank { "Invoice Subtitle Info" },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }

            // 2. Company Details Inputs
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text(
                            text = "COMPANY DETAILS",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )

                        OutlinedTextField(
                            value = companyNameInput,
                            onValueChange = { companyNameInput = it },
                            label = { Text("Company Name") },
                            leadingIcon = { Icon(Icons.Default.Business, contentDescription = null, tint = MaterialTheme.colorScheme.secondary) },
                            modifier = Modifier.fillMaxWidth().testTag("company_name_input"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = companyTaxIdInput,
                            onValueChange = { companyTaxIdInput = it },
                            label = { Text("Tax ID / Invoice Subtitle") },
                            leadingIcon = { Icon(Icons.Default.Receipt, contentDescription = null, tint = MaterialTheme.colorScheme.secondary) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = companyPhoneInput,
                            onValueChange = { companyPhoneInput = it },
                            label = { Text("Contact Phone") },
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = MaterialTheme.colorScheme.secondary) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = companyAddressInput,
                            onValueChange = { companyAddressInput = it },
                            label = { Text("Billing Address") },
                            leadingIcon = { Icon(Icons.Default.Place, contentDescription = null, tint = MaterialTheme.colorScheme.secondary) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            maxLines = 3
                        )
                    }
                }
            }

            // 3. Logo Management section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text(
                            text = "COMPANY LOGO",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = {
                                    logoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Upload, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Upload Image")
                            }

                            OutlinedButton(
                                onClick = {
                                    if (monogramText.isNotBlank()) {
                                        val base64 = generateMonogramLogo(monogramText)
                                        companyLogoBase64 = base64
                                        Toast.makeText(context, "Logo generated from text", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Enter text for monogram", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Use Monogram")
                            }
                        }

                        if (companyLogoBase64.isNotBlank()) {
                            val logoBitmap = remember(companyLogoBase64) {
                                decodeBase64ToBitmap(companyLogoBase64)
                            }
                            if (logoBitmap != null) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(120.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        bitmap = logoBitmap.asImageBitmap(),
                                        contentDescription = "Uploaded Logo Preview",
                                        modifier = Modifier.fillMaxHeight().padding(8.dp)
                                    )
                                }
                            }
                        }

                        if (!companyLogoBase64.isBlank() || monogramText.isNotBlank()) {
                            OutlinedTextField(
                                value = monogramText,
                                onValueChange = { monogramText = it },
                                label = { Text("Monogram Text (e.g. NIT)") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                        }

                        if (companyLogoBase64.isNotBlank()) {
                            TextButton(
                                onClick = { companyLogoBase64 = "" },
                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Remove Logo")
                            }
                        }
                    }
                }
            }

            // 4. Authorized Signature Canvas
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(
                                text = "SIGNATURE",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Row {
                                TextButton(onClick = {
                                    mainViewModel.initialSignaturePoints = mainViewModel.draftSignaturePoints.toList()
                                    mainViewModel.onSignatureCapturedAction = { points ->
                                        mainViewModel.draftSignaturePoints.clear()
                                        mainViewModel.draftSignaturePoints.addAll(points)
                                    }
                                    onNavigateTo("signature_capture")
                                }) {
                                    Icon(Icons.Default.Fullscreen, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Full Screen", style = MaterialTheme.typography.labelLarge)
                                }
                                TextButton(onClick = {
                                    mainViewModel.draftSignaturePoints.clear()
                                    companySignatureBase64 = ""
                                    Toast.makeText(context, "Signature Cleared", Toast.LENGTH_SHORT).show()
                                }) {
                                    Text("Clear", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Drawing canvas
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                        ) {
                            SignaturePad(
                                modifier = Modifier.fillMaxSize(),
                                points = mainViewModel.draftSignaturePoints
                            )

                            if (mainViewModel.draftSignaturePoints.isEmpty()) {
                                val savedBitmap = remember(companySignatureBase64) {
                                    decodeBase64ToBitmap(companySignatureBase64)
                                }
                                if (savedBitmap != null) {
                                    Image(
                                        bitmap = savedBitmap.asImageBitmap(),
                                        contentDescription = "Saved Signature",
                                        modifier = Modifier.fillMaxSize().padding(16.dp)
                                    )
                                } else {
                                    Text(
                                        text = "Draw signature here",
                                        color = androidx.compose.ui.graphics.Color.LightGray,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = genNameText,
                                onValueChange = { genNameText = it },
                                placeholder = { Text("Or type name to generate signature...", fontSize = 13.sp) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            )
                            Button(
                                onClick = {
                                    if (genNameText.isNotBlank()) {
                                        val base64 = generateTextSignature(genNameText)
                                        companySignatureBase64 = base64
                                        mainViewModel.draftSignaturePoints.clear()
                                        Toast.makeText(context, "Signature generated!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                Text("Generate", fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            // 5. Save Configuration Button
            item {
                Button(
                    onClick = {
                        isSaving = true

                        // Auto-capture drawing if signature pad is not empty
                        if (mainViewModel.draftSignaturePoints.isNotEmpty()) {
                            val bitmap = createBitmapFromPoints(mainViewModel.draftSignaturePoints.toList(), 800, 320)
                            val outputStream = ByteArrayOutputStream()
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                            val base64 = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
                            companySignatureBase64 = base64
                        }

                        settingsViewModel.saveSettings(
                            name = companyNameInput,
                            address = companyAddressInput,
                            phone = companyPhoneInput,
                            taxId = companyTaxIdInput,
                            logoBase64 = companyLogoBase64,
                            signatureBase64 = companySignatureBase64
                        )
                        Toast.makeText(context, "Settings Saved Successfully", Toast.LENGTH_SHORT).show()
                        isSaving = false
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save Configuration", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }

            // 6. Sync status
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudSync,
                                contentDescription = null,
                                tint = if (pendingSync.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(28.dp)
                            )
                            Column {
                                Text(
                                    text = if (pendingSync.isNotEmpty()) "${pendingSync.size} items pending sync" else "Database in sync",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Offline SQLite DB Cache",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Sync Mode: ${com.nit.crm.core.database.FirebaseSyncHelper.statusMessage}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (com.nit.crm.core.database.FirebaseSyncHelper.isMockMode()) 
                                        MaterialTheme.colorScheme.secondary 
                                    else 
                                        MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        if (pendingSync.isNotEmpty()) {
                            TextButton(
                                onClick = { dashboardViewModel.triggerManualSync() }
                            ) {
                                Text("Sync Now", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }

}

// Helpers
private fun decodeBase64ToBitmap(base64Str: String): Bitmap? {
    if (base64Str.isBlank()) return null
    return try {
        val decodedBytes = Base64.decode(base64Str, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
    } catch (e: Exception) {
        null
    }
}

private fun generateMonogramLogo(text: String): String {
    val bitmap = Bitmap.createBitmap(120, 120, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val paint = Paint().apply {
        color = Color.rgb(30, 41, 59) // Slate 800
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    canvas.drawCircle(60f, 60f, 60f, paint)

    paint.apply {
        color = Color.rgb(14, 165, 233) // Sky 500
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }
    canvas.drawCircle(60f, 60f, 54f, paint)

    paint.apply {
        color = Color.WHITE
        style = Paint.Style.FILL
        textSize = 42f
        typeface = Typeface.create("sans-serif", Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }
    val textHeight = paint.descent() - paint.ascent()
    val textOffset = textHeight / 2 - paint.descent()
    canvas.drawText(text.take(3).uppercase(), 60f, 60f + textOffset, paint)

    val outputStream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
    return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
}

private fun generateTextSignature(name: String): String {
    val bitmap = Bitmap.createBitmap(400, 150, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val paint = Paint().apply {
        color = Color.BLACK
        isAntiAlias = true
        textSize = 48f
        val baseTypeface = try {
            Typeface.create("cursive", Typeface.ITALIC)
        } catch (e: Exception) {
            Typeface.create(Typeface.SERIF, Typeface.ITALIC)
        }
        typeface = baseTypeface
        textAlign = Paint.Align.CENTER
    }

    val textHeight = paint.descent() - paint.ascent()
    val textOffset = textHeight / 2 - paint.descent()
    val textX = 200f
    val textY = 75f + textOffset

    canvas.drawText(name, textX, textY, paint)

    paint.apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        color = Color.BLACK
    }
    val textWidth = paint.measureText(name)
    val startX = textX - (textWidth / 2) - 15f
    val endX = textX + (textWidth / 2) + 25f
    val y = textY + 12f

    val path = android.graphics.Path().apply {
        moveTo(startX, y)
        quadTo(textX, y + 10f, endX, y - 5f)
    }
    canvas.drawPath(path, paint)

    val outputStream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
    return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
}

