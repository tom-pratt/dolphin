package org.dolphinemu.dolphinemu

import android.content.Intent
import android.os.ParcelFileDescriptor
import android.os.ParcelFileDescriptor.MODE_READ_ONLY
import com.gameswitchlauncher.common.Game
import com.gameswitchlauncher.common.SaveState
import com.gameswitchlauncher.provider.GameSwitchProvider
import org.dolphinemu.dolphinemu.activities.EmulationActivity
import org.dolphinemu.dolphinemu.activities.EmulationActivity.Companion.EXTRA_RIIVOLUTION
import org.dolphinemu.dolphinemu.activities.EmulationActivity.Companion.EXTRA_SELECTED_GAMES
import org.dolphinemu.dolphinemu.activities.EmulationActivity.Companion.EXTRA_STATE_SAVE_SLOT
import org.dolphinemu.dolphinemu.services.GameFileCacheManager
import org.dolphinemu.dolphinemu.ui.platform.Platform
import org.dolphinemu.dolphinemu.utils.CoilUtils
import org.dolphinemu.dolphinemu.utils.DirectoryInitialization
import java.io.File


class GameCubeProvider : GameSwitchProvider() {
    override fun onCreate(): Boolean {
        return true
    }

    override fun getGames(): List<Game> {
        return GameFileCacheManager.getGameFilesForPlatform(Platform.GAMECUBE)
            .map { Game(it.gameId, it.title) }
    }

    override fun getSaveStates(gameId: String): List<SaveState> {
        val saveState =
            File("${DirectoryInitialization.getUserDirectory()}/StateSaves/${gameId}.s09")
        return if (saveState.exists()) {
            listOf(SaveState(saveState.absolutePath, saveState.lastModified()))
        } else {
            emptyList()
        }
    }

    override fun getBoxArt(gameId: String): ParcelFileDescriptor {
        val game = GameFileCacheManager.getGameFilesForPlatform(Platform.GAMECUBE)
            .first { it.gameId == gameId }

        val customCoverUri = CoilUtils.findCustomCover(game)
        return context!!.contentResolver.openFileDescriptor(customCoverUri!!, "r", null)!!
    }

    override fun getSaveStateScreenshot(saveStateId: String): ParcelFileDescriptor {
        val screenshot = File("$saveStateId.png")
        return ParcelFileDescriptor.open(screenshot, MODE_READ_ONLY);
    }

    override fun getLaunchIntent(gameId: String, saveStateId: String?): Intent {
        val game = GameFileCacheManager.getGameFilesForPlatform(Platform.GAMECUBE)
            .first { it.gameId == gameId }

        return Intent(context, EmulationActivity::class.java).apply {
            putExtra(EXTRA_SELECTED_GAMES, arrayOf(game.path))
            putExtra(EXTRA_RIIVOLUTION, false)
            saveStateId?.let { putExtra(EXTRA_STATE_SAVE_SLOT, it.last().digitToInt()) }
        }
    }
}