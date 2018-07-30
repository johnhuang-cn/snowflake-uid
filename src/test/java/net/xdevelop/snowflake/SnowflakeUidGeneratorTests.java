package net.xdevelop.snowflake;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.util.StringUtils;

import net.xdevelop.snowflake.utils.DateUtils;

public class SnowflakeUidGeneratorTests {
	private int workerId = 132;
	private int timeBits = 28;
	private int workerBits = 24;
	private int seqBits = 11;
	private String baseDate = "2018-07-01";
	private static long speed = 0;
	
	private SnowflakeUidGenerator uidGenerator;
	
	@Test
	public void contextLoads() {
	}
	
	@Before
    public void setUp() throws Exception {
		uidGenerator = new SnowflakeUidGenerator(workerId, baseDate, timeBits, workerBits, seqBits);
    }
	
	@AfterClass
	public static void printSpeed() {
		System.out.println(String.format("Speed: %d/s", speed));
	}
	
	@Test(expected=Exception.class)
	public void testConstruct() {
		new SnowflakeUidGenerator(2<<24, "2017-07-1", 28, 24, 11);
	}
	
	@Test
	public void testIDBits() {
		String timestamp = DateUtils.formatByDatePattern(new Date());
		String timeInfo = String.format("\"timestamp\":\"%s", timestamp);
		String idsInfo = String.format("\"workerId\":\"%d\"", workerId);
		
		long uid = uidGenerator.getUID();
		System.out.println(String.format("uid: %d binary str: %s", uid, Long.toBinaryString(uid)));
			
		String parseInfo = uidGenerator.parseUID(uid);
		System.out.println(String.format("uid parsed: %s" , parseInfo));
		Assert.assertTrue(parseInfo.indexOf(timeInfo) > 0);
		Assert.assertTrue(parseInfo.indexOf(idsInfo) > 0);
	}
	
    private static final int SIZE = 100000; // 10w
    private static final boolean VERBOSE = true;
    private static final int THREADS = Runtime.getRuntime().availableProcessors() << 1;
    
    
    /**
     * Test for serially generate
     */
    @Test
    public void testSerialGenerate() {
        // Generate UID serially
        Set<Long> uidSet = new HashSet<Long>(SIZE);
        for (int i = 0; i < SIZE; i++) {
            doGenerate(uidSet, i);
        }

        // Check UIDs are all unique
        checkUniqueID(uidSet);
    }

    /**
     * Test for parallel generate
     * 
     * @throws InterruptedException
     */
    @Test
    public void testParallelGenerate() throws InterruptedException {
        AtomicInteger control = new AtomicInteger(-1);
        Set<Long> uidSet = new ConcurrentSkipListSet<>();

        // Initialize threads
        List<Thread> threadList = new ArrayList<Thread>(THREADS);
        long start = System.currentTimeMillis();
        for (int i = 0; i < THREADS; i++) {
            Thread thread = new Thread(() -> workerRun(uidSet, control));
            thread.setName("UID-generator-" + i);

            threadList.add(thread);
            thread.start();
        }

        // Wait for worker done
        for (Thread thread : threadList) {
            thread.join();
        }

        long end = System.currentTimeMillis();
        speed = SIZE / (end - start) * 1000;
        
        // Check generate 10w times
        Assert.assertEquals(SIZE, control.get());

        // Check UIDs are all unique
        checkUniqueID(uidSet);
    }

    /**
     * Worker run
     */
    private void workerRun(Set<Long> uidSet, AtomicInteger control) {
        for (;;) {
            int myPosition = control.updateAndGet(old -> (old == SIZE ? SIZE : old + 1));
            if (myPosition == SIZE) {
                return;
            }

            doGenerate(uidSet, myPosition);
        }
    }

    /**
     * Do generating
     */
    private void doGenerate(Set<Long> uidSet, int index) {
        long uid = uidGenerator.getUID();
        String parsedInfo = uidGenerator.parseUID(uid);
        uidSet.add(uid);

        // Check UID is positive, and can be parsed
        Assert.assertTrue(uid > 0L);
        Assert.assertTrue(!StringUtils.isEmpty(parsedInfo));

        if (VERBOSE) {
            System.out.println(Thread.currentThread().getName() + " No." + index + " >>> " + parsedInfo);
        }
    }

    /**
     * Check UIDs are all unique
     */
    private void checkUniqueID(Set<Long> uidSet) {
        System.out.println(uidSet.size());
        Assert.assertEquals(SIZE, uidSet.size());
    }	
}
