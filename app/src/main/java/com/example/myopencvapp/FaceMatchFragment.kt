package com.example.myopencvapp

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
    private var baseImage by Delegates.notNull<Boolean>()

    private val uiHandler = Handler(Looper.getMainLooper())

    private val pickMediaLauncher =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            // Callback is invoked after the user selects a media item or closes the
            // photo picker.
            if (uri != null) {
                val bitmap: Bitmap = MediaStore.Images.Media.getBitmap(
                    context?.contentResolver, Uri.parse(
                        uri.toString()
                    ))
                val myImgView = if (baseImage) binding.baseFaceImage else binding.probeFaceImage
                myImgView.setImageBitmap(bitmap)
                Log.d(TAG, "Selected URI: $uri")
            } else {
                Log.d(TAG, "No media selected")
            }
        }



    private fun initialize() {
        binding.probeFaceButton.setOnClickListener { pickPicture(false)  }
        binding.baseFaceButton.setOnClickListener { pickPicture(true) }
        binding.verifyFaceButton.setOnClickListener { verifyFace() }
    }

    private fun verifyFace() {

        val builder: AlertDialog.Builder? = activity?.let {
            AlertDialog.Builder(it)
        }

        if(binding.baseFaceImage.drawable == null || binding.probeFaceImage.drawable == null ){
//            val builder: AlertDialog.Builder? = activity?.let {
//                AlertDialog.Builder(it)
//            }

            builder?.setMessage("Please input both images to be compared")
                ?.setTitle("Invalid Request")

             builder?.show()


            return
        }

        CoroutineScope(Dispatchers.Default).launch {
            val result = callFaceMatch()


            uiHandler.post {

                builder?.setMessage(getMessageForResult(result))
                    ?.setTitle("Face Match Result")


                builder?.show()

            }

        }

    }

    private fun getMessageForResult(result: MatchResult?): String {
         val str : String
        if (result != null) {
            str = when (result.status) {
                ValidationStatus.MATCH -> {
                    "Both Faces Match with score ${result.score}"
                }

                ValidationStatus.NO_MATCH -> {
                    "Both Faces do not match as score ${result.score} is above threshold"
                }

                else -> {
                    "No information"
                }

            }

            return str
        }

        return "No information"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentFaceMatchBinding.inflate(inflater, container, false)
        initialize()
        return binding.root
    }

    private fun pickPicture(isBaseImage: Boolean) {

        baseImage = isBaseImage

        // Launch the photo picker and let the user choose only images.
        try {
            pickMediaLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))

        } catch (e: Exception) {
            Log.e(TAG, e.toString())
        }




    }


    private fun callFaceMatch(): MatchResult? {
        return try {
            val (image: ByteArray, imageProbe: ByteArray) = getByteArrayPair()
            val imageString = Base64.encodeToString(image, Base64.NO_WRAP)
            val imageStringProbe = Base64.encodeToString(imageProbe, Base64.NO_WRAP)
            val faceKin = FaceKin(requireContext())
            val result = faceKin.verify(imageString, imageStringProbe, 0.2f)
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
        val baseImageBitmap = (binding.baseFaceImage.drawable as BitmapDrawable).bitmap
        val probeImageBitmap = (binding.probeFaceImage.drawable as BitmapDrawable).bitmap
        val baseStream = ByteArrayOutputStream()
        val probeStream = ByteArrayOutputStream()
        baseImageBitmap.compress(Bitmap.CompressFormat.PNG, 100, baseStream)
        probeImageBitmap.compress(Bitmap.CompressFormat.PNG, 100, probeStream)
        val image: ByteArray = baseStream.toByteArray()
        val imageProbe: ByteArray = probeStream.toByteArray()
        return Pair(image, imageProbe)
    }


}