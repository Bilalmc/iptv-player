package com.bilalmc.iptvplayer.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.*
import tv.own.owntv.core.database.entity.ChannelEntity
import tv.own.owntv.core.launcher.LauncherDeepLink

private const val OWN_TV_ACTIVITY = "tv.own.owntv.MainActivity"

class IptvShellActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent?.data != null) {
            openOwnTv(intent.data)
            finish()
            return
        }
        setContent { IptvPlayerShell(::openOwnTv, ::playChannel) }
    }

    private fun openOwnTv(data: android.net.Uri? = null) {
        startActivity(Intent().setClassName(packageName, OWN_TV_ACTIVITY).apply { this.data = data })
    }

    private fun playChannel(channel: ChannelEntity) {
        openOwnTv(LauncherDeepLink.Live(channel.sourceId, channel.remoteId, channel.name, channel.id).toUri())
    }
}

private data class NavItem(val label: String, val icon: ImageVector)

@Composable
private fun IptvPlayerShell(onOpenPlayer: () -> Unit, onPlayChannel: (ChannelEntity) -> Unit) {
    val state by viewModel<ProductHomeViewModel>().state.collectAsStateWithLifecycle()
    val nav = listOf(
        NavItem("Home", androidx.compose.material.icons.Icons.Default.Home),
        NavItem("Live TV", androidx.compose.material.icons.Icons.Default.LiveTv),
        NavItem("Movies", androidx.compose.material.icons.Icons.Default.Movie),
        NavItem("Series", androidx.compose.material.icons.Icons.Default.VideoLibrary),
        NavItem("Favorites", androidx.compose.material.icons.Icons.Default.Favorite),
        NavItem("Settings", androidx.compose.material.icons.Icons.Default.Settings),
    )
    var selected by remember { mutableIntStateOf(0) }
    MaterialTheme {
        Row(Modifier.fillMaxSize().background(Color(0xFF08090D)).padding(32.dp)) {
            NavigationRail(nav, selected) { selected = it }
            Spacer(Modifier.width(30.dp))
            Column(Modifier.fillMaxSize()) {
                TopBar(state.profileName)
                Spacer(Modifier.height(18.dp))
                when (selected) {
                    0 -> HomeContent(state, onOpenPlayer, onPlayChannel)
                    1 -> LiveContent(state, onOpenPlayer, onPlayChannel)
                    2 -> CatalogSection("Movies", state.movieCount, Color(0xFF8B5CF6), onOpenPlayer)
                    3 -> CatalogSection("Series", state.seriesCount, Color(0xFF00A6A6), onOpenPlayer)
                    4 -> FavoritesContent(state.favoriteChannels, onOpenPlayer, onPlayChannel)
                    else -> SectionPlaceholder("Settings", onOpenPlayer)
                }
            }
        }
    }
}

@Composable private fun NavigationRail(items: List<NavItem>, selected: Int, onSelected: (Int) -> Unit) {
    Column(Modifier.fillMaxHeight().width(150.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("IPTV", fontSize = 25.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text("PLAYER", fontSize = 11.sp, letterSpacing = 2.sp, color = Color(0xFF8B93A7))
        Spacer(Modifier.height(28.dp))
        items.forEachIndexed { index, item -> FocusNavItem(item, selected == index) { onSelected(index) }; Spacer(Modifier.height(8.dp)) }
    }
}

@Composable private fun FocusNavItem(item: NavItem, selected: Boolean, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(if (selected) Color(0xFF1C294D) else Color.Transparent).border(2.dp, if (focused) Color.White else Color.Transparent, RoundedCornerShape(12.dp)).onFocusChanged { focused = it.isFocused }.focusable().clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(item.icon, item.label, tint = if (selected) Color.White else Color(0xFF8B93A7))
        Spacer(Modifier.width(10.dp))
        Text(item.label, fontSize = 13.sp, color = Color.White)
    }
}

@Composable private fun TopBar(profileName: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Column { Text("IPTV Player", fontSize = 13.sp, color = Color(0xFF8B93A7)); Text("What do you want to watch?", fontSize = 25.sp, fontWeight = FontWeight.SemiBold) }
        Row(verticalAlignment = Alignment.CenterVertically) { Icon(androidx.compose.material.icons.Icons.Default.Search, "Search", tint = Color(0xFFB8BFCE)); Spacer(Modifier.width(18.dp)); Text(profileName, fontSize = 13.sp, color = Color(0xFFB8BFCE)) }
    }
}

@Composable private fun HomeContent(state: ProductHomeState, onOpenPlayer: () -> Unit, onPlayChannel: (ChannelEntity) -> Unit) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        item { Hero(state, onOpenPlayer) }
        item { SectionTitle("Live now"); if (state.channels.isEmpty()) EmptyText("No channels indexed yet.") else ChannelRow(state.channels, onPlayChannel) }
        item { SectionTitle("Quick access"); LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            item { ContentCardView("LIVE TV", "${state.channelCount} channels", Color(0xFF5B7CFF), onOpenPlayer) }
            item { ContentCardView("MOVIES", "${state.movieCount} titles", Color(0xFF8B5CF6), onOpenPlayer) }
            item { ContentCardView("SERIES", "${state.seriesCount} series", Color(0xFF00A6A6), onOpenPlayer) }
        }}
        item { SectionTitle("Favorites"); if (state.favoriteChannels.isEmpty()) EmptyText("No live favorites yet.") else ChannelRow(state.favoriteChannels, onPlayChannel) }
    }
}

@Composable private fun Hero(state: ProductHomeState, onOpenPlayer: () -> Unit) {
    Box(Modifier.fillMaxWidth().height(250.dp).clip(RoundedCornerShape(22.dp)).background(Brush.horizontalGradient(listOf(Color(0xFF10192D), Color(0xFF17131F), Color(0xFF0D0F15)))).padding(30.dp)) {
        Column(Modifier.align(Alignment.CenterStart)) {
            Text("YOUR TV. YOUR WAY.", color = Color(0xFF7EA2FF), fontSize = 12.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp)); Text(if (state.hasSources) "Live TV made simple." else "Connect your IPTV source.", fontSize = 32.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp)); Text(if (state.hasSources) "${state.channelCount} live channels · ${state.movieCount} movies · ${state.seriesCount} series" else "Add an M3U, Xtream or Stalker source to start.", color = Color(0xFFB8BFCE), fontSize = 14.sp)
            Spacer(Modifier.height(18.dp)); Button(onClick = onOpenPlayer) { Text(if (state.hasSources) "Open Live TV" else "Add source") }
        }
    }
}

@Composable private fun LiveContent(state: ProductHomeState, onOpenPlayer: () -> Unit, onPlayChannel: (ChannelEntity) -> Unit) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item { Text("Live TV", fontSize = 34.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(4.dp)); Text("${state.channelCount} channels", color = Color(0xFF9CA3AF)) }
        if (state.channels.isNotEmpty()) item { SectionTitle("All channels · preview"); ChannelRow(state.channels, onPlayChannel) }
        item { if (state.favoriteChannels.isNotEmpty()) { SectionTitle("Favorites"); ChannelRow(state.favoriteChannels, onPlayChannel) }; Spacer(Modifier.height(8.dp)); Button(onClick = onOpenPlayer) { Text("Open full Live TV catalog") } }
    }
}

@Composable private fun FavoritesContent(channels: List<ChannelEntity>, onOpenPlayer: () -> Unit, onPlayChannel: (ChannelEntity) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) { Text("Favorites", fontSize = 34.sp, fontWeight = FontWeight.Bold); if (channels.isEmpty()) EmptyText("No live favorites yet.") else ChannelRow(channels, onPlayChannel); Button(onClick = onOpenPlayer) { Text("Open full catalog") } }
}

@Composable private fun CatalogSection(title: String, count: Int, accent: Color, onOpenPlayer: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) { Text(title, fontSize = 34.sp, fontWeight = FontWeight.Bold); Text("$count items indexed", color = Color(0xFF9CA3AF)); ContentCardView(title.uppercase(), "Open the complete catalog", accent, onOpenPlayer) }
}

@Composable private fun SectionPlaceholder(title: String, onOpenPlayer: () -> Unit) {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) { Text(title, fontSize = 34.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(8.dp)); Text("This section is backed by the integrated OwnTV catalog.", color = Color(0xFF9CA3AF)); Spacer(Modifier.height(22.dp)); Button(onClick = onOpenPlayer) { Text("Open catalog") } }
}

@Composable private fun SectionTitle(title: String) { Text(title, fontSize = 20.sp, fontWeight = FontWeight.SemiBold); Spacer(Modifier.height(12.dp)) }
@Composable private fun EmptyText(text: String) { Text(text, color = Color(0xFF9CA3AF), fontSize = 14.sp) }

@Composable private fun ChannelRow(channels: List<ChannelEntity>, onPlay: (ChannelEntity) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) { items(channels, key = { it.id }) { ChannelCard(it, onPlay) } }
}

@Composable private fun ChannelCard(channel: ChannelEntity, onPlay: (ChannelEntity) -> Unit) {
    Card(onClick = { onPlay(channel) }, modifier = Modifier.width(300.dp).height(145.dp)) {
        Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0xFF263B6B), Color(0xFF11141C)))).padding(18.dp), contentAlignment = Alignment.BottomStart) {
            Column { Text(channel.number?.toString() ?: "LIVE", fontSize = 12.sp, color = Color(0xFFB8BFCE), fontWeight = FontWeight.Bold); Spacer(Modifier.height(4.dp)); Text(channel.name, fontSize = 18.sp, fontWeight = FontWeight.SemiBold) }
        }
    }
}

@Composable private fun ContentCardView(title: String, subtitle: String, accent: Color, onOpenPlayer: () -> Unit) {
    Card(onClick = onOpenPlayer, modifier = Modifier.width(300.dp).height(145.dp)) {
        Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(accent.copy(alpha = 0.75f), Color(0xFF11141C)))).padding(18.dp), contentAlignment = Alignment.BottomStart) {
            Column { Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp); Spacer(Modifier.height(4.dp)); Text(subtitle, fontSize = 15.sp, fontWeight = FontWeight.SemiBold) }
        }
    }
}
