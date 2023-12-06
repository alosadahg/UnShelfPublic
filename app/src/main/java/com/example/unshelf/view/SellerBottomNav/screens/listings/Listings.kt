package com.example.unshelf.view.SellerBottomNav.screens.listings

// or, if you're using StateFlow or LiveData:
import JostFontFamily
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberAsyncImagePainter
import com.example.unshelf.R
import com.example.unshelf.model.entities.Product
import com.example.unshelf.ui.theme.DeepMossGreen
import com.example.unshelf.ui.theme.MiddleGreenYellow
import com.example.unshelf.ui.theme.WatermelonRed
import com.example.unshelf.view.SellerBottomNav.screens.dashboard.sellerId
import com.example.unshelf.view.SellerBottomNav.screens.dashboard.storeId
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow



var productID = mutableStateOf<String?>(null)
//data class Product(
//    val thumbnail: String,
//    val name: String,
//    val quantity: Int,
//    val price: Double
//)

// ViewModel to handle fetching products



// Sample data

@Preview(showBackground = true)
@Composable
fun PreviewListings() {
    // Mock NavController for the preview
    val navController = rememberNavController()


    // Pass the mockSellerId to the Listings composable
    Listings(navController, sellerId.value, storeId.value)
}


@Composable
fun Listings(navController: NavController, sellerId: String, storeId: String) {
    Column {

        TopBar(navController)
        FilterTabs()
        ProductList(sellerId = sellerId, storeId = storeId, navController)
    }
}

@Composable
fun TopBar(navController: NavController) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Transparent) // Use your desired background color
            .border(
                width = 1.dp, // Set the border width
                color = DeepMossGreen, // Set the border color
                shape = RectangleShape // Use RectangleShape for a straight line
            )
            .padding(bottom = 1.dp) // This is to offset the border's width from the content
    ){
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DeepMossGreen), // Replace with your desired background color
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Listings",
                fontFamily = JostFontFamily,
                color = Color.White,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 16.dp),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.weight(1f))  // This spacer will push the elements to each end of the Row
            Text(
                text = "Add Items",
                color = Color.White,
                modifier = Modifier.align(Alignment.CenterVertically),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            IconButton(onClick = {
                productID.value = null  // Set productID to null for adding a new product
                navController.navigate("addProduct/${productID.value}")},

            ) {
                Icon(
                    painter = painterResource(id = R.drawable.button_plus), // Replace with your add icon resource
                    contentDescription = "Add",
                    tint = Color.White
                )
            }
        }
    }

}


@Composable
fun FilterTabs() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)

    ) {
        Button(

            onClick = { /* TODO: Handle Active filter action */ },
            colors = ButtonDefaults.buttonColors(MiddleGreenYellow), // Replace with your active tab color
        ) {
            Text(text = "Active")
        }
        Spacer(modifier = Modifier.size(8.dp))
        Button(
            onClick = { /* TODO: Handle Unlisted filter action */ },
            colors = ButtonDefaults.buttonColors(WatermelonRed) // Replace with your unlisted tab color
        ) {
            Text(text = "Unlisted")
        }
        Spacer(modifier = Modifier.weight(1f))
        IconButton(onClick = { /* TODO: Handle sort action */ }) {
            Icon(
                painter = painterResource(id = R.drawable.button_sort), // Replace with your sort icon resource
                contentDescription = "Sort",
                tint = DeepMossGreen
            )
        }
    }
}

@Composable
fun ProductList(sellerId: String, storeId: String, navController: NavController) {
    // Use a ViewModel to manage the state and business logic
    val productViewModel: ProductViewModel = viewModel()

    // Call a function in your ViewModel to fetch products for the given sellerId
    LaunchedEffect(sellerId) {
        productViewModel.fetchProductsForSeller(sellerId, storeId)
    }

    // Observe the product list from your ViewModel
    val products = productViewModel.products.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFE8F5E9)) // Adjust the background color as needed
    ) {
        items(products.value) { product ->
            ProductCard(product, navController)
        }
    }
}
@Composable
fun ProductCard(product: Product, navController: NavController) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = MaterialTheme.shapes.medium // This gives the Card rounded corners.
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.background(Color.White)
        ) {
            val painter = rememberAsyncImagePainter(model = product.thumbnail)
            Image(
                painter = painter,
                contentDescription = product.productName,
                modifier = Modifier
                    .size(80.dp)
                    .padding(8.dp)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp)
            ) {
                Text(
                    text = product.productName,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold, // Set the font weight to bold
                    fontFamily = FontFamily.Serif
                )
                Text(text = "Quantity: ${product.quantity}", color = Color.Gray)
                Text(text = "₱${product.marketPrice}", color = Color.Gray)
            }
            IconButton(onClick = { navController.navigate("addProduct/${productID.value}")
            }) {
                Icon(
                    painter = painterResource(id = R.drawable.button_edit),
                    contentDescription = "Edit",
                    tint = Color(0xFF4CAF50) // You can also remove the tint if your drawable has its own colors
                )
            }

            IconButton(onClick = { /* TODO: Handle delete action */ }) {
                Icon(
                    painter = painterResource(id = R.drawable.button_delete),
                    contentDescription = "Delete",
                    tint = WatermelonRed // Adjust the tint color as needed
                )
            }
        }
    }
}


class ProductViewModel : ViewModel() {
    private val _products = MutableStateFlow<List<Product>>(listOf())
    val products = _products.asStateFlow()

    fun fetchProductsForSeller(sellerId: String, storeId: String) {
        val db = Firebase.firestore
        db.collection("sellers").document(sellerId)
            .collection("store").document(storeId)
            .collection("products")
            .get()
            .addOnSuccessListener { documents ->
                val productList = documents.map { document ->
                    // Log the product ID
                    Log.d("ProductViewModel", "Product ID: ${document.id}")
                    productID.value = document.id
                    val categories = document.get("categories") as? List<String> ?: listOf("Unknown")
                    val description = document.getString("description") ?: "Unknown"
                    val discount = document.getLong("discount") ?: 0L
                    val expirationDate = document.getString("expirationDate") ?: "0/00/0000"
                    val gallery = document.getString("gallery") ?: ""  // Assuming gallery is a single String
                    val hashtags = document.get("hashtags") as? List<String> ?: listOf()
                    val name = document.getString("productName") ?: "Unknown"
                    val quantity = document.getLong("quantity")?.toInt() ?: 0
                    val price = document.getDouble("marketPrice")?.toLong() ?: 0L
                    val thumbnailUri = document.getString("thumbnail") ?: ""
                    Product(name, categories, thumbnailUri, gallery, description, price, hashtags, expirationDate, discount, quantity)
                }
                _products.value = productList
            }
            .addOnFailureListener { exception ->
                Log.w("ProductViewModel", "Error getting documents: ", exception)
            }
    }


}