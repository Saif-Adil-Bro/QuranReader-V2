import re

with open('app/src/main/java/com/example/ui/components/PrayerTimesDetailSheet.kt', 'r') as f:
    text = f.read()

old_when = """                        when (prayerName) {
                            com.example.data.model.PrayerName.FAJR -> isNotifFajr = isEnabled
                            com.example.data.model.PrayerName.DHUHR -> isNotifDhuhr = isEnabled
                            com.example.data.model.PrayerName.ASR -> isNotifAsr = isEnabled
                            com.example.data.model.PrayerName.MAGHRIB -> isNotifMaghrib = isEnabled
                            com.example.data.model.PrayerName.ISHA -> isNotifIsha = isEnabled
                            com.example.data.model.PrayerName.SAHRI -> isNotifSahri = isEnabled
                            com.example.data.model.PrayerName.IFTAR -> isNotifIftar = isEnabled
                        }"""

new_when = """                        when (prayerName) {
                            com.example.data.model.PrayerName.FAJR -> isNotifFajr = isEnabled
                            com.example.data.model.PrayerName.DHUHR -> isNotifDhuhr = isEnabled
                            com.example.data.model.PrayerName.ASR -> isNotifAsr = isEnabled
                            com.example.data.model.PrayerName.MAGHRIB -> isNotifMaghrib = isEnabled
                            com.example.data.model.PrayerName.ISHA -> isNotifIsha = isEnabled
                            com.example.data.model.PrayerName.SAHRI -> isNotifSahri = isEnabled
                            com.example.data.model.PrayerName.IFTAR -> isNotifIftar = isEnabled
                            else -> { /* No-op */ }
                        }"""

text = text.replace(old_when, new_when)

with open('app/src/main/java/com/example/ui/components/PrayerTimesDetailSheet.kt', 'w') as f:
    f.write(text)

