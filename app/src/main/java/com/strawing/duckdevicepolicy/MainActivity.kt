package com.strawing.duckdevicepolicy

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Typeface
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.google.android.material.materialswitch.MaterialSwitch
import io.github.libxposed.service.XposedService

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var masterSwitch: MaterialSwitch
    private lateinit var statusText: TextView
    private lateinit var categoryContainer: LinearLayout
    private lateinit var scopeHint: TextView
    private val categorySwitches = LinkedHashMap<String, MaterialSwitch>()

    /** True when [prefs] is the framework's remote store rather than the local fallback. */
    private var usingRemotePrefs = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_main)
        applyInsets()

        prefs = resolvePrefs()
        masterSwitch = findViewById(R.id.masterSwitch)
        statusText = findViewById(R.id.statusText)
        categoryContainer = findViewById(R.id.categoryContainer)
        scopeHint = findViewById(R.id.scopeHint)

        masterSwitch.isChecked = prefs.getBoolean(Prefs.KEY_MASTER, true)
        masterSwitch.setOnCheckedChangeListener { _, checked ->
            prefs.edit()?.putBoolean(Prefs.KEY_MASTER, checked)?.apply()
            updateStatus(checked)
            setCategoriesEnabled(checked)
        }

        buildCategoryToggles()
        findViewById<TextView>(R.id.selectAll).setOnClickListener { setAllCategories(true) }
        findViewById<TextView>(R.id.selectNone).setOnClickListener { setAllCategories(false) }

        updateStatus(masterSwitch.isChecked)
        setCategoriesEnabled(masterSwitch.isChecked)
        updateScopeHint()
    }

    // ------------------------------------------------------- framework service (libxposed)

    /**
     * The framework binder arrives asynchronously and may land after this activity is built,
     * which would leave the UI editing the local fallback file while the hook reads the remote
     * store. Rebuild once when it shows up (or goes away).
     */
    private val serviceListener: (XposedService?) -> Unit = { svc ->
        runOnUiThread {
            if ((svc != null) != usingRemotePrefs && !isFinishing) recreate()
        }
    }

    override fun onStart() {
        super.onStart()
        App.addListener(serviceListener)
    }

    override fun onStop() {
        App.removeListener(serviceListener)
        super.onStop()
    }

    /**
     * Settings live in the framework's remote preferences, which is how the hook reads them
     * from inside system_server and every scoped app. This replaces the old
     * MODE_WORLD_READABLE file that XSharedPreferences used to redirect: a modern module gets
     * no such redirect, so that file would be written and never read. Falling back to a local
     * file keeps the screen usable when the framework is absent — nothing is hooked in that
     * state anyway, so the values would have no effect either way.
     */
    private fun resolvePrefs(): SharedPreferences {
        App.service?.let { svc ->
            runCatching { svc.getRemotePreferences(Prefs.NAME) }.getOrNull()?.let { remote ->
                usingRemotePrefs = true
                importLegacyPrefs(remote)
                return remote
            }
        }
        usingRemotePrefs = false
        return getSharedPreferences(Prefs.NAME, Context.MODE_PRIVATE)
    }

    /**
     * Carry 3.x settings across once. Those lived in a MODE_WORLD_READABLE file that the old
     * framework redirected out of the app's data dir, so the read may legitimately find nothing
     * and the user simply starts from defaults. Best effort by design — never let a failed
     * import block the UI.
     */
    private fun importLegacyPrefs(remote: SharedPreferences) {
        if (remote.getBoolean(Prefs.KEY_IMPORTED, false)) return
        @Suppress("DEPRECATION")
        val legacy = runCatching {
            getSharedPreferences(Prefs.NAME, Context.MODE_WORLD_READABLE)
        }.getOrElse {
            runCatching { getSharedPreferences(Prefs.NAME, Context.MODE_PRIVATE) }.getOrNull()
        }
        val edit = remote.edit() ?: return
        legacy?.let {
            for (key in Prefs.allKeys()) {
                if (it.contains(key)) edit.putBoolean(key, it.getBoolean(key, true))
            }
        }
        edit.putBoolean(Prefs.KEY_IMPORTED, true).apply()
    }

    /**
     * DuckPolicy's own LSPosed scope, straight from the framework. The legacy API could not
     * read this at all, so the hint below could only ever be generic advice.
     */
    private val moduleScope: List<String>? by lazy {
        runCatching { App.service?.scope }.getOrNull()
    }

    private fun updateScopeHint() {
        val scope = moduleScope
        scopeHint.text = when {
            !usingRemotePrefs -> getString(R.string.hint_no_framework)
            scope.isNullOrEmpty() -> getString(R.string.hint_empty_scope)
            else -> getString(R.string.hint_scope, scope.joinToString(", "))
        }
    }

    /** Edge-to-edge: pad the content past the status/navigation bars. */
    private fun applyInsets() {
        val content = findViewById<View>(R.id.content)
        val baseTop = content.paddingTop
        val baseBottom = content.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(content) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(top = baseTop + bars.top, bottom = baseBottom + bars.bottom)
            insets
        }
    }

    private fun buildCategoryToggles() {
        val dp = resources.displayMetrics.density
        val padH = (18 * dp).toInt()
        val padV = (13 * dp).toInt()
        val secondary = ContextCompat.getColor(this, R.color.text_secondary)
        val rippleBg = resolveAttr(android.R.attr.selectableItemBackground)

        Restrictions.CATEGORIES.forEachIndexed { index, cat ->
            if (index > 0) categoryContainer.addView(divider(dp, padH))

            val sw = MaterialSwitch(this).apply {
                isChecked = prefs.getBoolean(Prefs.key(cat.key), Prefs.CATEGORY_DEFAULT)
                isClickable = false          // the whole row is the touch target
                isFocusable = false
            }
            categorySwitches[cat.key] = sw

            val texts = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            texts.addView(TextView(this).apply {
                text = cat.title
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 15.5f)
                setTypeface(typeface, Typeface.BOLD)
            })
            texts.addView(TextView(this).apply {
                text = cat.subtitle
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
                setTextColor(secondary)
                setPadding(0, (2 * dp).toInt(), 0, 0)
            })

            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(padH, padV, padH, padV)
                isClickable = true
                setBackgroundResource(rippleBg)
                setOnClickListener {
                    if (!masterSwitch.isChecked) return@setOnClickListener
                    val nv = !sw.isChecked
                    sw.isChecked = nv
                    prefs.edit()?.putBoolean(Prefs.key(cat.key), nv)?.apply()
                }
            }
            row.addView(texts)
            row.addView(sw)
            categoryContainer.addView(row)
        }
    }

    private fun setAllCategories(value: Boolean) {
        if (!masterSwitch.isChecked) return
        val e = prefs.edit() ?: return
        for ((key, sw) in categorySwitches) {
            sw.isChecked = value
            e.putBoolean(Prefs.key(key), value)
        }
        e.apply()
    }

    private fun divider(dp: Float, insetH: Int) = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, (1 * dp).toInt()
        ).apply { setMargins(insetH, 0, insetH, 0) }
        setBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.divider))
    }

    private fun setCategoriesEnabled(enabled: Boolean) {
        categoryContainer.alpha = if (enabled) 1f else 0.4f
        for (i in 0 until categoryContainer.childCount) {
            categoryContainer.getChildAt(i).isEnabled = enabled
        }
        categorySwitches.values.forEach { it.isEnabled = enabled }
    }

    private fun updateStatus(active: Boolean) {
        if (active) {
            statusText.setText(R.string.status_active)
            statusText.setTextColor(ContextCompat.getColor(this, R.color.status_active))
        } else {
            statusText.setText(R.string.status_inactive)
            statusText.setTextColor(ContextCompat.getColor(this, R.color.status_inactive))
        }
    }

    private fun resolveAttr(attr: Int): Int {
        val tv = TypedValue()
        theme.resolveAttribute(attr, tv, true)
        return tv.resourceId
    }
}
