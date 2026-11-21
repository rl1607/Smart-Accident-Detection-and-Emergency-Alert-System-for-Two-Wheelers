package com.example.smartaccidentdetection.ui.screens.profile

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartaccidentdetection.data.repository.AppRepository
import com.example.smartaccidentdetection.ui.theme.PrimaryBlue
import com.example.smartaccidentdetection.ui.theme.SafetyGreen

@Composable
fun ProfileScreen(
    repository: AppRepository
) {
    val userProfile by repository.userProfile.collectAsState()
    val context = LocalContext.current

    var isEditing by remember { mutableStateOf(false) }
    var name by remember(userProfile) {
        mutableStateOf(userProfile.fullName?.takeIf { it.isNotBlank() && it != "null" } ?: "Rahul")
    }
    var phone by remember(userProfile) {
        mutableStateOf(userProfile.phoneNumber?.takeIf { it.isNotBlank() && it != "null" } ?: "+919876543210")
    }
    var email by remember(userProfile) {
        mutableStateOf(userProfile.email?.takeIf { it.isNotBlank() && it != "null" } ?: "")
    }
    var vehicleNumber by remember(userProfile) {
        mutableStateOf(userProfile.vehicleNumber?.takeIf { it.isNotBlank() && it != "null" } ?: "ACCIDENT-001")
    }
    var vehicleModel by remember(userProfile) {
        mutableStateOf(userProfile.vehicleModel?.takeIf { it.isNotBlank() && it != "null" } ?: "Two-Wheeler (ESP32)")
    }
    var bloodGroup by remember(userProfile) {
        mutableStateOf(userProfile.bloodGroup?.takeIf { it.isNotBlank() && it != "null" } ?: "O+ve")
    }
    var medicalNotes by remember(userProfile) {
        mutableStateOf(userProfile.medicalNotes?.takeIf { it.isNotBlank() && it != "null" } ?: "None")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Rider Profile",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Personal details and vehicle registration for emergency responders.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Avatar Header Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(PrimaryBlue.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = PrimaryBlue,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    val displayName = userProfile.fullName?.takeIf { it.isNotBlank() && it != "null" } ?: "Rahul"
                    val displayModel = userProfile.vehicleModel?.takeIf { it.isNotBlank() && it != "null" } ?: "Two-Wheeler (ESP32)"
                    val displayNumber = userProfile.vehicleNumber?.takeIf { it.isNotBlank() && it != "null" } ?: "ACCIDENT-001"

                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$displayModel • $displayNumber",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Profile Form Fields
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Personal & Contact Info",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    enabled = isEditing,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Rider Phone Number") },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                    enabled = isEditing,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                    enabled = isEditing,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Vehicle Information",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = vehicleModel,
                    onValueChange = { vehicleModel = it },
                    label = { Text("Vehicle Make & Model") },
                    leadingIcon = { Icon(Icons.Default.DirectionsBike, contentDescription = null) },
                    enabled = isEditing,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = vehicleNumber,
                    onValueChange = { vehicleNumber = it },
                    label = { Text("Vehicle Registration Plate") },
                    enabled = isEditing,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Medical & Emergency Info",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = bloodGroup,
                    onValueChange = { bloodGroup = it },
                    label = { Text("Blood Group") },
                    leadingIcon = { Icon(Icons.Default.MedicalServices, contentDescription = null) },
                    enabled = isEditing,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = medicalNotes,
                    onValueChange = { medicalNotes = it },
                    label = { Text("Medical Notes / Allergies") },
                    enabled = isEditing,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (isEditing) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                // Revert
                                name = userProfile.fullName?.takeIf { it.isNotBlank() && it != "null" } ?: "Rahul"
                                phone = userProfile.phoneNumber?.takeIf { it.isNotBlank() && it != "null" } ?: "+919876543210"
                                email = userProfile.email?.takeIf { it.isNotBlank() && it != "null" } ?: ""
                                vehicleModel = userProfile.vehicleModel?.takeIf { it.isNotBlank() && it != "null" } ?: "Two-Wheeler (ESP32)"
                                vehicleNumber = userProfile.vehicleNumber?.takeIf { it.isNotBlank() && it != "null" } ?: "ACCIDENT-001"
                                bloodGroup = userProfile.bloodGroup?.takeIf { it.isNotBlank() && it != "null" } ?: "O+ve"
                                medicalNotes = userProfile.medicalNotes?.takeIf { it.isNotBlank() && it != "null" } ?: "None"
                                isEditing = false
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Cancel")
                        }

                        Button(
                            onClick = {
                                repository.updateProfile(
                                    userProfile.copy(
                                        fullName = name.trim(),
                                        phoneNumber = phone.trim(),
                                        email = email.trim(),
                                        vehicleModel = vehicleModel.trim(),
                                        vehicleNumber = vehicleNumber.trim(),
                                        bloodGroup = bloodGroup.trim(),
                                        medicalNotes = medicalNotes.trim()
                                    )
                                )
                                isEditing = false
                                Toast.makeText(context, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save Profile")
                        }
                    }
                } else {
                    Button(
                        onClick = { isEditing = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Edit Profile")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
