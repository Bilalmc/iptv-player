package com.bilalmc.iptvplayer.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import tv.own.owntv.core.database.dao.ChannelDao
import tv.own.owntv.core.database.dao.EpgDao
import tv.own.owntv.core.database.dao.MovieDao
import tv.own.owntv.core.database.dao.SeriesDao
import tv.own.owntv.core.database.dao.SourceDao
import tv.own.owntv.core.database.entity.ChannelEntity
import tv.own.owntv.core.database.entity.EpisodeEntity
import tv.own.owntv.core.database.entity.EpgProgrammeEntity
import tv.own.owntv.core.database.entity.MovieEntity
import tv.own.owntv.core.database.entity.SeasonEntity
import tv.own.owntv.core.database.entity.SeriesEntity
import tv.own.owntv.core.repository.EpgRepository
import tv.own.owntv.core.repository.SeriesRepository
import tv.own.owntv.core.settings.SettingsRepository

data class ProductSearchResults(val channels: List<ChannelEntity> = emptyList(), val movies: List<MovieEntity> = emptyList(), val series: List<SeriesEntity> = emptyList())
data class ProductEpgRow(val channel: ChannelEntity, val now: EpgProgrammeEntity?, val upcoming: List<EpgProgrammeEntity> = emptyList())
data class ProductSettingsState(val vodViewMode: SettingsRepository.VodViewMode = SettingsRepository.VodViewMode.GRID, val episodeViewMode: SettingsRepository.VodViewMode = SettingsRepository.VodViewMode.LIST, val resumeMode: SettingsRepository.ResumeMode = SettingsRepository.ResumeMode.ASK, val updateCheckOnStart: Boolean = true, val rememberSeriesCategory: Boolean = true, val rememberSeriesItem: Boolean = false)

class ProductCatalogViewModel : ViewModel(), KoinComponent {
    private val settings: SettingsRepository by inject()
    private val sourceDao: SourceDao by inject()
    private val channelDao: ChannelDao by inject()
    private val movieDao: MovieDao by inject()
    private val seriesDao: SeriesDao by inject()
    private val epgDao: EpgDao by inject()
    private val epgRepository: EpgRepository by inject()
    private val seriesRepository: SeriesRepository by inject()

    private val sourceIds: StateFlow<List<Long>> = settings.activeProfileId.flatMapLatest { profileId -> if (profileId < 0L) flowOf(emptyList()) else sourceDao.observeForProfile(profileId).map { it.map { source -> source.id } } }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query
    val searchResults: StateFlow<ProductSearchResults> = combine(_query, sourceIds) { q, ids -> q.trim() to ids }.flatMapLatest { (q, ids) -> if (q.length < 2 || ids.isEmpty()) flowOf(ProductSearchResults()) else flow { delay(180); emit(ProductSearchResults(channelDao.searchList(q, ids, 12), movieDao.searchList(q, ids, 12), seriesDao.searchList(q, ids, 12))) } }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProductSearchResults())

    private val _selectedSeries = MutableStateFlow<SeriesEntity?>(null)
    val selectedSeries: StateFlow<SeriesEntity?> = _selectedSeries
    val seasons: StateFlow<List<SeasonEntity>> = _selectedSeries.flatMapLatest { series -> if (series == null) flowOf(emptyList()) else seriesDao.seasons(series.id) }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    private val _selectedSeasonId = MutableStateFlow<Long?>(null)
    val selectedSeasonId: StateFlow<Long?> = _selectedSeasonId
    val episodes: StateFlow<List<EpisodeEntity>> = combine(listOf<Flow<Any?>>(_selectedSeries, _selectedSeasonId)) { values ->
        val series = values[0] as SeriesEntity?
        val seasonId = values[1] as Long?
        series?.id to seasonId
    }.flatMapLatest { (seriesId, seasonId) -> if (seriesId == null) flowOf(emptyList()) else if (seasonId == null) seriesDao.episodesBySeries(seriesId) else seriesDao.episodesBySeason(seasonId) }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val settingsState: StateFlow<ProductSettingsState> = combine(listOf<Flow<Any?>>(
        settings.vodViewMode,
        settings.episodeViewMode,
        settings.resumeMode,
        settings.updateCheckOnStart,
        settings.rememberCategorySeries,
        settings.rememberLastSeries,
    )) { values ->
        ProductSettingsState(
            vodViewMode = values[0] as SettingsRepository.VodViewMode,
            episodeViewMode = values[1] as SettingsRepository.VodViewMode,
            resumeMode = values[2] as SettingsRepository.ResumeMode,
            updateCheckOnStart = values[3] as Boolean,
            rememberSeriesCategory = values[4] as Boolean,
            rememberSeriesItem = values[5] as Boolean,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProductSettingsState())

    val epgRows: StateFlow<List<ProductEpgRow>> = combine(listOf<Flow<Any?>>(
        sourceIds,
        flow { while (true) { emit(System.currentTimeMillis()); delay(30_000) } },
    )) { values -> values[0] as List<Long> to (values[1] as Long) }
        .flatMapLatest { (ids, now) ->
            if (ids.isEmpty()) flowOf(emptyList()) else flow {
                val channels = channelDao.snapshotAll(ids, 40)
                emit(channels.mapNotNull { channel ->
                    val key = channel.epgChannelId?.trim()?.lowercase()?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                    ProductEpgRow(channel, epgDao.nowPlaying(key, now), epgDao.upcoming(key, now, 4).first())
                })
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _epgRefreshing = MutableStateFlow(false)
    val epgRefreshing: StateFlow<Boolean> = _epgRefreshing

    fun setQuery(value: String) { _query.value = value }
    fun clearSearch() { _query.value = "" }
    fun selectSeries(series: SeriesEntity) { _selectedSeries.value = series; _selectedSeasonId.value = null; viewModelScope.launch { seriesRepository.loadEpisodes(series) } }
    fun closeSeries() { _selectedSeries.value = null; _selectedSeasonId.value = null }
    fun selectSeason(season: SeasonEntity?) { _selectedSeasonId.value = season?.id }
    fun refreshEpg() = viewModelScope.launch { if (_epgRefreshing.value) return@launch; _epgRefreshing.value = true; try { val profileId = settings.activeProfileId.first(); if (profileId >= 0L) sourceDao.observeForProfile(profileId).first().forEach { source -> runCatching { epgRepository.refresh(source) } } } finally { _epgRefreshing.value = false } }
    fun setVodViewMode(mode: SettingsRepository.VodViewMode) = viewModelScope.launch { settings.setVodViewMode(mode) }
    fun setEpisodeViewMode(mode: SettingsRepository.VodViewMode) = viewModelScope.launch { settings.setEpisodeViewMode(mode) }
    fun setResumeMode(mode: SettingsRepository.ResumeMode) = viewModelScope.launch { settings.setResumeMode(mode) }
    fun setUpdateCheckOnStart(enabled: Boolean) = viewModelScope.launch { settings.setUpdateCheckOnStart(enabled) }
    fun setRememberSeriesCategory(enabled: Boolean) = viewModelScope.launch { settings.setRememberCategorySeries(enabled) }
    fun setRememberSeriesItem(enabled: Boolean) = viewModelScope.launch { settings.setRememberLastSeries(enabled) }
    suspend fun currentProfileId(): Long = settings.activeProfileId.first()
}
