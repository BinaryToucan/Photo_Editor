package org.hyperskill.photoeditor

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.PermissionChecker
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.lifecycleScope
import com.google.android.material.slider.Slider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import org.hyperskill.photoeditor.BitmapHelper.applyBrightness
import org.hyperskill.photoeditor.BitmapHelper.applyContrast
import org.hyperskill.photoeditor.BitmapHelper.applyGamma
import org.hyperskill.photoeditor.BitmapHelper.applySaturation
import org.hyperskill.photoeditor.BitmapHelper.createBitmap

class MainActivity : AppCompatActivity() {

    companion object {
        var PERMISSION_REQUEST_CODE = 0
    }

    /**
     * Текущее изображение на экране
     */
    private lateinit var currentImage: ImageView

    /**
     * Исходное изображение (фильтры применям к нему)
     */
    private lateinit var originalBitmap: Bitmap

    private var brightValue: Double = 0.0
    private var contrastValue: Double = 0.0
    private var saturationValue: Double = 0.0
    private var gammaValue: Double = 1.0

    private var filterJob: Job? = null

    /**
     * Выбираем новое изображение из галереи
     */
    private val activityNewImageLauncher =
        registerForActivityResult(StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val photoUri = result.data?.data ?: return@registerForActivityResult
                val ivPhoto = findViewById<ImageView>(R.id.ivPhoto)

                val bitmap = contentResolver.openInputStream(photoUri)?.use {
                    BitmapFactory.decodeStream(it)
                } ?: return@registerForActivityResult

                originalBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, false)
                ivPhoto.setImageBitmap(originalBitmap)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        bindViews()

        val buttonGallery: Button = findViewById<Button>(R.id.btnGallery)
        val buttonSave: Button = findViewById<Button>(R.id.btnSave)

        val sliderBright: Slider = findViewById<Slider>(R.id.slBrightness)
        val sliderContrast: Slider = findViewById<Slider>(R.id.slContrast)
        val sliderSaturation: Slider = findViewById<Slider>(R.id.slSaturation)
        val sliderGamma: Slider = findViewById<Slider>(R.id.slGamma)

        buttonSave.setOnClickListener { _ ->
            val permissions = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)

            requestPermission(permissions){
                saveImage()
            }
        }

        buttonGallery.setOnClickListener(){
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            activityNewImageLauncher.launch(intent).let {
                sliderBright.value = 0f
                sliderContrast.value = 0f
                sliderSaturation.value = 0f
                sliderGamma.value = 1f
            }
        }

        sliderBright.addOnChangeListener(){ _, value, _ ->
            brightValue = value.toDouble()
            onFiltersChanged()
        }

        sliderContrast.addOnChangeListener(){ _, value, _ ->
            contrastValue = value.toDouble()
            onFiltersChanged()
        }

        sliderSaturation.addOnChangeListener(){ _, value, _ ->
            saturationValue = value.toDouble()
            onFiltersChanged()
        }

        sliderGamma.addOnChangeListener(){ _, value, _ ->
            gammaValue = value.toDouble()
            onFiltersChanged()
        }

        originalBitmap = createBitmap()
        currentImage.setImageBitmap(originalBitmap)
    }

    private fun bindViews() {
        currentImage = findViewById(R.id.ivPhoto)
    }

    private fun hasPermission(manifestPermission: String): Boolean {
        return when{
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ->
                this.checkSelfPermission(manifestPermission) == PackageManager.PERMISSION_GRANTED
            else ->
                PermissionChecker.checkSelfPermission(this, manifestPermission) == PermissionChecker.PERMISSION_GRANTED ||
                        !ActivityCompat.shouldShowRequestPermissionRationale(this, manifestPermission)
        }
    }

    private fun requestPermission(permissions: Array<String>, action: ()-> Unit) {
        if ( hasPermission(permissions.first()) ) {
            action()
        } else {
            ActivityCompat.requestPermissions(
                this,
                permissions,
                PERMISSION_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    saveImage()
                    Toast.makeText(this, getString(R.string.toast_save_success), Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
                }
            }
            else -> {
                super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            }
        }
    }

    /**
     * Сохранение изображения
     */
    fun saveImage(){
        val bitmap: Bitmap = currentImage.drawable.toBitmap()
        val values = ContentValues()
        values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        values.put(MediaStore.Images.ImageColumns.WIDTH, bitmap.width)
        values.put(MediaStore.Images.ImageColumns.HEIGHT, bitmap.height)

        val uri = this@MainActivity.contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values
        ) ?: return

        contentResolver.openOutputStream(uri).use {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it!!)
        }
    }

    /**
     * Применение фильтров
     */
    fun onFiltersChanged() {
        // отменяем старую задачу
        filterJob?.cancel()

        filterJob = lifecycleScope.launch(Dispatchers.Default) {

            val steps = listOf<BitmapStep>(
                { bmp -> bmp.applyBrightness(brightValue) },
                { bmp -> bmp.applyContrast(contrastValue) },
                { bmp -> bmp.applySaturation(saturationValue)},
                { bmp -> bmp.applyGamma(gammaValue)},
            )

            var result = originalBitmap
            for (step in steps) {
                ensureActive() // важно для отмены
                result = step(result)
            }

            runOnUiThread {
                currentImage.setImageBitmap(result)
            }
        }
    }

}