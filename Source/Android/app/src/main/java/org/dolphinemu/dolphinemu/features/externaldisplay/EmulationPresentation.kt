// SPDX-License-Identifier: GPL-2.0-or-later

package org.dolphinemu.dolphinemu.features.externaldisplay

import android.app.Presentation
import android.content.Context
import android.os.Bundle
import android.view.Display
import android.view.SurfaceView
import org.dolphinemu.dolphinemu.R

class EmulationPresentation(context: Context, display: Display) : Presentation(context, display) {
    /**
     * The [SurfaceView] hosting the game render output on the external display. Available after
     * [show] has been called (which triggers [onCreate]).
     */
    val surfaceView: SurfaceView
        get() = findViewById(R.id.presentation_surface_emulation)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.presentation_emulation)
        surfaceView.holder.surface.release()
    }
}
