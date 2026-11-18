package com.jaiho.sss.`in`.ui.overlay

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.ViewTreeLifecycleOwner
import androidx.lifecycle.ViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.ViewTreeSavedStateRegistryOwner

class OverlayOwner : LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private val registry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val controller = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = registry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = controller.savedStateRegistry

    fun create() {
        try {
            controller.performRestore(null)
            registry.currentState = Lifecycle.State.RESUMED
        } catch (_: Throwable) {
            // an overlay without life-cycle plumbing still draws
        }
    }

    fun destroy() {
        try {
            registry.currentState = Lifecycle.State.DESTROYED
            store.clear()
        } catch (_: Throwable) {
            // ignore
        }
    }
}

fun Context.createOverlayView(
    owner: OverlayOwner,
    content: @Composable () -> Unit
): ComposeView {
    val view = ComposeView(this)
    ViewTreeLifecycleOwner.set(view, owner)
    ViewTreeViewModelStoreOwner.set(view, owner)
    ViewTreeSavedStateRegistryOwner.set(view, owner)
    view.setContent(content)
    return view
}
