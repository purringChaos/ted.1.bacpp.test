/**
 * 
 */
package test.tv.blackarrow.cpp.managers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static tv.blackarrow.cpp.notifications.configuration.NotificationServiceConfig.DATAMANAGER;
import static tv.blackarrow.cpp.notifications.configuration.NotificationServiceConfig.GSON;
import static tv.blackarrow.cpp.notifications.upstream.scheduler.NotificationSchedulerService.EXPIRATION_TIME_DELTA;

import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.google.gson.reflect.TypeToken;

import tv.blackarrow.cpp.managers.DataManager;
import tv.blackarrow.cpp.managers.DataManagerFactory;
import tv.blackarrow.cpp.notifications.upstream.scheduler.NotificationSchedulerService;

/**
 * @author Amit Kumar Sharma
 *
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class DataManagerCouchbaseImplTest {

	private static final DataManager DATA_MANAGER = DataManagerFactory.getInstance();
	private static final int DELTA_FROM_CURRENT_TIME = 20;// In Seconds
	
	/**
	 * @throws java.lang.Exception
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
	}

	/**
	 * Test method for {@link tv.blackarrow.cpp.managers.DataManagerCouchbaseImpl#appendToQueue(java.lang.String, java.lang.String, int)}.
	 */
	@Test
	public final void testAppendToQueue() {
		final long currentSystemTimeUptoSeconds = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());
		final long notificationTimeInSeconds = currentSystemTimeUptoSeconds + DELTA_FROM_CURRENT_TIME + 5;
		final String queueName = NotificationSchedulerService.getUpstreamNotificationQueueName(notificationTimeInSeconds);
		HashSet<String> oneBatch = new HashSet<>();
		oneBatch.add("one");
		oneBatch.add("two");
		oneBatch.add("three");
		
		DATAMANAGER.appendToQueue(queueName, GSON.toJson(oneBatch,new TypeToken<HashSet<String>>(){}.getType()), 
				(int)TimeUnit.MILLISECONDS.toSeconds(notificationTimeInSeconds + EXPIRATION_TIME_DELTA));
		
		assertEquals(1,	DATA_MANAGER.getQueueSize(NotificationSchedulerService.getUpstreamNotificationQueueName(notificationTimeInSeconds)));
		
		HashSet<String> secondBatch = new HashSet<>();
		oneBatch.add("four");
		oneBatch.add("five");
		oneBatch.add("six");
		
		DATAMANAGER.appendToQueue(queueName, GSON.toJson(secondBatch,new TypeToken<HashSet<String>>(){}.getType()), 
				(int)TimeUnit.MILLISECONDS.toSeconds(notificationTimeInSeconds + EXPIRATION_TIME_DELTA));
		
		assertEquals(2,	DATA_MANAGER.getQueueSize(NotificationSchedulerService.getUpstreamNotificationQueueName(notificationTimeInSeconds)));
	}

	/**
	 * Test method for {@link tv.blackarrow.cpp.managers.DataManagerCouchbaseImpl#getQueueSize(java.lang.String)}.
	 */
	@Test
	public final void testGetQueueSize() {
		final long currentSystemTimeUptoSeconds = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());
		final long notificationTimeInSeconds = currentSystemTimeUptoSeconds + DELTA_FROM_CURRENT_TIME + 10;
		final String queueName = NotificationSchedulerService.getUpstreamNotificationQueueName(notificationTimeInSeconds);
		
		DATAMANAGER.appendToQueue(queueName, "one", (int)TimeUnit.MILLISECONDS.toSeconds(notificationTimeInSeconds + EXPIRATION_TIME_DELTA));
		DATAMANAGER.appendToQueue(queueName, "two", (int)TimeUnit.MILLISECONDS.toSeconds(notificationTimeInSeconds + EXPIRATION_TIME_DELTA));
		DATAMANAGER.appendToQueue(queueName, "three", (int)TimeUnit.MILLISECONDS.toSeconds(notificationTimeInSeconds + EXPIRATION_TIME_DELTA));
		
		assertEquals(3,	DATA_MANAGER.getQueueSize(NotificationSchedulerService.getUpstreamNotificationQueueName(notificationTimeInSeconds)));
	}

	/**
	 * Test method for {@link tv.blackarrow.cpp.managers.DataManagerCouchbaseImpl#popFromQueue(java.lang.String)}.
	 */
	@Test
	public final void testPopFromQueue() {
		final long currentSystemTimeUptoSeconds = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());
		final long notificationTimeInSeconds = currentSystemTimeUptoSeconds + DELTA_FROM_CURRENT_TIME + 15;
		final String queueName = NotificationSchedulerService.getUpstreamNotificationQueueName(notificationTimeInSeconds);
		
		DATAMANAGER.appendToQueue(queueName, "one", (int)TimeUnit.MILLISECONDS.toSeconds(notificationTimeInSeconds + EXPIRATION_TIME_DELTA));
		DATAMANAGER.appendToQueue(queueName, "two", (int)TimeUnit.MILLISECONDS.toSeconds(notificationTimeInSeconds + EXPIRATION_TIME_DELTA));
		DATAMANAGER.appendToQueue(queueName, "three", (int)TimeUnit.MILLISECONDS.toSeconds(notificationTimeInSeconds + EXPIRATION_TIME_DELTA));
		
		assertEquals(3,	DATA_MANAGER.getQueueSize(NotificationSchedulerService.getUpstreamNotificationQueueName(notificationTimeInSeconds)));
		
		DATAMANAGER.popFromQueue(queueName);
		DATAMANAGER.popFromQueue(queueName);
		
		assertEquals(1,	DATA_MANAGER.getQueueSize(NotificationSchedulerService.getUpstreamNotificationQueueName(notificationTimeInSeconds)));
		//Pop one more and then test the queue with hash sets as elements.
		
		DATAMANAGER.popFromQueue(queueName);
		assertEquals(0,	DATA_MANAGER.getQueueSize(NotificationSchedulerService.getUpstreamNotificationQueueName(notificationTimeInSeconds)));
		
		HashSet<String> oneBatch = new HashSet<>();
		oneBatch.add("one");
		oneBatch.add("two");
		oneBatch.add("three");
		
		DATAMANAGER.appendToQueue(queueName, GSON.toJson(oneBatch,new TypeToken<HashSet<String>>(){}.getType()), 
				(int)TimeUnit.MILLISECONDS.toSeconds(notificationTimeInSeconds + EXPIRATION_TIME_DELTA));
		
		assertEquals(1,	DATA_MANAGER.getQueueSize(NotificationSchedulerService.getUpstreamNotificationQueueName(notificationTimeInSeconds)));
		
		HashSet<String> secondBatch = new HashSet<>();
		oneBatch.add("four");
		oneBatch.add("five");
		oneBatch.add("six");
		
		DATAMANAGER.appendToQueue(queueName, GSON.toJson(secondBatch,new TypeToken<HashSet<String>>(){}.getType()), 
				(int)TimeUnit.MILLISECONDS.toSeconds(notificationTimeInSeconds + EXPIRATION_TIME_DELTA));
		
		assertEquals(2,	DATA_MANAGER.getQueueSize(NotificationSchedulerService.getUpstreamNotificationQueueName(notificationTimeInSeconds)));
		
		DATAMANAGER.popFromQueue(queueName);
		DATAMANAGER.popFromQueue(queueName);
		
		assertEquals(0,	DATA_MANAGER.getQueueSize(NotificationSchedulerService.getUpstreamNotificationQueueName(notificationTimeInSeconds)));
	}

	/**
	 * Test method for {@link tv.blackarrow.cpp.managers.DataManagerCouchbaseImpl#popFromQueue(java.lang.String)}.
	 */
	@Test
	public final void testPopFromQueueMultiThreaded() {
		final long currentSystemTimeUptoSeconds = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());
		final long notificationTimeInSeconds = currentSystemTimeUptoSeconds + DELTA_FROM_CURRENT_TIME + 15;
		final String queueName = NotificationSchedulerService.getUpstreamNotificationQueueName(notificationTimeInSeconds);
		
		for(int i=0; i< 100 ; i++) {
			HashSet<String> oneBatch = new HashSet<>();
			oneBatch.add("one" + i);
			oneBatch.add("two" + i);
			oneBatch.add("three" + i);
			
			DATAMANAGER.appendToQueue(queueName, GSON.toJson(oneBatch,new TypeToken<HashSet<String>>(){}.getType()), 
					(int)TimeUnit.MILLISECONDS.toSeconds(notificationTimeInSeconds + EXPIRATION_TIME_DELTA));
		}
		
		assertEquals(100, DATA_MANAGER.getQueueSize(NotificationSchedulerService.getUpstreamNotificationQueueName(notificationTimeInSeconds)));
		ExecutorService executorService = Executors.newCachedThreadPool();
		AtomicInteger numberOfTimesPopCalled = new AtomicInteger(0);
		for(int i=0; i<10; i++) {
			executorService.submit(() -> {
				String queueElement = DATA_MANAGER.popFromQueue(queueName);
				numberOfTimesPopCalled.incrementAndGet();
				while(queueElement!=null) {
					queueElement = DATA_MANAGER.popFromQueue(queueName);
					numberOfTimesPopCalled.incrementAndGet();
				}
			});
		}
		executorService.shutdown();
		try {
			executorService.awaitTermination(10, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			fail(e.getMessage());
		}
		//100 Real Pops, and 10 last pops, one each by all ten threads when they will get null because there are no more elements.
		assertEquals(100 + 10, numberOfTimesPopCalled.get());
		assertEquals(0,	DATA_MANAGER.getQueueSize(NotificationSchedulerService.getUpstreamNotificationQueueName(notificationTimeInSeconds)));
	}
}
