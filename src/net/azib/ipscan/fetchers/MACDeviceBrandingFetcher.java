package net.azib.ipscan.fetchers;

import net.azib.ipscan.core.ScanningSubject;

public class MACDeviceBrandingFetcher extends AbstractFetcher {
	public static final String ID = "fetcher.mac.device.branding";

	private MACFetcher macFetcher;

	public MACDeviceBrandingFetcher(MACFetcher macFetcher) {
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
		return entry.shortName.isEmpty() ? "[n/a]" : entry.shortName;
	}
}
