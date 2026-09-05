package com.bilalmc.iptvplayer.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import tv.own.owntv.core.database.dao.ChannelDao
import tv.own.owntv.core.database.dao.MovieDao
import tv.own.owntv.core.database.dao.ProfileDao
import tv.own.owntv.core.database.dao.SeriesDao
import tv.own.owntv.core.database.dao.SourceDao
import tv.own.owntv.core.database.entity.ChannelEntity
import tv.own.owntv.core.database.entity.ProfileEntity
import tv.own.owntv.core.settings.SettingsRepository

/** Product-owned presentation state backed directly by the shared OwnTV_Core database. */
data class ProductHomeState(
    val profileName: String = "Profile",
    val channelCount: Int = 0,
    val movieCount: Int = 0,
    val seriesCount: Int = 0,
    val favoriteChannels: List<ChannelEntity> = emptyList(),
    val channels: List<ChannelEntity> = emptyList(),
    val hasSources: Boolean = false,
)

class ProductHomeViewModel : ViewModel(), KoinComponent {
    private val settings: SettingsRepository by inject()
    private val profileDao: ProfileDao by inject()
    private val sourceDao: SourceDao by inject()
    private val channelDao: ChannelDao by inject()
    private val movieDao: MovieDao by inject()
    private val seriesDao: SeriesDao by inject()

    val state: StateFlow<ProductHomeState> = settings.activeProfileId
        .flatMapLatest { profileId ->
            if (profileId < 0L) {
                flowOf(ProductHomeState())
            } else {
                val profileFlow = profileDao.observeById(profileId)
                val sourcesFlow = sourceDao.observeForProfile(profileId)
                sourcesFlow.flatMapLatest { sources ->
                    if (sources.isEmpty()) {
                        profileFlow.map { profile -> ProductHomeState(profileName = profile?.name ?: "Profile") }
                    } else {
                        val sourceIds = sources.map { it.id }
                        combine(
                            profileFlow,
                            channelDao.countAll(sourceIds),
                            movieDao.countAll(sourceIds),
                            seriesDao.countAll(sourceIds),
                            channelDao.favoritesListAlpha(profileId),
                        ) { profile, channelCount, movieCount, seriesCount, favorites ->
                            ProductHomeState(
                                profileName = profile?.name ?: "Profile",
                                channelCount = channelCount,
                                movieCount = movieCount,
                                seriesCount = seriesCount,
                                favoriteChannels = favorites.filter { it.sourceId in sourceIds }.take(12),
                                hasSources = true,
                            )
                        }
                    }
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProductHomeState())

    fun refreshPreviewChannels() {
        viewModelScope.launch {
            val profileId = settings.activeProfileId.first()
            if (profileId < 0L) return@launch
            val sourceIds = sourceDao.sourceIdsForProfile(profileId)
            if (sourceIds.isEmpty()) return@launch
            // The product shell deliberately uses a bounded snapshot: the full catalog stays in
            // Paging/OwnTV's Live screen, while Home only needs enough rows to render a fast TV rail.
            channelDao.snapshotAll(sourceIds, 12)
        }
    }
}
