 ****************************************************************************
 * Copyright (C) 2007-2011 Steven Osborn - http://steven.bitsetters.com     *
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

---------------------------------------------------------
release: 1.2.9
date: 2011-??-??
- removed up/down swipe in PassView
- delete password from clipboard when logging out
  (issue 297, patch by Rachee Singh)

---------------------------------------------------------
release: 1.2.8
date: 2011-02-05
- new application icon for Android 2.0 or higher.
- allow app installation on external storage (requires Android 2.2 or higher)
- support hardware search button if available
- search results now show category
- better handling of low memory

---------------------------------------------------------
release: 1.2.7
date: 2011-01-25
- fixed bug that prevented to open search results
  (issue 311)
- further bug fixes (issue 312)

---------------------------------------------------------
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

---------------------------------------------------------
release: 1.2.5
date: 2010-04-03
- fixed latent service notification
- translations: Japanese, Occitan (post 1500), Romanian, 
  Russian

---------------------------------------------------------
release: 1.2.4
date: 2009-11-27
- fixed WRITE_EXTERNAL_STORAGE permission
- translations: Spanish

---------------------------------------------------------
release: 1.2.3
date: 2009-11-23
- add counts to Category List
- support small, normal, large screens

---------------------------------------------------------
release: 1.2.2
date: 2009-10-29
- translations: French, German
- change in the autolocking methodology
- new Search feature
- integration of MyBackup Pro

---------------------------------------------------------
release: 1.1.1
date: 2009-05-30

- Secure deletion of CSV files after import.
- Add support for file stream encryption,
  to be used by Obscura.
- Add Trivium stream cipher (estreamJ implementation).

---------------------------------------------------------
release: 1.1.0
date: 2009-03-17

- New touch keypad to unlock screen.
- Fixed "salt" error.

---------------------------------------------------------
release: 1.0.0
date: 2009-02-02

- First public release on Android SDK 1.0.

Features: 
- Store encrypted passwords in categories.
- Open intents: encypt, decrypt, get & set password.
