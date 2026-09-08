package com.bilalmc.iptvplayer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.Card
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import kotlinx.coroutines.flow.StateFlow
import tv.own.owntv.core.database.entity.ChannelEntity
import tv.own.owntv.core.database.entity.EpisodeEntity
import tv.own.owntv.core.database.entity.MovieEntity
import tv.own.owntv.core.database.entity.SeasonEntity
import tv.own.owntv.core.database.entity.SeriesEntity
import tv.own.owntv.core.settings.SettingsRepository
import java.text.DateFormat
import java.util.Date

private val panel = Color(0xFF121620)
private val muted = Color(0xFF9CA3AF)
private val accent = Color(0xFF7EA2FF)

@Composable
fun ProductSearchScreen(vm: ProductCatalogViewModel, onBack: () -> Unit, onPlayChannel: (ChannelEntity) -> Unit, onPlayMovie: (MovieEntity) -> Unit, onOpenSeries: (SeriesEntity) -> Unit) {
    val query by vm.query.collectAsStateWithLifecycleCompat()
    val results by vm.searchResults.collectAsStateWithLifecycleCompat()
    Column(Modifier.fillMaxSize().padding(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) { FocusIconButton("Back", Icons.Default.ArrowBack, onBack); Spacer(Modifier.width(18.dp)); SearchField(query, vm::setQuery) { vm.clearSearch() } }
        Spacer(Modifier.height(24.dp))
        if (query.trim().length < 2) {
            Text("Search Live TV, Movies and Series", fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text("Type at least two characters with the TV keyboard or remote", color = muted, modifier = Modifier.padding(top = 8.dp))
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(22.dp)) {
                if (results.channels.isNotEmpty()) item { SearchSection("Live TV", results.channels.size) { SearchChannelRow(results.channels, onPlayChannel) } }
                if (results.movies.isNotEmpty()) item { SearchSection("Movies", results.movies.size) { SearchMovieRow(results.movies, onPlayMovie) } }
                if (results.series.isNotEmpty()) item { SearchSection("Series", results.series.size) { SearchSeriesRow(results.series, onOpenSeries) } }
                if (results.channels.isEmpty() && results.movies.isEmpty() && results.series.isEmpty()) item { Text("No results for \"$query\"", fontSize = 22.sp, color = muted) }
            }
        }
    }
}

@Composable private fun SearchField(value: String, onValueChange: (String) -> Unit, onClear: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Row(Modifier.width(560.dp).height(58.dp).clip(RoundedCornerShape(14.dp)).background(panel).border(2.dp, if (focused) Color.White else Color(0xFF293142), RoundedCornerShape(14.dp)).onFocusChanged { focused = it.isFocused }.focusable().padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Search, "Search", tint = muted); Spacer(Modifier.width(12.dp)); BasicTextField(value, onValueChange, singleLine = true, modifier = Modifier.weight(1f), textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 18.sp)); if (value.isNotEmpty()) FocusIconButton("Clear", Icons.Default.Clear, onClear)
    }
}

@Composable private fun SearchSection(title: String, count: Int, content: @Composable () -> Unit) { Column { Text(title, fontSize = 21.sp, fontWeight = FontWeight.SemiBold); Text("$count results", color = muted, fontSize = 12.sp, modifier = Modifier.padding(top = 3.dp, bottom = 10.dp)); content() } }
@Composable private fun SearchChannelRow(items: List<ChannelEntity>, onPlay: (ChannelEntity) -> Unit) { LazyRow(Modifier.focusGroup(), horizontalArrangement = Arrangement.spacedBy(12.dp)) { items(items, key = { it.id }) { item -> Card(onClick = { onPlay(item) }, modifier = Modifier.width(250.dp).height(90.dp)) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.Center) { Text(item.number?.toString() ?: "LIVE", color = accent, fontSize = 11.sp); Text(item.name, fontWeight = FontWeight.SemiBold) } } } } }
@Composable private fun SearchMovieRow(items: List<MovieEntity>, onPlay: (MovieEntity) -> Unit) { LazyRow(Modifier.focusGroup(), horizontalArrangement = Arrangement.spacedBy(12.dp)) { items(items, key = { it.id }) { item -> Card(onClick = { onPlay(item) }, modifier = Modifier.width(250.dp).height(90.dp)) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.Center) { Text(item.year?.toString() ?: "MOVIE", color = accent, fontSize = 11.sp); Text(item.name, fontWeight = FontWeight.SemiBold, maxLines = 2) } } } } }
@Composable private fun SearchSeriesRow(items: List<SeriesEntity>, onOpen: (SeriesEntity) -> Unit) { LazyRow(Modifier.focusGroup(), horizontalArrangement = Arrangement.spacedBy(12.dp)) { items(items, key = { it.id }) { item -> Card(onClick = { onOpen(item) }, modifier = Modifier.width(250.dp).height(90.dp)) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.Center) { Text(item.year?.toString() ?: "SERIES", color = accent, fontSize = 11.sp); Text(item.name, fontWeight = FontWeight.SemiBold, maxLines = 2) } } } } }

@Composable
fun ProductSeriesDetail(vm: ProductCatalogViewModel, onBack: () -> Unit, onPlayEpisode: (EpisodeEntity, SeriesEntity) -> Unit) {
    val series by vm.selectedSeries.collectAsStateWithLifecycleCompat(); val seasons by vm.seasons.collectAsStateWithLifecycleCompat(); val selectedSeasonId by vm.selectedSeasonId.collectAsStateWithLifecycleCompat(); val episodes by vm.episodes.collectAsStateWithLifecycleCompat()
    if (series == null) return
    Column(Modifier.fillMaxSize().padding(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) { FocusIconButton("Back", Icons.Default.ArrowBack) { vm.closeSeries(); onBack() }; Spacer(Modifier.width(18.dp)); Column { Text(series!!.name, fontSize = 30.sp, fontWeight = FontWeight.Bold); Text("${seasons.size} seasons · ${episodes.size} episodes", color = muted, fontSize = 13.sp) } }
        Spacer(Modifier.height(18.dp))
        if (seasons.isNotEmpty()) { Text("Seasons", fontSize = 20.sp, fontWeight = FontWeight.SemiBold); Spacer(Modifier.height(10.dp)); LazyRow(Modifier.focusGroup(), horizontalArrangement = Arrangement.spacedBy(10.dp)) { items(seasons, key = { it.id }) { season -> Button(onClick = { vm.selectSeason(season) }) { Text(if (season.name.isNullOrBlank()) "Season ${season.seasonNumber}" else season.name!!) } } } }
        Spacer(Modifier.height(18.dp)); Text(if (selectedSeasonId == null) "All episodes" else "Episodes", fontSize = 20.sp, fontWeight = FontWeight.SemiBold); Spacer(Modifier.height(10.dp))
        if (episodes.isEmpty()) Text("Episodes are loading or this series has no episodes yet.", color = muted) else LazyColumn(Modifier.fillMaxSize().focusGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) { items(episodes, key = { it.id }) { episode -> EpisodeRow(episode) { onPlayEpisode(episode, series!!) } } }
    }
}

@Composable private fun EpisodeRow(episode: EpisodeEntity, onPlay: () -> Unit) { Card(onClick = onPlay, modifier = Modifier.fillMaxWidth().height(76.dp)) { Row(Modifier.fillMaxSize().padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) { Text("S${episode.seasonNumber.toString().padStart(2, '0')} E${episode.episodeNumber.toString().padStart(2, '0')}", color = accent, fontWeight = FontWeight.Bold, modifier = Modifier.width(105.dp)); Column(Modifier.weight(1f)) { Text(episode.name.ifBlank { "Episode ${episode.episodeNumber}" }, fontWeight = FontWeight.SemiBold, maxLines = 1); episode.plot?.takeIf { it.isNotBlank() }?.let { Text(it, color = muted, fontSize = 11.sp, maxLines = 1) } }; Text("▶", color = Color.White, fontSize = 18.sp) } } }

@Composable
fun ProductEpgScreen(vm: ProductCatalogViewModel, onBack: () -> Unit, onPlayChannel: (ChannelEntity) -> Unit) {
    val rows by vm.epgRows.collectAsStateWithLifecycleCompat()
    Column(Modifier.fillMaxSize().padding(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) { FocusIconButton("Back", Icons.Default.ArrowBack, onBack); Spacer(Modifier.width(18.dp)); Column { Text("TV Guide", fontSize = 32.sp, fontWeight = FontWeight.Bold); Text("Now & next · ${rows.count { it.now != null }} channels with live programme data", color = muted, fontSize = 13.sp) } }
        Spacer(Modifier.height(20.dp))
        if (rows.isEmpty()) { Text("No EPG data available for the active channels yet.", color = muted, fontSize = 18.sp); Text("The guide populates after the source's XMLTV/EPG feed is synchronized.", color = muted, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp)) }
        else LazyColumn(Modifier.fillMaxSize().focusGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) { items(rows, key = { it.channel.id }) { row -> EpgRow(row, onPlayChannel) } }
    }
}

@Composable private fun EpgRow(row: ProductEpgRow, onPlay: (ChannelEntity) -> Unit) { Card(onClick = { onPlay(row.channel) }, modifier = Modifier.fillMaxWidth().height(108.dp)) { Row(Modifier.fillMaxSize().padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.width(240.dp)) { Text(row.channel.number?.toString() ?: "LIVE", color = accent, fontSize = 11.sp, fontWeight = FontWeight.Bold); Text(row.channel.name, fontWeight = FontWeight.SemiBold, maxLines = 1) }; if (row.now == null) Text("No programme currently airing", color = muted) else Column(Modifier.weight(1f)) { Text("NOW  ${formatTime(row.now.startMs)}–${formatTime(row.now.stopMs)}", color = accent, fontSize = 11.sp, fontWeight = FontWeight.Bold); Text(row.now.title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, maxLines = 1); row.upcoming.drop(1).firstOrNull()?.let { Text("NEXT  ${formatTime(it.startMs)} · ${it.title}", color = muted, fontSize = 12.sp, maxLines = 1) } }; Text("▶", color = Color.White, fontSize = 18.sp) } } }

@Composable
fun ProductSettingsScreen(vm: ProductCatalogViewModel, onBack: () -> Unit) {
    val state by vm.settingsState.collectAsStateWithLifecycleCompat()
    LazyColumn(Modifier.fillMaxSize().padding(8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Row(verticalAlignment = Alignment.CenterVertically) { FocusIconButton("Back", Icons.Default.ArrowBack, onBack); Spacer(Modifier.width(18.dp)); Column { Text("Settings", fontSize = 32.sp, fontWeight = FontWeight.Bold); Text("IPTV Player preferences", color = muted, fontSize = 13.sp) } } }
        item { SettingsGroup("Playback") { ChoiceRow("Resume playback", state.resumeMode.name, listOf(SettingsRepository.ResumeMode.AUTO, SettingsRepository.ResumeMode.ASK, SettingsRepository.ResumeMode.NEVER)) { vm.setResumeMode(it) }; ToggleRow("Auto-check for updates", state.updateCheckOnStart) { vm.setUpdateCheckOnStart(it) } } }
        item { SettingsGroup("Browse") { ChoiceRow("Movie / Series view", state.vodViewMode.name, listOf(SettingsRepository.VodViewMode.GRID, SettingsRepository.VodViewMode.LIST)) { vm.setVodViewMode(it) }; ChoiceRow("Episode view", state.episodeViewMode.name, listOf(SettingsRepository.VodViewMode.LIST, SettingsRepository.VodViewMode.GRID)) { vm.setEpisodeViewMode(it) }; ToggleRow("Remember Series category", state.rememberSeriesCategory) { vm.setRememberSeriesCategory(it) }; ToggleRow("Remember Series position", state.rememberSeriesItem) { vm.setRememberSeriesItem(it) } } }
        item { Text("All controls above are native to IPTV Player. No Settings action opens the OwnTV UI.", color = muted, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp)) }
    }
}

@Composable private fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) { Column(Modifier.fillMaxWidth().background(panel, RoundedCornerShape(16.dp)).padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { Text(title, fontSize = 18.sp, fontWeight = FontWeight.SemiBold); content() } }
@Composable private fun ToggleRow(title: String, value: Boolean, onChange: (Boolean) -> Unit) { Card(onClick = { onChange(!value) }, modifier = Modifier.fillMaxWidth().height(64.dp)) { Row(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) { Text(title, fontWeight = FontWeight.SemiBold); Text(if (value) "ON" else "OFF", color = if (value) accent else muted, fontWeight = FontWeight.Bold) } } }
@Composable private fun <T> ChoiceRow(title: String, current: String, choices: List<T>, onChange: (T) -> Unit) { Column { Text(title, color = muted, fontSize = 12.sp, modifier = Modifier.padding(bottom = 6.dp)); LazyRow(Modifier.focusGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { items(choices) { choice -> Button(onClick = { onChange(choice) }) { Text(choice.toString().substringAfterLast('.').replace('_', ' ')) } } }; Text("Current: $current", color = muted, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp)) } }
@Composable private fun FocusIconButton(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) { var focused by remember { mutableStateOf(false) }; Box(Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)).border(2.dp, if (focused) Color.White else Color.Transparent, RoundedCornerShape(14.dp)).onFocusChanged { focused = it.isFocused }.focusable().clickable(onClick = onClick), contentAlignment = Alignment.Center) { Icon(icon, label, tint = Color.White) } }
private fun formatTime(ms: Long): String = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(ms))
@Composable private fun <T> StateFlow<T>.collectAsStateWithLifecycleCompat(): State<T> = collectAsState()
