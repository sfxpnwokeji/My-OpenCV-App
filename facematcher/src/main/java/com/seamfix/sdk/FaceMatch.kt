package com.seamfix.sdk

class FaceMatch {

    companion object {
        private var faceMatch: FaceMatch? = null
        fun getInstance(): FaceMatch {
            if (faceMatch == null) {
                faceMatch = FaceMatch()
            }
            return faceMatch as FaceMatch
        }

        // Used to load the 'sdk' library on application startup.
        init {
            System.loadLibrary("mynative")
        }

    }

    external fun intMethod(int: Int): Int

    external fun singleImagePreprocess(base64Image: String, base64ProbeImage: String): String

    external fun faceMatch(prediction: Array<FloatArray>): Float

    external fun imagePreprocess(
        base64Image: String, base64ProbeImage: String, modelDir: String
    ): String


}