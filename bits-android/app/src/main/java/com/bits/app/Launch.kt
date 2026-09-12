package com.bits.app

import android.content.Context
import android.content.Intent
import android.net.Uri

/** Intents the widget uses to open the app or a floating action at the right place. */
object Launch {
    const val EXTRA_CATEGORY = "com.bits.app.extra.CATEGORY"
    const val EXTRA_SETTINGS = "com.bits.app.extra.SETTINGS"
    const val EXTRA_GAMES = "com.bits.app.extra.GAMES"
    const val EXTRA_HOME = "com.bits.app.extra.HOME"
    const val EXTRA_ITEM_ID = "com.bits.app.extra.ITEM_ID"

    private fun base(context: Context, data: String): Intent =
        Intent(context, MainActivity::class.java)
            // A unique data URI keeps each tap target distinct from the others.
            .setData(Uri.parse(data))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)

    fun app(context: Context): Intent = base(context, "bits://app").putExtra(EXTRA_HOME, true)

    fun category(context: Context, categoryId: String): Intent =
        base(context, "bits://category/" + Uri.encode(categoryId))
            .putExtra(EXTRA_CATEGORY, categoryId)

    fun settings(context: Context): Intent =
        base(context, "bits://settings")
            .putExtra(EXTRA_SETTINGS, true)

    fun games(context: Context): Intent =
        base(context, "bits://games")
            .putExtra(EXTRA_GAMES, true)

    /** Opens the small floating card for editing one item, without launching the full app. */
    fun quickEdit(context: Context, itemId: String): Intent =
        Intent(context, QuickEditActivity::class.java)
            .setData(Uri.parse("bits://edit/" + Uri.encode(itemId)))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .putExtra(EXTRA_ITEM_ID, itemId)
}
