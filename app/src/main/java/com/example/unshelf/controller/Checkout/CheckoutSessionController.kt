package com.example.unshelf.controller.Checkout

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.unshelf.controller.DataFetch.DataFetchController
import com.example.unshelf.model.checkout.*
import com.example.unshelf.model.entities.Order
import com.example.unshelf.model.entities.Product
import com.example.unshelf.model.entities.ProductDetailsModel
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import java.io.IOException
import java.util.Date

class CheckoutSessionController() {
    private val gson = Gson()
    val db = Firebase.firestore

    suspend fun createCheckoutSession(cart :  MutableMap<String, MutableList<Product>>) : FullCheckoutModel? {
        var products = mutableListOf<Product>()
        for((storeID, productList) in cart) {
            productList.forEach() {
                if(it.active) {
                    products.add(it)
                    println(it.productName + ": " + it.quantity)
                }
            }
        }
        //val products = db.collection("products").get().await().toObjects(Product::class.java)
        val currentUser = FirebaseAuth.getInstance().currentUser
        var email = ""
        var name = ""
        var number = ""
        if (currentUser != null) {
            val uid = currentUser.uid
            email = currentUser.email as String
            try {
                val userSnapshot = db.collection("customers").document(uid).get().await()

                if (userSnapshot.exists()) {
                    name = userSnapshot.getString("fullName").orEmpty()
                    number = userSnapshot.getLong("phoneNumber").toString()
                }
            } catch (e: Exception) {
                Log.e("Checkout", "Error retrieving user information", e)
            }
        }
        val billing = partBilling (
            email = email,
            name = name,
            phone = number,
        )
        var checkoutList = mutableListOf<partLineItem>()
        products.forEach{ product ->
            val lineItem = partLineItem(
                amount = (product.sellingPrice * 100).toInt(),
                name = product.productName,
                sellerID = product.storeName,
                quantity = product.quantity,
                images = listOf(product.thumbnail),
            )
            checkoutList.add(lineItem)
        }
//        val liSalad = partLineItem(
//            amount = (products.get(0).price * 100).toInt(),
//            name = products.get(0).productName,
//            sellerID = products.get(0).storeName,
//            quantity = 2,
//            images = listOf(products.get(0).thumbnail),
//        )
//        val liSalad2 = partLineItem(
//            amount = (products.get(1).price * 100).toInt(),
//            name = products.get(1).productName,
//            sellerID = products.get(1).storeName,
//            quantity = 2,
//            images = listOf(products.get(1).thumbnail),
//        )
//        val basketList: List<partLineItem> = listOf(
//            liSalad, liSalad2
//        )
        val att = partAttributes(
            billing = billing,
            line_items = checkoutList,
            payment_method_types = listOf("gcash", "paymaya"),
            send_email_receipt = true,
            show_line_items = true
        )
        val testCheckout = partCheckout(partData(att))

        val checkoutJson: String = gson.toJson(testCheckout)

        try {
            val client = OkHttpClient()

            val mediaType = "application/json".toMediaTypeOrNull()
            val body = RequestBody.create(mediaType, checkoutJson)
            val request = Request.Builder()
                .url("https://api.paymongo.com/v1/checkout_sessions")
                .post(body)
                .addHeader("accept", "application/json")
                .addHeader("Content-Type", "application/json")
                .addHeader("authorization", "Basic c2tfdGVzdF9VMkNSQ0o3UGlTOFpUSlN4VDluUGtLUzQ6")
                .build()

            val response = withContext(Dispatchers.IO) {
                client.newCall(request).execute()
            }
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                val retrievedCheckout: FullCheckoutModel = gson.fromJson(responseBody, FullCheckoutModel::class.java)
                return retrievedCheckout
            } else {
                println(response.code)
            }
        } catch (e: IOException) {
            e.printStackTrace()
            throw e
        }
        return null
    }

    suspend fun retrieveCheckout(checkoutID: String): CheckoutResponse? {
        val client = OkHttpClient()

        val request = Request.Builder()
            .url("https://api.paymongo.com/v1/checkout_sessions/$checkoutID")
            .get()
            .addHeader("accept", "application/json")
            .addHeader("authorization", "Basic c2tfdGVzdF9VMkNSQ0o3UGlTOFpUSlN4VDluUGtLUzQ6")
            .build()

        val response = client.newCall(request).execute()
        if (response.isSuccessful) {
            val responseBody = response.body?.string()
            val retrievedCheckout: CheckoutResponse = gson.fromJson(responseBody, CheckoutResponse::class.java)
            return retrievedCheckout
        } else {
            println(response.code)
        }
        return null
    }

    suspend fun placeOrder(checkoutID: String) {
        val checkoutResponse = retrieveCheckout(checkoutID)
        if(checkoutResponse!= null) {
            var amountPerProduct = checkoutResponse.data.attributes.lineItems
            for(product in amountPerProduct) {
                product.amount = product.amount / 100.0
            }
            val timestamp = checkoutResponse.data.attributes.paidAt
            val date = Date(timestamp * 1000)
            val paymentID = checkoutResponse.data.attributes.payments.get(0).id
            val customerID = FirebaseAuth.getInstance().currentUser?.uid
            val totalAmount = checkoutResponse.data.attributes.payments.get(0).attributes.amount / 100.0
            val fee = checkoutResponse.data.attributes.payments.get(0).attributes.fee / 100.0
            val netAmount = totalAmount - fee
            val method = checkoutResponse.data.attributes.paymentMethodUsed
            val products = checkoutResponse.data.attributes.lineItems
            val status = checkoutResponse.data.attributes.payments.get(0).attributes.status
            val order = Order(checkoutID,paymentID,date,customerID.toString(),products,totalAmount,fee,netAmount,status,method)
            db.collection("orders").add(order).await()
        }
    }
}


