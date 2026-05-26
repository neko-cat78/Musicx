package com.flowtune.music.ui.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.text.format.Formatter
import android.widget.Toast
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.snapping.SnapLayoutInfoProvider
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridItemInfo
import androidx.compose.foundation.lazy.grid.LazyGridLayoutInfo
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.TopAppBarState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import com.flowtune.innertube.YouTube
import com.flowtune.innertube.models.MediaInfo
import com.flowtune.music.LocalDatabase
import com.flowtune.music.LocalPlayerConnection
import com.flowtune.music.R
import com.flowtune.music.constants.DarkModeKey
import com.flowtune.music.constants.PureBlackKey
import com.flowtune.music.db.entities.FormatEntity
import com.flowtune.music.db.entities.Song
import com.flowtune.music.ui.component.LocalMenuState
import com.flowtune.music.ui.component.MenuState
import com.flowtune.music.ui.component.shimmer.ShimmerHost
import com.flowtune.music.ui.component.shimmer.TextPlaceholder
import com.flowtune.music.ui.screens.Screens
import com.flowtune.music.ui.screens.settings.DarkMode
import com.flowtune.music.utils.rememberEnumPreference
import com.flowtune.music.utils.rememberPreference
import java.text.DecimalFormat
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.absoluteValue

class ItemWrapper<T>(
    val item: T,
) {
    private val _isSelected = mutableStateOf(true)

    var isSelected: Boolean
        get() = _isSelected.value
        set(value) {
            _isSelected.value = value
        }
}

object KeyUtils {
    private val counter = AtomicLong(0)

    fun generateUniqueKey(baseId: String, prefix: String = ""): String {
        val uniqueId = counter.incrementAndGet()
        return if (prefix.isNotEmpty()) {
            "${prefix}_${baseId}_$uniqueId"
        } else {
            "${baseId}_$uniqueId"
        }
    }

    fun generateIndexedKey(baseId: String, index: Int, prefix: String = ""): String {
        val uniqueId = counter.incrementAndGet()
        return if (prefix.isNotEmpty()) {
            "${prefix}_${baseId}_${index}_$uniqueId"
        } else {
            "${baseId}_${index}_$uniqueId"
        }
    }

    fun generateTimestampKey(baseId: String, prefix: String = ""): String {
        val timestamp = System.currentTimeMillis()
        val uniqueId = counter.incrementAndGet()
        return if (prefix.isNotEmpty()) {
            "${prefix}_${baseId}_${timestamp}_$uniqueId"
        } else {
            "${baseId}_${timestamp}_$uniqueId"
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun appBarScrollBehavior(
    state: TopAppBarState = rememberTopAppBarState(),
    canScroll: () -> Boolean = { true },
    snapAnimationSpec: AnimationSpec<Float>? = spring(stiffness = Spring.StiffnessMediumLow),
    flingAnimationSpec: DecayAnimationSpec<Float>? = rememberSplineBasedDecay(),
): TopAppBarScrollBehavior =
    AppBarScrollBehavior(
        state = state,
        snapAnimationSpec = snapAnimationSpec,
        flingAnimationSpec = flingAnimationSpec,
        canScroll = canScroll,
    )

@ExperimentalMaterial3Api
class AppBarScrollBehavior(
    override val state: TopAppBarState,
    override val snapAnimationSpec: AnimationSpec<Float>?,
    override val flingAnimationSpec: DecayAnimationSpec<Float>?,
    val canScroll: () -> Boolean = { true },
) : TopAppBarScrollBehavior {
    override val isPinned: Boolean = true
    override var nestedScrollConnection =
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                if (!canScroll()) return Offset.Zero
                state.contentOffset += consumed.y
                if (state.heightOffset == 0f || state.heightOffset == state.heightOffsetLimit) {
                    if (consumed.y == 0f && available.y > 0f) {

                        state.contentOffset = 0f
                    }
                }
                state.heightOffset += consumed.y
                return Offset.Zero
            }
        }
}

@OptIn(ExperimentalMaterial3Api::class)
suspend fun TopAppBarState.resetHeightOffset() {
    if (heightOffset != 0f) {
        animate(
            initialValue = heightOffset,
            targetValue = 0f,
        ) { value, _ ->
            heightOffset = value
        }
    }
}

private val LazyGridLayoutInfo.singleAxisViewportSize: Int
    get() = if (orientation == Orientation.Vertical) viewportSize.height else viewportSize.width

fun calculateDistanceToDesiredSnapPosition(
    layoutInfo: LazyGridLayoutInfo,
    item: LazyGridItemInfo,
    positionInLayout: (layoutSize: Float, itemSize: Float) -> Float,
): Float {
    val containerSize =
        layoutInfo.singleAxisViewportSize - layoutInfo.beforeContentPadding - layoutInfo.afterContentPadding

    val desiredDistance = positionInLayout(containerSize.toFloat(), item.size.width.toFloat())
    val itemCurrentPosition = item.offset.x.toFloat()

    return itemCurrentPosition - desiredDistance
}

@ExperimentalFoundationApi
fun SnapLayoutInfoProvider(
    lazyGridState: LazyGridState,
    positionInLayout: (layoutSize: Float, itemSize: Float) -> Float = { layoutSize, itemSize ->
        (layoutSize / 2f - itemSize / 2f)
    },
): SnapLayoutInfoProvider = object : SnapLayoutInfoProvider {
    private val layoutInfo: LazyGridLayoutInfo
        get() = lazyGridState.layoutInfo

    override fun calculateApproachOffset(velocity: Float, decayOffset: Float): Float = 0f

    override fun calculateSnapOffset(velocity: Float): Float {
        val bounds = calculateSnappingOffsetBounds()
        return when {
            velocity < 0 -> bounds.start
            velocity > 0 -> bounds.endInclusive
            else -> 0f
        }
    }

    fun calculateSnappingOffsetBounds(): ClosedFloatingPointRange<Float> {
        var lowerBoundOffset = Float.NEGATIVE_INFINITY
        var upperBoundOffset = Float.POSITIVE_INFINITY

        layoutInfo.visibleItemsInfo.forEach { item ->
            val offset = calculateDistanceToDesiredSnapPosition(layoutInfo, item, positionInLayout)

            if (offset <= 0 && offset > lowerBoundOffset) {
                lowerBoundOffset = offset
            }

            if (offset >= 0 && offset < upperBoundOffset) {
                upperBoundOffset = offset
            }
        }

        return lowerBoundOffset.rangeTo(upperBoundOffset)
    }
}

fun CornerBasedShape.top(): CornerBasedShape =
    copy(bottomStart = CornerSize(0.dp), bottomEnd = CornerSize(0.dp))

fun Modifier.fadingEdge(
    left: Dp? = null,
    top: Dp? = null,
    right: Dp? = null,
    bottom: Dp? = null,
) = graphicsLayer(alpha = 0.99f)
    .drawWithContent {
        drawContent()
        if (top != null) {
            drawRect(
                brush =
                Brush.verticalGradient(
                    colors =
                    listOf(
                        Color.Transparent,
                        Color.Black,
                    ),
                    startY = 0f,
                    endY = top.toPx(),
                ),
                blendMode = BlendMode.DstIn,
            )
        }
        if (bottom != null) {
            drawRect(
                brush =
                Brush.verticalGradient(
                    colors =
                    listOf(
                        Color.Black,
                        Color.Transparent,
                    ),
                    startY = size.height - bottom.toPx(),
                    endY = size.height,
                ),
                blendMode = BlendMode.DstIn,
            )
        }
        if (left != null) {
            drawRect(
                brush =
                Brush.horizontalGradient(
                    colors =
                    listOf(
                        Color.Black,
                        Color.Transparent,
                    ),
                    startX = 0f,
                    endX = left.toPx(),
                ),
                blendMode = BlendMode.DstIn,
            )
        }
        if (right != null) {
            drawRect(
                brush =
                Brush.horizontalGradient(
                    colors =
                    listOf(
                        Color.Transparent,
                        Color.Black,
                    ),
                    startX = size.width - right.toPx(),
                    endX = size.width,
                ),
                blendMode = BlendMode.DstIn,
            )
        }
    }

fun Modifier.fadingEdge(
    horizontal: Dp? = null,
    vertical: Dp? = null,
) = fadingEdge(
    left = horizontal,
    right = horizontal,
    top = vertical,
    bottom = vertical,
)

fun NavController.backToMain() {
    val mainRoutes = Screens.MainScreens.map { it.route }

    while (previousBackStackEntry != null &&
        currentBackStackEntry?.destination?.route !in mainRoutes
    ) {
        popBackStack()
    }
}

fun String.resize(
    width: Int? = null,
    height: Int? = null,
): String {
    if (width == null && height == null) return this
    "https://lh3\\.googleusercontent\\.com/.*=w(\\d+)-h(\\d+).*".toRegex()
        .matchEntire(this)?.groupValues?.let { group ->
        val (W, H) = group.drop(1).map { it.toInt() }
        var w = width
        var h = height
        if (w != null && h == null) h = (w / W) * H
        if (w == null && h != null) w = (h / H) * W
        return "${split("=w")[0]}=w$w-h$h-p-l90-rj"
    }
    if (this matches "https://yt3\\.ggpht\\.com/.*=s(\\d+)".toRegex()) {
        return "$this-s${width ?: height}"
    }
    return this
}

@Composable
fun LazyListState.isScrollingUp(): Boolean {
    var previousIndex by remember(this) { mutableStateOf(firstVisibleItemIndex) }
    var previousScrollOffset by remember(this) { mutableStateOf(firstVisibleItemScrollOffset) }
    return remember(this) {
        derivedStateOf {
            if (previousIndex != firstVisibleItemIndex) {
                previousIndex > firstVisibleItemIndex
            } else {
                previousScrollOffset >= firstVisibleItemScrollOffset
            }.also {
                previousIndex = firstVisibleItemIndex
                previousScrollOffset = firstVisibleItemScrollOffset
            }
        }
    }.value
}

@Composable
fun LazyGridState.isScrollingUp(): Boolean {
    var previousIndex by remember(this) { mutableStateOf(firstVisibleItemIndex) }
    var previousScrollOffset by remember(this) { mutableStateOf(firstVisibleItemScrollOffset) }
    return remember(this) {
        derivedStateOf {
            if (previousIndex != firstVisibleItemIndex) {
                previousIndex > firstVisibleItemIndex
            } else {
                previousScrollOffset >= firstVisibleItemScrollOffset
            }.also {
                previousIndex = firstVisibleItemIndex
                previousScrollOffset = firstVisibleItemScrollOffset
            }
        }
    }.value
}

@Composable
fun ScrollState.isScrollingUp(): Boolean {
    var previousScrollOffset by remember(this) { mutableStateOf(value) }
    return remember(this) {
        derivedStateOf {
            (previousScrollOffset >= value).also {
                previousScrollOffset = value
            }
        }
    }.value
}

fun formatFileSize(sizeBytes: Long): String {
    val prefix = if (sizeBytes < 0) "-" else ""
    var result: Long = sizeBytes.absoluteValue
    var suffix = "B"
    if (result > 900) {
        suffix = "KB"
        result /= 1024
    }
    if (result > 900) {
        suffix = "MB"
        result /= 1024
    }
    if (result > 900) {
        suffix = "GB"
        result /= 1024
    }
    if (result > 900) {
        suffix = "TB"
        result /= 1024
    }
    if (result > 900) {
        suffix = "PB"
        result /= 1024
    }
    return "$prefix$result $suffix"
}

fun numberFormatter(n: Int) =
    DecimalFormat("#,###")
        .format(n)
        .replace(",", ".")

@Composable
fun ShowMediaInfo(videoId: String) {
    if (videoId.isBlank() || videoId.isEmpty()) return

    val windowInsets = WindowInsets.systemBars

    var info by remember {
        mutableStateOf<MediaInfo?>(null)
    }

    val database = LocalDatabase.current
    var song by remember { mutableStateOf<Song?>(null) }

    var currentFormat by remember { mutableStateOf<FormatEntity?>(null) }

    val playerConnection = LocalPlayerConnection.current
    val context = LocalContext.current
    val clipboardManager = LocalClipboard.current

    LaunchedEffect(Unit, videoId) {
        info = YouTube.getMediaInfo(videoId).getOrNull()
    }
    LaunchedEffect(Unit, videoId) {
        database.song(videoId).collect {
            song = it
        }
    }
    LaunchedEffect(Unit, videoId) {
        database.format(videoId).collect {
            currentFormat = it
        }
    }

    LazyColumn(
        state = rememberLazyListState(),
        modifier = Modifier
            .padding(
                windowInsets
                    .asPaddingValues()
            )
            .padding(horizontal = 16.dp)
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (song != null) {
            item(contentType = "TitleDetails") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        painter = painterResource(R.drawable.info),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = stringResource(R.string.details),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center
                    )
                }
            }

            item(contentType = "MediaDetails") {
                Column {
                    val baseList = listOf(
                        stringResource(R.string.song_title) to song?.title,
                        stringResource(R.string.song_artists) to song?.artists?.joinToString { it.name },
                        stringResource(R.string.media_id) to song?.id
                    )
                    val extendedList = baseList + if (currentFormat != null) {
                        listOf(
                            "Itag" to currentFormat?.itag?.toString(),
                            stringResource(R.string.mime_type) to currentFormat?.mimeType,
                            stringResource(R.string.codecs) to currentFormat?.codecs,
                            stringResource(R.string.bitrate) to currentFormat?.bitrate?.let { "${it / 1000} Kbps" },
                            stringResource(R.string.sample_rate) to currentFormat?.sampleRate?.let { "$it Hz" },
                            stringResource(R.string.loudness) to currentFormat?.loudnessDb?.let { "$it dB" },
                            stringResource(R.string.volume) to if (playerConnection != null)
                                "${(playerConnection.player.volume * 100).toInt()}%" else null,
                            stringResource(R.string.file_size) to
                                    currentFormat?.contentLength?.let {
                                        Formatter.formatShortFileSize(
                                            context,
                                            it
                                        )
                                    }
                        )
                    } else {
                        emptyList()
                    }

                    extendedList.forEach { (label, text) ->
                        val displayText = text ?: stringResource(R.string.unknown)
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = displayText,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier =
                            Modifier
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = {
                                        val cm = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        cm.setPrimaryClip(ClipData.newPlainText("text", displayText))
                                        Toast.makeText(
                                            context,
                                            R.string.copied,
                                            Toast.LENGTH_SHORT
                                        )
                                            .show()
                                    },
                                )
                                .padding(horizontal = 16.dp),
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }

        item(contentType = "TitleMediaInfo") {
            Text(
                text = stringResource(R.string.information),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (info != null) {
            if (song == null)
                item(contentType = "MediaTitle") {
                    Column(
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.Top,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                    ) {
                        Text(
                            text = "" + info?.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            textAlign = TextAlign.Start
                        )
                    }
                }

            item(contentType = "MediaAuthor") {
                Column(
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.Top,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.artists),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Start
                    )
                    BasicText(
                        text = "" + info?.author,
                        style = MaterialTheme.typography.titleMedium.copy(color = MaterialTheme.colorScheme.onBackground),
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 16.dp)
                    )
                }
            }
            item(contentType = "MediaDescription") {
                Column(
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.Top,
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.description),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Start
                    )
                    BasicText(
                        text = info?.description ?: "",
                        style = MaterialTheme.typography.titleMedium.copy(color = MaterialTheme.colorScheme.onBackground),
                        modifier = Modifier
                            .padding(all = 16.dp)
                    )
                }
            }
            item(contentType = "MediaNumbers") {
                Column(
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.Top,
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.numbers),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Start
                    )
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(all = 16.dp)
                    ) {
                        Column {
                            BasicText(
                                text = stringResource(R.string.subscribers),
                                style = MaterialTheme.typography.titleSmall.copy(color = MaterialTheme.colorScheme.onBackground),
                            )
                            BasicText(
                                text = info?.subscribers ?: "",
                                style = MaterialTheme.typography.titleSmall.copy(color = MaterialTheme.colorScheme.onBackground),
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                        Column {
                            BasicText(
                                text = stringResource(R.string.views),
                                style = MaterialTheme.typography.titleSmall.copy(color = MaterialTheme.colorScheme.onBackground),
                                modifier = Modifier
                            )
                            BasicText(
                                text = info?.viewCount?.let(::numberFormatter).orEmpty(),
                                style = MaterialTheme.typography.titleSmall.copy(color = MaterialTheme.colorScheme.onBackground),
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                        Column {
                            BasicText(
                                text = stringResource(R.string.likes),
                                style = MaterialTheme.typography.titleSmall.copy(color = MaterialTheme.colorScheme.onBackground),
                                modifier = Modifier
                            )
                            BasicText(
                                text = info?.like?.let(::numberFormatter).orEmpty(),
                                style = MaterialTheme.typography.titleSmall.copy(color = MaterialTheme.colorScheme.onBackground),
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                        Column {
                            BasicText(
                                text = stringResource(R.string.dislikes),
                                style = MaterialTheme.typography.titleSmall.copy(color = MaterialTheme.colorScheme.onBackground),
                                modifier = Modifier
                            )
                            BasicText(
                                text = info?.dislike?.let(::numberFormatter).orEmpty(),
                                style = MaterialTheme.typography.titleSmall.copy(color = MaterialTheme.colorScheme.onBackground),
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }
            }
        } else {
            item(contentType = "MediaInfoLoader") {
                ShimmerHost {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(all = 16.dp)
                    ) {
                        TextPlaceholder()
                    }
                }
            }
        }
    }
}
