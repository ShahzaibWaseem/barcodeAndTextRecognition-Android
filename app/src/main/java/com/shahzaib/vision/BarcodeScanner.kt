package com.shahzaib.vision

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
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
import com.google.android.gms.vision.barcode.Barcode
import com.google.android.gms.vision.barcode.BarcodeDetector

class BarcodeScanner : AppCompatActivity() {
    private val CAMERA_REQUEST_CODE = 123
    private lateinit var svBarcode: SurfaceView
    private lateinit var detector: BarcodeDetector
    private lateinit var cameraSource: CameraSource

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.barcode)

        val actionBar = supportActionBar
        actionBar?.setTitle(R.string.barcode_text)

        val captureButton = findViewById<Button>(R.id.captureButton)
        val button = findViewById<Button>(R.id.button)
        button.setOnClickListener{
            val intent = Intent(this, TextRecognition::class.java)
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

        svBarcode = findViewById(R.id.sv_barcode)

        detector = BarcodeDetector.Builder(this).setBarcodeFormats(Barcode.ALL_FORMATS).build()
        detector.setProcessor(object : Detector.Processor<Barcode> {
            override fun release() {}
            @SuppressLint("MissingPermission")
            override fun receiveDetections(p0: Detector.Detections<Barcode>?) {
                captureButton.setOnClickListener {
                    val barcodes = p0?.detectedItems
                    if (barcodes!!.size() > 0) {
                        builder.setMessage(barcodes.valueAt(0).displayValue)
                        builder.setPositiveButton("Okay") { dialog, which ->
                            cameraSource.start(svBarcode.holder)
                        }
                        taskHandler.post(runnable)
                    }
                }
            }
        })

        val height = resources.getInteger(R.integer.cameraHeight)
        val width = resources.getInteger(R.integer.cameraWidth)
        val fps = resources.getInteger(R.integer.requestedFps).toFloat()

        cameraSource = CameraSource.Builder(this, detector).setRequestedPreviewSize(height, width)
            .setRequestedFps(fps).setAutoFocusEnabled(true).build()
        svBarcode.holder.addCallback(object : SurfaceHolder.Callback2 {
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
                if (ContextCompat.checkSelfPermission(this@BarcodeScanner, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
                    cameraSource.start(svBarcode.holder)
                else ActivityCompat.requestPermissions(this@BarcodeScanner, arrayOf(android.Manifest.permission.CAMERA), CAMERA_REQUEST_CODE)
            }
        })
    }

    @SuppressLint("MissingPermission")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                cameraSource.start(svBarcode.holder)
            else Toast.makeText(this, "scanner", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        detector.release()
        cameraSource.stop()
        cameraSource.release()
    }
}