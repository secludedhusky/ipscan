/*
  This file is a part of Angry IP Scanner source code,
  see http://www.angryip.org/ for more information.
  Licensed under GPLv2.
 */
package net.azib.ipscan.util;

import net.azib.ipscan.config.LoggerFactory;
import net.azib.ipscan.config.Platform;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.util.Collections.list;
import static java.util.Collections.reverse;
import static java.util.regex.Pattern.CASE_INSENSITIVE;

/**
 * This class provides various utility static methods,
 * useful for transforming InetAddress objects.
 *
 * @author Anton Keks
 */
public class InetAddressUtils {
	static final Logger LOG = LoggerFactory.getLogger();

	public static final Pattern HOSTNAME_REGEX = Pattern.compile("(\\b(((?!25?[6-9])[12]\\d|[1-9])?\\d\\.?\\b){4}\\b)|(?<!(\\w|:))(([0-9a-f]{1,4}:){7,7}[0-9a-f]{1,4}|([0-9a-f]{1,4}:){1,7}:|([0-9a-f]{1,4}:){1,6}:[0-9a-f]{1,4}|([0-9a-f]{1,4}:){1,5}(:[0-9a-f]{1,4}){1,2}|([0-9a-f]{1,4}:){1,4}(:[0-9a-f]{1,4}){1,3}|([0-9a-f]{1,4}:){1,3}(:[0-9a-f]{1,4}){1,4}|([0-9a-f]{1,4}:){1,2}(:[0-9a-f]{1,4}){1,5}|[0-9a-f]{1,4}:((:[0-9a-f]{1,4}){1,6})|:((:[0-9a-f]{1,4}){1,7}|:)|fe80:(:[0-9a-f]{0,4}){0,4}%[0-9a-zA-Z]{1,}|::(ffff(:0{1,4}){0,1}:){0,1}((25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])\\.){3,3}(25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])|([0-9a-f]{1,4}:){1,4}:((25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])\\.){3,3}(25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9]))(?!(\\w|:))|(\\b(([a-z]|[a-z0-9][a-z0-9\\-]*[a-z0-9])\\.)+([a-z0-9]{2,})\\.?(?!(\\w|\\.)))", CASE_INSENSITIVE);

	public static InetAddress startRangeByNetmask(InetAddress address, InetAddress netmask) {
		var addressBytes = address.getAddress();
		var netmaskBytes = netmask.getAddress();
		for (var i = 0; i < addressBytes.length; i++) {
			addressBytes[i] = i < netmaskBytes.length ? (byte) (addressBytes[i] & netmaskBytes[i]) : 0;
		}
		try {
			return InetAddress.getByAddress(addressBytes);
		}
		catch (UnknownHostException e) {
			// this should never happen as we are modifying the same bytes received from the InetAddress
			throw new IllegalArgumentException(e);
		}
	}

	public static InetAddress endRangeByNetmask(InetAddress address, InetAddress netmask) {
		var netmaskBytes = netmask.getAddress();
		var addressBytes = address.getAddress();
		for (var i = 0; i < addressBytes.length; i++) {
			addressBytes[i] = (byte) (i < netmaskBytes.length ? (addressBytes[i] | ~(netmaskBytes[i])) : 255);
		}
		try {
			return InetAddress.getByAddress(addressBytes);
		}
		catch (UnknownHostException e) {
			// this should never happen as we are modifying the same bytes received from the InetAddress
			throw new IllegalArgumentException(e);
		}
	}

	/**
	 * Compares two IP addresses.
	 * @return true in case inetAddress1 is greater than inetAddress2
	 */
	public static boolean greaterThan(InetAddress inetAddress1, InetAddress inetAddress2) {
		var address1 = inetAddress1.getAddress();
		var address2 = inetAddress2.getAddress();
		for (var i = 0; i < address1.length; i++) {
			if ((address1[i] & 0xFF) > (address2[i] & 0xFF))
				return true;
			else
			if ((address1[i] & 0xFF) < (address2[i] & 0xFF))
				break;
		}
		return false;
	}

	public static InetAddress increment(InetAddress address) {
		return modifyInetAddress(address, true);
	}

	public static InetAddress decrement(InetAddress address) {
		return modifyInetAddress(address, false);
	}

	/**
	 * Increments or decrements an IP address by 1.
	 * @return incremented/decremented IP address
	 */
	private static InetAddress modifyInetAddress(InetAddress address, boolean isIncrement) {
		try {
			var newAddress = address.getAddress();
			for (var i = newAddress.length-1; i >= 0; i--) {
				if (isIncrement) {
					if (++newAddress[i] != 0x00) {
						break;
					}
				} else {
					if (--newAddress[i] != 0x00) {
						break;
					}
				}

			}
			return InetAddress.getByAddress(newAddress);
		}
		catch (UnknownHostException e) {
			// this exception is unexpected here
			assert false : e;
			return null;
		}
	}

	/**
	 * Parses the netmask string provided in special text format:
	 * A.B.C.D, where each term is 0-255 or empty. If any term is empty, it is the same as 255.
	 * Another supported format is CIDR ("/24").
	 * <p/>
	 * Only IPv4 is supported.
	 */
	public static InetAddress parseNetmask(String netmaskString) throws UnknownHostException {
		if (netmaskString.startsWith("/")) {
			// CIDR netmask, e.g. "/24" - number of bits set from the left
			var totalBits = Integer.parseInt(netmaskString.substring(1));
			return parseNetmask(totalBits);
		}

		// IP-like netmask (IPv4)
		netmaskString = netmaskString.replaceAll("\\.\\.", ".255.");
		netmaskString = netmaskString.replaceAll("\\.\\.", ".255.");
		return InetAddress.getByName(netmaskString);
	}

	public static InetAddress parseNetmask(int prefixBits) {
		var mask = new byte[prefixBits > 32 ? 16 : 4];
		for (var i = 0; i < mask.length; i++) {
			var curByteBits = Math.min(prefixBits, 8);
			prefixBits -= curByteBits;
			mask[i] = (byte)((((1 << curByteBits)-1)<<(8-curByteBits)) & 0xFF);
		}
		try {
			return InetAddress.getByAddress(mask);
		} catch (UnknownHostException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Where mask bits are set, we use prototype bits.
	 * Where mask bits are cleared, we leave bits as is.
	 * @param addressBytes this array is modified according to the maskBytes and prototypeBytes
	 */
	public static void maskPrototypeAddressBytes(byte[] addressBytes, byte[] maskBytes, byte[] prototypeBytes) {
		for (var i = 0; i < addressBytes.length; i++) {
			addressBytes[i] = (byte) ((addressBytes[i] & ~maskBytes[i]) | (prototypeBytes[i] & maskBytes[i]));
		}
	}

	/**
	 * Checks whether the passed address is likely either a broadcast or network address
	 */
	public static boolean isLikelyBroadcast(InetAddress address, InterfaceAddress ifAddr) {
		var bytes = address.getAddress();
		var last = bytes.length - 1;
		if (ifAddr != null) {
			return address.equals(ifAddr.getBroadcast()) ||
					// TODO: 0 is actually not correct for smaller networks than /24
					bytes[last] == 0 && Arrays.equals(bytes, 0, last, ifAddr.getAddress().getAddress(), 0, last);
		}
		return bytes[last] == 0 || bytes[last] == (byte)0xFF;
	}

	public static InterfaceAddress getLocalInterface() {
		InterfaceAddress anyAddress = null;
		try {
			var interfaces = getNetworkInterfaces().stream()
					.filter(i -> i.getParent() == null && !i.isVirtual()).toList();

			for (var networkInterface : interfaces) {
				try {
					if (networkInterface.getHardwareAddress() == null) continue;
				} catch (SocketException ignore) {}

				for (var ifAddr : networkInterface.getInterfaceAddresses()) {
					anyAddress = ifAddr;
					var addr = ifAddr.getAddress();
					if (!addr.isLoopbackAddress() && addr instanceof Inet4Address)
						return ifAddr;
				}
			}
		}
		catch (SocketException e) {
			LOG.log(Level.FINE, "Cannot enumerate network interfaces", e);
		}
		return anyAddress;
	}

	public static NetworkInterface getInterface(InterfaceAddress address) {
		try {
			if (address == null) return null;
			return NetworkInterface.getByInetAddress(address.getAddress());
		}
		catch (SocketException e) {
			return null;
		}
	}

	public static NetworkInterface getInterface(InetAddress address, Stream<NetworkInterface> interfaceStream) {
		try {
			if (address == null) return null;
			return interfaceStream.filter(i -> i.getInterfaceAddresses().stream().anyMatch(ifAddr -> {
				var netmask = parseNetmask(ifAddr.getNetworkPrefixLength());
				return startRangeByNetmask(address, netmask).equals(startRangeByNetmask(ifAddr.getAddress(), netmask));
			})).findFirst().orElse(null);
		}
		catch (Exception e) {
			return null;
		}
	}

	public static NetworkInterface getInterface(InetAddress address) {
		try {
			return getInterface(address, NetworkInterface.networkInterfaces());
		}
		catch (Exception e) {
			return null;
		}
	}

	public static InterfaceAddress matchingAddress(NetworkInterface netIf, Class<? extends InetAddress> addressClass) {
		if (netIf == null) return null;
		return netIf.getInterfaceAddresses().stream().filter(i -> i.getAddress().getClass() == addressClass).findFirst().orElse(null);
	}

	public static List<NetworkInterface> getNetworkInterfaces() throws SocketException {
		List<NetworkInterface> interfaces = list(NetworkInterface.getNetworkInterfaces());
		if (!Platform.WINDOWS) reverse(interfaces);
		return interfaces;
	}

	public record InterfaceWithMetric(NetworkInterface networkInterface, InterfaceAddress interfaceAddress, int metric) {}

	/**
	 * On Windows, queries interface metrics via PowerShell and returns interfaces sorted by metric.
	 * On other platforms, returns interfaces in default order with metric 0.
	 */
	public static List<InterfaceWithMetric> getAllInterfacesWithMetrics() {
		var result = new ArrayList<InterfaceWithMetric>();
		Map<Integer, Integer> metricsByIndex = Platform.WINDOWS ? getWindowsInterfaceMetrics() : Map.of();

		try {
			for (var networkInterface : getNetworkInterfaces()) {
				try {
					if (networkInterface.getHardwareAddress() == null) continue;
				} catch (SocketException ignore) {}

				for (var ifAddr : networkInterface.getInterfaceAddresses()) {
					var addr = ifAddr.getAddress();
					if (!addr.isLoopbackAddress() && addr instanceof Inet4Address) {
						int metric = metricsByIndex.getOrDefault(networkInterface.getIndex(), Integer.MAX_VALUE);
						result.add(new InterfaceWithMetric(networkInterface, ifAddr, metric));
					}
				}
			}
		} catch (SocketException e) {
			LOG.log(Level.FINE, "Cannot enumerate network interfaces", e);
		}

		result.sort(Comparator.comparingInt(InterfaceWithMetric::metric));
		return result;
	}

	/**
	 * Returns the local interface selected by lowest InterfaceMetric on Windows,
	 * falling back to the existing getLocalInterface() on other platforms or on failure.
	 */
	public static InterfaceAddress getLocalInterfaceByMetric() {
		if (Platform.WINDOWS) {
			var interfaces = getAllInterfacesWithMetrics();
			if (!interfaces.isEmpty()) {
				return interfaces.get(0).interfaceAddress();
			}
		}
		return getLocalInterface();
	}

	/**
	 * Returns the InterfaceAddress for a specific interface identified by display name.
	 * Falls back to getLocalInterfaceByMetric() if not found.
	 */
	public static InterfaceAddress getInterfaceByName(String displayName) {
		if (displayName == null || displayName.isEmpty()) {
			return getLocalInterfaceByMetric();
		}
		try {
			for (var networkInterface : getNetworkInterfaces()) {
				if (networkInterface.getDisplayName().equals(displayName)) {
					for (var ifAddr : networkInterface.getInterfaceAddresses()) {
						if (ifAddr.getAddress() instanceof Inet4Address && !ifAddr.getAddress().isLoopbackAddress()) {
							return ifAddr;
						}
					}
				}
			}
		} catch (SocketException e) {
			LOG.log(Level.FINE, "Cannot find interface by name: " + displayName, e);
		}
		return getLocalInterfaceByMetric();
	}

	private static Map<Integer, Integer> getWindowsInterfaceMetrics() {
		var metrics = new HashMap<Integer, Integer>();
		try {
			var process = new ProcessBuilder("powershell", "-NoProfile", "-Command",
				"Get-NetIPInterface -AddressFamily IPv4 | Select-Object ifIndex,InterfaceMetric | ForEach-Object { $_.ifIndex.ToString() + ',' + $_.InterfaceMetric.ToString() }")
				.redirectErrorStream(true).start();
			try (var reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
				String line;
				while ((line = reader.readLine()) != null) {
					line = line.trim();
					var parts = line.split(",");
					if (parts.length == 2) {
						try {
							metrics.put(Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim()));
						} catch (NumberFormatException ignore) {}
					}
				}
			}
			process.waitFor();
		} catch (Exception e) {
			LOG.log(Level.FINE, "Cannot query Windows interface metrics", e);
		}
		return metrics;
	}
}
