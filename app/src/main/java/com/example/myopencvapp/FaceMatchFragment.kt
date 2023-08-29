package com.example.myopencvapp

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.view.*
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.myopencvapp.databinding.FragmentFaceMatchBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.IOException
import kotlin.properties.Delegates


/**
 * A simple [Fragment] subclass.
 */
class FaceMatchFragment : Fragment() {
    companion object {
        const val TAG = "FaceMatchFragment"
    }

    private lateinit var binding: FragmentFaceMatchBinding
    private var sourceImage by Delegates.notNull<Boolean>()


    private val uiHandler = Handler(Looper.getMainLooper())

    private val pickMediaLauncher =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            // Callback is invoked after the user selects a media item or closes the
            // photo picker.
            if (uri != null) {
                val bitmap: Bitmap = MediaStore.Images.Media.getBitmap(
                    context?.contentResolver, Uri.parse(
                        uri.toString()
                    )
                )
                setImageViewBitmapWith(bitmap)
            } else {
                Log.d(TAG, "No media selected")
            }
        }

    private var resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // There are no request codes
                val data: Intent? = result.data
                val imageBitmap = data?.extras?.get("data") as Bitmap
                setImageViewBitmapWith(imageBitmap)
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentFaceMatchBinding.inflate(inflater, container, false)
        initialize()
        return binding.root
    }


    private fun initialize() {
        binding.btnInputTargetImage.setOnClickListener { showPictureDialog(false) }
        binding.btnInputSourceImage.setOnClickListener { showPictureDialog(true) }
        binding.btnMatchFaces.setOnClickListener { verifyFace() }
    }


    private fun showPictureDialog(isSourceImage: Boolean) {
        sourceImage = isSourceImage

        val pictureDialog = AlertDialog.Builder(requireContext())
        pictureDialog.setTitle("Select Action")
        val pictureDialogItems = arrayOf("Select photo from gallery", "Capture photo from camera")
        pictureDialog.setItems(
            pictureDialogItems
        ) { _, which ->
            when (which) {
                0 -> pickPicture()
                1 -> takePhotoFromCamera()
            }
        }
        pictureDialog.show()
    }

    private fun setImageViewBitmapWith(bitmap: Bitmap) {
        val myImgView = if (sourceImage) binding.displaySourceImage else binding.displayTargetImage
        myImgView.setImageBitmap(bitmap)
    }


    private fun takePhotoFromCamera() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

        resultLauncher.launch(takePictureIntent)
    }

    private fun pickPicture() {
        // Launch the photo picker and let the user choose only images.
        try {

            val pickPictureRequest =
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            pickMediaLauncher.launch(pickPictureRequest)

        } catch (e: Exception) {
            Log.e(TAG, e.toString())
        }

    }


    private fun verifyFace() {

        binding.progressBar.visibility = View.VISIBLE
        val placeholderConstantState = resources.getDrawable(R.drawable.face, null).constantState



        val sourceResource = binding.displaySourceImage.drawable.constantState
        val targetResource = binding.displayTargetImage.drawable.constantState




        if (binding.displaySourceImage.drawable == null || sourceResource == placeholderConstantState || binding.displayTargetImage.drawable == null || targetResource == placeholderConstantState ) {
            val builder: AlertDialog.Builder? = activity?.let {
                AlertDialog.Builder(it)

            }

            builder?.setMessage("Please input both images to be compared")
                ?.setTitle("Invalid Request")

            builder?.show()

            binding.progressBar.visibility = View.GONE

            return
        }

        CoroutineScope(Dispatchers.Default).launch {
            val startTime = System.currentTimeMillis()
            val result = callFaceMatch()
            val endTime = System.currentTimeMillis()
            val (status, score, percentage) = getDetailsOfResult(result)
            val processTime = "${endTime - startTime} ms"


            uiHandler.post {
                binding.progressBar.visibility = View.GONE
                binding.matchResult.text = status
                binding.percentage.text = percentage.toString()
                binding.matchScore.text = score.toString()
                binding.processingTime.text = processTime
            }
        }
    }

    private fun getDetailsOfResult(result: MatchResult?): Triple<String, Float, Float> {
        val status: String
        val score: Float
        val percentage: Float

        if (result != null) {
            score = result.score
            percentage = calcPercentage(result.score)
            status = result.status.description
        } else {
            status = "None"
            score = 0f
            percentage = 0f
        }


        return Triple(status, score, percentage)
    }

    private fun calcPercentage(score: Float): Float {
        return 1 / (1 + score) * 100
    }


    private fun callFaceMatch(): MatchResult? {
        return try {
            val (image: ByteArray, imageProbe: ByteArray) = getByteArrayPair()
            val imageString = Base64.encodeToString(image, Base64.NO_WRAP)
            val imageStringProbe = Base64.encodeToString(imageProbe, Base64.NO_WRAP)
            val faceKin = FaceKin(requireContext())
            val result = faceKin.verify(imageString, imageStringProbe, 0.9f)
            Log.i(TAG, result.score.toString())
            Log.i(TAG, result.status.toString())
            faceKin.release()
            result
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    private fun getByteArrayPair(): Pair<ByteArray, ByteArray> {
        val baseImageBitmap = (binding.displaySourceImage.drawable as BitmapDrawable).bitmap
        val probeImageBitmap = (binding.displayTargetImage.drawable as BitmapDrawable).bitmap
        val baseStream = ByteArrayOutputStream()
        val probeStream = ByteArrayOutputStream()
        baseImageBitmap.compress(Bitmap.CompressFormat.PNG, 100, baseStream)
        probeImageBitmap.compress(Bitmap.CompressFormat.PNG, 100, probeStream)
        val image: ByteArray = baseStream.toByteArray()
        val imageProbe: ByteArray = probeStream.toByteArray()
        return Pair(image, imageProbe)
    }


}
