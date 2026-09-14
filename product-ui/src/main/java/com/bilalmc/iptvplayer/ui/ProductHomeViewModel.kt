package com.bilalmc.iptvplayer.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import tv.own.owntv.core.database.dao.ChannelDao
import tv.own.owntv.core.database.dao.MovieDao
import tv.own.owntv.core.database.dao.ProfileDao
import tv.own.owntv.core.database.dao.SeriesDao
import tv.own.owntv.core.database.dao.SourceDao
import tv.own.owntv.core.database.entity.ChannelEntity
import tv.own.owntv.core.database.entity.MovieEntity
import tv.own.owntv.core.database.entity.ProfileEntity
import tv.own.owntv.core.database.entity.SeriesEntity
import tv.own.owntv.core.settings.SettingsRepository

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

    private val sourceIds: Flow<List<Long>> = settings.activeProfileId.flatMapLatest { profileId ->
        if (profileId < 0L) flowOf(emptyList()) else sourceDao.observeForProfile(profileId).map { sources -> sources.map { it.id } }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val movies: Flow<PagingData<MovieEntity>> = sourceIds.flatMapLatest { ids ->
        if (ids.isEmpty()) flowOf(PagingData.empty()) else Pager(PagingConfig(pageSize = 24, prefetchDistance = 8, enablePlaceholders = false)) { movieDao.pagingAllOriginal(ids) }.flow
    }.cachedIn(viewModelScope)

    val series: Flow<PagingData<SeriesEntity>> = sourceIds.flatMapLatest { ids ->
        if (ids.isEmpty()) flowOf(PagingData.empty()) else Pager(PagingConfig(pageSize = 24, prefetchDistance = 8, enablePlaceholders = false)) { seriesDao.pagingAllOriginal(ids) }.flow
    }.cachedIn(viewModelScope)

    val state: StateFlow<ProductHomeState> = settings.activeProfileId.flatMapLatest { profileId ->
        if (profileId < 0L) flowOf(ProductHomeState()) else {
            val profileFlow = profileDao.observeById(profileId)
            sourceDao.observeForProfile(profileId).flatMapLatest { sources ->
                if (sources.isEmpty()) {
                    profileFlow.map { profile -> ProductHomeState(profileName = profile?.name ?: "Profile") }
                } else {
                    val ids = sources.map { it.id }
                    val channelsFlow: Flow<List<ChannelEntity>> = flow { emit(channelDao.snapshotAll(ids, 64)) }
                    combine(listOf<Flow<Any?>>(
                        profileFlow,
                        channelDao.countAll(ids),
                        movieDao.countAll(ids),
                        seriesDao.countAll(ids),
                        channelDao.favoritesListAlpha(profileId),
                        channelsFlow,
                    )) { values ->
                        val profile = values[0] as ProfileEntity?
                        val channelCount = values[1] as Int
                        val movieCount = values[2] as Int
                        val seriesCount = values[3] as Int
                        val favorites = values[4] as List<ChannelEntity>
                        val channels = values[5] as List<ChannelEntity>
                        ProductHomeState(
                            profileName = profile?.name ?: "Profile",
                            channelCount = channelCount,
                            movieCount = movieCount,
                            seriesCount = seriesCount,
                            favoriteChannels = favorites.filter { it.sourceId in ids }.take(16),
                            channels = channels,
                            hasSources = true,
                        )
                    }
                }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProductHomeState())

    suspend fun currentProfileId(): Long = settings.activeProfileId.first()
}
