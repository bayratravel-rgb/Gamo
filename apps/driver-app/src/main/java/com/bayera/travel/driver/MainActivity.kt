// ... (Keeping the Imports and Crash Handler from before)
// ... (Update the RideCard and ActiveJobCard to include:)

@Composable
fun RideCard(trip: Trip, driverName: String) {
    Card(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("📍 From: ${trip.pickupLocation.address}", fontWeight = FontWeight.Bold)
            Text("🏁 To: ${trip.dropoffLocation.address}")
            if(trip.notes.isNotEmpty()) {
                Text("📝 Note: ${trip.notes}", color = Color.Red)
            }
            Text("💰 Fare: ${trip.price} ETB (${trip.paymentMethod})")
            
            Button(onClick = { 
                FirebaseDatabase.getInstance().getReference("trips").child(trip.tripId)
                   .updateChildren(mapOf(
                       "status" to "ACCEPTED", 
                       "driverId" to driverName,
                       "driverName" to driverName
                   ))
            }, modifier = Modifier.fillMaxWidth()) { Text("Accept Ride") }
        }
    }
}
