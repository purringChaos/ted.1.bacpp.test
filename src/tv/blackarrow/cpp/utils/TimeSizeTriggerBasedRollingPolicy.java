/*
 * -----------------------------------------------------------------------------
 *                 B L A C K A R R O W      I N C.
 * -----------------------------------------------------------------------------
 *
 * %W%\t%G%
 *
 * Copyright 2008 by BlackArrow Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information
 * of BlackArrow Inc. ("Confidential Information").  You
 * shall not disclose such Confidential Information and shall use
 * it only in accordance with the terms of the license agreement
 * you entered into with Black Arrow Inc.
 *
 * @author Tom Nevin (tnevin@blackarrow.tv)
 *
 * @version  %I%, %G%
 *
 * @note 
 *        
 * -----------------------------------------------------------------------------
 */

package tv.blackarrow.cpp.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;
import java.util.zip.Deflater;

import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.rolling.AbstractRolloverStrategy;
import org.apache.logging.log4j.core.appender.rolling.RollingFileManager;
import org.apache.logging.log4j.core.appender.rolling.RolloverDescription;
import org.apache.logging.log4j.core.appender.rolling.RolloverDescriptionImpl;
import org.apache.logging.log4j.core.appender.rolling.TriggeringPolicy;
import org.apache.logging.log4j.core.appender.rolling.action.Action;
import org.apache.logging.log4j.core.appender.rolling.action.CompositeAction;
import org.apache.logging.log4j.core.appender.rolling.action.FileRenameAction;
import org.apache.logging.log4j.core.appender.rolling.action.GzCompressAction;
import org.apache.logging.log4j.core.appender.rolling.action.ZipCompressAction;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.lookup.StrSubstitutor;


@Plugin(name = "TimeSizeTriggerBasedRollingPolicy", category = Core.CATEGORY_NAME, printObject = true)
public final class TimeSizeTriggerBasedRollingPolicy extends AbstractRolloverStrategy implements TriggeringPolicy {

	private static final String CURRENT_BA_STANDARD_ROLLOVER_FILE_PATTERN = "%i";

	/**
	 * Time for next determination if time for rollover.
	 */
	private long nextCheck = 0;

	/**
	 * File name at last rollover.
	 */
	private String lastFileName = null;

	/**
	 * Length of any file type suffix (.gz, .zip).
	 */
	private int suffixLength = 0;

	/**
	 * Rollover threshold size in bytes.
	 */
	private long maxFileSize = 10 * 1024 * 1024; // let 10 MB the default max

	/**
	 * Rollover threshold time in seconds
	 */
	private long maxTimeSecs = 24 * 60 * 60; // one d.

	/**
	 * The string to trigger a rollover on, if in message.
	 */
	private String stringToTrigger = null;

	private RollingFileManager manager;

	protected TimeSizeTriggerBasedRollingPolicy(StrSubstitutor strSubstitutor) {
		super(strSubstitutor);
	}

	protected TimeSizeTriggerBasedRollingPolicy() {
		this(new StrSubstitutor());
	}

	protected TimeSizeTriggerBasedRollingPolicy(long maxFileSize, long maxTimeSecs) {
		this();
		this.maxFileSize = maxFileSize;
		this.maxTimeSecs = maxTimeSecs;
	}

	@Override
	public void initialize(RollingFileManager paramRollingFileManager) {
		manager = paramRollingFileManager;
		manager.setRolloverStrategy(this);
		long n = getUTCCurrentTimeinMilliseconds(System.currentTimeMillis());
		nextCheck = n + (maxTimeSecs * 1000L);
		lastFileName = paramRollingFileManager.getPatternProcessor().getPattern()
				.replaceAll(CURRENT_BA_STANDARD_ROLLOVER_FILE_PATTERN, Long.toString(n));
		suffixLength = 0;
		if (lastFileName.endsWith(".gz")) {
			suffixLength = 3;
		} else if (lastFileName.endsWith(".zip")) {
			suffixLength = 4;
		}
	}

	@Override
	public RolloverDescription rollover(RollingFileManager paramRollingFileManager) throws SecurityException {
		long n = getUTCCurrentTimeinMilliseconds(System.currentTimeMillis());
		nextCheck = n + (maxTimeSecs * 1000L);

		String newFileName = paramRollingFileManager.getPatternProcessor().getPattern()
				.replaceAll(CURRENT_BA_STANDARD_ROLLOVER_FILE_PATTERN, Long.toString(n));

		Action renameAction = null;
		Action compressAction = null;

		// the last rename into the file name pattern.
		Action finalRenameAction = null;
		// the composite action containing compressAction and finalRenameAction.
		Action compressAndFinalRenameAction = null;

		lastFileName = newFileName; // BAS-16055 : as per HLD file should use Rollover time, to fix this issue, this
									// code is moved from below to here.

		String lastBaseName = lastFileName.substring(0, lastFileName.length() - suffixLength);

		// temp compressed file
		String tmpLastFileName = lastFileName + ".tmp";

		String nextActiveFile = newFileName.substring(0, newFileName.length() - suffixLength);

		//
		// if currentActiveFile is not lastBaseName then
		// active file name is not following file pattern
		// and requires a rename plus maintaining the same name
		final String currentActiveFile = paramRollingFileManager.getFileName();

		if (!currentActiveFile.equals(lastBaseName)) {
			renameAction = new FileRenameAction(new File(currentActiveFile), new File(lastBaseName), true);
			nextActiveFile = currentActiveFile;
		}

		if (suffixLength == 3) {
			compressAction = new GzCompressAction(new File(lastBaseName), new File(tmpLastFileName), true);
		}

		if (suffixLength == 4) {
			compressAction = new ZipCompressAction(new File(lastBaseName), new File(tmpLastFileName), true,
					Deflater.DEFAULT_COMPRESSION);
		}

		finalRenameAction = new FileRenameAction(new File(tmpLastFileName), new File(lastFileName), true);
		List<Action> compositeActionList = new ArrayList<Action>();
		compositeActionList.add(compressAction);
		compositeActionList.add(finalRenameAction);
		compressAndFinalRenameAction = new CompositeAction(compositeActionList, false);

		// lastFileName = newFileName; //BAS-16055 : as per HLD file should use Rollover
		// time, to fix this issue, we moved this code above

		return new RolloverDescriptionImpl(nextActiveFile, false, renameAction, compressAndFinalRenameAction);
	}

	@Override
	public boolean isTriggeringEvent(LogEvent paramLogEvent) {
		final long fileLength = manager.getFileSize();
		return (fileLength > 0 && (System.currentTimeMillis() >= nextCheck || fileLength >= maxFileSize));
	}
	
	/**
	 * Creates a TimeSizeTriggerBasedRollingPolicy.
	 * @param interval The interval between rollovers.
	 * @param modulate If true the time will be rounded to occur on a boundary aligned with the increment.
	 * @return a TimeSizeTriggerBasedRollingPolicy.
	 */
	@PluginFactory
	public static TimeSizeTriggerBasedRollingPolicy createPolicy(
	        @PluginAttribute("MaxFileSize") final String maxFileSize,
	        @PluginAttribute("MaxTimeSecs") final String maxTimeSecs) {
	    final long maxFileSizeL = Long.parseLong(maxFileSize);
	    final long maxTimeSecsL = Long.parseLong(maxTimeSecs);
	    return new TimeSizeTriggerBasedRollingPolicy(maxFileSizeL, maxTimeSecsL);
	}

	public long getMaxTimeSecs() {
		return maxTimeSecs;
	}

	public void setMaxTimeSecs(long maxTimeSecs) {
		this.maxTimeSecs = maxTimeSecs;
	}

	public String getStringToTrigger() {
		return stringToTrigger;
	}

	public void setStringToTrigger(String stringToTrigger) {
		this.stringToTrigger = stringToTrigger;
	}

	/**
	 * Gets rollover threshold size in bytes.
	 * 
	 * @return rollover threshold size in bytes.
	 */
	public long getMaxFileSize() {
		return maxFileSize;
	}

	/**
	 * Sets rollover threshold size in bytes.
	 * 
	 * @param l
	 *            new value for rollover threshold size.
	 */
	public void setMaxFileSize(long l) {
		maxFileSize = l;
	}

	private static long getUTCCurrentTimeinMilliseconds(long time) {
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(time);// 14:32
		cal.setTimeZone(TimeZone.getTimeZone("UTC"));
		return cal.getTimeInMillis();
	}

}
