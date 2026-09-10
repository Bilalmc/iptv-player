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
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.first
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import tv.own.owntv.core.database.dao.ChannelDao
import tv.own.owntv.core.database.dao.MovieDao
import tv.own.owntv.core.database.dao.ProfileDao
import tv.own.owntv.core.database.dao.SeriesDao
import tv.own.owntv.core.database.dao.SourceDao
import tv.own.owntv.core.database.entity.ChannelEntity
import tv.own.owntv.core.database.entity.MovieEntity
import tv.own.owntv.core.database.entity.SeriesEntity
import tv.own.owntv.core.settings.SettingsRepository

/** Product-facing projection of the shared OwnTV catalog. */
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

    private val sourceIds: Flow<List<Long>> = settings.activeProfileId
        .flatMapLatest { profileId ->
            if (profileId < 0L) flowOf(emptyList())
            else sourceDao.observeForProfile(profileId).map { sources -> sources.map { it.id } }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val movies: Flow<PagingData<MovieEntity>> = sourceIds.flatMapLatest { ids ->
        if (ids.isEmpty()) flowOf(PagingData.empty())
        else Pager(PagingConfig(pageSize = 24, prefetchDistance = 8, enablePlaceholders = false)) {
            movieDao.pagingAllOriginal(ids)
        }.flow
    }.cachedIn(viewModelScope)

    val series: Flow<PagingData<SeriesEntity>> = sourceIds.flatMapLatest { ids ->
        if (ids.isEmpty()) flowOf(PagingData.empty())
        else Pager(PagingConfig(pageSize = 24, prefetchDistance = 8, enablePlaceholders = false)) {
            seriesDao.pagingAllOriginal(ids)
        }.flow
    }.cachedIn(viewModelScope)

    val state: StateFlow<ProductHomeState> = settings.activeProfileId
        .flatMapLatest { profileId ->
            if (profileId < 0L) {
                flowOf(ProductHomeState())
            } else {
                val profileFlow = profileDao.observeById(profileId)
                sourceDao.observeForProfile(profileId).flatMapLatest { sources ->
                    if (sources.isEmpty()) {
                        profileFlow.map { profile ->
                            ProductHomeState(profileName = profile?.name ?: "Profile")
                        }
                    } else {
                        val sourceIds = sources.map { it.id }
                        val channelsFlow = flow { emit(channelDao.snapshotAll(sourceIds, 64)) }
                        combine(
                            profileFlow,
                            channelDao.countAll(sourceIds),
                            movieDao.countAll(sourceIds),
                            seriesDao.countAll(sourceIds),
                            channelDao.favoritesListAlpha(profileId),
                            channelsFlow,
                        ) { profile, channelCount, movieCount, seriesCount, favorites, channels ->
                            ProductHomeState(
                                profileName = profile?.name ?: "Profile",
                                channelCount = channelCount,
                                movieCount = movieCount,
                                seriesCount = seriesCount,
                                favoriteChannels = favorites.filter { it.sourceId in sourceIds }.take(16),
                                channels = channels,
                                hasSources = true,
                            )
                        }
                    }
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProductHomeState())

    suspend fun currentProfileId(): Long = settings.activeProfileId.first()
}
