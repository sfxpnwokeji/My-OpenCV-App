package com.example.myopencvapp


enum class ValidationStatus(var code: Int, var description: String) {
    MATCH(0, "Face verified"),

    NO_FACE_FOUND_SOURCE(-1, "No face found in source image"),
    NO_FACE_FOUND_TARGET(-1, "No face found in target image"),

    MULTI_FACE_FOUND_SOURCE(-2, "Multiple faces found in source"),
    MULTI_FACE_FOUND_TARGET(-2, "Multiple faces found in target"),

    FACE_OUT_OF_BOX_SOURCE(-3, "Face too close in source image"),
    FACE_OUT_OF_BOX_TARGET(-3, "Face too close in target image"),

    FAILED_INIT(-4, "Unable to load components"),
    NO_MATCH(-5, "Face not verified"),
    IMAGE_PROCESSING_FAILED(-6, "Image processing failed");

}
