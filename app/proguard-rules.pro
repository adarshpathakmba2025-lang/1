# Bits has no reflection, no serialization library, and no dynamic class loading, so the
# default optimised rules cover almost everything. Compose, Glance and Billing all ship
# their own consumer rules inside their AARs, which R8 applies automatically.

# Keep the widget receiver and activities referenced only from the manifest. R8 already
# keeps manifest-declared components, but naming them makes the intent explicit and
# survives future manifest edits.
-keep class com.bits.app.widget.BitsWidgetReceiver { *; }
-keep class com.bits.app.time.DayChangeReceiver { *; }
-keep class com.bits.app.MainActivity { *; }
-keep class com.bits.app.QuickEditActivity { *; }
-keep class com.bits.app.ReorderActivity { *; }

# The Glance widget class is instantiated by the framework from the receiver.
-keep class com.bits.app.widget.BitsWidget { *; }

# Action callbacks are looked up by class name by Glance.
-keep class com.bits.app.widget.ToggleItemAction { *; }
