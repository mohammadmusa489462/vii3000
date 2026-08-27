Android's res/raw folder can't actually contain sub-folders (a flat
namespace is a platform rule, not a choice made here) - so "separate
folders" below means separate NAME PREFIXES instead. Same effect,
different mechanism.

=== Athan (full audio at each prayer time) ===
  - One file for every prayer:      athan.mp3
  - A different file per prayer:    athan_fajr.mp3, athan_dhuhr.mp3,
                                     athan_asr.mp3, athan_maghrib.mp3,
                                     athan_isha.mp3
    (useful since Fajr's athan traditionally has an extra phrase)
    Per-prayer files are checked first; athan.mp3 is the fallback.

  Athan now plays via a proper foreground service, so it plays to full
  completion even for long recordings - it no longer gets cut short.

A GENUINELY FREE, PUBLIC-DOMAIN-LICENSED SOURCE:
    https://archive.org/details/adhan.recordings.from.doha.qatar
    (marked "Public Domain Mark 1.0"). Has separate Fajr/Dhuhr/Asr/
    Maghrib/Isha recordings already, matching the naming above.

=== Notification sounds - three categories per prayer ===

1) "ON TRACK" - the routine heads-up before this prayer starts, and the
   alert at its exact start time, whenever the prayer before it was
   already marked done:
        notification_fajr.mp3      notification_dhuhr.mp3
        notification_asr.mp3       notification_maghrib.mp3
        notification_isha.mp3

2) "MISSED" - the dedicated qada reminder (if you turned that on in
   Settings), warning that a specific prayer's make-up window is about
   to close:
        missed_fajr.mp3      missed_dhuhr.mp3
        missed_asr.mp3       missed_maghrib.mp3
        missed_isha.mp3

3) "REMINDER" - fires as part of the normal heads-up-before-next-prayer
   alert, but specifically when the PREVIOUS prayer hasn't been marked
   done yet - e.g. between Dhuhr and Asr, if Dhuhr isn't done, this
   fires (named after Dhuhr, the one it's actually about):
        reminder_fajr.mp3    reminder_dhuhr.mp3
        reminder_asr.mp3     reminder_maghrib.mp3
        reminder_isha.mp3

Example walking through Dhuhr -> Asr:
  - Dhuhr marked done  -> Asr's heads-up plays notification_asr.mp3
  - Dhuhr NOT done yet -> plays reminder_dhuhr.mp3 instead (about Dhuhr,
    not Asr - it's telling you what you're about to miss)

All go in this same folder (app/src/main/res/raw/). If you're not sure
this three-way split is exactly what you want, it's easy to collapse
back to two or one - just say so and it takes one line to change.

Fallback order (so nothing breaks with only some files added):
  category 1: its own file -> notification_sound.mp3 -> system default
  category 2: its own file -> notification_sound.mp3 -> system default
  category 3: its own file -> notification_sound.mp3 -> system default
  (Older file names from earlier instructions - notification_<id>.mp3,
  notification_<id>_missed.mp3, notification_reminder.mp3 - still work
  too, as additional fallbacks, so nothing you already added stops
  working.)

--- Filename rule for everything in this folder ---
Android resource files must be lowercase, letters/numbers/underscores
only - "Athan.MP3" or "athan-1.mp3" are NOT valid, "athan_1.mp3" is.

--- If you're changing sounds on an app you've already installed ---
Android locks a notification channel's sound once it's first created
on the device. If you add/change a sound file and reinstall but don't
hear the new sound, that's why - not a bug. Fully uninstalling the app
before reinstalling clears the old channels so new sounds take effect.
