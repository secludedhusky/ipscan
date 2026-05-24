package net.azib.ipscan.fetchers;

import net.azib.ipscan.core.ScanningSubject;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MACVendorFetcherTest {
	@BeforeClass
	public static void loadData() {
		MACVendorFetcher.loadOuiData();
	}

	@Test
	public void manufacturerFromOui() {
		// 00:08:E1 -> Barix AG
		var entry = MACVendorFetcher.findEntry("00:08:E1:01:08:32");
		assertEquals("Barix AG", entry.manufacturer);
	}

	@Test
	public void shortNameFromOui() {
		var entry = MACVendorFetcher.findEntry("00:08:E1:01:08:32");
		assertEquals("Barix", entry.shortName);
	}

	@Test
	public void deviceTypeFromOui() {
		// E8:0A:B9 -> Cisco -> Router
		var entry = MACVendorFetcher.findEntry("E8:0A:B9:02:73:41");
		assertEquals("Router", entry.deviceType);
	}

	@Test
	public void addressFromOui() {
		var entry = MACVendorFetcher.findEntry("E8:0A:B9:02:73:41");
		assertEquals("80 West Tasman Drive San Jose CA US 94568", entry.address);
	}

	@Test
	public void unknownOuiReturnsNull() {
		var entry = MACVendorFetcher.findEntry("FF:FF:FF:00:00:00");
		assertEquals(null, entry);
	}

	private static MACFetcher dummyMacFetcher() {
		return new MACFetcher() {
			@Override protected String resolveMAC(ScanningSubject subject) { return null; }
		};
	}
}
