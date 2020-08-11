package com.shahzaib.vision

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.os.Bundle
import android.os.Handler
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.vision.CameraSource
import com.google.android.gms.vision.Detector
import com.google.android.gms.vision.text.TextBlock
import com.google.android.gms.vision.text.TextRecognizer

class TextRecognition : AppCompatActivity() {
    private val CAMERA_REQUEST_CODE = 123
    private lateinit var svTextRecognizer: SurfaceView
    private lateinit var recognizer: TextRecognizer
    private lateinit var cameraSource: CameraSource

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.textrecognition)

        val actionBar = supportActionBar
        actionBar?.setTitle(R.string.textRecognition_text)

        val transparentView = findViewById<SurfaceView>(R.id.sv_transparentview)

        val holderTransparent = transparentView.holder
        holderTransparent.setFormat(PixelFormat.TRANSPARENT)
        holderTransparent.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS)

        val captureButton = findViewById<Button>(R.id.captureButton)
        val button = findViewById<Button>(R.id.button)
        button.setOnClickListener{
            val intent = Intent(this, BarcodeScanner::class.java)
            startActivity(intent)
        }

        val builder = AlertDialog.Builder(this)
        var taskHandler = Handler()
        var runnable = object:Runnable{
            override fun run() {
                cameraSource.stop()
                val alert = builder.create()
                alert.show()
                taskHandler.removeCallbacksAndMessages(null)
            }
        }

        svTextRecognizer = findViewById(R.id.sv_textrecognition)

        recognizer = TextRecognizer.Builder(this).build()

        captureButton.setOnClickListener {

            recognizer.setProcessor(object : Detector.Processor<TextBlock> {
                override fun release() {}

                @SuppressLint("MissingPermission")
                override fun receiveDetections(textBlockDetections: Detector.Detections<TextBlock>?) {
                    captureButton.setOnClickListener {
                        val textBlocks = textBlockDetections?.detectedItems
                        if (textBlocks!!.size() > 0) {
                            val stringBuilder = StringBuilder()

                            for (i in 0 until textBlocks.size()) {
                                val item: TextBlock = textBlocks.valueAt(i)
                                stringBuilder.append(item.value)
                                stringBuilder.append("\n")
                            }
                            builder.setTitle(R.string.textRecognition_text)
                            builder.setMessage(stringBuilder.toString())
                            builder.setPositiveButton("Okay") { dialog, which ->
                                cameraSource.start(svTextRecognizer.holder)
                            }
                            taskHandler.post(runnable)
                        }
                    }
                }
            })
        }

        val height = resources.getInteger(R.integer.cameraHeight)
        val width = resources.getInteger(R.integer.cameraWidth)
        val fps = resources.getInteger(R.integer.requestedFps).toFloat()

        cameraSource = CameraSource.Builder(this, recognizer).setRequestedPreviewSize(height, width)
            .setRequestedFps(fps).setAutoFocusEnabled(true).build()
        svTextRecognizer.holder.addCallback(object : SurfaceHolder.Callback2 {
            override fun surfaceRedrawNeeded(p0: SurfaceHolder) {
                print("1")
            }
            override fun surfaceChanged(p0: SurfaceHolder, format: Int, width: Int, height: Int) {
                print("2")
            }
            override fun surfaceDestroyed(p0: SurfaceHolder) {
                print("3")
                cameraSource.stop()
            }
            override fun surfaceCreated(p0: SurfaceHolder) {
                print("4")
                if (ContextCompat.checkSelfPermission(this@TextRecognition, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
                    cameraSource.start(svTextRecognizer.holder)
                else ActivityCompat.requestPermissions(this@TextRecognition, arrayOf(android.Manifest.permission.CAMERA), CAMERA_REQUEST_CODE)
            }
        })
    }

    @SuppressLint("MissingPermission")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                cameraSource.start(svTextRecognizer.holder)
            else Toast.makeText(this, "detector", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        recognizer.release()
        cameraSource.stop()
        cameraSource.release()
    }
}