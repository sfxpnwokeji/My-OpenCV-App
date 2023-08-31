package com.example.myopencvapp

import android.content.Context
import com.google.gson.Gson
import com.seamfix.sdk.FaceMatch
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer

import java.io.IOException
import java.nio.MappedByteBuffer

class FaceKin(context: Context) {
    /**
     * Tf lite interpreter instance
     */
    private lateinit var tflite: Interpreter

    /**
     * File directory for models
     */
    private val fileDir: String

    /**
     * The tensor flow model
     */
    private lateinit var tfliteModel: MappedByteBuffer

    /**
     * The initialization status
     */
    private var status: ValidationStatus

    init {
        val cascadeDir = context.getDir("model", Context.MODE_PRIVATE)

        status = ValidationStatus.FAILED_INIT

        fileDir = Util().moveRawFileToPrivateDir(
            cascadeDir.absolutePath, context,
            "det1.caffemodel", "det1.prototxt", "det2.caffemodel",
            "det2.prototxt", "det3.caffemodel", "det3.prototxt"
        )
        try {
            val tfliteOptions: Interpreter.Options = Interpreter.Options()
            tfliteModel = FileUtil.loadMappedFile(context, "SavedModelFormat.tflite")
            tfliteOptions.setNumThreads(1)
            tflite = Interpreter(tfliteModel, tfliteOptions)
        } catch (e: IOException) {
            e.printStackTrace()
            status = ValidationStatus.FAILED_INIT
        }
    }

    /**
     * Performs kin verification on the supplied probe and candidate images
     * @param probeFace the probe face Image
     * @param candidateFace candidate face image
     * @param threshold the threshold range between 0 to 1
     * @return Match result containing the match status and the match score
     */
    fun verify(candidateFace: String, probeFace: String, threshold: Float): MatchResult {
        val data = FaceMatch.getInstance().imagePreprocess(
            candidateFace,
            probeFace,
            fileDir,
        )
        val preprocessResult: PreprocessResult = Gson().fromJson(data, PreprocessResult::class.java)
        if (preprocessResult.code != 0) {
            return when (preprocessResult.code) {
                -1 -> {
                    if (preprocessResult.status!!.contains("source", true)) MatchResult(
                        -1f,
                        ValidationStatus.NO_FACE_FOUND_SOURCE
                    ) else MatchResult(-1f, ValidationStatus.NO_FACE_FOUND_TARGET)
                }

                -2 -> {
                    if (preprocessResult.status!!.contains("source", true)) MatchResult(
                        -1f,
                        ValidationStatus.MULTI_FACE_FOUND_SOURCE
                    ) else MatchResult(-1f, ValidationStatus.MULTI_FACE_FOUND_TARGET)
                }

                -3 -> {
                    if (preprocessResult.status!!.contains("source", true)) MatchResult(
                        -1f,
                        ValidationStatus.FACE_OUT_OF_BOX_SOURCE
                    ) else MatchResult(-1f, ValidationStatus.FACE_OUT_OF_BOX_TARGET)
                }

                else -> MatchResult(-1f, ValidationStatus.IMAGE_PROCESSING_FAILED)
            }

        }

        val imageTensorIndex = 0
        val imageShape: IntArray =
            tflite.getInputTensor(imageTensorIndex).shape() // {1, height, width, 3}
        val imageDataType: DataType = tflite.getInputTensor(imageTensorIndex).dataType()
        val probabilityShape: IntArray = tflite.getOutputTensor(0).shape() // {1, NUM_CLASSES}
        val probabilityDataType: DataType = tflite.getOutputTensor(0).dataType()

        // Creates the input tensor.
        var inputImageBuffer = TensorImage(imageDataType)
        val outputProbabilityBuffer: TensorBuffer =
            TensorBuffer.createFixedSize(probabilityShape, probabilityDataType)
        val outFloat: Array<FloatArray>
        val out: MutableList<FloatArray> = ArrayList()

        for (datum in preprocessResult.data) {
            inputImageBuffer.load(datum, imageShape)
            val imageProcessor: ImageProcessor = ImageProcessor.Builder().build()
            inputImageBuffer = imageProcessor.process(inputImageBuffer)
            tflite.run(inputImageBuffer.buffer, outputProbabilityBuffer.buffer.rewind())
            val processed: FloatArray = outputProbabilityBuffer.floatArray
            out.add(processed)
        }
        outFloat = out.toTypedArray()
        val result = FaceMatch.getInstance().faceMatch(outFloat)
        return MatchResult(
            result,
            if (result < threshold) ValidationStatus.MATCH else ValidationStatus.NO_MATCH
        )
    }

    /**
     * Release tensor flow resources
     */
    fun release() {
        tflite.close()
//        tflite = null
//        tfliteModel = null
    }

    /**
     * Preprocess result class
     */
    private class PreprocessResult {
        lateinit var data: Array<FloatArray>
        var code = 0
        var status: String? = null
    }
}
