package com.shahzaib.vision

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.*
import android.os.*
import android.util.Log
import android.util.SparseArray
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.util.valueIterator
import com.google.android.gms.common.images.Size
import com.google.android.gms.vision.CameraSource
import com.google.android.gms.vision.Detector
import com.google.android.gms.vision.text.Text
import com.google.android.gms.vision.text.TextBlock
import com.google.android.gms.vision.text.TextRecognizer
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.abs

class TextRecognition : AppCompatActivity() {
    private var widthScaleFactor = 2.0f
    private var heightScaleFactor = 2.0f
    private var previewWidth = 1.0f
    private var previewHeight = 1.0f
    private var recognizedTextCount = 0

    private lateinit var svTextRecognizer: SurfaceView
    private lateinit var recognizer: TextRecognizer
    private lateinit var cameraSource: CameraSource
    private lateinit var textBlocks : SparseArray<TextBlock>

    private var recognizedTextHistory: ArrayList<String> = kotlin.collections.ArrayList()
    private var previousFrameDetectionCount = 0     // number of textBlocks detected in the previous frame

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.textrecognition)

        val actionBar = supportActionBar
        actionBar?.setTitle(R.string.textRecognition_text)

        val transparentView = findViewById<SurfaceView>(R.id.sv_transparentview)

        val holderTransparent = transparentView.holder
        holderTransparent.setFormat(PixelFormat.TRANSPARENT)
        holderTransparent.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS)

        val button = findViewById<Button>(R.id.button)
        button.setOnClickListener{
            val intent = Intent(this, BarcodeScanner::class.java)
            startActivity(intent)
            finish()
        }

        val builder = AlertDialog.Builder(this)
        val taskHandler = Handler()
        val runnable = Runnable {
            cameraSource.stop()
            val alert = builder.create()
            alert.show()
            taskHandler.removeCallbacksAndMessages(null)
        }

        // Bounding Box Paint Object
        val rectPaint = Paint()
        rectPaint.color = Color.WHITE
        rectPaint.style = Paint.Style.STROKE
        rectPaint.strokeWidth = 4.0f

        svTextRecognizer = findViewById(R.id.sv_textrecognition)
        recognizer = TextRecognizer.Builder(this).build()
        recognizer.setProcessor(object : Detector.Processor<TextBlock> {
            override fun release() {}

            @SuppressLint("MissingPermission")
            override fun receiveDetections(textBlockDetections: Detector.Detections<TextBlock>?) {
                textBlocks = textBlockDetections?.detectedItems as SparseArray<TextBlock>
                val numberOfDetections = textBlocks.size()

                clearCanvas(holderTransparent)
                // TODO Add recognizedTextCount <= SIZE_OF_DETECTION_HISTORY check here
                if (textBlocks.size() > 0) {
                    val stringBuilder = StringBuilder()

                    // Break the text into multiple lines and draw each one according to its own bounding box.
                    for (block in textBlocks.valueIterator()) {
                        val textComponents: List<Text?> = block.components

                        stringBuilder.append(block.value)

                        for (currentText: Text? in textComponents) {
                            val canvas = holderTransparent.lockCanvas()
                            if (currentText != null) {
                                var rect = RectF(currentText.boundingBox)

                                Log.i("Bounding Box", currentText.boundingBox.toString())
                                rect = translateRect(rect, currentText)
                                canvas.drawRect(rect, rectPaint)

                                holderTransparent.unlockCanvasAndPost(canvas)
                            }
                        }
                        stringBuilder.append("\n")
                    }
                    clearCanvas(holderTransparent)
                    if (abs(numberOfDetections - previousFrameDetectionCount) <= BOUNDING_BOXES_DIFFERENCE_ALLOWED) {
                        recognizedTextCount++
                        Log.i("History Recognized", "Number of Items added to the History: $recognizedTextCount")
//                        TODO("Add conditional checks so that the text is only added if a similarity is found compared to the previous text appended.")
                        recognizedTextHistory.add(stringBuilder.toString().trim().toLowerCase(Locale.ROOT).capitalize(Locale.ROOT))
                        Log.i("Recognized Text Count", recognizedTextCount.toString())
                    } else {
                        recognizedTextCount = 0
                        recognizedTextHistory.clear()
                        previousFrameDetectionCount = numberOfDetections
                    }

                    if (recognizedTextCount >= SIZE_OF_DETECTION_HISTORY){
                        val intent = Intent(this@TextRecognition, RecognizedActivity::class.java)
                        intent.putStringArrayListExtra("recognizedTextHistory", recognizedTextHistory)
                        startActivity(intent)
                        finish()
                    }
                }
            }
        })

        val height: Int = Resources.getSystem().displayMetrics.heightPixels
        val width: Int = Resources.getSystem().displayMetrics.widthPixels
        val fps: Float = resources.getInteger(R.integer.requestedFps).toFloat()

        Log.i("Display Size", "$height x $width")

        cameraSource = CameraSource.Builder(this, recognizer).setRequestedPreviewSize(height, width).setRequestedFps(fps).setAutoFocusEnabled(true).build()
        svTextRecognizer.holder.addCallback(object : SurfaceHolder.Callback2 {
            override fun surfaceRedrawNeeded(p0: SurfaceHolder) {
                Log.i("Surface Log", "Surface Redrawn Needed")
            }

            override fun surfaceChanged(p0: SurfaceHolder, format: Int, width: Int, height: Int) {
                Log.i("Surface Log", "Surface Changed")
                if (ContextCompat.checkSelfPermission(this@TextRecognition, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    cameraSource.start(svTextRecognizer.holder)
                    setScalingFactors(svTextRecognizer.holder)
                }
                else ActivityCompat.requestPermissions(this@TextRecognition, arrayOf(android.Manifest.permission.CAMERA), CAMERA_REQUEST_CODE)
            }

            override fun surfaceDestroyed(p0: SurfaceHolder) {
                Log.i("Surface Log", "Surface Destroyed")
            }

            override fun surfaceCreated(p0: SurfaceHolder) {
                Log.i("Surface Log", "Surface Created")
                if (ContextCompat.checkSelfPermission(this@TextRecognition, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    cameraSource.start(svTextRecognizer.holder)
                    setScalingFactors(svTextRecognizer.holder)
                } else ActivityCompat.requestPermissions(this@TextRecognition, arrayOf(android.Manifest.permission.CAMERA),CAMERA_REQUEST_CODE)
            }
        })
    }

    private fun setScalingFactors(holder: SurfaceHolder){
        val size: Size = cameraSource.previewSize

        previewWidth = svTextRecognizer.width.toFloat()
        previewHeight = svTextRecognizer.height.toFloat()
        Log.i("Preview Size", "$previewHeight x $previewWidth")
        Log.i("Camera Resolution Size", size.height.toString() + " x " + size.width.toString())

        if (previewWidth != 0f && previewHeight != 0f) {
            widthScaleFactor = previewWidth / size.height
            heightScaleFactor = previewHeight / size.width
            Log.i("Scaling Factor", "Width Scaling: $widthScaleFactor, Height Scaling: $heightScaleFactor")
        }
    }

    private fun clearCanvas(holder: SurfaceHolder){
        val canvas = holder.lockCanvas()
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        holder.unlockCanvasAndPost(canvas)
    }

    private fun scaleX(horizontal: Float): Float {
        return horizontal * widthScaleFactor
    }

    private fun scaleY(vertical: Float): Float {
        return vertical * heightScaleFactor
    }

    fun translateRect(inputRect: RectF, text: Text): RectF {
        val returnRect = RectF(text.boundingBox)
        returnRect.left = scaleX(inputRect.left)
        returnRect.top = scaleY(inputRect.top)
        returnRect.right = scaleX(inputRect.right)
        returnRect.bottom = scaleY(inputRect.bottom)

        Log.i("Translated Rectangle", returnRect.toString())
        return returnRect
    }

    @SuppressLint("MissingPermission")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                cameraSource.start(svTextRecognizer.holder)
                setScalingFactors(svTextRecognizer.holder)
            }
            else Toast.makeText(this, "detector", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        recognizer.release()
        cameraSource.stop()
        cameraSource.release()
    }

    // Constants
    companion object {
        const val CAMERA_REQUEST_CODE = 123
        const val SIZE_OF_DETECTION_HISTORY = 3
        const val BOUNDING_BOXES_DIFFERENCE_ALLOWED = 10      // Allowance for number of boundingBoxes created between two frames
    }
}