package com.example.wellnessguide.ui.activity

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View.generateViewId
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.wellnessguide.MainActivity
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

class RouteTrackerFragment : Fragment() {

    private val primaryColor = Color.rgb(26, 107, 114)
    private val backgroundColor = Color.rgb(238, 248, 247)
    private val surfaceColor = Color.WHITE
    private val textPrimary = Color.rgb(28, 43, 45)
    private val textSecondary = Color.rgb(93, 122, 126)
    private val borderColor = Color.rgb(221, 237, 234)

    private var googleMap: GoogleMap? = null
    private lateinit var mapContainer: FrameLayout

    private val fusedLocationClient by lazy {
        LocationServices.getFusedLocationProviderClient(requireActivity())
    }

    private val routePoints = mutableListOf<LatLng>()

    private var tracking = false
    private var startTime = 0L
    private var totalDistanceMeters = 0.0
    private var lastLocation: Location? = null

    private lateinit var statusText: TextView
    private lateinit var distanceText: TextView
    private lateinit var durationText: TextView
    private lateinit var pointsText: TextView
    private lateinit var startButton: Button
    private lateinit var stopButton: Button

    private val auth: FirebaseAuth
        get() = FirebaseAuth.getInstance()

    private val db: FirebaseFirestore
        get() = FirebaseFirestore.getInstance()

    private val locationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                enableMapLocation()
                startTracking()
            } else {
                Toast.makeText(
                    requireContext(),
                    "Location permission is needed for route tracking.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            if (!tracking) return

            val location = result.lastLocation ?: return
            val point = LatLng(location.latitude, location.longitude)

            if (lastLocation != null) {
                totalDistanceMeters += lastLocation!!.distanceTo(location)
            }

            lastLocation = location
            routePoints.add(point)

            drawRoute()
            updateSummary()

            googleMap?.animateCamera(
                CameraUpdateFactory.newLatLngZoom(point, 17f)
            )
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): android.view.View {
        val scroll = ScrollView(requireContext()).apply {
            setBackgroundColor(backgroundColor)
            isFillViewport = true
        }

        val root = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(18), dp(22), dp(42))
        }

        addTopBar(root)

        root.addView(bigTitle("Walk Route Tracker"))

        root.addView(
            paragraph(
                "Start tracking before walking. The app will use GPS to draw your walking route on the map."
            )
        )

        mapContainer = FrameLayout(requireContext()).apply {
            id = generateViewId()
            background = roundedBg(
                surfaceColor,
                borderColor,
                2,
                24f
            )

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(360)
            ).apply {
                topMargin = dp(14)
            }
        }

        root.addView(mapContainer)

        val mapFragment = SupportMapFragment.newInstance()

        childFragmentManager.beginTransaction()
            .replace(mapContainer.id, mapFragment)
            .commit()

        mapFragment.getMapAsync { map ->
            googleMap = map
            googleMap?.uiSettings?.isZoomControlsEnabled = true
            enableMapLocation()
        }

        root.addView(buildSummaryCard())

        startButton = actionButton("Start Walk")
        startButton.setOnClickListener {
            checkLocationPermissionThenStart()
        }
        root.addView(startButton)

        stopButton = secondaryButton("Stop & Save Walk")
        stopButton.setOnClickListener {
            stopTrackingAndSave()
        }
        root.addView(stopButton)

        scroll.addView(root)

        updateSummary()

        return scroll
    }

    private fun buildSummaryCard(): LinearLayout {
        val card = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(16), dp(18), dp(16))

            background = roundedBg(
                surfaceColor,
                borderColor,
                2,
                26f
            )

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(14)
            }
        }

        statusText = metric("Status: Not tracking")
        distanceText = metric("Distance: 0.00 km")
        durationText = metric("Duration: 0 minutes")
        pointsText = metric("GPS Points: 0")

        card.addView(statusText)
        card.addView(distanceText)
        card.addView(durationText)
        card.addView(pointsText)

        return card
    }

    private fun checkLocationPermissionThenStart() {
        val granted = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (granted) {
            startTracking()
        } else {
            locationPermissionLauncher.launch(
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
    }

    private fun startTracking() {
        if (tracking) {
            Toast.makeText(
                requireContext(),
                "Already tracking.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val granted = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!granted) return

        routePoints.clear()
        totalDistanceMeters = 0.0
        lastLocation = null
        startTime = System.currentTimeMillis()
        tracking = true

        googleMap?.clear()
        updateSummary()

        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            5000L
        )
            .setMinUpdateDistanceMeters(5f)
            .build()

        fusedLocationClient.requestLocationUpdates(
            request,
            locationCallback,
            Looper.getMainLooper()
        )

        Toast.makeText(
            requireContext(),
            "Route tracking started.",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun stopTrackingAndSave() {
        if (!tracking) {
            Toast.makeText(
                requireContext(),
                "Start a walk first.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        tracking = false

        fusedLocationClient.removeLocationUpdates(locationCallback)

        updateSummary()
        saveRoute()
    }

    private fun saveRoute() {
        val user = auth.currentUser

        if (user == null) {
            Toast.makeText(
                requireContext(),
                "Please login to save route.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        if (routePoints.size < 2) {
            Toast.makeText(
                requireContext(),
                "Not enough route points to save.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val durationMillis = System.currentTimeMillis() - startTime
        val durationMinutes = (durationMillis / 60000).coerceAtLeast(1)
        val distanceKm = totalDistanceMeters / 1000.0
        val calories = (distanceKm * 55).roundToInt()

        val routeData = routePoints.map {
            mapOf(
                "lat" to it.latitude,
                "lng" to it.longitude
            )
        }

        val summary = """
            Walk Distance: ${"%.2f".format(distanceKm)} km
            Duration: $durationMinutes minutes
            GPS Points: ${routePoints.size}
            Estimated Calories: $calories kcal
        """.trimIndent()

        val data = hashMapOf<String, Any>(
            "userId" to user.uid,
            "logType" to "route_activity",
            "title" to "Walk Route",
            "status" to "Green - Route saved",
            "summary" to summary,
            "recommendations" to "Good walk. Hydrate and rest if needed.",
            "createdAt" to System.currentTimeMillis(),
            "dayKey" to todayKey(),
            "details" to mapOf(
                "distanceKm" to distanceKm,
                "durationMinutes" to durationMinutes,
                "calories" to calories,
                "routePoints" to routeData
            )
        )

        db.collection("wellness_logs")
            .add(data)
            .addOnSuccessListener {
                Toast.makeText(
                    requireContext(),
                    "Walk route saved.",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .addOnFailureListener { error ->
                Toast.makeText(
                    requireContext(),
                    error.message ?: "Failed to save route.",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun drawRoute() {
        val map = googleMap ?: return

        map.clear()

        if (routePoints.isNotEmpty()) {
            map.addMarker(
                MarkerOptions()
                    .position(routePoints.first())
                    .title("Start")
            )
        }

        if (routePoints.size > 1) {
            map.addPolyline(
                PolylineOptions()
                    .addAll(routePoints)
                    .width(10f)
                    .color(primaryColor)
            )

            map.addMarker(
                MarkerOptions()
                    .position(routePoints.last())
                    .title("Current")
            )
        }
    }

    private fun updateSummary() {
        if (
            !::statusText.isInitialized ||
            !::distanceText.isInitialized ||
            !::durationText.isInitialized ||
            !::pointsText.isInitialized
        ) {
            return
        }

        val durationMinutes = if (startTime > 0L) {
            ((System.currentTimeMillis() - startTime) / 60000).coerceAtLeast(0)
        } else {
            0
        }

        statusText.text = if (tracking) {
            "Status: Tracking route"
        } else {
            "Status: Not tracking"
        }

        distanceText.text = "Distance: ${"%.2f".format(totalDistanceMeters / 1000.0)} km"
        durationText.text = "Duration: $durationMinutes minutes"
        pointsText.text = "GPS Points: ${routePoints.size}"
    }

    private fun enableMapLocation() {
        val granted = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (granted) {
            googleMap?.isMyLocationEnabled = true
        }
    }

    override fun onPause() {
        super.onPause()

        if (tracking) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
            tracking = false
            updateSummary()

            Toast.makeText(
                requireContext(),
                "Tracking paused because the page was closed.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun todayKey(): String {
        return SimpleDateFormat(
            "yyyyMMdd",
            Locale.getDefault()
        ).format(Date())
    }

    private fun addTopBar(root: LinearLayout) {
        val header = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(56)
            )
        }

        val menu = TextView(requireContext()).apply {
            text = "☰"
            textSize = 24f
            gravity = Gravity.CENTER
            setTextColor(textPrimary)

            background = roundedBg(
                surfaceColor,
                borderColor,
                2,
                40f
            )

            layoutParams = LinearLayout.LayoutParams(
                dp(44),
                dp(44)
            )

            setOnClickListener {
                (requireActivity() as MainActivity).openDrawer()
            }
        }

        val title = TextView(requireContext()).apply {
            text = "Route Tracker"
            textSize = 20f
            setTypeface(null, Typeface.BOLD)
            setTextColor(textPrimary)

            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            ).apply {
                leftMargin = dp(12)
            }
        }

        header.addView(menu)
        header.addView(title)
        root.addView(header)
    }

    private fun bigTitle(text: String): TextView {
        return TextView(requireContext()).apply {
            this.text = text
            textSize = 26f
            setTypeface(null, Typeface.BOLD)
            setTextColor(textPrimary)
            setPadding(0, dp(18), 0, dp(4))
        }
    }

    private fun paragraph(text: String): TextView {
        return TextView(requireContext()).apply {
            this.text = text
            textSize = 15f
            setTextColor(textSecondary)
            setLineSpacing(5f, 1f)
            setPadding(0, dp(8), 0, dp(8))
        }
    }

    private fun metric(text: String): TextView {
        return TextView(requireContext()).apply {
            this.text = text
            textSize = 15f
            setTextColor(textSecondary)
            setPadding(0, dp(6), 0, 0)
        }
    }

    private fun actionButton(textValue: String): Button {
        return Button(requireContext()).apply {
            text = textValue
            textSize = 14f
            isAllCaps = false
            setTextColor(Color.WHITE)

            background = roundedBg(
                primaryColor,
                primaryColor,
                2,
                22f
            )

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(56)
            ).apply {
                topMargin = dp(14)
            }
        }
    }

    private fun secondaryButton(textValue: String): Button {
        return Button(requireContext()).apply {
            text = textValue
            textSize = 14f
            isAllCaps = false
            setTextColor(textPrimary)

            background = roundedBg(
                surfaceColor,
                borderColor,
                2,
                22f
            )

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(56)
            ).apply {
                topMargin = dp(12)
            }
        }
    }

    private fun roundedBg(
        bgColor: Int,
        strokeColor: Int,
        strokeWidth: Int,
        radius: Float
    ): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radius
            setColor(bgColor)
            setStroke(strokeWidth, strokeColor)
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
}