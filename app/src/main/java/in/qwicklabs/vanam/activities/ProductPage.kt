package `in`.qwicklabs.vanam.activities

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.bumptech.glide.Glide
import `in`.qwicklabs.vanam.databinding.ActivityProductPageBinding
import `in`.qwicklabs.vanam.model.OrderItem
import `in`.qwicklabs.vanam.model.StoreItem
import `in`.qwicklabs.vanam.repository.FirebaseRepository
import `in`.qwicklabs.vanam.utils.UtilityFunctions
import `in`.qwicklabs.vanam.viewModel.UserViewModel

class ProductPage : AppCompatActivity() {
    private lateinit var binding: ActivityProductPageBinding
    private val userViewModel: UserViewModel by viewModels()
    private var storeItem: StoreItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProductPageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        storeItem = intent.getParcelableExtra("item")

        binding.apply {
            itemName.text = storeItem?.name ?: "Not Available"
            itemDes.text = storeItem?.description ?: "Not Available"
            itemCost.text = storeItem?.price?.toString() ?: "0"
            Glide.with(this@ProductPage).load(storeItem?.image).into(itemImage)
        }

        setupUI()
        observeUserData()

        binding.apply {
            toolbar.setNavigationOnClickListener {
                onBackPressedDispatcher.onBackPressed()
            }

            confirmRedeem.setOnClickListener {
                if (validateForm()) {
                    showLoading(true)
                    submitRedemption()
                }
            }
        }
    }

    private fun setupUI() {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
    }

    private fun observeUserData() {
        try {
            userViewModel.user.observe(this) { user ->
                if (user !== null) {
                    binding.apply {
                        fullName.setText(user.name)
                        phoneNo.setText(user.phoneNumber)
                        userEmail.setText(user.email)
                        balance.text =
                            UtilityFunctions.formatNumberShort(user.greenCoins.toString())
                    }
                } else {
                    Toast.makeText(this, "Profile data not found.", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Toast.makeText(
                this, "Failed to load balance: ${e.message}", Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun validateForm(): Boolean {
        val name = binding.fullName.text.toString().trim()
        val email = binding.userEmail.text.toString().trim()
        val address = binding.shippingAddress.text.toString().trim()
        val phone = binding.phoneNo.text.toString().trim()
        val agreed = binding.checkboxTerms.isChecked

        if (name.isEmpty()) {
            binding.fullName.error = "Name is required"
            binding.fullName.requestFocus()
            return false
        }

        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.userEmail.error = "Valid email is required"
            binding.userEmail.requestFocus()
            return false
        }

        if (address.isEmpty()) {
            binding.shippingAddress.error = "Address is required"
            binding.shippingAddress.requestFocus()
            return false
        }

        if (phone.isEmpty() || phone.length < 10) {
            binding.phoneNo.error = "Valid phone number is required"
            binding.phoneNo.requestFocus()
            return false
        }

        if (!agreed) {
            Toast.makeText(this, "You must agree to the terms", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    private fun submitRedemption() {
        val user = userViewModel.user.value

        if (storeItem == null || user == null) {
            Toast.makeText(this, "Something went wrong. Try again later.", Toast.LENGTH_SHORT)
                .show()
            showLoading(false)
            return
        }

        if (user.greenCoins < (storeItem?.price ?: 0)) {
            Toast.makeText(
                this,
                "Insufficient Green Coins to redeem this item.",
                Toast.LENGTH_SHORT
            ).show()
            showLoading(false)
            return
        }

        val orderId = System.currentTimeMillis().toString()

        val order = OrderItem(
            id = orderId,
            itemId = storeItem!!.id,
            itemName = storeItem!!.name,
            itemImage = storeItem!!.image,
            itemPrice = storeItem!!.price,
            userId = FirebaseRepository.getCurrentUserId(),
            userName = binding.fullName.text.toString().trim(),
            userEmail = binding.userEmail.text.toString().trim(),
            shippingAddress = binding.shippingAddress.text.toString().trim(),
            phoneNumber = binding.phoneNo.text.toString().trim(),
            deliveryInstructions = binding.deliveryInstructions.text.toString().trim(),
            status = "Pending",
            timestamp = System.currentTimeMillis()
        )

        userViewModel.addOrder(
            order,
            onSuccess = {
                val updatedUser = user.copy(greenCoins = user.greenCoins - (storeItem?.price ?: 0))
                userViewModel.updateUser(updatedUser)

                Toast.makeText(this, "Redemption successful!", Toast.LENGTH_LONG).show()
                finish()
            },
            onFailure = { e ->
                showLoading(false)
                Toast.makeText(this, "Failed to submit order: ${e.message}", Toast.LENGTH_LONG)
                    .show()
            }
        )
    }

    private fun setFormEnabled(enabled: Boolean) {
        binding.apply {
            fullName.isEnabled = enabled
            userEmail.isEnabled = enabled
            shippingAddress.isEnabled = enabled
            phoneNo.isEnabled = enabled
            deliveryInstructions.isEnabled = enabled
            checkboxTerms.isEnabled = enabled
            confirmRedeem.isEnabled = enabled
        }
    }

    private fun showLoading(show: Boolean) {
        binding.confirmRedeem.text = if (show) "Redeeming..." else "Confirm Redemption"
        setFormEnabled(!show)
    }
}