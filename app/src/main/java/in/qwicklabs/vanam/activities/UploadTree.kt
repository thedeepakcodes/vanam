package `in`.qwicklabs.vanam.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.location.Geocoder
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.Timestamp
import `in`.qwicklabs.vanam.R
import `in`.qwicklabs.vanam.databinding.ActivityUploadTreeBinding
import `in`.qwicklabs.vanam.model.Tree
import `in`.qwicklabs.vanam.repository.FirebaseRepository
import `in`.qwicklabs.vanam.repository.UserRepository
import `in`.qwicklabs.vanam.utils.UtilityFunctions
import `in`.qwicklabs.vanam.viewModel.TreeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit

class UploadTree : AppCompatActivity() {
    private lateinit var binding: ActivityUploadTreeBinding

    private val treeViewModel: TreeViewModel by viewModels()

    private lateinit var pickImageLauncher: ActivityResultLauncher<String>
    private lateinit var takePhotoLauncher: ActivityResultLauncher<Intent>
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var base64Image: String? = null
    private var isLivePhoto: Boolean = false
    private var isVerified: Boolean = false
    private var isInternalAIError: Boolean = false

    private lateinit var plantData: JSONObject

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUploadTreeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        setupUI()
        registerActivityResultLaunchers()
        setupClickListeners()
    }

    private fun setupUI() {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
    }

    private fun registerActivityResultLaunchers() {
        pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                binding.imagePreview.setImageURI(it)
                positionCapturedImage()
                isLivePhoto = false
                base64Image = UtilityFunctions.uriToBase64Image(it, this)
                base64Image?.let { image -> callGeminiApi(image) }
                    ?: showToast("Failed to convert image to Base64.")
            }
        }

        takePhotoLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                val bitmap = result.data?.extras?.get("data") as? Bitmap
                if (bitmap != null) {
                    binding.imagePreview.setImageBitmap(bitmap)
                    positionCapturedImage()
                    isLivePhoto = true
                    base64Image = UtilityFunctions.bitmapToBase64Image(bitmap)
                    base64Image?.let { image -> callGeminiApi(image) }
                }
            }
    }

    private fun setupClickListeners() {
        binding.apply {
            toolbar.setNavigationOnClickListener {
                onBackPressedDispatcher.onBackPressed()
            }
            openCamera.setOnClickListener { checkPermissionAndOpenCamera() }
            openGallery.setOnClickListener { openImagePicker() }
            getGeolocation.setOnClickListener { checkPermissionAndGetUserLocation() }
            uploadTree.setOnClickListener { uploadTree() }
        }
    }

    private fun openImagePicker() {
        pickImageLauncher.launch("image/*")
    }

    private fun checkPermissionAndOpenCamera() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            openCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun openCamera() {
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        takePhotoLauncher.launch(cameraIntent)
    }

    private fun checkPermissionAndGetUserLocation() {
        val fineGranted = ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!fineGranted && !coarseGranted) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            return
        }

        val priority =
            if (fineGranted) Priority.PRIORITY_HIGH_ACCURACY else Priority.PRIORITY_BALANCED_POWER_ACCURACY
        val originalButtonText = binding.getGeolocation.text.toString()

        binding.getGeolocation.text = "Fetching location..."
        binding.getGeolocation.isEnabled = false

        fusedLocationClient.getCurrentLocation(priority, null).addOnSuccessListener { location ->
            binding.getGeolocation.text = originalButtonText
            binding.getGeolocation.isEnabled = true

            if (location != null) {
                val lat = location.latitude
                val lon = location.longitude
                binding.lat.setText(lat.toString())
                binding.lon.setText(lon.toString())
                resolveLocationName(lat, lon)
            } else {
                showToast("Could not get location.")
            }
        }.addOnFailureListener {
            binding.getGeolocation.text = originalButtonText
            binding.getGeolocation.isEnabled = true
            showToast("Location fetch failed: ${it.message}")
        }
    }

    private fun resolveLocationName(lat: Double, lon: Double) {
        try {
            val geocoder = Geocoder(this, Locale.getDefault())
            val addresses = geocoder.getFromLocation(lat, lon, 1)
            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                val city = address.locality ?: ""
                val state = address.adminArea ?: ""
                val country = address.countryName ?: ""
                binding.locationName.text = "$city, $state, $country"
                binding.locationName.visibility = View.VISIBLE
            } else {
                showToast("Could not find location name.")
            }
        } catch (e: IOException) {
            showToast("Geocoder error: ${e.message}")
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            CAMERA_PERMISSION_REQUEST_CODE -> {
                if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
                    openCamera()
                } else {
                    showToast("Camera permission denied.")
                }
            }

            LOCATION_PERMISSION_REQUEST_CODE -> {
                val fineGranted = ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
                val coarseGranted = ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED

                if (fineGranted || coarseGranted) {
                    checkPermissionAndGetUserLocation()
                } else {
                    showToast("Location permission denied.")
                }
            }
        }
    }

    private fun positionCapturedImage() {
        val imageView = binding.imagePreview
        imageView.scaleType = ImageView.ScaleType.MATRIX

        val drawable = imageView.drawable ?: return
        val matrix = Matrix()
        val scale = maxOf(
            imageView.width / drawable.intrinsicWidth.toFloat(),
            imageView.height / drawable.intrinsicHeight.toFloat()
        )
        matrix.setScale(scale, scale)

        matrix.postTranslate(0f, 0f)
        imageView.imageMatrix = matrix
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun callGeminiApi(base64Image: String) = lifecycleScope.launch(Dispatchers.Main) {
        actionButtonsStatus(false)
        showAiFeedback("AI analysis in progress...", true)

        val prompt = """
            You are a plant verification assistant.
            You will be shown a base64-encoded image. Your job is to determine whether it shows a **real, physical tree or plant**, planted by a **person** (e.g. a sapling in the ground, a tree with visible soil, tags, or signs of human care like pots or fencing).
            
            ✅ Respond with this JSON if the image shows a **real, planted tree** (not a forest, decorative plant, or random greenery):
            
            {
              "isPlant": true,
              "commonName": "...",
              "scientificName": "...",
              "description": "...",
              "nativeRegion": "...",
              "averageCO2SavingKg": 123.4
            }
            
            averageCO2SavingKg is measured annually and varies based on the approx age of the tree.
            
            🚫 If the image is:
            - A drawing, cartoon, vector, illustration, or animation
            - A forest or general greenery with no sign of planting
            - A poster, wallpaper, or product photo
            - A fake, AI-generated image
            - A potted houseplant not planted in soil outdoors
            - Any image not clearly showing a planted tree
            
            Then respond **only with**:
            
            {
              "isPlant": false
            }
            
            🔒 Respond in **valid JSON only**, no explanations, no extra text, no comments, no markdown.
    
            
            """.trimIndent()
        withContext(Dispatchers.IO) {

            val body = JSONObject().put(
                "contents", JSONArray().put(
                    JSONObject().put(
                        "parts",
                        JSONArray().put(
                            JSONObject().put(
                                "inline_data",
                                JSONObject().put("mime_type", "image/jpeg").put("data", base64Image)
                            )
                        )
                            .put(JSONObject().put("text", prompt))
                    )
                )
            )

            val request = Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=AIzaSyBnQsucIzIP_MnrGzTefJjLuoOx_6rmbjM")
                .post(body.toString().toRequestBody("application/json".toMediaTypeOrNull()))
                .build()

            try {
                client.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string() ?: ""
                    if (!response.isSuccessful || responseBody.isEmpty()) {
                        withContext(Dispatchers.Main) {
                            isVerified = false
                            isInternalAIError = true
                            showAiFeedback(
                                "AI analysis failed. Manual verification required.",
                                false
                            )
                            actionButtonsStatus(true)
                        }
                        return@use
                    }


                    val textContent = JSONObject(responseBody)
                        .getJSONArray("candidates").getJSONObject(0)
                        .getJSONObject("content").getJSONArray("parts")
                        .getJSONObject(0).getString("text")

                    val cleanedJsonText =
                        textContent.removePrefix("```json\n").removeSuffix("\n```")

                    plantData = JSONObject(cleanedJsonText)

                    withContext(Dispatchers.Main) {
                        actionButtonsStatus(true)
                        isVerified = plantData.optBoolean("isPlant", false)
                        isInternalAIError = false
                        if (isVerified) {
                            binding.commonName.setText(plantData.optString("commonName", ""))
                            binding.scientificName.setText(
                                plantData.optString(
                                    "scientificName",
                                    ""
                                )
                            )
                            showAiFeedback("Tree identified successfully!", true)
                        } else {
                            showAiFeedback(
                                "Not a valid planted tree. Please upload a suitable image.",
                                false
                            )
                            binding.commonName.text?.clear()
                            binding.scientificName.text?.clear()
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showAiFeedback("AI request failed: ${e.message}", false)
                    actionButtonsStatus(true)
                }
            }
        }
    }

    private fun actionButtonsStatus(enabled: Boolean) {
        binding.openCamera.isEnabled = enabled
        binding.openGallery.isEnabled = enabled
        binding.getGeolocation.isEnabled = enabled
    }

    private fun showAiFeedback(message: String, success: Boolean) {
        val color = if (success) R.color.primary else R.color.error_text_color
        binding.aiFeedback.apply {
            text = message
            visibility = View.VISIBLE
            setTextColor(ContextCompat.getColor(this@UploadTree, color))
        }
    }

    private fun validateInputs(): Boolean {
        var isValid = true

        val commonName = binding.commonName.text.toString().trim()
        if (commonName.isEmpty()) {
            binding.commonName.error = "Common Name is required."
            isValid = false
        } else {
            binding.commonName.error = null
        }

        val scientificName = binding.scientificName.text.toString().trim()
        if (scientificName.isEmpty()) {
            binding.scientificName.error = "Scientific Name is required."
            isValid = false
        } else {
            binding.scientificName.error = null
        }

        val latStr = binding.lat.text.toString().trim()
        val latitude = latStr.toDoubleOrNull()
        if (latitude == null || latitude !in -90.0..90.0) {
            binding.lat.error = "Valid Latitude (-90 to 90) is required."
            isValid = false
        } else {
            binding.lat.error = null
        }

        val lonStr = binding.lon.text.toString().trim()
        val longitude = lonStr.toDoubleOrNull()
        if (longitude == null || longitude !in -180.0..180.0) {
            binding.lon.error = "Valid Longitude (-180 to 180) is required."
            isValid = false
        } else {
            binding.lon.error = null
        }

        if (base64Image == null) {
            showToast("Please select or capture an image first.")
            isValid = false
        }

        if (isValid && !isInternalAIError && !isVerified) {
            showToast("Image has not been verified by the AI. Try with another image.")
            isValid = false
        }

        return isValid
    }

    private fun uploadTree() {
        if (!validateInputs()) {
            return
        }

        uploadButtonState(true)

        lifecycleScope.launch {
            try {
                val tree = prepareTreeObject()
                if (tree == null) {
                    showToast("Failed to prepare tree data.")
                    uploadButtonState(false)
                    return@launch
                }

                val imageUrl = uploadImageToFirebase(tree.id, base64Image!!)

                tree.imageUrl = imageUrl

                treeViewModel.addTree(tree)
                if (isVerified) {
                    UserRepository.incrementTreeCountAndCoins()
                }

                showToast("Tree uploaded successfully!")

                val resultIntent = Intent()
                resultIntent.putExtra("new_tree", tree)
                setResult(RESULT_OK, resultIntent)
                finish()
            } catch (e: Exception) {
                showToast("Upload failed: ${e.message}")
            } finally {
                uploadButtonState(false)
            }
        }
    }

    private fun prepareTreeObject(): Tree? {
        val tree = Tree()
        val treeId = UUID.randomUUID().toString()
        tree.id = treeId

        val latStr = binding.lat.text.toString().trim()
        val lonStr = binding.lon.text.toString().trim()

        tree.latitude = latStr.toDoubleOrNull()
        tree.longitude = lonStr.toDoubleOrNull()

        tree.commonName = binding.commonName.text.toString().trim()
        tree.scientificName = binding.scientificName.text.toString().trim()

        if (::plantData.isInitialized && plantData.optBoolean("isPlant", false)) {
            tree.description = plantData.optString("description", "").trim()
            tree.nativeRegion = plantData.optString("nativeRegion", "").trim()
            tree.averageCO2SavingKg = plantData.optDouble("averageCO2SavingKg", 0.0)
        } else {
            tree.description = ""
            tree.nativeRegion = ""
            tree.averageCO2SavingKg = 0.0
        }

        val locationParts = binding.locationName.text.toString().split(",")
        tree.city = locationParts.getOrNull(0)?.trim().orEmpty()
        tree.state = locationParts.getOrNull(1)?.trim().orEmpty()
        tree.country = locationParts.getOrNull(2)?.trim().orEmpty()

        tree.isLivePhoto = isLivePhoto
        tree.isVerified = isVerified
        tree.timestamp = Timestamp.now()
        tree.userId = FirebaseRepository.getCurrentUserId()

        if (tree.latitude == null || tree.longitude == null || tree.commonName!!.isEmpty() || tree.scientificName!!.isEmpty() || tree.userId.isNullOrEmpty()) {
            return null
        }
        return tree
    }

    private suspend fun uploadImageToFirebase(treeId: String, imageBase64: String): String =
        withContext(Dispatchers.IO) {
            val imageRef = FirebaseRepository.getTreeImageStorageRef(treeId)
            val tempUri = UtilityFunctions.base64ToTempUri(imageBase64, this@UploadTree)

            val uploadTask = imageRef.putFile(tempUri)

            return@withContext uploadTask.continueWithTask { task ->
                if (!task.isSuccessful) {
                    task.exception?.let { throw it }
                }
                imageRef.downloadUrl
            }.await().toString()
        }

    private fun uploadButtonState(uploading: Boolean) {
        val originalUploadButtonText = binding.uploadTree.text.toString()
        binding.uploadTree.isEnabled = !uploading
        binding.uploadTree.text = if (uploading) "Uploading..." else originalUploadButtonText

        binding.commonName.isEnabled = !uploading
        binding.scientificName.isEnabled = !uploading
        binding.lat.isEnabled = !uploading
        binding.lon.isEnabled = !uploading
        binding.openCamera.isEnabled = !uploading
        binding.openGallery.isEnabled = !uploading
        binding.getGeolocation.isEnabled = !uploading
    }

    private companion object {
        private const val CAMERA_PERMISSION_REQUEST_CODE = 103
        private const val LOCATION_PERMISSION_REQUEST_CODE = 104
    }
}