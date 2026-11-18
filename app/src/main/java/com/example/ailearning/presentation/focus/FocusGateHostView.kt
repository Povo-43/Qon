package com.example.ailearning.presentation.focus

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.AbstractComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner

/**
 * WindowManager に直接載せる Compose ホスト。
 *
 * ComposeView をオーバーレイに載せるには Lifecycle / SavedStateRegistry の
 * ViewTree オーナーを自前で供給する必要がある。
 */
class FocusGateHostView(context: Context) : AbstractComposeView(context) {

    private val hostLifecycleOwner = OverlayLifecycleOwner()
    private var gateContent: @Composable () -> Unit = {}

    init {
        setViewTreeLifecycleOwner(hostLifecycleOwner)
        setViewTreeViewModelStoreOwner(hostLifecycleOwner)
        setViewTreeSavedStateRegistryOwner(hostLifecycleOwner)
        hostLifecycleOwner.performCreate()
    }

    fun setGateContent(content: @Composable () -> Unit) {
        gateContent = content
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        hostLifecycleOwner.performStart()
        hostLifecycleOwner.performResume()
    }

    override fun onDetachedFromWindow() {
        hostLifecycleOwner.performPause()
        hostLifecycleOwner.performStop()
        super.onDetachedFromWindow()
    }

    fun destroyHost() {
        hostLifecycleOwner.performDestroy()
    }

    @Composable
    override fun Content() {
        gateContent()
    }
}

private class OverlayLifecycleOwner :
    LifecycleOwner,
    ViewModelStoreOwner,
    SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val store = ViewModelStore()

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    fun performCreate() {
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }

    fun performStart() = lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
    fun performResume() = lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun performPause() = lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun performStop() = lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun performDestroy() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()
    }
}
