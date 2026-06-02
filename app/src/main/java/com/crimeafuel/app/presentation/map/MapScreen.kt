package com.crimeafuel.app.presentation.map

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crimeafuel.app.domain.model.*
import com.crimeafuel.app.util.LocationHelper
import com.crimeafuel.app.presentation.theme.FuelRed
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import kotlinx.coroutines.launch

// Yandex MapKit imports
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.InputListener
import com.yandex.mapkit.map.Map
import com.yandex.mapkit.map.MapObjectCollection
import com.yandex.mapkit.map.MapObjectTapListener
import com.yandex.mapkit.map.ClusterListener
import com.yandex.mapkit.map.ClusterizedPlacemarkCollection
import com.yandex.mapkit.mapview.MapView
import com.yandex.mapkit.user_location.UserLocationLayer
import com.yandex.mapkit.user_location.UserLocationObjectListener
import com.yandex.mapkit.user_location.UserLocationView
import com.yandex.mapkit.layers.ObjectEvent
import com.yandex.runtime.image.ImageProvider
import com.yandex.mapkit.Animation

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MapScreen(
    onNavigateToEdit: (String) -> Unit,
    onNavigateToLogin: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: MapViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Location permissions
    val locationPermissions = rememberMultiplePermissionsState(
        permissions = listOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    LaunchedEffect(Unit) {
        if (!locationPermissions.allPermissionsGranted) {
            locationPermissions.launchMultiplePermissionRequest()
        }
    }

    // Get user location
    LaunchedEffect(locationPermissions.allPermissionsGranted) {
        if (locationPermissions.allPermissionsGranted) {
            val locationHelper = LocationHelper(context)
            val location = locationHelper.getLastLocation()
            location?.let { viewModel.setUserLocation(it) }
        }
    }

    // Map view reference
    var mapView by remember { mutableStateOf<MapView?>(null) }
    var mapObjects by remember { mutableStateOf<MapObjectCollection?>(null) }
    var userLocationLayer by remember { mutableStateOf<UserLocationLayer?>(null) }
    var showAddStationDialog by remember { mutableStateOf<Point?>(null) }
    var showDeleteStationDialog by remember { mutableStateOf<Station?>(null) }

    val currentUiState by rememberUpdatedState(uiState)

    val inputListener = remember {
        object : InputListener {
            override fun onMapTap(map: Map, point: Point) {}
            override fun onMapLongTap(map: Map, point: Point) {
                if (currentUiState.isLoggedIn) {
                    showAddStationDialog = point
                } else {
                    scope.launch { snackbarHostState.showSnackbar("Войдите для добавления АЗС") }
                }
            }
        }
    }

    val currentOnStationClick by rememberUpdatedState { station: Station ->
        viewModel.selectStation(station)
    }

    val tapListener = remember {
        MapObjectTapListener { mapObject, _ ->
            val clickedStation = mapObject.userData as? Station
            if (clickedStation != null) {
                currentOnStationClick(clickedStation)
                true
            } else {
                false
            }
        }
    }

    val clusterListener = remember {
        ClusterListener { cluster ->
            val bitmap = createClusterBitmap(context, cluster.size)
            cluster.appearance.setIcon(ImageProvider.fromBitmap(bitmap))
            cluster.addClusterTapListener { clickedCluster ->
                // Zoom in on cluster click
                mapView?.mapWindow?.map?.let { map ->
                    val currentZoom = map.cameraPosition.zoom
                    val targetZoom = (currentZoom + 1.5f).coerceAtMost(18f)
                    map.move(
                        CameraPosition(
                            clickedCluster.appearance.geometry,
                            targetZoom,
                            0f,
                            0f
                        ),
                        Animation(Animation.Type.SMOOTH, 0.4f),
                        null
                    )
                }
                true
            }
        }
    }

    val userLocationObjectListener = remember {
        object : UserLocationObjectListener {
            override fun onObjectAdded(view: UserLocationView) {
                view.pin.setIcon(ImageProvider.fromBitmap(createUserLocationPinBitmap(context)))
                view.arrow.setIcon(ImageProvider.fromBitmap(createUserLocationArrowBitmap(context)))
                view.accuracyCircle.fillColor = android.graphics.Color.parseColor("#26007AFF") // 15% opacity blue
            }
            override fun onObjectRemoved(view: UserLocationView) {}
            override fun onObjectUpdated(view: UserLocationView, event: ObjectEvent) {}
        }
    }

    // Start/Stop MapKit
    DisposableEffect(mapView) {
        if (mapView != null) {
            MapKitFactory.getInstance().onStart()
            mapView!!.onStart()
        }
        onDispose {
            if (mapView != null) {
                mapView!!.onStop()
                MapKitFactory.getInstance().onStop()
            }
        }
    }

    // Show bottom sheet for selected station
    if (uiState.selectedStation != null) {
        StationInfoSheet(
            station = uiState.selectedStation!!,
            distance = viewModel.getDistanceToStation(uiState.selectedStation!!),
            isLoggedIn = uiState.isLoggedIn,
            onEditClick = {
                if (uiState.isLoggedIn) {
                    onNavigateToEdit(uiState.selectedStation!!.id)
                } else {
                    onNavigateToLogin()
                }
            },
            onRouteClick = {
                openNavigation(context, uiState.selectedStation!!)
            },
            onDeleteClick = {
                showDeleteStationDialog = uiState.selectedStation
                viewModel.selectStation(null)
            },
            onDismiss = { viewModel.selectStation(null) }
        )
    }

    if (showDeleteStationDialog != null) {
        DeleteStationDialog(
            station = showDeleteStationDialog!!,
            onDismiss = { showDeleteStationDialog = null },
            onConfirm = { reason ->
                viewModel.deleteStation(showDeleteStationDialog!!.id, reason)
                showDeleteStationDialog = null
            }
        )
    }

    // Error snackbar
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Map
            AndroidView(
                factory = { ctx ->
                    MapView(ctx).apply {
                        mapWindow.map.move(
                            CameraPosition(
                                Point(LocationHelper.DEFAULT_LAT, LocationHelper.DEFAULT_LNG),
                                LocationHelper.DEFAULT_ZOOM.toFloat(),
                                0.0f,
                                0.0f
                            )
                        )

                        mapObjects = mapWindow.map.mapObjects.addCollection()

                        // My location layer
                        if (locationPermissions.allPermissionsGranted) {
                            userLocationLayer = MapKitFactory.getInstance().createUserLocationLayer(mapWindow).apply {
                                isVisible = true
                                isHeadingEnabled = true
                                setObjectListener(userLocationObjectListener)
                            }
                        }

                        // Long press listener
                        mapWindow.map.addInputListener(inputListener)

                        mapView = this
                    }
                },
                update = { map ->
                    // Update markers
                    mapObjects?.let { collection ->
                        updateMarkers(map.context, collection, uiState.filteredStations, uiState.filterState, tapListener, clusterListener)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            var showBottomSheet by remember { mutableStateOf(false) }

            if (showBottomSheet) {
                com.crimeafuel.app.presentation.filter.FilterBottomSheet(
                    filterState = uiState.filterState,
                    onToggleRegion = viewModel::toggleRegionFilter,
                    onTogglePayment = viewModel::togglePaymentFilter,
                    onClearFilters = viewModel::clearFilters,
                    onDismiss = { showBottomSheet = false }
                )
            }
            
            val clipboardManager = LocalClipboardManager.current

            if (uiState.importedStation != null && uiState.importedStation!!.lat != null && uiState.importedStation!!.lng != null) {
                AddStationDialog(
                    initialNetwork = uiState.importedStation!!.name,
                    initialAddress = uiState.importedStation!!.address,
                    point = Point(uiState.importedStation!!.lat!!, uiState.importedStation!!.lng!!),
                    onDismiss = { viewModel.clearImportedStation() },
                    onAdd = { network, address ->
                        viewModel.addUserStation(
                            network = network,
                            address = address,
                            latitude = uiState.importedStation!!.lat!!,
                            longitude = uiState.importedStation!!.lng!!
                        )
                        viewModel.clearImportedStation()
                    },
                    onPasteFromClipboard = {
                        clipboardManager.getText()?.text?.let { text ->
                            viewModel.processSharedText(text, uiState.importedStation!!.lat, uiState.importedStation!!.lng)
                        }
                    }
                )
            } else if (showAddStationDialog != null) {
                AddStationDialog(
                    point = showAddStationDialog!!,
                    onDismiss = { showAddStationDialog = null },
                    onAdd = { network, address ->
                        viewModel.addUserStation(
                            network = network,
                            address = address,
                            latitude = showAddStationDialog!!.latitude,
                            longitude = showAddStationDialog!!.longitude
                        )
                        showAddStationDialog = null
                    },
                    onPasteFromClipboard = {
                        clipboardManager.getText()?.text?.let { text ->
                            viewModel.processSharedText(text, showAddStationDialog!!.latitude, showAddStationDialog!!.longitude)
                            showAddStationDialog = null
                        }
                    }
                )
            }

            // Top overlay (Floating Top Bar + Fuel Chips)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
            ) {
                // Floating Top Bar
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    shadowElevation = 6.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Filters Button
                        IconButton(onClick = { showBottomSheet = true }) {
                            Icon(
                                imageVector = Icons.Default.FilterAlt,
                                contentDescription = "Фильтры",
                                tint = if (uiState.filterState.regions.isNotEmpty() || uiState.filterState.paymentMethods.isNotEmpty()) 
                                    MaterialTheme.colorScheme.primary 
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Title/Hint inside the search bar style
                        Text(
                            text = "Поиск заправок",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                        )

                        // Refresh button
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp).padding(2.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            IconButton(onClick = { viewModel.refresh() }) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Обновить",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Profile button
                        IconButton(onClick = {
                            if (!uiState.isLoggedIn) {
                                onNavigateToLogin()
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Профиль",
                                tint = if (uiState.isLoggedIn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Settings button
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Настройки",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Scrollable Fuel Chips Row
                androidx.compose.foundation.lazy.LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(com.crimeafuel.app.domain.model.FuelType.entries.toTypedArray()) { fuelType ->
                        com.crimeafuel.app.presentation.components.FuelChip(
                            fuelType = fuelType,
                            isSelected = fuelType in uiState.filterState.fuelTypes,
                            onClick = { viewModel.toggleFuelTypeFilter(fuelType) }
                        )
                    }
                }
            }

            // My location FAB
            FloatingActionButton(
                onClick = {
                    scope.launch {
                        val locationHelper = LocationHelper(context)
                        val location = locationHelper.getCurrentLocation()
                        location?.let { loc ->
                            viewModel.setUserLocation(loc)
                            mapView?.mapWindow?.map?.move(
                                CameraPosition(
                                    Point(loc.latitude, loc.longitude),
                                    14.0f, 0.0f, 0.0f
                                ),
                                Animation(Animation.Type.SMOOTH, 1f),
                                null
                            )
                        }
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = "Моё местоположение")
            }

            // Station counter
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp),
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                shadowElevation = 2.dp
            ) {
                Text(
                    text = "АЗС: ${uiState.filteredStations.size}",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

private fun updateMarkers(
    context: Context,
    collection: MapObjectCollection,
    stations: List<Station>,
    filterState: FilterState,
    tapListener: MapObjectTapListener,
    clusterListener: ClusterListener
) {
    collection.clear()
    
    if (stations.isEmpty()) return
    
    val clusterizedCollection = collection.addClusterizedPlacemarkCollection(clusterListener)

    stations.forEach { station ->
        // Color based on availability
        val availability = station.bestAvailability(
            if (filterState.fuelTypes.isEmpty()) null else filterState.fuelTypes
        )
        val color = if (station.isUserAdded) {
            android.graphics.Color.parseColor("#9C27B0") // Purple for user added
        } else {
            when (availability) {
                Availability.FREE_SALE -> android.graphics.Color.parseColor("#4CAF50")
                Availability.CARDS_ONLY -> android.graphics.Color.parseColor("#FFC107")
                Availability.NOT_AVAILABLE -> android.graphics.Color.parseColor("#F44336")
                Availability.UNKNOWN -> android.graphics.Color.parseColor("#9E9E9E")
            }
        }

        val bitmap = createColoredMarkerBitmap(context, color)
        
        val placemark = clusterizedCollection.addPlacemark(Point(station.latitude, station.longitude))
        placemark.setIcon(ImageProvider.fromBitmap(bitmap))
        placemark.userData = station
        
        placemark.addTapListener(tapListener)
    }
    
    clusterizedCollection.clusterPlacemarks(60.0, 15)
}

private fun createColoredMarkerBitmap(context: Context, color: Int): Bitmap {
    val density = context.resources.displayMetrics.density
    
    // Size of the pin: 36dp width, 48dp height
    val widthPx = (36 * density).toInt()
    val heightPx = (48 * density).toInt()
    
    val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    
    // Draw teardrop pin path
    val pinPath = Path().apply {
        val centerX = widthPx / 2f
        val radius = widthPx / 2f
        arcTo(
            centerX - radius, 0f, centerX + radius, radius * 2,
            -225f, 270f, true
        )
        lineTo(centerX, heightPx.toFloat())
        close()
    }
    
    val paintBg = Paint().apply {
        this.color = color
        isAntiAlias = true
        style = Paint.Style.FILL
    }
    val paintOutline = Paint().apply {
        this.color = android.graphics.Color.WHITE
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 2f * density
    }
    
    canvas.drawPath(pinPath, paintBg)
    canvas.drawPath(pinPath, paintOutline)
    
    // Draw fuel pump inside in white
    val paintIcon = Paint().apply {
        this.color = android.graphics.Color.WHITE
        isAntiAlias = true
        style = Paint.Style.FILL
    }
    
    val centerX = widthPx / 2f
    val centerY = widthPx / 2f
    
    // Pump dimensions
    val pumpW = 10f * density
    val pumpH = 14f * density
    val pumpL = centerX - pumpW / 2
    val pumpR = centerX + pumpW / 2
    val pumpT = centerY - pumpH / 2
    val pumpB = centerY + pumpH / 2
    
    // Pump body
    canvas.drawRoundRect(pumpL, pumpT, pumpR, pumpB, 1f * density, 1f * density, paintIcon)
    
    // Display screen window (color is status color)
    val paintScreen = Paint().apply {
        this.color = color
        isAntiAlias = true
        style = Paint.Style.FILL
    }
    val screenW = 6f * density
    val screenH = 3f * density
    val screenL = centerX - screenW / 2
    val screenT = pumpT + 2f * density
    canvas.drawRect(screenL, screenT, screenL + screenW, screenT + screenH, paintScreen)
    
    // Side hose
    val paintHose = Paint().apply {
        this.color = android.graphics.Color.WHITE
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 1.5f * density
        strokeCap = Paint.Cap.ROUND
    }
    
    val hosePath = Path().apply {
        moveTo(pumpR, pumpT + 4f * density)
        cubicTo(
            pumpR + 4f * density, pumpT + 4f * density,
            pumpR + 4f * density, pumpB - 2f * density,
            pumpR + 2f * density, pumpB - 2f * density
        )
        lineTo(pumpR + 2f * density, pumpT + 6f * density)
    }
    canvas.drawPath(hosePath, paintHose)
    
    return bitmap
}

private fun createClusterBitmap(context: Context, size: Int): Bitmap {
    val sizeDp = 40
    val density = context.resources.displayMetrics.density
    val sizePx = (sizeDp * density).toInt()
    
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    
    // Draw background circle with border
    val paintBg = Paint().apply {
        color = android.graphics.Color.parseColor("#1A73E8") // Yandex blue
        isAntiAlias = true
        style = Paint.Style.FILL
    }
    val paintBorder = Paint().apply {
        color = android.graphics.Color.WHITE
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 3 * density
    }
    
    val radius = sizePx / 2f
    canvas.drawCircle(radius, radius, radius - 4 * density, paintBg)
    canvas.drawCircle(radius, radius, radius - 4 * density, paintBorder)
    
    // Draw text
    val paintText = Paint().apply {
        color = android.graphics.Color.WHITE
        isAntiAlias = true
        textSize = 14 * density
        textAlign = Paint.Align.CENTER
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
    }
    
    val textY = radius - (paintText.descent() + paintText.ascent()) / 2
    canvas.drawText(size.toString(), radius, textY, paintText)
    
    return bitmap
}

private fun createUserLocationPinBitmap(context: Context): Bitmap {
    val sizeDp = 18
    val density = context.resources.displayMetrics.density
    val sizePx = (sizeDp * density).toInt()
    
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    
    val paintBg = Paint().apply {
        color = android.graphics.Color.parseColor("#007AFF")
        isAntiAlias = true
        style = Paint.Style.FILL
    }
    val paintBorder = Paint().apply {
        color = android.graphics.Color.WHITE
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 2 * density
    }
    
    val radius = sizePx / 2f
    canvas.drawCircle(radius, radius, radius - 2 * density, paintBg)
    canvas.drawCircle(radius, radius, radius - 2 * density, paintBorder)
    
    return bitmap
}

private fun createUserLocationArrowBitmap(context: Context): Bitmap {
    val sizeDp = 24
    val density = context.resources.displayMetrics.density
    val sizePx = (sizeDp * density).toInt()
    
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    
    // Draw Yandex-style directional navigation arrow
    val paint = Paint().apply {
        color = android.graphics.Color.parseColor("#007AFF")
        isAntiAlias = true
        style = Paint.Style.FILL
    }
    
    val path = Path().apply {
        moveTo(sizePx / 2f, 2 * density) // tip
        lineTo(sizePx - 4 * density, sizePx - 4 * density) // bottom right
        lineTo(sizePx / 2f, sizePx - 8 * density) // center indent
        lineTo(4 * density, sizePx - 4 * density) // bottom left
        close()
    }
    
    val paintShadow = Paint().apply {
        color = android.graphics.Color.parseColor("#40000000")
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 2 * density
    }
    canvas.drawPath(path, paintShadow)
    canvas.drawPath(path, paint)
    
    return bitmap
}

private fun openNavigation(context: Context, station: Station) {
    // Try Yandex Navigator first, fall back to Google Maps
    val yandexUri = Uri.parse(
        "yandexnavi://build_route_on_map?lat_to=${station.latitude}&lon_to=${station.longitude}"
    )
    val yandexIntent = Intent(Intent.ACTION_VIEW, yandexUri).apply {
        setPackage("ru.yandex.yandexnavi")
    }

    if (yandexIntent.resolveActivity(context.packageManager) != null) {
        context.startActivity(yandexIntent)
    } else {
        // Fall back to Google Maps
        val googleUri = Uri.parse(
            "google.navigation:q=${station.latitude},${station.longitude}"
        )
        val googleIntent = Intent(Intent.ACTION_VIEW, googleUri).apply {
            setPackage("com.google.android.apps.maps")
        }

        if (googleIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(googleIntent)
        } else {
            // Generic map intent
            val genericUri = Uri.parse(
                "geo:${station.latitude},${station.longitude}?q=${station.latitude},${station.longitude}(${station.network} №${station.number})"
            )
            context.startActivity(Intent(Intent.ACTION_VIEW, genericUri))
        }
    }
}

@Composable
fun AddStationDialog(
    initialNetwork: String = "",
    initialAddress: String = "",
    point: Point,
    onDismiss: () -> Unit,
    onAdd: (network: String, address: String) -> Unit,
    onPasteFromClipboard: () -> Unit
) {
    var network by remember(initialNetwork) { mutableStateOf(initialNetwork) }
    var address by remember(initialAddress) { mutableStateOf(initialAddress) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Добавить заправку") },
        text = {
            Column {
                OutlinedButton(
                    onClick = onPasteFromClipboard,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Вставить из Яндекс.Карт (Буфер)")
                }
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = network,
                    onValueChange = { network = it },
                    label = { Text("Название сети") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Адрес") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onAdd(network, address) },
                enabled = network.isNotBlank() && address.isNotBlank()
            ) {
                Text("Добавить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

@Composable
fun DeleteStationDialog(
    station: Station,
    onDismiss: () -> Unit,
    onConfirm: (reason: String) -> Unit
) {
    val options = listOf(
        "АЗС закрылась / прекратила работу",
        "Дубликат существующей АЗС",
        "Неверное местоположение / координаты",
        "Другое (укажите причину)"
    )
    var selectedOption by remember { mutableStateOf(options[0]) }
    var customReason by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Удалить АЗС №${station.number}?") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Выберите причину удаления АЗС ${station.network} по адресу: ${station.address}",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                options.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .selectable(
                                selected = (selectedOption == option),
                                onClick = { selectedOption = option }
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (selectedOption == option),
                            onClick = { selectedOption = option }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = option,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                
                if (selectedOption == options[3]) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = customReason,
                        onValueChange = { customReason = it },
                        label = { Text("Причина") },
                        placeholder = { Text("Например: АЗС сменила бренд") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val finalReason = if (selectedOption == options[3]) {
                        customReason.ifBlank { "Другая причина" }
                    } else {
                        selectedOption
                    }
                    onConfirm(finalReason)
                },
                enabled = selectedOption != options[3] || customReason.isNotBlank()
            ) {
                Text("Удалить", color = FuelRed)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}
