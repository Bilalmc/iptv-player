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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.tv.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
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

private data class NavItem(val label: String, val icon: ImageVector)

@Composable
private fun IptvPlayerShell(onOpenPlayer: () -> Unit, onPlayChannel: (ChannelEntity) -> Unit, onPlayMovie: (MovieEntity) -> Unit, onPlayEpisode: (EpisodeEntity, SeriesEntity) -> Unit) {
    val homeVm: ProductHomeViewModel = viewModel()
    val catalogVm: ProductCatalogViewModel = viewModel()
    val state by homeVm.state.collectAsStateWithLifecycle()
    var selected by rememberSaveable { mutableIntStateOf(0) }
    var seriesDetail by rememberSaveable { mutableStateOf(false) }
    val navFocusRequester = remember { FocusRequester() }
    val nav = listOf(
        NavItem("Home", Icons.Default.Home), NavItem("Live TV", Icons.Default.LiveTv),
        NavItem("TV Guide", Icons.Default.CalendarMonth), NavItem("Movies", Icons.Default.Movie),
        NavItem("Series", Icons.Default.VideoLibrary), NavItem("Search", Icons.Default.Search),
        NavItem("Favorites", Icons.Default.Favorite), NavItem("Settings", Icons.Default.Settings),
    )
    BackHandler { when { seriesDetail -> seriesDetail = false; selected != 0 -> selected = 0 } }
    LaunchedEffect(Unit) { navFocusRequester.requestFocus() }
    MaterialTheme {
        Row(Modifier.fillMaxSize().background(Color(0xFF08090D)).padding(28.dp)) {
            Column(Modifier.fillMaxHeight().width(152.dp).focusGroup(), verticalArrangement = Arrangement.Center) {
                Text("IPTV", fontSize = 25.sp, fontWeight = FontWeight.Bold)
                Text("PLAYER", fontSize = 11.sp, color = Color(0xFF8B93A7), letterSpacing = 2.sp)
                Spacer(Modifier.height(24.dp))
                nav.forEachIndexed { i, item ->
                    NavButton(item, i == selected, if (i == 0) Modifier.focusRequester(navFocusRequester) else Modifier) { selected = i; seriesDetail = false }
                    Spacer(Modifier.height(7.dp))
                }
            }
            Spacer(Modifier.width(28.dp))
            Column(Modifier.fillMaxSize()) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column { Text("IPTV Player", fontSize = 13.sp, color = Color(0xFF8B93A7)); Text("What do you want to watch?", fontSize = 25.sp, fontWeight = FontWeight.SemiBold) }
                    Row(verticalAlignment = Alignment.CenterVertically) { IconButtonNative(Icons.Default.Search, "Search") { selected = 5; seriesDetail = false }; Spacer(Modifier.width(14.dp)); Text(state.profileName, fontSize = 13.sp, color = Color(0xFFB8BFCE)) }
                }
                Spacer(Modifier.height(16.dp))
                if (seriesDetail) ProductSeriesDetail(catalogVm, { seriesDetail = false }) { e, s -> onPlayEpisode(e, s) }
                else when (selected) {
                    0 -> HomeContent(state, { selected = 1 }, { selected = 2 }, { selected = 3 }, { selected = 4 }, onPlayChannel)
                    1 -> LiveContent(state, { selected = 2 }, onPlayChannel)
                    2 -> ProductEpgScreen(catalogVm, { selected = 0 }, onPlayChannel)
                    3 -> MovieCatalog(homeVm, onPlayMovie)
                    4 -> SeriesCatalog(homeVm) { s -> catalogVm.selectSeries(s); seriesDetail = true }
                    5 -> ProductSearchScreen(catalogVm, { selected = 0 }, onPlayChannel, onPlayMovie) { s -> catalogVm.selectSeries(s); seriesDetail = true }
                    6 -> FavoritesContent(state.favoriteChannels, onPlayChannel)
                    else -> ProductSettingsScreen(catalogVm) { selected = 0 }
                }
            }
        }
    }
}

@Composable private fun NavButton(item: NavItem, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Row(modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(if (selected) Color(0xFF1C294D) else Color.Transparent).border(2.dp, if (focused) Color.White else Color.Transparent, RoundedCornerShape(12.dp)).onFocusChanged { focused = it.isFocused }.focusable().clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(item.icon, item.label, tint = if (selected) Color.White else Color(0xFF8B93A7)); Spacer(Modifier.width(9.dp)); Text(item.label, fontSize = 12.sp)
    }
}

@Composable private fun IconButtonNative(icon: ImageVector, label: String, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Box(Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFF121620)).border(2.dp, if (focused) Color.White else Color.Transparent, RoundedCornerShape(14.dp)).onFocusChanged { focused = it.isFocused }.focusable().clickable(onClick = onClick), contentAlignment = Alignment.Center) { Icon(icon, label, tint = Color.White) }
}

@Composable private fun HomeContent(state: ProductHomeState, goLive: () -> Unit, goGuide: () -> Unit, goMovies: () -> Unit, goSeries: () -> Unit, onPlay: (ChannelEntity) -> Unit) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(22.dp)) {
        item { Hero(state, goLive) }
        item { SectionTitle("Live now"); if (state.channels.isEmpty()) EmptyText("No channels indexed yet.") else ChannelRow(state.channels, onPlay) }
        item { SectionTitle("Quick access"); LazyRow(Modifier.focusGroup(), horizontalArrangement = Arrangement.spacedBy(14.dp)) { item { ContentCardView("LIVE TV", "${state.channelCount} channels", goLive) }; item { ContentCardView("TV GUIDE", "Now & next", goGuide) }; item { ContentCardView("MOVIES", "${state.movieCount} titles", goMovies) }; item { ContentCardView("SERIES", "${state.seriesCount} series", goSeries) } } }
        item { SectionTitle("Favorites"); if (state.favoriteChannels.isEmpty()) EmptyText("No live favorites yet.") else ChannelRow(state.favoriteChannels, onPlay) }
    }
}

@Composable private fun Hero(state: ProductHomeState, onOpen: () -> Unit) { Box(Modifier.fillMaxWidth().height(230.dp).clip(RoundedCornerShape(22.dp)).background(Brush.horizontalGradient(listOf(Color(0xFF10192D), Color(0xFF17131F), Color(0xFF0D0F15)))).padding(30.dp)) { Column(Modifier.align(Alignment.CenterStart)) { Text("YOUR TV. YOUR WAY.", color = Color(0xFF7EA2FF), fontSize = 12.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(8.dp)); Text(if (state.hasSources) "Live TV made simple." else "Connect your IPTV source.", fontSize = 32.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(8.dp)); Text(if (state.hasSources) "${state.channelCount} live channels · ${state.movieCount} movies · ${state.seriesCount} series" else "Add an M3U, Xtream or Stalker source to start.", color = Color(0xFFB8BFCE), fontSize = 14.sp); Spacer(Modifier.height(16.dp)); Button(onClick = onOpen) { Text(if (state.hasSources) "Open Live TV" else "Open Sources") } } } }
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
