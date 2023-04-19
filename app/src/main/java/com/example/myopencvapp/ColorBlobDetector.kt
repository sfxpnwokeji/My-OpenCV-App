package com.example.myopencvapp

import org.opencv.core.*
import org.opencv.imgproc.Imgproc


class ColorBlobDetector {
    // Lower and Upper bounds for range checking in HSV color space
    private val mLowerBound = Scalar(0.0)
    private val mUpperBound = Scalar(0.0)

    // Color radius for range checking in HSV color space
    private var mColorRadius = Scalar(25.0, 50.0, 50.0, 0.0)
    val spectrum = Mat()
    private val mContours: MutableList<MatOfPoint> = ArrayList()

    // Cache
    var mPyrDownMat = Mat()
    var mHsvMat = Mat()
    var mMask = Mat()
    var mDilatedMask = Mat()
    var mHierarchy = Mat()
    fun setColorRadius(radius: Scalar) {
        mColorRadius = radius
    }

    companion object {
        // Minimum contour area in percent for contours filtering
        private var mMinContourArea = 0.1
    }

    fun setHsvColor(hsvColor: Scalar) {
        val minH: Double =
            if (hsvColor.`val`[0] >= mColorRadius.`val`[0]) hsvColor.`val`[0] - mColorRadius.`val`[0] else 0.0
        val maxH: Double =
            if (hsvColor.`val`[0] + mColorRadius.`val`[0] <= 255) hsvColor.`val`[0] + mColorRadius.`val`[0] else 255.0
        mLowerBound.`val`[0] = minH
        mUpperBound.`val`[0] = maxH
        mLowerBound.`val`[1] = hsvColor.`val`[1] - mColorRadius.`val`[1]
        mUpperBound.`val`[1] = hsvColor.`val`[1] + mColorRadius.`val`[1]
        mLowerBound.`val`[2] = hsvColor.`val`[2] - mColorRadius.`val`[2]
        mUpperBound.`val`[2] = hsvColor.`val`[2] + mColorRadius.`val`[2]
        mLowerBound.`val`[3] = 0.0
        mUpperBound.`val`[3] = 255.0
        val spectrumHsv = Mat(1, (maxH - minH).toInt(), CvType.CV_8UC3)
        var j = 0
        while (j < maxH - minH) {
            val tmp = byteArrayOf((minH + j).toInt().toByte(), 255.toByte(), 255.toByte())
            spectrumHsv.put(0, j, tmp)
            j++
        }
        Imgproc.cvtColor(spectrumHsv, spectrum, Imgproc.COLOR_HSV2RGB_FULL, 4)
    }

    fun setMinContourArea(area: Double) {
       mMinContourArea = area
    }

    fun process(rgbaImage: Mat?) {
        Imgproc.pyrDown(rgbaImage, mPyrDownMat)
        Imgproc.pyrDown(mPyrDownMat, mPyrDownMat)
        Imgproc.cvtColor(mPyrDownMat, mHsvMat, Imgproc.COLOR_RGB2HSV_FULL)
        Core.inRange(mHsvMat, mLowerBound, mUpperBound, mMask)
        Imgproc.dilate(mMask, mDilatedMask, Mat())
        val contours: List<MatOfPoint> = ArrayList()
        Imgproc.findContours(
            mDilatedMask,
            contours,
            mHierarchy,
            Imgproc.RETR_EXTERNAL,
            Imgproc.CHAIN_APPROX_SIMPLE
        )

        // Find max contour area
        var maxArea = 0.0
        var each = contours.iterator()
        while (each.hasNext()) {
            val wrapper = each.next()
            val area = Imgproc.contourArea(wrapper)
            if (area > maxArea) maxArea = area
        }

        // Filter contours by area and resize to fit the original image size
        mContours.clear()
        each = contours.iterator()
        while (each.hasNext()) {
            val contour = each.next()
            if (Imgproc.contourArea(contour) > ColorBlobDetector.Companion.mMinContourArea * maxArea) {
                Core.multiply(contour, Scalar(4.0, 4.0), contour)
                mContours.add(contour)
            }
        }
    }

    val contours: List<MatOfPoint>
        get() = mContours

}

//class MainActivity : AppCompatActivity(), View.OnTouchListener, CvCameraViewListener2 {
//
//    private var mIsColorSelected = false
//    private var mRgba: Mat? = null
//    private var mBlobColorRgba: Scalar? = null
//    private var mBlobColorHsv: Scalar? = null
//    private var mDetector: ColorBlobDetector? = null
//    private var mSpectrum: Mat? = null
//    private var SPECTRUM_SIZE: Size? = null
//    private var CONTOUR_COLOR: Scalar? = null
//    private var mOpenCvCameraView: CameraBridgeViewBase? = null
//    private val mLoaderCallback: BaseLoaderCallback = object : BaseLoaderCallback(this) {
//        override fun onManagerConnected(status: Int) {
//            when (status) {
//                LoaderCallbackInterface.SUCCESS -> {
//                    Log.i(TAG, "OpenCV loaded successfully")
//                    mOpenCvCameraView?.enableView()
//                    mOpenCvCameraView?.setOnTouchListener(this@MainActivity)
//                }
//                else -> super.onManagerConnected(status)
//            }
//        }
//    }
//
//    companion object {
//        private val TAG = "MainActivity"
//    }
//
//
//
//    init {
//        Log.i(TAG, "Instantiated new ${this::class}")
//    }
//
//
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        Log.i(TAG, "called onCreate")
////        OpenCVLoader.initDebug()
//        supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
//        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
//        setContentView(R.layout.color_blob_detection_surface_view)
//        mOpenCvCameraView =
//            findViewById(R.id.color_blob_detection_activity_surface_view)
//        mOpenCvCameraView!!.visibility = SurfaceView.VISIBLE
//        mOpenCvCameraView!!.setCvCameraViewListener(this)
////        setContentView(R.layout.activity_main)
//    }
//
//    override fun onPause() {
//        super.onPause()
//        if (mOpenCvCameraView != null) mOpenCvCameraView!!.disableView()
//    }
//
//    override fun onResume() {
//        super.onResume()
//        if (!OpenCVLoader.initDebug()) {
//            Log.d(
//                TAG,
//                "Internal OpenCV library not found. Using OpenCV Manager for initialization"
//            )
//            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback)
//        } else {
//            Log.d(TAG, "OpenCV library found inside package. Using it!")
//            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS)
//        }
//    }
//
//    override fun onDestroy() {
//        super.onDestroy()
//        if (mOpenCvCameraView != null) mOpenCvCameraView!!.disableView()
//    }
//
//    fun makeGray(bitmap: Bitmap) : Bitmap {
//
//        // Create OpenCV mat object and copy content from bitmap
//        val mat = Mat()
//        Utils.bitmapToMat(bitmap, mat)
//
//        // Convert to grayscale
//        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGB2GRAY)
//
//        // Make a mutable bitmap to copy grayscale image
//        val grayBitmap = bitmap.copy(bitmap.config, true)
//        Utils.matToBitmap(mat, grayBitmap)
//
//        return grayBitmap
//    }
//
//    override fun onTouch(view: View?, event: MotionEvent?): Boolean {
//        val cols = mRgba!!.cols()
//        val rows = mRgba!!.rows()
//        val xOffset = (mOpenCvCameraView!!.width - cols) / 2
//        val yOffset = (mOpenCvCameraView!!.height - rows) / 2
//        val x = (event?.x?.toInt() ?: 0) - xOffset
//        val y = (event?.y?.toInt() ?: 0) - yOffset
//        Log.i(TAG, "Touch image coordinates: ($x, $y)")
//        if (x < 0 || y < 0 || x > cols || y > rows) return false
//        val touchedRect = Rect()
//        touchedRect.x = if (x > 4) x - 4 else 0
//        touchedRect.y = if (y > 4) y - 4 else 0
//        touchedRect.width = if (x + 4 < cols) x + 4 - touchedRect.x else cols - touchedRect.x
//        touchedRect.height = if (y + 4 < rows) y + 4 - touchedRect.y else rows - touchedRect.y
//        val touchedRegionRgba: Mat = mRgba!!.submat(touchedRect)
//        val touchedRegionHsv = Mat()
//        Imgproc.cvtColor(touchedRegionRgba, touchedRegionHsv, Imgproc.COLOR_RGB2HSV_FULL)
//
//        // Calculate average color of touched region
//        mBlobColorHsv = Core.sumElems(touchedRegionHsv)
//        val pointCount: Int = touchedRect.width * touchedRect.height
//        for (i in (mBlobColorHsv as Scalar).`val`.indices) (mBlobColorHsv as Scalar).`val`[i] /= pointCount.toDouble()
//        mBlobColorRgba = converScalarHsv2Rgba(mBlobColorHsv)
//        Log.i(
//            TAG,
//            "Touched rgba color: (" + mBlobColorRgba!!.`val`[0] + ", " + mBlobColorRgba!!.`val`[1] +
//                    ", " + mBlobColorRgba!!.`val`[2] + ", " + mBlobColorRgba!!.`val`[3] + ")"
//        )
//        mDetector!!.setHsvColor((mBlobColorHsv as Scalar))
//        Imgproc.resize(
//            mDetector!!.spectrum,
//            mSpectrum,
//            SPECTRUM_SIZE,
//            0.0,
//            0.0,
//            Imgproc.INTER_LINEAR_EXACT
//        )
//        mIsColorSelected = true
//        touchedRegionRgba.release()
//        touchedRegionHsv.release()
//        return false // don't need subsequent touch events
//    }
//
//    override fun onCameraViewStarted(width: Int, height: Int) {
//        mRgba = Mat(height, width, CvType.CV_8UC4)
//        mDetector = ColorBlobDetector()
//        mSpectrum = Mat()
//        mBlobColorRgba = Scalar(255.0)
//        mBlobColorHsv = Scalar(255.0)
//        SPECTRUM_SIZE = Size(200.0, 64.0)
//        CONTOUR_COLOR = Scalar(255.0, 0.0, 0.0, 255.0)
//
//    }
//
//    override fun onCameraViewStopped() {
//        mRgba!!.release()
//
//    }
//
//    override fun onCameraFrame(inputFrame: CameraBridgeViewBase.CvCameraViewFrame?): Mat? {
//        if (inputFrame != null) {
//            mRgba = inputFrame.rgba()
//        }
//        if (mIsColorSelected) {
//            mDetector!!.process(mRgba)
//            val contours = mDetector!!.contours
//            Log.e(TAG, "Contours count: " + contours.size)
//            Imgproc.drawContours(mRgba, contours, -1, CONTOUR_COLOR)
//            mRgba?.submat(4, 68, 4, 68)?.setTo(mBlobColorRgba)
//            val spectrumLabel =
//                mRgba?.submat(4, 4 + mSpectrum!!.rows(), 70, 70 + mSpectrum!!.cols())
//            mSpectrum!!.copyTo(spectrumLabel)
//        }
//        return mRgba
//    }
//
//
//    private fun converScalarHsv2Rgba(hsvColor: Scalar?): Scalar {
//        val pointMatRgba = Mat()
//        val pointMatHsv = Mat(1, 1, CvType.CV_8UC3, hsvColor)
//        Imgproc.cvtColor(pointMatHsv, pointMatRgba, Imgproc.COLOR_HSV2RGB_FULL, 4)
//        return Scalar(pointMatRgba[0, 0])
//    }
//
//
//}