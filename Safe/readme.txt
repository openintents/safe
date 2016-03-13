 ****************************************************************************
 * Copyright (C) 2007-2012 Steven Osborn - http://steven.bitsetters.com     *
 *                     and Randy McEoin (and others, see Help)              *
 *                                                                          *
 * Licensed under the Apache License, Version 2.0 (the "License");          *
 * you may not use this file except in compliance with the License.         *
 * You may obtain a copy of the License at                                  *
 *                                                                          *
 *      http://www.apache.org/licenses/LICENSE-2.0                          *
 *                                                                          *
 * Unless required by applicable law or agreed to in writing, software      *
 * distributed under the License is distributed on an "AS IS" BASIS,        *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. *
 * See the License for the specific language governing permissions and      *
 * limitations under the License.                                           *
 ****************************************************************************


OI Safe keeps all your private data encrypted.

The core application stores your passwords. Other applications can encrypt
or decrypt data, and get or set passwords by connecting to OI Safe.

To obtain the current release, visit
  http://www.openintents.org

----------------
release: 1.4.2
date: 2012-??-??

- For ICS and higher, no longer show screenshot on recent apps for PassView and PassEdit
- Fixed bug where master password would be required twice in order to open safe
- Internal changes to clean up code
- For ICS and higher, added decreasing progress bar to notification to indicate auto lock timeout

----------------
release: 1.4.1
date: 2012-08-01

- fix security hole for rooted devices

----------------
release: 1.4
date: 2012-07-16

- automatically open on-screen keyboard on "Master password" screen (issue 328, patch by Eldwin)
- clear master password if wrong (patch by Kumar Sukhani)
- empty password records not saved (patch by Eldwin)
- checks for empty website on Go button (patch by Kumar Sukhani) 
- corrected issue with Jelly Bean where wrong password entry was displayed
- corrected issue with Jelly Bean on Search activity where touching the Search button
  on the keyboard did not invoke the search
- internal changes working toward fragments use

----------------
release: 1.3
date: 2012-02-18

Thanks to Google Code-in for the following patches:
(see http://www.google-melange.com/gci/homepage/google/gci2011 )

- uses OI Filemanager for backup/restore/import/export file selection (issue 186, GCI patch by Matěj Konečný)
- added a timer to the logoff intent to show how long until the OI Safe service will be stopped (issue 175, GCI patch by Darriel)
- hardware keyboard filtering of categories and passwords (issue 292, GCI patch by Matěj Konečný)
- restore on first run is more insistent (issue 201, GCI patch by Matěj Konečný)
  
- new icons and translations by Google Code-in students

----------------
release: 1.2.9
date: 2011-05-28
- removed up/down swipe in PassView
- delete password from clipboard when logging out (issue 297, patch by Rachee Singh)

----------------
release: 1.2.8
date: 2011-02-05
- new application icon for Android 2.0 or higher.
- allow app installation on external storage (requires Android 2.2 or higher)
- support hardware search button if available
- search results now show category
- better handling of low memory

----------------
release: 1.2.7
date: 2011-01-25
- fixed bug that prevented to open search results (issue 311)
- further bug fixes (issue 312)

----------------
release: 1.2.6
date: 2011-01-20
- added auto backup
- fixed change master password bug
- swipe left/right or up/down to move from password
  to password.
- on-screen switch button to switch from numeric
  keypad mode.
- support small screens (issue 259)
- don't copy password if user name is copied (issue 291)
- fixed bugs in issues 281, 276
- support Android 2.3
- translations into various languages.

----------------
release: 1.2.5
date: 2010-04-03
- fixed latent service notification
- translations: Japanese, Occitan (post 1500), Romanian, 
  Russian

----------------
release: 1.2.4
date: 2009-11-27
- fixed WRITE_EXTERNAL_STORAGE permission
- translations: Spanish

----------------
release: 1.2.3
date: 2009-11-23
- add counts to Category List
- support small, normal, large screens

----------------
release: 1.2.2
date: 2009-10-29
- translations: French, German
- change in the autolocking methodology
- new Search feature
- integration of MyBackup Pro

----------------
release: 1.1.1
date: 2009-05-30

- Secure deletion of CSV files after import.
- Add support for file stream encryption,
  to be used by Obscura.
- Add Trivium stream cipher (estreamJ implementation).

----------------
release: 1.1.0
date: 2009-03-17

- New touch keypad to unlock screen.
- Fixed "salt" error.

----------------
release: 1.0.0
date: 2009-02-02

- First public release on Android SDK 1.0.

Features: 
- Store encrypted passwords in categories.
- Open intents: encypt, decrypt, get & set password.
