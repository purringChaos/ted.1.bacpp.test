package tv.blackarrow.cpp.pretrigger.beans;

import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

import tv.blackarrow.cpp.setting.SettingUtils;

/**
 * defined the pre-trigger setting based on the configuration
 * pretrigger_bean.xml that may be found under ess configuration
 * directory
 * 
 * @author jwang
 *
 */
public class PretriggerSettingBean {
	private static final int defaultBiSQueryInterval = 30;
	private static final String PRETRIGGER_BEAN_XML = "pretrigger_bean.xml";
	private static final Logger LOG = LogManager.getLogger(PretriggerSettingBean.class);
	private static PretriggerSettingBean INSTANCE = null;
	
	private String bisUrl;
	private long triggerAdvanceTime = 300000; // in millisecond
	private int bisQueryInterval = 5; // how frequently to query BIS in minutes
	private long pretriggerFrequency = 4; // in second
	private int bisQueryTimeout = 20000; // in ms
	private int maxRetry = 3; //
	private int bisBreakWindow = 60;  // in minutes
	private boolean serviceEnabled = false;
	private int advanceTimeMinimum = ADVANCE_TIME_MINIMUM;

	public static final int ADVANCE_TIME_MINIMUM = 10000; // ms

	private PretriggerSettingBean() {	}
	
	public static PretriggerSettingBean getInstance() {
		if(INSTANCE == null) {
			reload();
		}
		
		return INSTANCE;
	}
	
	public long getTriggerAdvanceTime() {
		return triggerAdvanceTime;
	}

	public void setTriggerAdvanceTime(long triggerAdvanceTime) {
		if(triggerAdvanceTime < ADVANCE_TIME_MINIMUM || triggerAdvanceTime > 300000) {
			LOG.warn(triggerAdvanceTime + "ms  as triggerAdvanceTime value is either "
					+ "to big or too small. The recommended value is 60000 ~ 180000 ms (60 seconds - 180 seconds)");;
		}
		this.triggerAdvanceTime = triggerAdvanceTime;
	}

	public int getBisQueryInterval() {
		return bisQueryInterval;
	}

	public void setBisQueryInterval(int bisQueryInterval) {
		if(bisQueryInterval < 1 || bisQueryInterval > 24*60) {
			LOG.warn(bisQueryInterval + "minutes  as bisQueryInterval value is either "
					+ "to big or too small. The recommended value is 10 ~ 120 minutes. Set it to a default value"  + defaultBiSQueryInterval);
			this.bisQueryInterval = defaultBiSQueryInterval;
		}
		this.bisQueryInterval = bisQueryInterval;
	}

	public long getPretriggerFrequency() {
		return pretriggerFrequency;
	}

	public void setPretriggerFrequency(long pretriggerFrequency) {
		this.pretriggerFrequency = pretriggerFrequency;
	}

	public int getBisQueryTimeout() {
		return bisQueryTimeout;
	}

	public void setBisQueryTimeout(int bisQueryTimeout) {
		this.bisQueryTimeout = bisQueryTimeout;
	}


	public int getMaxRetry() {
		return maxRetry;
	}

	public void setMaxRetry(int maxRetry) {
		this.maxRetry = maxRetry;
	}

	public boolean isServiceEnabled() {
		return serviceEnabled;
	}

	public void setServiceEnabled(boolean serviceEnabled) {
		this.serviceEnabled = serviceEnabled;
	}
	
	public String getBisUrl() {
		return bisUrl;
	}

	public void setBisUrl(String bisUrl) {
		if(bisUrl == null || bisUrl.trim().length() < 10) {
			LOG.error("BIS url is invalid. Please check the value: " + bisUrl);
			bisUrl = "";
		}
		this.bisUrl = bisUrl;
	}

	public int getBisBreakWindow() {
		return bisBreakWindow;
	}

	public void setBisBreakWindow(int bisBreakWindow) {
		this.bisBreakWindow = bisBreakWindow;
	}
	
	public int getAdvanceTimeMinimum() {
		/*
		if(advanceTimeMinimum <= 0 || advanceTimeMinimum > 100000) {
			LOG.warn("advanceTimeMinium value was not set to a correct value range (0, 100000]. "
					+ "The defaut value (in seconds) will be used as " + ADVANCE_TIME_MINIMUM);
			advanceTimeMinimum = ADVANCE_TIME_MINIMUM;
		} */
		return advanceTimeMinimum;
	}

	public void setAdvanceTimeMinimum(int advanceTimeMinium) {
		this.advanceTimeMinimum = advanceTimeMinium;
	}

	@Override
	public String toString() {
		return "PretriggerSettingBean [bisUrl=" + bisUrl + ", triggerAdvanceTime=" + triggerAdvanceTime
				+ ", bisQueryInterval=" + bisQueryInterval + ", pretriggerFrequency=" + pretriggerFrequency
				+ ", bisQueryTimeout=" + bisQueryTimeout + ", maxRetry=" + maxRetry + ", bisBreakWindow="
				+ bisBreakWindow + ", serviceEnabled=" + serviceEnabled + ", advanceTimeMinium=" + advanceTimeMinimum
				+ "]";
	}

	public static void reload() {
		String filePath = SettingUtils.getConfigurationPath() + PRETRIGGER_BEAN_XML;

		ApplicationContext ctx = null;
		try {
			ctx = new FileSystemXmlApplicationContext(new File(filePath).toURI().toURL().toString());
			LOG.debug("Successfully load " + filePath);
		} catch (Exception ex) {
			LOG.error(ex.getMessage() + " ** Cannot load " + filePath);
		}

		INSTANCE = (PretriggerSettingBean) ctx.getBean("settingBean");
		LOG.debug(INSTANCE);
	}

	public void monitorFileChange() {
		try {
			// create a new watch service
			WatchService watcher = FileSystems.getDefault().newWatchService();

			// the path to be watched
			final Path mypath = Paths.get(SettingUtils.getConfigurationPath());
			if(mypath == null) {
				throw new IOException("the directory/file does not exist: " + SettingUtils.getConfigurationPath());
			}
			// registers the files located by the path with a watch service.
			// invoke KeyEvent when: ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE
			mypath.register(watcher, ENTRY_MODIFY);

			Runnable runnable = createRunnable(watcher);
			final Thread consumer = new Thread(runnable);
			consumer.start();
		} catch(IOException ex) {
			LOG.error(ex.getMessage());
		}
	}

	private Runnable createRunnable(final WatchService watcher) {
		return new Runnable() {
			public void run() {
				try {
					WatchKey key = watcher.take();
					Thread.sleep(50);  // to avoid the modified file were alerted twice
					while(key != null) {
						for (WatchEvent<?> event : key.pollEvents()) {
							WatchEvent.Kind<?> kind = event.kind();
							@SuppressWarnings("unchecked")
							WatchEvent<Path> ev = (WatchEvent<Path>) event;
							Path filename = ev.context();

							if (kind == StandardWatchEventKinds.OVERFLOW) {
								Thread.yield();
								continue;
							}
							else if (kind == java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY
									&& filename.toString().equals(PRETRIGGER_BEAN_XML)) {
								LOG.info(filename + " - was modified: ");
								reload(); // reload the configuration
							}
						}
						key.reset();
						key = watcher.take();
						Thread.sleep(150);  // to avoid the modified file were alerted twice
					}
				} catch (InterruptedException ie) {
					LOG.error(ie.getMessage(), ie);
				}
			}
		};
	}
}
