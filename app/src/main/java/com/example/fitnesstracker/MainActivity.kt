package com.example.fitnesstracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fitnesstracker.ui.theme.FitnessTrackerTheme
// Class 1
import android.annotation.SuppressLint
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.math.sqrt

@SuppressLint ("SetTextI18n")
class MainActivity : ComponentActivity(), SensorEventListener {

    // compose state - read by the fun, written from sensor callbacks
    private var motionText by mutableStateOf("Motions: Stationary")
    private var gyroText by mutableStateOf("Rotation (Pitch, Roll, Yaw): 0.00, 0.00, 0.00")
    private var directionText by mutableStateOf("Direction:")

    // Sensors
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var gyroscope: Sensor? = null
    private var magnetometer: Sensor? = null

    // Sensor fusion - both needed for the compass
    private var accelValues = FloatArray(3)
    private var magnetValues = FloatArray(3)


    // Baseline - cancels gravity
    private var baselineX = 0f
    private var baselineY = 0f

    private var baselineZ = 9.8f

    companion object {
        private const val STATIONARY_THRESHOLD = 2.0f
        private const val WALKING_THRESHOLD = 12.0f
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initSensors()
        setContent {
            FitnessTrackerTheme {
                Surface(modifier = Modifier.fillMaxSize()){
                    FitnessTrackerScreen(
                        motionText = motionText,
                        gyroText = gyroText,
                        directionText = directionText
                    )
                }
            }
        }
    }
    override fun onResume() {
        super.onResume()
        accelerometer?.let {
            sensorManager.registerListener(
                this,
                it,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }
        gyroscope?.let {
            sensorManager.registerListener(
                this,
                it,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }
        magnetometer?.let {
            sensorManager.registerListener(
                this,
                it,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    private fun initSensors(){
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                accelValues = event.values.clone()
                handleAccelerometer(event.values)
            }
        }
            // ASSIGNMENT 1
            // Add two more cases to this when block, copying the shape of the
            // accelerometer case above:
            // - Sensor.TYPE_GYROSCOPE: call handleGyroscope(event.values)
            // - Sensor.TYPE_MAGENTIC_FIELD: save a CLONE of event.values into
            // magnetValues (same reason as above), then call update Compass()
        when (event.sensor.type) {
            Sensor.TYPE_GYROSCOPE -> {
                handleGyroscope(event.values)
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                magnetValues = event.values.clone()
                updateCompass()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) { }

    private fun handleAccelerometer(values: FloatArray) {
        val x = values[0] - baselineX
        val y = values[1] - baselineY
        val z = values[2] - baselineZ
        val magnitude = sqrt(x * x + y * y + z * z)
        motionText = "Motion: ${classifyMotion(magnitude)}"
    }

    private fun classifyMotion(magnitude: Float): String {
        return when {
            magnitude < STATIONARY_THRESHOLD -> "Stationary"
            magnitude < WALKING_THRESHOLD -> "Walking"
            else -> "Jogging"
        }
    }

    // values [] are rotation rates in radians/second - %.2f keeps 2 decimal places
    private fun handleGyroscope(values: FloatArray) {
        gyroText = "Rotation (Pitch, Roll, Yaw): ${"%.2f".format(values[0])}, ${"%.2f".format(values[1])}, ${
            "%.2f".format(
                values[2]
            )
        }"
    }

    private fun updateCompass() {
        val rm = FloatArray(9); val im = FloatArray(9)
        if (SensorManager.getRotationMatrix(rm, im, accelValues, magnetValues)) {
            val orientation = FloatArray(3); SensorManager.getOrientation(rm, orientation)
            val deg = (Math.toDegrees(orientation[0].toDouble()).toFloat() + 360) % 360
            directionText = "Direction: ${getDirection(deg)}"
        }
    }

    private fun getDirection(d: Float): String = when {
        d < 45 || d >= 315  -> "N" // 0-44 and 315-359
        d < 135             -> "E" // 45-134
        d < 225             -> "S" // 135-224
        else                -> "W" // 225-314
    }
}


@Composable
fun FitnessTrackerScreen(motionText: String, gyroText: String, directionText: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text(text = motionText, fontSize = 20.sp, modifier = Modifier.padding(20.dp))
        Text(text = gyroText, fontSize = 16.sp, modifier = Modifier.padding(20.dp))
        Text(text = directionText, fontSize = 16.sp, modifier = Modifier.padding(20.dp))
    }
}
