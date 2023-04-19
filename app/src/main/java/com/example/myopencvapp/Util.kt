package com.example.myopencvapp

import android.content.Context
import org.apache.commons.io.FileUtils
import java.io.File
import java.io.IOException

class Util {


     fun moveRawFileToPrivateDir(
        absoluteDir: String,
        context: Context,
        vararg files: String
    ): String {
        for (fileName in files) {
            moveFiles(fileName, absoluteDir, context)
        }
        return absoluteDir
    }

    /**
     * Move files to specified private directory
     *
     * @param context application context
     */
    private fun moveFiles(fileNme: String, absoluteDir: String, context: Context) {
        try {
            val `is` = context.assets.open(fileNme)
            val out = File(absoluteDir, fileNme)
            FileUtils.copyInputStreamToFile(`is`, out)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}
