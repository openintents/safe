package org.openintents.safe;

/**
 * The main activity prior to version 1.2.4 was ".FrontDoor". Home screens
 * may still contain a direct link to the old activity, therefore this class
 * must never be renamed or moved.
 * 
 * Prior to version 1.3.1, activity-alias was used.  Unfortunately with that in use
 * the application would not automatically launch from Eclipse and would not debug
 * correctly.
 * 
 * This class is derived from .Safe which contains the actual
 * implementation.
*/
public class FrontDoor extends Safe {

}
