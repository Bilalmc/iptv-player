package com.bilalmc.iptvplayer.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.tv.material3.Button
import androidx.tv.material3.Card
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text

class IptvShellActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent?.data != null) {
            openOwnTv()
            finish()
            return
        }
        setContent { IptvPlayerShell(onOpenPlayer = ::openOwnTv) }
    }

    private fun openOwnTv() {
        startActivity(Intent().setClassName(packageName, "tv.own.owntv.MainActivity"))
    }
}

private data class NavItem(val label: String, val icon: ImageVector)
private data class ContentCard(val title: String, val subtitle: String, val accent: Color)

@Composable
private fun IptvPlayerShell(onOpenPlayer: () -> Unit) {
    val nav = listOf(
        NavItem("Home", Icons.Default.Home),
        NavItem("Live TV", Icons.Default.LiveTv),
        NavItem("Movies", Icons.Default.Movie),
        NavItem("Series", Icons.Default.VideoLibrary),
        NavItem("Favorites", Icons.Default.Favorite),
        NavItem("Settings", Icons.Default.Settings),
    )
    var selected by remember { mutableIntStateOf(0) }
    val featured = listOf(
        ContentCard("LIVE CHANNELS", "Start watching live television", Color(0xFF5B7CFF)),
        ContentCard("MOVIES", "Your on-demand library", Color(0xFF8B5CF6)),
        ContentCard("SERIES", "Continue your shows", Color(0xFF00A6A6)),
    )

    MaterialTheme {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF08090D))
                .padding(32.dp),
        ) {
            NavigationRail(nav, selected) { selected = it }
            Spacer(Modifier.width(30.dp))
            Column(modifier = Modifier.fillMaxSize()) {
                TopBar()
                Spacer(Modifier.height(18.dp))
                when (selected) {
                    0 -> HomeContent(featured, onOpenPlayer)
                    else -> SectionPlaceholder(nav[selected].label, onOpenPlayer)
                }
            }
        }
    }
}

@Composable
private fun NavigationRail(items: List<NavItem>, selected: Int, onSelected: (Int) -> Unit) {
    Column(
        modifier = Modifier.fillMaxHeight().width(150.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("IPTV", fontSize = 25.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text("PLAYER", fontSize = 11.sp, letterSpacing = 2.sp, color = Color(0xFF8B93A7))
        Spacer(Modifier.height(28.dp))
        items.forEachIndexed { index, item ->
            FocusNavItem(item, selected == index) { onSelected(index) }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun FocusNavItem(item: NavItem, selected: Boolean, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) Color(0xFF1C294D) else Color.Transparent)
            .border(2.dp, if (focused) Color.White else Color.Transparent, RoundedCornerShape(12.dp))
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(item.icon, contentDescription = item.label, tint = if (selected) Color.White else Color(0xFF8B93A7))
        Spacer(Modifier.width(10.dp))
        Text(item.label, fontSize = 13.sp, color = Color.White)
    }
}

@Composable
private fun TopBar() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text("Good evening", fontSize = 13.sp, color = Color(0xFF8B93A7))
            Text("What do you want to watch?", fontSize = 25.sp, fontWeight = FontWeight.SemiBold)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Search, contentDescription = "Search", tint = Color(0xFFB8BFCE))
            Spacer(Modifier.width(18.dp))
            Text("Profile", fontSize = 13.sp, color = Color(0xFFB8BFCE))
        }
    }
}

@Composable
private fun HomeContent(featured: List<ContentCard>, onOpenPlayer: () -> Unit) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        item { Hero(onOpenPlayer) }
        item {
            Text("Quick access", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                items(featured) { card -> ContentCardView(card, onOpenPlayer) }
            }
        }
        item {
            Text("Continue watching", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ContentCardView(ContentCard("LIVE", "Resume your last channel", Color(0xFF334155)), onOpenPlayer)
                ContentCardView(ContentCard("RECENT", "Open your watch history", Color(0xFF334155)), onOpenPlayer)
            }
        }
    }
}

@Composable
private fun Hero(onOpenPlayer: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(Brush.horizontalGradient(listOf(Color(0xFF10192D), Color(0xFF17131F), Color(0xFF0D0F15))))
            .padding(30.dp),
    ) {
        Column(modifier = Modifier.align(Alignment.CenterStart)) {
            Text("YOUR TV. YOUR WAY.", color = Color(0xFF7EA2FF), fontSize = 12.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("Live TV made simple.", fontSize = 32.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("Fast navigation, EPG, VOD and a remote-first experience.", color = Color(0xFFB8BFCE), fontSize = 14.sp)
            Spacer(Modifier.height(18.dp))
            Button(onClick = onOpenPlayer) { Text("Open player") }
        }
    }
}

@Composable
private fun ContentCardView(card: ContentCard, onOpenPlayer: () -> Unit) {
    Card(onClick = onOpenPlayer, modifier = Modifier.width(300.dp).height(145.dp)) {
        Box(
            modifier = Modifier.fillMaxSize()
                .background(Brush.linearGradient(listOf(card.accent.copy(alpha = 0.75f), Color(0xFF11141C))))
                .padding(18.dp),
            contentAlignment = Alignment.BottomStart,
        ) {
            Column {
                Text(card.title, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                Spacer(Modifier.height(4.dp))
                Text(card.subtitle, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun SectionPlaceholder(title: String, onOpenPlayer: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
        Text(title, fontSize = 34.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("The full catalog and playback engine are provided by the integrated OwnTV core.", color = Color(0xFF9CA3AF))
        Spacer(Modifier.height(22.dp))
        Button(onClick = onOpenPlayer) { Text("Open catalog") }
    }
}
