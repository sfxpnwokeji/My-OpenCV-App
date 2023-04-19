package com.example.myopencvapp

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.*
import android.provider.MediaStore
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.example.myopencvapp.databinding.FragmentHomeBinding
import com.seamfix.sdk.FaceMatch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.opencv.android.BaseLoaderCallback
import org.opencv.android.LoaderCallbackInterface
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import org.opencv.imgproc.Imgproc.INTER_AREA
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Paths


/**
 * A simple [Fragment] subclass.
 */
class HomeFragment : Fragment() {

    companion object {
        const val TAG = "HomeFragment"
        const val SCALE = 224.0
    }

    private lateinit var binding: FragmentHomeBinding


    private lateinit var imageMat: Mat
    private var processedImage: Bitmap? = null

    // Define a handler that's attached to the main thread's looper
    private val uiHandler = Handler(Looper.getMainLooper())

    private val mLoaderCallback: BaseLoaderCallback = object : BaseLoaderCallback(context) {
        override fun onManagerConnected(status: Int) {
            when (status) {
                LoaderCallbackInterface.SUCCESS -> {
                    Log.i(TAG, "OpenCV loaded successfully")
                    imageMat = Mat()
                }
                else -> super.onManagerConnected(status)
            }
        }
    }

    private var resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // There are no request codes
                val data: Intent? = result.data
                val imageBitmap = data?.extras?.get("data") as Bitmap
                binding.srcImgViewer.setImageBitmap(imageBitmap)
                CoroutineScope(Dispatchers.Default).launch {
                    processedImage = processImage(imageBitmap)
                    uiHandler.post {
                        binding.resultImgViewer.setImageBitmap(processedImage)
                    }

                }


            }
        }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        initListeners()
        Log.i(TAG, FaceMatch().intMethod(15).toString())
        return binding.root
    }




    private fun initListeners() {

        binding.btnCamera.setOnClickListener {
            dispatchTakePictureIntent()
        }

        binding.resultImgViewer.viewTreeObserver.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                val myImageView = binding.resultImgViewer
                // Remove the listener to prevent duplicate calls
                myImageView.viewTreeObserver.removeOnGlobalLayoutListener(this)

                // Get the current bitmap of the ImageView
                var currentBitmap = getBitmap(myImageView)

                // Set up a new observer to listen for changes in the ImageView's layout
                myImageView.viewTreeObserver.addOnGlobalLayoutListener {
                    // Get the new bitmap of the ImageView
                    val newBitmap = getBitmap(myImageView)

                    // Compare the current and new bitmaps to see if the bitmap has changed
                    if (currentBitmap != newBitmap) {
                        onCompleteProcess()
                    }

                    // Set the new bitmap as the current bitmap
                    currentBitmap = newBitmap
                }
            }
        })


        binding.nameField.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                // Do nothing
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // Do nothing
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.submitButton.isEnabled = s.toString().isNotBlank()
            }
        })

        binding.submitButton.setOnClickListener {
            try {
                getStoragePermission()
            } catch (e: Exception) {
                Toast.makeText(context, "Please grant permission", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            saveImage()

            // reset screen
            resetScreen()

            val message = "Image Saved!"

            // Display toast
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun resetScreen() {
        binding.resultImgViewer.setImageBitmap(null)
    }

    private fun getBitmap(myImageView: ImageView): Bitmap? {
        val currentBitmap = try {
            (myImageView.drawable as BitmapDrawable).bitmap
        } catch (e: Exception) {
            null
        }
        return currentBitmap
    }

    private fun getStoragePermission() {
        if (Build.VERSION.SDK_INT >= 30) {
            if (!Environment.isExternalStorageManager()) {
                val getPermission = Intent()
                getPermission.action = Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                startActivity(getPermission)
            }

            // confirm if permission has been granted
            if (!Environment.isExternalStorageManager()) {
                throw Exception("Permission denied")

            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (!OpenCVLoader.initDebug()) {
            Log.d(
                TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization"
            )
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, context, mLoaderCallback)
        } else {
            Log.d("OpenCV", "OpenCV library found inside package. Using it!")
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS)
        }
    }

    private fun dispatchTakePictureIntent() {

        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        try {
            resultLauncher.launch(takePictureIntent)
        } catch (e: ActivityNotFoundException) {
            // display error state to the user
            Log.e(TAG, e.toString())
        }

    }



    private fun processImage(imageBitmap: Bitmap): Bitmap? {
        val src = Mat()

        // load the bitmap into src mat
        Utils.bitmapToMat(imageBitmap, src)

        val dst: Mat = blurImage(src)


        // resize the blurred image and
        // convert resulting mat to bitmap
        val resultBitmap =
            Bitmap.createScaledBitmap(imageBitmap, SCALE.toInt(), SCALE.toInt(), true)
        Utils.matToBitmap(resizeImage(dst), resultBitmap)

        return resultBitmap

    }


    fun makeGray(bitmap: Bitmap): Bitmap {


        // Create OpenCV mat object and copy content from bitmap
        val mat = imageMat
        Utils.bitmapToMat(bitmap, mat)

        // Convert to grayscale
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGB2GRAY)

        // Make a mutable bitmap to copy grayscale image
        val grayBitmap = bitmap.copy(bitmap.config, true)
        Utils.matToBitmap(mat, grayBitmap)



        return grayBitmap
    }

    private fun resizeImage(src: Mat): Mat {
        val dst = Mat()

        val scaleSize = Size(SCALE, SCALE)
        Imgproc.resize(src, dst, scaleSize, 0.0, 0.0, INTER_AREA)
        Log.i(TAG, src.size().toString())

        Log.i(TAG, dst.size().toString())

        return dst
    }

    private fun blurImage(src: Mat): Mat {

        val dst = Mat()

        val blurKernelSize = Size(9.0, 9.0) // kernel size should be odd

        Imgproc.GaussianBlur(src, dst, blurKernelSize, 0.0)

        return dst


    }

    private fun createDirectoryAndSaveFile(imageToSave: Bitmap, fileName: String) {

        val folderPath = Environment.getExternalStorageDirectory().toString() + "/CVImage/"
        val direct = File(folderPath)
        if (!direct.exists()) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    Files.createDirectory(
                        Paths.get(folderPath)
                    )
                } else {
                    if (!direct.mkdirs()) throw Exception("Folder could not be created!")
                }
            } catch (e: Exception) {
                // could not create folder
                Log.e(TAG, e.toString())
            }
        }

        // create new file
        var counter = 0
        var newFileName = "$fileName.jpg"

        var file = File(folderPath, newFileName)

        while (file.exists()) {
            counter += 1
            newFileName = fileName + "_$counter.jpg"
            file = File(folderPath, newFileName)
        }

        Log.i(TAG, file.toString())

        try {
            file = File(folderPath, newFileName)
            file.createNewFile()
            val out = FileOutputStream(file)
            imageToSave.compress(Bitmap.CompressFormat.JPEG, 100, out)
            out.flush()
            out.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveImage() {
        val fileName = binding.nameField.text.toString()
        processedImage?.let { createDirectoryAndSaveFile(it, fileName) }
    }

    fun onCompleteProcess() {
        val imageView = binding.resultImgViewer
        val nameLayout = binding.nameLayout
        val bitmap = getBitmap(imageView)
        if (bitmap != null) {
            // Set the bitmap in the ImageView
            imageView.setImageBitmap(bitmap)
            // Display the text field
            nameLayout.visibility = View.VISIBLE
        } else {
            // Hide the text field
            nameLayout.visibility = View.GONE
        }
    }


}



