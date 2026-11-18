package com.jaiho.sss.`in`.ui.overlay

import android.content.Context
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import com.jaiho.sss.`in`.util.L
import java.lang.reflect.Method
import java.lang.reflect.Modifier

/**
 * A stand-in owner for the floating window so Compose has a life-cycle, a view-model store
 * and a saved-state registry. Needed because the overlay is not hosted by an Activity.
 */
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
        } catch (t: Throwable) {
            L.e("overlay owner create failed", t)
        }
    }

    fun destroy() {
        try {
            registry.currentState = Lifecycle.State.DESTROYED
            store.clear()
        } catch (t: Throwable) {
            L.e("overlay owner destroy failed", t)
        }
    }
}

/**
 * These three helpers moved between packages and between Kotlin and Java across library
 * versions, so they are looked up by name at runtime and every candidate is tried.
 */
private val LIFECYCLE_OWNER_CLASSES = listOf(
    "androidx.lifecycle.ViewTreeLifecycleOwner",
    "androidx.lifecycle.runtime.ViewTreeLifecycleOwner"
)

private val VIEW_MODEL_OWNER_CLASSES = listOf(
    "androidx.lifecycle.ViewTreeViewModelStoreOwner",
    "androidx.lifecycle.viewmodel.ViewTreeViewModelStoreOwner"
)

private val SAVED_STATE_OWNER_CLASSES = listOf(
    "androidx.savedstate.ViewTreeSavedStateRegistryOwner",
    "androidx.savedstate.runtime.ViewTreeSavedStateRegistryOwner"
)

private fun objectOf(clazz: Class<*>): Any? {
    // Kotlin singletons expose INSTANCE; plain Java classes are instantiated instead.
    try {
        return clazz.getField("INSTANCE").get(null)
    } catch (_: Throwable) {
        // fall through
    }
    return try {
        clazz.getDeclaredConstructor().newInstance()
    } catch (_: Throwable) {
        null
    }
}

private fun attachClass(candidate: String, view: View, owner: Any): Boolean {
    val clazz = try {
        Class.forName(candidate)
    } catch (_: Throwable) {
        return false
    }
    val method: Method = try {
        clazz.methods.firstOrNull { m ->
            m.name == "set" &&
                m.parameterTypes.size == 2 &&
                m.parameterTypes[0].isAssignableFrom(View::class.java) &&
                m.parameterTypes[1].isInstance(owner)
        } ?: return false
    } catch (_: Throwable) {
        return false
    }
    val static = Modifier.isStatic(method.modifiers)
    val target = if (static) null else objectOf(clazz)
    if (!static && target == null) return false
    return try {
        method.isAccessible = true
        method.invoke(target, view, owner)
        method.isAccessible = false
        true
    } catch (_: Throwable) {
        false
    }
}

private fun attach(view: View, candidates: List<String>, owner: Any): Boolean {
    for (candidate in candidates) {
        try {
            if (attachClass(candidate, view, owner)) return true
        } catch (_: Throwable) {
            // keep trying the other names
        }
    }
    return false
}

fun Context.createOverlayView(
    owner: OverlayOwner,
    content: @Composable () -> Unit
): ComposeView {
    val view = ComposeView(this)
    try {
        if (!attach(view, LIFECYCLE_OWNER_CLASSES, owner)) {
            L.e("life-cycle owner could not be attached to the overlay view", null)
        }
        attach(view, VIEW_MODEL_OWNER_CLASSES, owner)
        attach(view, SAVED_STATE_OWNER_CLASSES, owner)
    } catch (t: Throwable) {
        L.e("overlay owner attach failed", t)
    }
    view.setContent(content)
    return view
}
