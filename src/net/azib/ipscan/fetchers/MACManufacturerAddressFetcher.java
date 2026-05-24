package net.azib.ipscan.fetchers;

import net.azib.ipscan.core.ScanningSubject;

public class MACManufacturerAddressFetcher extends AbstractFetcher {
	public static final String ID = "fetcher.mac.manufacturer.address";

	private MACFetcher macFetcher;

	public MACManufacturerAddressFetcher(MACFetcher macFetcher) {
		this.macFetcher = macFetcher;
	}

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public void init() {
		MACVendorFetcher.loadOuiData();
	}

	@Override
	public Object scan(ScanningSubject subject) {
		var mac = (String) subject.getParameter(MACFetcher.ID);
		if (mac == null) {
			macFetcher.scan(subject);
			mac = (String) subject.getParameter(MACFetcher.ID);
		}
		if (mac == null) return null;
		var entry = MACVendorFetcher.findEntry(mac);
		if (entry == null) return null;
		return entry.address.isEmpty() ? "[n/a]" : entry.address;
	}
}
