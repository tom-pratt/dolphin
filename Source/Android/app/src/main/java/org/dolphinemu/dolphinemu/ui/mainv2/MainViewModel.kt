package org.dolphinemu.dolphinemu.ui.mainv2

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.dolphinemu.dolphinemu.services.GameFileCacheManager
import org.dolphinemu.dolphinemu.ui.platform.Platform

class MainViewModel : ViewModel() {

//    val gameFiles = GameFileCacheManager.getGameFiles().asFlow()
//        .map { it.toList() }
//        .stateIn(
//            viewModelScope,
//            SharingStarted.WhileSubscribed(),
//            GameFileCacheManager.getGameFiles().value?.toList() ?: emptyList()
//        )

    val gameFiles = GameFileCacheManager.getGameFiles().asFlow()
        .map { games ->
            games.groupBy { game -> Platform.fromInt(game.getPlatform()).toPlatformTab() }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyMap())

    init {
        GameFileCacheManager.startLoad()
    }

    class Factory(
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel() as T
        }
    }
}
