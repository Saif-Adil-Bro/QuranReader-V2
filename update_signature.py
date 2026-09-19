import re

with open('app/src/main/java/com/example/ui/components/PrayerSunPathCard.kt', 'r') as f:
    text = f.read()

# Update signature
old_sig = """fun PrayerSunPathCard(
    schedule: DailyPrayerSchedule,
    modifier: Modifier = Modifier,
    onDetailsClick: () -> Unit = {},
    onNotificationClick: () -> Unit = {}
) {"""

new_sig = """fun PrayerSunPathCard(
    schedule: DailyPrayerSchedule,
    modifier: Modifier = Modifier,
    onDetailsClick: () -> Unit = {},
    notificationStates: Map<com.example.data.model.PrayerName, Boolean> = emptyMap(),
    onToggleNotification: (com.example.data.model.PrayerName, Boolean) -> Unit = { _, _ -> }
) {"""
text = text.replace(old_sig, new_sig)

# Remove local var
text = re.sub(r'\s*var isNotificationOn by remember \{ mutableStateOf\(true\) \}\n', '\n', text)

# Update next prayer block
text = re.sub(r'var isNotificationOn = false // we will compute it', '', text) # just in case

# Find the next prayer block computation
# We need to compute `isNotificationOn` based on `next.iconType` and `notificationStates`
# In the `Row` where the notification toggle is:
old_toggle_logic = """                        // Right: Notification Toggle
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                            modifier = Modifier.clickable { 
                                isNotificationOn = !isNotificationOn
                                onNotificationClick()
                            }
                        ) {"""

new_toggle_logic = """                        // Right: Notification Toggle
                        val isNotificationOn = notificationStates[next.iconType] ?: true
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                            modifier = Modifier.clickable { 
                                onToggleNotification(next.iconType, !isNotificationOn)
                            }
                        ) {"""
text = text.replace(old_toggle_logic, new_toggle_logic)

with open('app/src/main/java/com/example/ui/components/PrayerSunPathCard.kt', 'w') as f:
    f.write(text)

with open('app/src/main/java/com/example/ui/components/PrayerTimesDetailSheet.kt', 'r') as f:
    sheet_text = f.read()

# Find the call to PrayerSunPathCard
old_call = """            if (isToday) {
                com.example.ui.components.PrayerSunPathCard(
                    schedule = activeSchedule,
                    onDetailsClick = { /* Already in detail view */ }
                )
                Spacer(modifier = Modifier.height(14.dp))
            }"""

new_call = """            if (isToday) {
                com.example.ui.components.PrayerSunPathCard(
                    schedule = activeSchedule,
                    onDetailsClick = { /* Already in detail view */ },
                    notificationStates = mapOf(
                        com.example.data.model.PrayerName.FAJR to isNotifFajr,
                        com.example.data.model.PrayerName.DHUHR to isNotifDhuhr,
                        com.example.data.model.PrayerName.ASR to isNotifAsr,
                        com.example.data.model.PrayerName.MAGHRIB to isNotifMaghrib,
                        com.example.data.model.PrayerName.ISHA to isNotifIsha,
                        com.example.data.model.PrayerName.SAHRI to isNotifSahri,
                        com.example.data.model.PrayerName.IFTAR to isNotifIftar
                    ),
                    onToggleNotification = { prayerName, isEnabled ->
                        com.example.utils.PrayerNotificationHelper.setPrayerEnabled(context, prayerName, isEnabled)
                        when (prayerName) {
                            com.example.data.model.PrayerName.FAJR -> isNotifFajr = isEnabled
                            com.example.data.model.PrayerName.DHUHR -> isNotifDhuhr = isEnabled
                            com.example.data.model.PrayerName.ASR -> isNotifAsr = isEnabled
                            com.example.data.model.PrayerName.MAGHRIB -> isNotifMaghrib = isEnabled
                            com.example.data.model.PrayerName.ISHA -> isNotifIsha = isEnabled
                            com.example.data.model.PrayerName.SAHRI -> isNotifSahri = isEnabled
                            com.example.data.model.PrayerName.IFTAR -> isNotifIftar = isEnabled
                        }
                    }
                )
                Spacer(modifier = Modifier.height(14.dp))
            }"""

sheet_text = sheet_text.replace(old_call, new_call)

with open('app/src/main/java/com/example/ui/components/PrayerTimesDetailSheet.kt', 'w') as f:
    f.write(sheet_text)

