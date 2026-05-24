package net.azib.ipscan.fetchers;

import net.azib.ipscan.core.ScanningSubject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class MACVendorFetcher extends AbstractFetcher {
	public static final String ID = "fetcher.mac.vendor";

	static final String CSV_URL = "https://raw.githubusercontent.com/Ringmast4r/OUI-Master-Database/refs/heads/master/LISTS/master_oui.csv";

	// Shared parsed OUI data: key = 6-char uppercase hex OUI (no colons), value = OuiEntry
	static Map<String, OuiEntry> ouiData = new HashMap<>();

	private MACFetcher macFetcher;

	public MACVendorFetcher(MACFetcher macFetcher) {
		this.macFetcher = macFetcher;
	}

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public void init() {
		loadOuiData();
	}

	static void loadOuiData() {
		if (!ouiData.isEmpty()) return;
		try (var reader = openCsvReader()) {
			String line = reader.readLine(); // skip header
			while ((line = reader.readLine()) != null) {
				if (line.isEmpty()) continue;
				var entry = OuiEntry.parse(line);
				if (entry != null) ouiData.put(entry.oui, entry);
			}
		} catch (IOException e) {
			throw new RuntimeException("Failed to load OUI data: " + e.getMessage(), e);
		}
	}

	private static BufferedReader openCsvReader() throws IOException {
		var localStream = MACVendorFetcher.class.getResourceAsStream("/master_oui.csv");
		if (localStream != null) {
			return new BufferedReader(new InputStreamReader(localStream, "UTF-8"));
		}
		return new BufferedReader(new InputStreamReader(new URL(CSV_URL).openStream(), "UTF-8"));
	}

	@Override
	public Object scan(ScanningSubject subject) {
		var mac = (String) subject.getParameter(MACFetcher.ID);
		if (mac == null) {
			macFetcher.scan(subject);
			mac = (String) subject.getParameter(MACFetcher.ID);
		}
		if (mac == null) return null;
		var entry = findEntry(mac);
		if (entry == null) return null;
		return entry.manufacturer.isEmpty() ? "[n/a]" : entry.manufacturer;
	}

	static OuiEntry findEntry(String mac) {
		return ouiData.get(mac.replace(":", "").replace("-", "").substring(0, 6).toUpperCase());
	}

	static class OuiEntry {
		final String oui;
		final String manufacturer;
		final String shortName;
		final String deviceType;
		final String address;

		OuiEntry(String oui, String manufacturer, String shortName, String deviceType, String address) {
			this.oui = oui;
			this.manufacturer = manufacturer;
			this.shortName = shortName;
			this.deviceType = deviceType;
			this.address = address;
		}

		// Parses a CSV line with possible quoted fields containing commas.
		// Columns: oui, manufacturer, registry, short_name, device_type, registered_date, address, sources
		static OuiEntry parse(String line) {
			String[] cols = splitCsvLine(line);
			if (cols.length < 7) return null;
			String oui = stripQuotes(cols[0]).replace(":", "").replace("-", "").toUpperCase();
			if (oui.length() < 6) return null;
			String manufacturer = stripQuotes(cols[1]);
			String shortName = cols.length > 3 ? stripQuotes(cols[3]) : "";
			String deviceType = cols.length > 4 ? stripQuotes(cols[4]) : "";
			String address = cols.length > 6 ? stripQuotes(cols[6]).trim() : "";
			return new OuiEntry(oui.substring(0, 6), manufacturer, shortName, deviceType, address);
		}

		private static String stripQuotes(String s) {
			s = s.trim();
			if (s.startsWith("\"") && s.endsWith("\""))
				s = s.substring(1, s.length() - 1);
			return s.trim();
		}

		// Minimal RFC-4180-style CSV splitter (handles quoted fields with commas).
		private static String[] splitCsvLine(String line) {
			var parts = new java.util.ArrayList<String>();
			var sb = new StringBuilder();
			boolean inQuote = false;
			for (int i = 0; i < line.length(); i++) {
				char c = line.charAt(i);
				if (c == '"') {
					inQuote = !inQuote;
					sb.append(c);
				} else if (c == ',' && !inQuote) {
					parts.add(sb.toString());
					sb.setLength(0);
				} else {
					sb.append(c);
				}
			}
			parts.add(sb.toString());
			return parts.toArray(new String[0]);
		}
	}
}
