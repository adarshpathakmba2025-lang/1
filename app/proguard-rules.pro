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

# Play Billing. The library ships its own consumer rules, so this is belt and braces for
# the classes R8 can't see being used while Monetization.ENABLED is false - without it,
# a release build could strip the billing code as dead, and switching Pro on later would
# fail only in release, which is the worst place to find out.
-keep class com.bits.app.billing.** { *; }
-keep class com.bits.app.data.Monetization { *; }
