package com.bits.app

import android.content.Context
import android.content.Intent
import android.net.Uri

/** Intents the widget uses to open the app at the right place. */
object Launch {
    const val EXTRA_CATEGORY = "com.bits.app.extra.CATEGORY"
    const val EXTRA_SETTINGS = "com.bits.app.extra.SETTINGS"

    private fun base(context: Context, data: String): Intent =
        Intent(context, MainActivity::class.java)
            // A unique data URI keeps each tap target distinct from the others.
            .setData(Uri.parse(data))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)

    fun category(context: Context, categoryId: String): Intent =
        base(context, "bits://category/" + Uri.encode(categoryId))
            .putExtra(EXTRA_CATEGORY, categoryId)

    fun settings(context: Context): Intent =
        base(context, "bits://settings")
            .putExtra(EXTRA_SETTINGS, true)
}
