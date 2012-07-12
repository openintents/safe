/* ClipboardManager.java
 * 
 * PROGRAMMER:    Jeffrey T. Darlington
 * DATE:          June 17, 2011
 * PROJECT:       Perfect Paper Passwords for Android
 * ANDROID V.:	  1.1
 * REQUIRES:      
 * REQUIRED BY:   
 * 
 * This class provides a basic wrapper around the ClipboardManager object
 * provided by Android.  In Android 3.0 (Honeycomb, API 11), the older
 * android.text.ClipboardManager object was deprecated in favor of the
 * new android.content.ClipboardManager object, which supports more
 * types of clipboard data than just text.  Unfortunately, this means that
 * our old code now begins to throw warnings due to the deprecated code.
 * 
 * Since we don't need must in the way of functionality for our clipboard
 * in PPP (we'll only be putting text there, not reading it back or
 * putting anything else), this class is pretty simple.  The abstract
 * wrapper provides a newInstance() method that returns the appropriate
 * ClipboardManager object for the API level we are currently running under.
 * Otherwise, it provides only one other abstract method, setText(), which
 * the subclasses must implement.  Each subclass uses the methods appropriate
 * for the API to copy the supplied text to the clipboard.
 * 
 * This is based heavily on a blog post from the official Android developer's
 * blog, found here:
 * http://android-developers.blogspot.com/2010/07/how-to-have-your-cupcake-and-eat-it-too.html
 * 
 * This program is Copyright 2011, Jeffrey T. Darlington.
 * E-mail:  android_apps@gpf-comics.com
 * Web:     https://code.google.com/p/android-ppp/
 * 
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this
 * program.  If not, see http://www.gnu.org/licenses/.
*/
package org.openintents.safe.wrappers.honeycomb;

import android.support.v2.os.Build;
import android.annotation.TargetApi;
import android.app.Application;
import android.content.ClipData;

/**
 * This class provides a basic wrapper around the built-in ClipboardManager
 * class that manages copying data to and from the system clipboard.  This
 * provides a wrapper around the API-specific versions of the class to return
 * the proper object for the platform we're currently running on.
 * 
 * Originally from https://code.google.com/p/android-ppp/
 * Refactored by Randy McEoin
 *
 * @author Jeffrey T. Darlington
 */
public abstract class ClipboardManager {

	/** A reference to our calling application.  We need this to get the
	 *  context and thus the system clipboard services. */
	protected static Application theApp;

	/**
	 * Sets the contents of the clipboard to the specified text.
	 * @param text The text to place on the clipboard
	 */
	public abstract void setText(CharSequence text);

	public abstract boolean hasText();

	public abstract CharSequence getText();

	/**
	 * Get the appropriate instance of the clipboard manager for the
	 * current Android platform.
	 * @param app A reference to the calling application
	 * @return The clipboard manager for the current platform.
	 */
	public static ClipboardManager newInstance(Application app)
	{
		// Take note of the app:
		theApp = app;
		// Get the integer version of the current API number:
		final int sdkVersion = Build.VERSION.SDK_INT;
		// If the API number is less than Honeycomb (Android 3.0, or API 11),
		// return the old clipboard manager.  Otherwise, get the newer version.
		// This should be safe because the compiler hard-codes the version
		// code during compilation.
		if (sdkVersion < Build.VERSION_CODES.HONEYCOMB)
			return new OldClipboardManager();
		else return new HoneycombClipboardManager();
	}
	
	/**
	 * The old ClipboardManager, which is a under android.text.  This is
	 * the version to use for all Android versions less than 3.0. 
	 * 
	 * @author Jeffrey T. Darlington
	 */
	private static class OldClipboardManager extends ClipboardManager {
		
		/** The actual ClipboardManager object */
		@SuppressWarnings("deprecation")
		private static android.text.ClipboardManager clippy = null;
		
		/** Our constructor */
		@SuppressWarnings("deprecation")
		public OldClipboardManager()
		{
			clippy = (android.text.ClipboardManager)theApp.getSystemService(
					android.content.Context.CLIPBOARD_SERVICE);
		}
		
		@SuppressWarnings("deprecation")
		@Override
		public void setText(CharSequence text)
		{
			clippy.setText(text);
		}

		@SuppressWarnings("deprecation")
		@Override
		public boolean hasText()
		{
			return clippy.hasText();
		}

		@SuppressWarnings("deprecation")
		@Override
		public CharSequence getText()
		{
			return clippy.getText();
		}

	}
	
	/**
	 * The Honeycomb-and-up version of the clipboard manager, this time derived from
	 * android.content.  This version technically supports more content types than
	 * just text, but we frankly don't care about that in PPP.  We just want to make
	 * sure that when the deprecated android.text.ClipboardManager class finally goes
	 * away, our application won't break.
	 * 
	 * @author Jeffrey T. Darlington
	 */
	@TargetApi(11) private static class HoneycombClipboardManager extends ClipboardManager {
		
		/** The actual ClipboardManager object */
		private static android.content.ClipboardManager clippy = null;
		/** The ClipData object into which we'll put the text */
		private static android.content.ClipData clipData = null;
		
		/** Our constructor */
		public HoneycombClipboardManager()
		{
			clippy = (android.content.ClipboardManager)theApp.getSystemService(
					android.content.Context.CLIPBOARD_SERVICE);
		}
		
		@Override
		public void setText(CharSequence text)
		{
			clipData = android.content.ClipData.newPlainText(
					android.content.ClipDescription.MIMETYPE_TEXT_PLAIN, text);
			clippy.setPrimaryClip(clipData);
		}
		
		@Override
		public boolean hasText()
		{
			return clippy.hasPrimaryClip();
		}

		@Override
		public CharSequence getText()
		{
			if (clippy.hasPrimaryClip()) {
				ClipData.Item item = clippy.getPrimaryClip().getItemAt(0);
				CharSequence pasteData = item.getText();
				if (pasteData==null) {
					pasteData="";
				}
				return pasteData;
			} else {
				return "";
			}
		}

	}

}