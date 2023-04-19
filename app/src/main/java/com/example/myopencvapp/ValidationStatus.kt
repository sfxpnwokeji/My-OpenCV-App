package com.example.myopencvapp


enum class ValidationStatus(var code: Int, var description: String) {
    MATCH(0, "Face verified"),
    NO_FACE_FOUND(-1, "No face found"),
    MULTI_FACE_FOUND(-2, "Multiple faces found"),
    FACE_OUT_OF_BOX(-3, "Face too close"),
    FAILED_INIT(-4, "Unable to load components"),
    NO_MATCH(-5, "Face not verified"),
    IMAGE_PROCESSING_FAILED(-6, "Image processing failed");

}
