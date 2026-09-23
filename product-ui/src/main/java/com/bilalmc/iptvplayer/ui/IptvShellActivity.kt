package com.bilalmc.iptvplayer.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.tv.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import coil3.compose.AsyncImage
import tv.own.owntv.core.database.entity.ChannelEntity
import tv.own.owntv.core.database.entity.EpisodeEntity
import tv.own.owntv.core.database.entity.MovieEntity
import tv.own.owntv.core.database.entity.SeriesEntity
import tv.own.owntv.core.launcher.LauncherDeepLink

private const val OWN_TV_ACTIVITY = "tv.own.owntv.MainActivity"

class IptvShellActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent?.data != null) { openOwnTv(intent.data); finish(); return }
        setContent { IptvPlayerShell(::openOwnTv, ::playChannel, ::playMovie, ::playEpisode) }
    }
    private fun openOwnTv(data: android.net.Uri? = null) { startActivity(Intent().setClassName(packageName, OWN_TV_ACTIVITY).apply { this.data = data }) }
    private fun playChannel(channel: ChannelEntity) = openOwnTv(LauncherDeepLink.Live(channel.sourceId, channel.remoteId, channel.name, channel.id).toUri())
    private fun playMovie(movie: MovieEntity) = openOwnTv(LauncherDeepLink.Movie(movie.sourceId, movie.remoteId, movie.name, movie.id).toUri())
    private fun playEpisode(episode: EpisodeEntity, series: SeriesEntity) = openOwnTv(LauncherDeepLink.Episode(series.sourceId, series.remoteId, series.name, episode.remoteId, episode.seasonNumber, episode.episodeNumber, series.id, episode.id).toUri())
}

private data class HomeAction(val label: String, val icon: ImageVector, val onClick: () -> Unit)

@Composable
private fun IptvPlayerShell(
    onOpenPlayer: () -> Unit,
    onPlayChannel: (ChannelEntity) -> Unit,
    onPlayMovie: (MovieEntity) -> Unit,
    onPlayEpisode: (EpisodeEntity, SeriesEntity) -> Unit
) {
    val homeVm: ProductHomeViewModel = viewModel()
    val catalogVm: ProductCatalogViewModel = viewModel()
    val state by homeVm.state.collectAsStateWithLifecycle()
    var selected by rememberSaveable { mutableIntStateOf(0) }
    var seriesDetail by rememberSaveable { mutableStateOf(false) }
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }

    val goHome = { selected = 0; seriesDetail = false }
    val goLive = { selected = 1; seriesDetail = false }
    val goGuide = { selected = 2; seriesDetail = false }
    val goMovies = { selected = 3; seriesDetail = false }
    val goSeries = { selected = 4; seriesDetail = false }
    val goSearch = { selected = 5; seriesDetail = false }
    val goFavorites = { selected = 6; seriesDetail = false }
    val goSettings = { selected = 7; seriesDetail = false }

    LaunchedEffect(Unit) {
        while (true) {
            nowMs = System.currentTimeMillis()
            delay(30_000)
        }
    }

    BackHandler {
        when {
            seriesDetail -> seriesDetail = false
            selected != 0 -> selected = 0
            else -> finish()
        }
    }

    MaterialTheme {
        Box(
            Modifier.fillMaxSize().background(
                Brush.linearGradient(listOf(Color(0xFF063B83), Color(0xFF0759B8), Color(0xFF021F52)))
            )
        ) {
            Column(Modifier.fillMaxSize().padding(horizontal = 44.dp, vertical = 30.dp)) {
                XcTopBar(state.profileName, nowMs, goSearch, goSettings)
                Spacer(Modifier.height(28.dp))
                if (seriesDetail) {
                    ProductSeriesDetail(catalogVm, { seriesDetail = false }) { episode, series -> onPlayEpisode(episode, series) }
                } else {
                    when (selected) {
                        0 -> XcHome(state, goLive, goGuide, goMovies, goSeries, goFavorites, goSearch, goSettings, onPlayChannel)
                        1 -> LiveContent(state, goGuide, onPlayChannel)
                        2 -> ProductEpgScreen(catalogVm, goHome, onPlayChannel)
                        3 -> MovieCatalog(homeVm, onPlayMovie)
                        4 -> SeriesCatalog(homeVm) { series -> catalogVm.selectSeries(series); seriesDetail = true }
                        5 -> ProductSearchScreen(catalogVm, goHome, onPlayChannel, onPlayMovie) { series -> catalogVm.selectSeries(series); seriesDetail = true }
                        6 -> FavoritesContent(state.favoriteChannels, onPlayChannel)
                        else -> ProductSettingsScreen(catalogVm, goHome)
                    }
                }
            }
        }
    }
}

@Composable
private fun XcTopBar(profileName: String, nowMs: Long, onSearch: () -> Unit, onSettings: () -> Unit) {
    val time = remember(nowMs) { SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(nowMs)) }
    val date = remember(nowMs) { SimpleDateFormat("EEE, dd MMM", Locale.getDefault()).format(Date(nowMs)).replaceFirstChar { it.uppercase() } }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.width(270.dp)) {
            Box(Modifier.size(58.dp).clip(RoundedCornerShape(29.dp)).background(Color(0xFF16B9E9)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.PlayArrow, "IPTV Player", tint = Color.White, modifier = Modifier.size(38.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text("IPTV", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("PLAYER", fontSize = 10.sp, letterSpacing = 2.sp, color = Color(0xFFB9E9FF))
            }
        }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(time, fontSize = 25.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            Text(date, fontSize = 12.sp, color = Color(0xFFB9D7F7))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            XcIconButton(Icons.Default.Search, "Search", onSearch)
            XcIconButton(Icons.Default.Refresh, "Refresh", {})
            XcIconButton(Icons.Default.Settings, "Settings", onSettings)
            Text(profileName.ifBlank { "Profile" }, fontSize = 13.sp, color = Color.White, modifier = Modifier.padding(start = 8.dp))
        }
    }
}

@Composable
private fun XcHome(
    state: ProductHomeState,
    goLive: () -> Unit,
    goGuide: () -> Unit,
    goMovies: () -> Unit,
    goSeries: () -> Unit,
    goFavorites: () -> Unit,
    goSearch: () -> Unit,
    goSettings: () -> Unit,
    onPlay: (ChannelEntity) -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            XcMainTile("LIVE TV", Icons.Default.Tv, "\${state.channelCount} channels", goLive)
            Spacer(Modifier.width(18.dp))
            XcMainTile("TV GUIDE", Icons.Default.List, "Now & next", goGuide)
            Spacer(Modifier.width(18.dp))
            XcMainTile("MOVIES", Icons.Default.Movie, "\${state.movieCount} titles", goMovies)
            Spacer(Modifier.width(18.dp))
            XcMainTile("SERIES", Icons.Default.VideoLibrary, "\${state.seriesCount} series", goSeries)
        }
        Spacer(Modifier.height(18.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            XcSmallAction(Icons.Default.Favorite, "FAVORITES", goFavorites)
            Spacer(Modifier.width(12.dp))
            XcSmallAction(Icons.Default.Search, "SEARCH", goSearch)
            Spacer(Modifier.width(12.dp))
            XcSmallAction(Icons.Default.Settings, "SETTINGS", goSettings)
            Spacer(Modifier.width(28.dp))
            Text(
                if (state.hasSources) "Connected · \${state.channelCount} Live · \${state.movieCount} Movies · \${state.seriesCount} Series" else "No playlist connected",
                color = Color(0xFFB9D7F7), fontSize = 13.sp
            )
        }
        if (state.channels.isNotEmpty()) {
            Spacer(Modifier.height(18.dp))
            Text("LIVE NOW", fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.6.sp, color = Color.White)
            Spacer(Modifier.height(8.dp))
            ChannelRow(state.channels.take(5), onPlay)
        }
    }
}

@Composable
private fun XcMainTile(title: String, icon: ImageVector, subtitle: String, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Box(
        Modifier.width(250.dp).height(205.dp).clip(RoundedCornerShape(4.dp))
            .background(Color(0xFF0A3975).copy(alpha = 0.72f))
            .border(if (focused) 4.dp else 2.dp, if (focused) Color.White else Color(0xFFD9F3FF), RoundedCornerShape(4.dp))
            .onFocusChanged { focused = it.isFocused }.focusable().clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, title, tint = Color.White, modifier = Modifier.size(62.dp))
            Spacer(Modifier.height(16.dp))
            Text(title, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            Spacer(Modifier.height(5.dp))
            Text(subtitle, fontSize = 12.sp, color = Color(0xFFB9D7F7))
        }
    }
}

@Composable
private fun XcSmallAction(icon: ImageVector, label: String, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Column(
        Modifier.width(100.dp).height(78.dp).clip(RoundedCornerShape(3.dp))
            .background(Color(0xFF082F68).copy(alpha = 0.82f))
            .border(2.dp, if (focused) Color.White else Color(0xFF6E9DD2), RoundedCornerShape(3.dp))
            .onFocusChanged { focused = it.isFocused }.focusable().clickable(onClick = onClick).padding(vertical = 9.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, label, tint = Color.White, modifier = Modifier.size(30.dp))
        Spacer(Modifier.height(4.dp))
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

@Composable
private fun XcIconButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Box(
        Modifier.size(48.dp).clip(RoundedCornerShape(4.dp))
            .border(2.dp, if (focused) Color.White else Color.Transparent, RoundedCornerShape(4.dp))
            .onFocusChanged { focused = it.isFocused }.focusable().clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, label, tint = Color.White, modifier = Modifier.size(28.dp))
    }
}

@Composable private fun LiveContent(state: ProductHomeState, goGuide: () -> Unit, onPlay: (ChannelEntity) -> Unit) { LazyColumn(verticalArrangement = Arrangement.spacedBy(18.dp)) { item { Text("Live TV", fontSize = 34.sp, fontWeight = FontWeight.Bold); Text("${state.channelCount} channels", color = Color(0xFF9CA3AF)) }; if (state.channels.isNotEmpty()) item { SectionTitle("Channels"); ChannelRow(state.channels, onPlay) }; item { Button(onClick = goGuide) { Text("Open TV Guide") } } } }
@Composable private fun MovieCatalog(vm: ProductHomeViewModel, onPlay: (MovieEntity) -> Unit) { val items = vm.movies.collectAsLazyPagingItems(); Column(Modifier.fillMaxSize()) { Text("Movies", fontSize = 34.sp, fontWeight = FontWeight.Bold); Text("Browse your movie library", color = Color(0xFF9CA3AF), modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)); LazyVerticalGrid(columns = GridCells.Fixed(5), contentPadding = PaddingValues(bottom = 32.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxSize().focusGroup()) { items(items.itemCount) { i -> items[i]?.let { MovieCard(it, onPlay) } } } } }
@Composable private fun SeriesCatalog(vm: ProductHomeViewModel, onOpen: (SeriesEntity) -> Unit) { val items = vm.series.collectAsLazyPagingItems(); Column(Modifier.fillMaxSize()) { Text("Series", fontSize = 34.sp, fontWeight = FontWeight.Bold); Text("Browse seasons and episodes", color = Color(0xFF9CA3AF), modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)); LazyVerticalGrid(columns = GridCells.Fixed(5), contentPadding = PaddingValues(bottom = 32.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxSize().focusGroup()) { items(items.itemCount) { i -> items[i]?.let { SeriesCard(it) { onOpen(it) } } } } } }
@Composable private fun MovieCard(movie: MovieEntity, onPlay: (MovieEntity) -> Unit) { Card(onClick = { onPlay(movie) }, modifier = Modifier.width(180.dp).height(250.dp)) { PosterCard(movie.posterUrl, movie.name, movie.year?.toString(), movie.rating?.let { "★ %.1f".format(it) }) } }
@Composable private fun SeriesCard(series: SeriesEntity, onOpen: () -> Unit) { Card(onClick = onOpen, modifier = Modifier.width(180.dp).height(250.dp)) { PosterCard(series.posterUrl, series.name, series.year?.toString(), series.rating?.let { "★ %.1f".format(it) }) } }
@Composable private fun PosterCard(url: String?, title: String, meta: String?, rating: String?) { Box(Modifier.fillMaxSize().background(Color(0xFF171A22))) { if (!url.isNullOrBlank()) AsyncImage(model = url, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop); Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xEE090B10))))); Column(Modifier.align(Alignment.BottomStart).padding(14.dp)) { Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 2); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { if (meta != null) Text(meta, fontSize = 11.sp, color = Color(0xFFB8BFCE)); if (rating != null) Text(rating, fontSize = 11.sp, color = Color(0xFFFFD166)) } } } }
@Composable private fun FavoritesContent(channels: List<ChannelEntity>, onPlay: (ChannelEntity) -> Unit) { Column(verticalArrangement = Arrangement.spacedBy(16.dp)) { Text("Favorites", fontSize = 34.sp, fontWeight = FontWeight.Bold); if (channels.isEmpty()) EmptyText("No live favorites yet.") else ChannelRow(channels, onPlay) } }
@Composable private fun SectionTitle(title: String) { Text(title, fontSize = 20.sp, fontWeight = FontWeight.SemiBold); Spacer(Modifier.height(10.dp)) }
@Composable private fun EmptyText(text: String) { Text(text, color = Color(0xFF9CA3AF), fontSize = 14.sp) }
@Composable private fun ChannelRow(channels: List<ChannelEntity>, onPlay: (ChannelEntity) -> Unit) { LazyRow(Modifier.focusGroup(), horizontalArrangement = Arrangement.spacedBy(16.dp)) { items(channels, key = { it.id }) { ChannelCard(it, onPlay) } } }
@Composable private fun ChannelCard(channel: ChannelEntity, onPlay: (ChannelEntity) -> Unit) { Card(onClick = { onPlay(channel) }, modifier = Modifier.width(300.dp).height(145.dp)) { Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0xFF263B6B), Color(0xFF11141C))))) { if (!channel.logoUrl.isNullOrBlank()) AsyncImage(model = channel.logoUrl, contentDescription = null, modifier = Modifier.fillMaxWidth().height(92.dp).padding(14.dp), contentScale = ContentScale.Fit); Column(Modifier.align(Alignment.BottomStart).padding(18.dp)) { Text(channel.number?.toString() ?: "LIVE", fontSize = 12.sp, color = Color(0xFFB8BFCE)); Text(channel.name, fontSize = 18.sp, fontWeight = FontWeight.SemiBold) } } } }
@Composable private fun ContentCardView(title: String, subtitle: String, onOpen: () -> Unit) { Card(onClick = onOpen, modifier = Modifier.width(250.dp).height(120.dp)) { Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0xFF263B6B), Color(0xFF11141C)))).padding(18.dp), contentAlignment = Alignment.BottomStart) { Column { Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp); Spacer(Modifier.height(4.dp)); Text(subtitle, fontSize = 15.sp, fontWeight = FontWeight.SemiBold) } } } }
