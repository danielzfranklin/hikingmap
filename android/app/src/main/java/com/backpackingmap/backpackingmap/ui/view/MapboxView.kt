package com.backpackingmap.backpackingmap.ui.view

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import com.backpackingmap.backpackingmap.R
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.MapboxMapOptions
import com.mapbox.mapboxsdk.maps.Style
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.CoroutineContext

@Composable
fun MapboxView(state: MapboxState, modifier: Modifier = Modifier) {
    val token = stringResource(R.string.public_token_mapbox)

    AndroidView(
        factory = { context ->
            Mapbox.getInstance(context, token)

            val options = MapboxMapOptions.createFromAttributes(context, null)
            options.camera(state.initialPosition)

            MapView(context, options).apply {
                getMapAsync {
                    it.setStyle(state.initialStyle)
                    state.registerMap(it)
                }
            }
        },
        modifier = modifier,
    )
}

@Preview
@Composable
fun MapboxViewPreview() {
    val initialPosition = CameraPosition.Builder()
        .target(LatLng(43.7383, 7.4094))
        .zoom(5.0)
        .build()

    val state = rememberMapboxState(initialPosition, Style.OUTDOORS)
    LaunchedEffect(state) {
        val update = CameraUpdateFactory.zoomBy(10.0)
        state.awaitMap().animateCamera(update, 10_000)
    }

    MapboxView(state)
}

@Composable
fun rememberMapboxState(initialPosition: CameraPosition, initialStyle: String): MapboxState {
    val coroutineContext = rememberCoroutineScope().coroutineContext
    return remember {
        MapboxState(initialPosition, initialStyle, coroutineContext)
    }
}

class MapboxState constructor(internal val initialPosition: CameraPosition, internal val initialStyle: String, override val coroutineContext: CoroutineContext):
    CoroutineScope {

    private val _map = CompletableDeferred<MapboxMap>()

    suspend fun awaitMap() =
        _map.await()

    internal fun registerMap(map: MapboxMap) {
        if (!_map.complete(map)) {
            throw IllegalStateException("MapState already registered to a MapboxMap")
        }
    }
}