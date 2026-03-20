/*
  This file is a part of Angry IP Scanner source code,
  see http://www.angryip.org/ for more information.
  Licensed under GPLv2.
 */
package net.azib.ipscan.gui;

import net.azib.ipscan.config.Config;
import net.azib.ipscan.config.GUIConfig;
import net.azib.ipscan.config.GUIConfig.DisplayMethod;
import net.azib.ipscan.config.Labels;
import net.azib.ipscan.config.ScannerConfig;
import net.azib.ipscan.core.PortIterator;
import net.azib.ipscan.core.net.PingerRegistry;
import net.azib.ipscan.fetchers.FetcherException;
import net.azib.ipscan.gui.util.LayoutHelper;
import net.azib.ipscan.util.InetAddressUtils;
import net.azib.ipscan.util.InetAddressUtils.InterfaceWithMetric;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.*;

import java.util.List;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

/**
 * Preferences Dialog
 *
 * @author Anton Keks
 */
public class PreferencesDialog extends AbstractModalDialog {
	private final PingerRegistry pingerRegistry;
	private final Config globalConfig;
	private final ScannerConfig scannerConfig;
	private final GUIConfig guiConfig;

	private Button okButton;
	private Button cancelButton;

	private TabFolder tabFolder;
	private Composite scanningTab;
	private TabItem scanningTabItem;
	private Composite displayTab;
	private Text threadDelayText;
	private Text maxThreadsText;
	private Button deadHostsCheckbox;
	private Text pingingTimeoutText;
	private Text pingingCountText;
	private Combo pingersCombo;
	private Button skipBroadcastsCheckbox;
	private Composite portsTab;
	private TabItem portsTabItem;
	private Text portTimeoutText;
	private Button adaptTimeoutCheckbox;
	private Button addRequestedPortsCheckbox;
	private Text minPortTimeoutText;
	private Text portsText;
	private Text notAvailableText;
	private Text notScannedText;
	private Button[] displayMethod;
	private Button showInfoCheckbox;
	private Button askConfirmationCheckbox;
	private Button versionCheckCheckbox;
	private Button allowReports;
	private Combo languageCombo;
	private Combo networkInterfaceCombo;
	private Combo startupModeCombo;
	private Text startupStartIPText;
	private Text startupEndIPText;
	private Text startupPrototypeText;
	private Combo startupMaskCombo;
	private Spinner startupCountSpinner;
	private Text startupFilePathText;
	private Composite startupRangeComposite;
	private Composite startupRandomComposite;
	private Composite startupFileComposite;
	private List<InterfaceWithMetric> availableInterfaces;

	public PreferencesDialog(PingerRegistry pingerRegistry, Config globalConfig, ScannerConfig scannerConfig, GUIConfig guiConfig) {
		this.pingerRegistry = pingerRegistry;
		this.globalConfig = globalConfig;
		this.scannerConfig = scannerConfig;
		this.guiConfig = guiConfig;
	}

	@Override public void open() {
		openTab(0);
	}
	
	/**
	 * Opens the specified tab of preferences dialog
	 */
	public void openTab(int tabIndex) {
		// widgets are created on demand
		createShell();
		loadPreferences();
		tabFolder.setSelection(tabIndex);
		
		// select ports text by default if ports tab is opened
		// this is needed for PortsFetcher that uses this tab as its preferences
		if (tabFolder.getItem(tabIndex) == portsTabItem) {
			portsText.forceFocus();
		}
		
		super.open();
	}

	@Override
	protected void populateShell() {
		shell.setText(Labels.getLabel("title.preferences"));
		shell.setLayout(LayoutHelper.formLayout(10, 10, 4));
		
		createTabFolder();

		okButton = new Button(shell, SWT.NONE);
		okButton.setText(Labels.getLabel("button.OK"));
		
		cancelButton = new Button(shell, SWT.NONE);
		cancelButton.setText(Labels.getLabel("button.cancel"));
		
		positionButtonsInFormLayout(okButton, cancelButton, tabFolder);
		
		shell.pack();
		okButton.setFocus();

		okButton.addSelectionListener(widgetSelectedAdapter(e -> {
			if (shell != null && !shell.isDisposed()) {
				savePreferences();
				close();
			}
		}));
		cancelButton.addSelectionListener(widgetSelectedAdapter(e -> close()));
	}

	/**
	 * This method initializes tabFolder	
	 */
	private void createTabFolder() {
		tabFolder = new TabFolder(shell, SWT.NONE);
		
		createScanningTab();
		var tabItem = new TabItem(tabFolder, SWT.NONE);
		tabItem.setText(Labels.getLabel("title.preferences.scanning"));
		tabItem.setControl(scanningTab);
		scanningTabItem = tabItem;
		
		createPortsTab();
		tabItem = new TabItem(tabFolder, SWT.NONE);
		tabItem.setText(Labels.getLabel("title.preferences.ports"));
		tabItem.setControl(portsTab);
		portsTabItem = tabItem;
		
		createDisplayTab();		
		tabItem = new TabItem(tabFolder, SWT.NONE);
		tabItem.setText(Labels.getLabel("title.preferences.display"));
		tabItem.setControl(displayTab);		

		tabFolder.pack();
	}

	/**
	 * This method initializes scanningTab	
	 */
	private void createScanningTab() {
		var rowLayout = createRowLayout();
		scanningTab = new Composite(tabFolder, SWT.NONE);
		scanningTab.setLayout(rowLayout);

		createNetworkGroup();

		var groupLayout = new GridLayout();
		groupLayout.numColumns = 2;
		var threadsGroup = new Group(scanningTab, SWT.NONE);
		threadsGroup.setText(Labels.getLabel("preferences.threads"));
		threadsGroup.setLayout(groupLayout);

		var gridData = new GridData(80, SWT.DEFAULT);
		
		Label label;
		
		label = new Label(threadsGroup, SWT.NONE);
		label.setText(Labels.getLabel("preferences.threads.delay"));
		threadDelayText = new Text(threadsGroup, SWT.BORDER);
		threadDelayText.setLayoutData(gridData);

		label = new Label(threadsGroup, SWT.NONE);
		label.setText(Labels.getLabel("preferences.threads.maxThreads"));
		maxThreadsText = new Text(threadsGroup, SWT.BORDER);
		maxThreadsText.setLayoutData(gridData);

		var pingingGroup = new Group(scanningTab, SWT.NONE);
		pingingGroup.setLayout(groupLayout);
		pingingGroup.setText(Labels.getLabel("preferences.pinging"));
		
		label = new Label(pingingGroup, SWT.NONE);
		label.setText(Labels.getLabel("preferences.pinging.type"));
		pingersCombo = new Combo(pingingGroup, SWT.DROP_DOWN | SWT.READ_ONLY);
		pingersCombo.setLayoutData(gridData);
		var pingerNames = pingerRegistry.getRegisteredNames();
		for (var i = 0; i < pingerNames.length; i++) {
			pingersCombo.add(Labels.getLabel(pingerNames[i]));
			// this is used by savePreferences()
			pingersCombo.setData(Integer.toString(i), pingerNames[i]);
		}
		pingersCombo.select(0);

		label = new Label(pingingGroup, SWT.NONE);
		label.setText(Labels.getLabel("preferences.pinging.count"));
		pingingCountText = new Text(pingingGroup, SWT.BORDER);
		pingingCountText.setLayoutData(gridData);

		label = new Label(pingingGroup, SWT.NONE);
		label.setText(Labels.getLabel("preferences.pinging.timeout"));
		pingingTimeoutText = new Text(pingingGroup, SWT.BORDER);
		pingingTimeoutText.setLayoutData(gridData);
		
		var gridDataWithSpan = new GridData();
		gridDataWithSpan.horizontalSpan = 2;
		deadHostsCheckbox = new Button(pingingGroup, SWT.CHECK);
		deadHostsCheckbox.setText(Labels.getLabel("preferences.pinging.deadHosts"));
		deadHostsCheckbox.setLayoutData(gridDataWithSpan);

		var skippingGroup = new Group(scanningTab, SWT.NONE);
		skippingGroup.setLayout(groupLayout);
		skippingGroup.setText(Labels.getLabel("preferences.skipping"));
		
		skipBroadcastsCheckbox = new Button(skippingGroup, SWT.CHECK);
		skipBroadcastsCheckbox.setText(Labels.getLabel("preferences.skipping.broadcast"));
		var gridDataWithSpan2 = new GridData();
		gridDataWithSpan2.horizontalSpan = 2;
		skipBroadcastsCheckbox.setLayoutData(gridDataWithSpan2);
	}

	private void createNetworkGroup() {
		var groupLayout = new GridLayout();
		groupLayout.numColumns = 2;
		var networkGroup = new Group(scanningTab, SWT.NONE);
		networkGroup.setText(Labels.getLabel("preferences.network"));
		networkGroup.setLayout(groupLayout);

		var gridData = new GridData(200, SWT.DEFAULT);

		var label = new Label(networkGroup, SWT.NONE);
		label.setText(Labels.getLabel("preferences.network.interface"));
		networkInterfaceCombo = new Combo(networkGroup, SWT.DROP_DOWN | SWT.READ_ONLY);
		networkInterfaceCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		networkInterfaceCombo.add(Labels.getLabel("preferences.network.interface.auto"));

		availableInterfaces = InetAddressUtils.getAllInterfacesWithMetrics();
		for (var iface : availableInterfaces) {
			var ni = iface.networkInterface();
			var addr = iface.interfaceAddress();
			var text = ni.getDisplayName() + ": " + addr.getAddress().getHostAddress()
				+ "/" + addr.getNetworkPrefixLength() + " (metric: " + iface.metric() + ")";
			networkInterfaceCombo.add(text);
		}
		networkInterfaceCombo.select(0);

		label = new Label(networkGroup, SWT.NONE);
		label.setText(Labels.getLabel("preferences.network.startup"));
		startupModeCombo = new Combo(networkGroup, SWT.DROP_DOWN | SWT.READ_ONLY);
		startupModeCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		startupModeCombo.add(Labels.getLabel("preferences.network.startup.auto"));
		startupModeCombo.add(Labels.getLabel("preferences.network.startup.range"));
		startupModeCombo.add(Labels.getLabel("preferences.network.startup.random"));
		startupModeCombo.add(Labels.getLabel("preferences.network.startup.file"));
		startupModeCombo.select(0);

		// Manual IP Range fields
		startupRangeComposite = new Composite(networkGroup, SWT.NONE);
		var rangeLayout = new GridLayout(2, false);
		rangeLayout.marginWidth = 0; rangeLayout.marginHeight = 0;
		startupRangeComposite.setLayout(rangeLayout);
		var spanData = new GridData(GridData.FILL_HORIZONTAL);
		spanData.horizontalSpan = 2;
		startupRangeComposite.setLayoutData(spanData);

		label = new Label(startupRangeComposite, SWT.NONE);
		label.setText(Labels.getLabel("preferences.network.startup.startIP"));
		startupStartIPText = new Text(startupRangeComposite, SWT.BORDER);
		startupStartIPText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		label = new Label(startupRangeComposite, SWT.NONE);
		label.setText(Labels.getLabel("preferences.network.startup.endIP"));
		startupEndIPText = new Text(startupRangeComposite, SWT.BORDER);
		startupEndIPText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		// Random fields
		startupRandomComposite = new Composite(networkGroup, SWT.NONE);
		var randomLayout = new GridLayout(2, false);
		randomLayout.marginWidth = 0; randomLayout.marginHeight = 0;
		startupRandomComposite.setLayout(randomLayout);
		spanData = new GridData(GridData.FILL_HORIZONTAL);
		spanData.horizontalSpan = 2;
		startupRandomComposite.setLayoutData(spanData);

		label = new Label(startupRandomComposite, SWT.NONE);
		label.setText(Labels.getLabel("preferences.network.startup.prototype"));
		startupPrototypeText = new Text(startupRandomComposite, SWT.BORDER);
		startupPrototypeText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		label = new Label(startupRandomComposite, SWT.NONE);
		label.setText(Labels.getLabel("preferences.network.startup.mask"));
		startupMaskCombo = new Combo(startupRandomComposite, SWT.NONE);
		startupMaskCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		startupMaskCombo.add("255...128");
		startupMaskCombo.add("255...0");
		startupMaskCombo.add("255..0.0");
		startupMaskCombo.add("255.0.0.0");
		startupMaskCombo.add("0.0.0.0");
		startupMaskCombo.select(3);

		label = new Label(startupRandomComposite, SWT.NONE);
		label.setText(Labels.getLabel("preferences.network.startup.count"));
		startupCountSpinner = new Spinner(startupRandomComposite, SWT.BORDER);
		startupCountSpinner.setMinimum(1);
		startupCountSpinner.setMaximum(100000000);
		startupCountSpinner.setSelection(100);
		startupCountSpinner.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		// File fields
		startupFileComposite = new Composite(networkGroup, SWT.NONE);
		var fileLayout = new GridLayout(3, false);
		fileLayout.marginWidth = 0; fileLayout.marginHeight = 0;
		startupFileComposite.setLayout(fileLayout);
		spanData = new GridData(GridData.FILL_HORIZONTAL);
		spanData.horizontalSpan = 2;
		startupFileComposite.setLayoutData(spanData);

		label = new Label(startupFileComposite, SWT.NONE);
		label.setText(Labels.getLabel("preferences.network.startup.filePath"));
		startupFilePathText = new Text(startupFileComposite, SWT.BORDER);
		startupFilePathText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		var browseButton = new Button(startupFileComposite, SWT.NONE);
		browseButton.setText(Labels.getLabel("preferences.network.startup.browse"));
		browseButton.addSelectionListener(widgetSelectedAdapter(e -> {
			var dialog = new FileDialog(shell);
			dialog.setText(Labels.getLabel("preferences.network.startup.browse"));
			var fileName = dialog.open();
			if (fileName != null) {
				startupFilePathText.setText(fileName);
			}
		}));

		// Show/hide fields based on startup mode
		startupModeCombo.addSelectionListener(widgetSelectedAdapter(e -> updateStartupModeVisibility()));
		updateStartupModeVisibility();
	}

	private void updateStartupModeVisibility() {
		int mode = startupModeCombo.getSelectionIndex();
		startupRangeComposite.setVisible(mode == 1);
		((GridData) startupRangeComposite.getLayoutData()).exclude = mode != 1;
		startupRandomComposite.setVisible(mode == 2);
		((GridData) startupRandomComposite.getLayoutData()).exclude = mode != 2;
		startupFileComposite.setVisible(mode == 3);
		((GridData) startupFileComposite.getLayoutData()).exclude = mode != 3;
		scanningTab.layout(true, true);
		shell.pack();
	}

	/**
	 * This method initializes displayTab
	 */
	private void createDisplayTab() {
		var rowLayout = createRowLayout();
		displayTab = new Composite(tabFolder, SWT.NONE);
		displayTab.setLayout(rowLayout);
		
		var groupLayout = new GridLayout();
		groupLayout.numColumns = 1;
		var listGroup = new Group(displayTab, SWT.NONE);
		listGroup.setText(Labels.getLabel("preferences.display.list"));
		listGroup.setLayout(groupLayout);
		listGroup.setLayoutData(new RowData(260, SWT.DEFAULT));
		displayMethod = new Button[DisplayMethod.values().length];
		var allRadio = new Button(listGroup, SWT.RADIO);
		allRadio.setText(Labels.getLabel("preferences.display.list" + '.' + DisplayMethod.ALL));
		displayMethod[DisplayMethod.ALL.ordinal()] = allRadio;
		var aliveRadio = new Button(listGroup, SWT.RADIO);
		aliveRadio.setText(Labels.getLabel("preferences.display.list" + '.' + DisplayMethod.ALIVE));
		displayMethod[DisplayMethod.ALIVE.ordinal()] = aliveRadio;
		var portsRadio = new Button(listGroup, SWT.RADIO);
		portsRadio.setText(Labels.getLabel("preferences.display.list" + '.' +  DisplayMethod.PORTS));
		displayMethod[DisplayMethod.PORTS.ordinal()] = portsRadio;
		
		groupLayout = new GridLayout();
		groupLayout.numColumns = 2;
		var labelsGroup = new Group(displayTab, SWT.NONE);
		labelsGroup.setText(Labels.getLabel("preferences.display.labels"));
		labelsGroup.setLayout(groupLayout);
		
		var gridData = new GridData();
		gridData.widthHint = 50;
		
		var label = new Label(labelsGroup, SWT.NONE);
		label.setText(Labels.getLabel("preferences.display.labels.notAvailable"));
		notAvailableText = new Text(labelsGroup, SWT.BORDER);
		notAvailableText.setLayoutData(gridData);
		
		label = new Label(labelsGroup, SWT.NONE);
		label.setText(Labels.getLabel("preferences.display.labels.notScanned"));
		notScannedText = new Text(labelsGroup, SWT.BORDER);
		notScannedText.setLayoutData(gridData);

		groupLayout = new GridLayout();
		groupLayout.numColumns = 1;
		var showStatsGroup = new Group(displayTab, SWT.NONE);
		showStatsGroup.setLayout(groupLayout);
		showStatsGroup.setText(Labels.getLabel("preferences.display.confirmation"));
		
		askConfirmationCheckbox = new Button(showStatsGroup, SWT.CHECK);
		askConfirmationCheckbox.setText(Labels.getLabel("preferences.display.confirmation.newScan"));
		showInfoCheckbox = new Button(showStatsGroup, SWT.CHECK);
		showInfoCheckbox.setText(Labels.getLabel("preferences.display.confirmation.showInfo"));

		groupLayout = new GridLayout();
		groupLayout.numColumns = 2;
		
		var languageGroup = new Group(displayTab, SWT.NONE);
		languageGroup.setLayout(groupLayout);
		languageGroup.setText(Labels.getLabel("preferences.language"));
		
		languageCombo = new Combo(languageGroup, SWT.DROP_DOWN | SWT.READ_ONLY);
		for (var language : Labels.LANGUAGES) {
			languageCombo.add(Labels.getLabel("language." + language));
		}
		languageCombo.select(0);

		label = new Label(languageGroup, SWT.NONE);
		label.setText(Labels.getLabel("preferences.language.someIncomplete"));

		versionCheckCheckbox = new Button(displayTab, SWT.CHECK);
		versionCheckCheckbox.setText(Labels.getLabel("preferences.versionCheck"));

		allowReports = new Button(displayTab, SWT.CHECK);
		allowReports.setText(Labels.getLabel("preferences.allowReports"));
	}
	
	/**
	 * This method initializes portsTab	
	 */
	private void createPortsTab() {
		var rowLayout = createRowLayout();
		portsTab = new Composite(tabFolder, SWT.NONE);
		portsTab.setLayout(rowLayout);
		
		var groupLayout = new GridLayout();
		groupLayout.numColumns = 2;
		var timingGroup = new Group(portsTab, SWT.NONE);
		timingGroup.setText(Labels.getLabel("preferences.ports.timing"));
		timingGroup.setLayout(groupLayout);

		var gridData = new GridData();
		gridData.widthHint = 50;
		
		var label = new Label(timingGroup, SWT.NONE);
		label.setText(Labels.getLabel("preferences.ports.timing.timeout"));
		portTimeoutText = new Text(timingGroup, SWT.BORDER);
		portTimeoutText.setLayoutData(gridData);
		
		var gridData1 = new GridData();
		gridData1.horizontalSpan = 2;
		adaptTimeoutCheckbox = new Button(timingGroup, SWT.CHECK);
		adaptTimeoutCheckbox.setText(Labels.getLabel("preferences.ports.timing.adaptTimeout"));
		adaptTimeoutCheckbox.setLayoutData(gridData1);
		adaptTimeoutCheckbox.addListener(SWT.Selection, event -> minPortTimeoutText.setEnabled(adaptTimeoutCheckbox.getSelection()));

		label = new Label(timingGroup, SWT.NONE);
		label.setText(Labels.getLabel("preferences.ports.timing.minTimeout"));
		minPortTimeoutText = new Text(timingGroup, SWT.BORDER);
		minPortTimeoutText.setLayoutData(gridData);

		var portsLayout = new RowLayout(SWT.VERTICAL);
		portsLayout.fill = true;
		portsLayout.marginHeight = 2;
		portsLayout.marginWidth = 2;
		var portsGroup = new Group(portsTab, SWT.NONE);
		portsGroup.setText(Labels.getLabel("preferences.ports.ports"));
		portsGroup.setLayout(portsLayout);
		
		label = new Label(portsGroup, SWT.WRAP);
		label.setText(Labels.getLabel("preferences.ports.portsDescription"));
		//label.setLayoutData(new RowData(300, SWT.DEFAULT));
		portsText = new Text(portsGroup, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL);
		portsText.setLayoutData(new RowData(SWT.DEFAULT, 60));
		portsText.addKeyListener(new PortsTextValidationListener());
		
		addRequestedPortsCheckbox = new Button(portsGroup, SWT.CHECK);
		addRequestedPortsCheckbox.setText(Labels.getLabel("preferences.ports.addRequested"));
		addRequestedPortsCheckbox.setToolTipText(Labels.getLabel("preferences.ports.addRequested.info"));
	}

	/**
	 * @return a pre-initialized RowLayout suitable for option tabs.
	 */
	private RowLayout createRowLayout() {
		var rowLayout = new RowLayout();
		rowLayout.type = org.eclipse.swt.SWT.VERTICAL;
		rowLayout.spacing = 9;
		rowLayout.marginHeight = 9;
		rowLayout.marginWidth = 11;
		rowLayout.fill = true;
		return rowLayout;
	}

	private void loadPreferences() {
		// Network settings
		networkInterfaceCombo.select(0);
		if (!guiConfig.startupNetworkInterface.isEmpty()) {
			for (var i = 0; i < availableInterfaces.size(); i++) {
				if (availableInterfaces.get(i).networkInterface().getDisplayName().equals(guiConfig.startupNetworkInterface)) {
					networkInterfaceCombo.select(i + 1);
					break;
				}
			}
		}
		startupModeCombo.select(guiConfig.startupFeederMode + 1); // -1 maps to 0 (auto)
		startupStartIPText.setText(guiConfig.startupRangeStart);
		startupEndIPText.setText(guiConfig.startupRangeEnd);
		startupPrototypeText.setText(guiConfig.startupRandomPrototype);
		startupMaskCombo.setText(guiConfig.startupRandomMask);
		startupCountSpinner.setSelection(guiConfig.startupRandomCount);
		startupFilePathText.setText(guiConfig.startupFilePath);
		updateStartupModeVisibility();

		maxThreadsText.setText(Integer.toString(scannerConfig.maxThreads));
		threadDelayText.setText(Integer.toString(scannerConfig.threadDelay));
		var pingerNames = pingerRegistry.getRegisteredNames();
		for (var i = 0; i < pingerNames.length; i++) {
			if (scannerConfig.selectedPinger.equals(pingerNames[i])) {
				pingersCombo.select(i);
			}
		}
		pingingCountText.setText(Integer.toString(scannerConfig.pingCount));
		pingingTimeoutText.setText(Integer.toString(scannerConfig.pingTimeout));
		deadHostsCheckbox.setSelection(scannerConfig.scanDeadHosts);
		skipBroadcastsCheckbox.setSelection(scannerConfig.skipBroadcastAddresses);
		portTimeoutText.setText(Integer.toString(scannerConfig.portTimeout));
		adaptTimeoutCheckbox.setSelection(scannerConfig.adaptPortTimeout);
		minPortTimeoutText.setText(Integer.toString(scannerConfig.minPortTimeout));
		minPortTimeoutText.setEnabled(scannerConfig.adaptPortTimeout);
		portsText.setText(scannerConfig.portString);
		addRequestedPortsCheckbox.setSelection(scannerConfig.useRequestedPorts);
		notAvailableText.setText(scannerConfig.notAvailableText);
		notScannedText.setText(scannerConfig.notScannedText);
		displayMethod[guiConfig.displayMethod.ordinal()].setSelection(true);
		showInfoCheckbox.setSelection(guiConfig.showScanStats);
		askConfirmationCheckbox.setSelection(guiConfig.askScanConfirmation);
		versionCheckCheckbox.setSelection(guiConfig.versionCheckEnabled);
		allowReports.setSelection(globalConfig.allowReports);
		for (var i = 0; i < Labels.LANGUAGES.length; i++) {
			if (globalConfig.language.equals(Labels.LANGUAGES[i])) {
				languageCombo.select(i);
			}
		}
	}
	
	private void savePreferences() {
		// Save network settings
		int ifaceIndex = networkInterfaceCombo.getSelectionIndex();
		if (ifaceIndex <= 0) {
			guiConfig.startupNetworkInterface = "";
		} else {
			guiConfig.startupNetworkInterface = availableInterfaces.get(ifaceIndex - 1).networkInterface().getDisplayName();
		}
		guiConfig.startupFeederMode = startupModeCombo.getSelectionIndex() - 1; // 0 maps to -1 (auto)
		guiConfig.startupRangeStart = startupStartIPText.getText().trim();
		guiConfig.startupRangeEnd = startupEndIPText.getText().trim();
		guiConfig.startupRandomPrototype = startupPrototypeText.getText().trim();
		guiConfig.startupRandomMask = startupMaskCombo.getText().trim();
		guiConfig.startupRandomCount = startupCountSpinner.getSelection();
		guiConfig.startupFilePath = startupFilePathText.getText().trim();

		// validate port string
		try {
			new PortIterator(portsText.getText());
		}
		catch (Exception e) {
			tabFolder.setSelection(portsTabItem);
			portsText.forceFocus();
			throw new FetcherException("unparseablePortString", e);
		}

		scannerConfig.selectedPinger = (String) pingersCombo.getData(Integer.toString(pingersCombo.getSelectionIndex()));
		scannerConfig.maxThreads = parseIntValue(maxThreadsText);
		scannerConfig.threadDelay = parseIntValue(threadDelayText);
		scannerConfig.pingCount = parseIntValue(pingingCountText);
		scannerConfig.pingTimeout = parseIntValue(pingingTimeoutText);
		scannerConfig.scanDeadHosts = deadHostsCheckbox.getSelection();
		scannerConfig.skipBroadcastAddresses = skipBroadcastsCheckbox.getSelection();
		scannerConfig.portTimeout = parseIntValue(portTimeoutText);
		scannerConfig.adaptPortTimeout = adaptTimeoutCheckbox.getSelection();
		scannerConfig.minPortTimeout = parseIntValue(minPortTimeoutText);
		scannerConfig.portString = portsText.getText();
		scannerConfig.useRequestedPorts = addRequestedPortsCheckbox.getSelection();
		scannerConfig.notAvailableText = notAvailableText.getText();
		scannerConfig.notScannedText = notScannedText.getText();
		for (var i = 0; i < displayMethod.length; i++) {
			if (displayMethod[i].getSelection())
				guiConfig.displayMethod = DisplayMethod.values()[i];
		}
		guiConfig.showScanStats = showInfoCheckbox.getSelection();
		guiConfig.askScanConfirmation = askConfirmationCheckbox.getSelection();
		guiConfig.versionCheckEnabled = versionCheckCheckbox.getSelection();
		globalConfig.allowReports = allowReports.getSelection();
		var newLanguage = Labels.LANGUAGES[languageCombo.getSelectionIndex()];
		if (!newLanguage.equals(globalConfig.language)) {
			globalConfig.language = newLanguage;
			var msgBox = new MessageBox(shell);
			msgBox.setMessage(Labels.getLabel("preferences.language.needsRestart"));
			msgBox.open();
		}
	}

	/**
	 * @return an int from the passed Text control.
	 */
	private static int parseIntValue(Text text) {
		try {
			return Integer.parseInt(text.getText());
		}
		catch (NumberFormatException e) {
			text.forceFocus();
			throw e;
		}
	}
	
	static class PortsTextValidationListener implements KeyListener {
		public void keyPressed(KeyEvent e) {
			var portsText = (Text) e.getSource();
			
			if (e.keyCode == SWT.TAB) {
				portsText.getShell().traverse(SWT.TRAVERSE_TAB_NEXT);
				e.doit = false;
				return;
			}
			else 
			if (e.keyCode == SWT.CR) {
				if ((e.stateMask & SWT.MOD1) > 0) {
					// allow ctrl+enter to insert newlines
					e.stateMask = 0; 
				}
				else {
					// single-enter will traverse
					portsText.getShell().traverse(SWT.TRAVERSE_RETURN);
					e.doit = false;
					return;
				}
			}
			else 
			if (Character.isISOControl(e.character)) {
				return;
			}
			
			e.doit = validateChar(e.character, portsText.getText(), portsText.getCaretPosition());
		}
		
		boolean validateChar(char c, String text, int caretPos) {
			// previous
			char pc = 0;
			for (var i = caretPos-1; i >= 0; i--) {
				pc = text.charAt(i);
				if (!Character.isWhitespace(pc))
					break;
			}

			var isCurDigit = c >= '0' && c <= '9';
			var isPrevDigit = pc >= '0' && pc <= '9';
			return isPrevDigit && (isCurDigit || c == '-' || c == ',') ||
				   isCurDigit && (pc == '-' || pc == ',' || pc == 0) ||
				   Character.isWhitespace(c) && pc == ',';
			
		}

		public void keyReleased(KeyEvent e) {
		}
	}
}
