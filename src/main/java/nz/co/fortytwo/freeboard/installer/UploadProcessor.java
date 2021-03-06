/*
 * Copyright 2012,2013 Robert Huitema robert@42.co.nz
 * 
 * This file is part of FreeBoard. (http://www.42.co.nz/freeboard)
 * 
 * FreeBoard is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * FreeBoard is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with FreeBoard. If not, see <http://www.gnu.org/licenses/>.
 */
package nz.co.fortytwo.freeboard.installer;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.swing.JTextArea;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.log4j.Logger;

/**
 * Uploads hex files to the arduinos
 * 
 * @author robert
 * 
 */
public class UploadProcessor {

	Logger logger = Logger.getLogger(UploadProcessor.class);
	// Properties config = null;
	private boolean manager = false;
	private JTextArea textArea;

	public UploadProcessor() throws Exception {

	}

	public UploadProcessor(boolean manager, JTextArea textArea) throws Exception {
		this.manager = manager;
		this.textArea = textArea;
	}

	public void processUpload(File hexFile, String commPort, String device, String dudeDir) throws Exception {
		// make a file
		if (!hexFile.exists()) {
			if (manager) {
				System.out.print("No file at " + hexFile.getAbsolutePath() + "\n");
			}
			logger.error("No file at " + hexFile.getAbsolutePath());
		}
		
			processHexFile(hexFile,commPort,device, dudeDir);
	
	}
	

	/**
	 * Upload the Hex file to the named port
	 * 
	 * @param hexFile
	 * @param device 
	 * @throws Exception
	 */
	public void processHexFile(File hexFile, String commPort, String device, String dudeDir) throws Exception {

		if (manager) {
			System.out.print("Uploading:" + hexFile.getPath() + "\n");
		}

		logger.debug("Uploading hex file:" + hexFile.getPath());
		// start by running avrdude
		//bayeans on eclipse windows
		//C:\Program Files\Arduino\hardware\tools\avr\bin\avrdude -patmega2560 -cwiring "-P\\.\COM6" -b115200 -Uflash:w:FreeBoardPLC.hex:a "-CC:\Program Files\Arduino\hardware\tools\avr\etc\avrdude.conf" 
		//ArduIMU - linux
		//tools/avrdude -patmega328p -carduino -P/dev/ttyUSB0 -b57600 -D -q -q -v -v -v -v -Uflash:w:FreeBoardIMU.cpp.hex:i -C$ARDUINO_HOME/hardware/tools/avrdude.conf
		
		//tools/avrdude -patmega1280 -carduino -P/dev/ttyUSB0 -b57600 -D -v -v -v -v -Uflash:w:FreeBoardPLC.hex:a -C$ARDUINO_HOME/hardware/tools/avrdude.conf
		
			String pDevice = "-p"+device;
			String avrdude = "avrdude";
			String avrType = ":a";
			if(device.equals("atmega328p")){
				avrType=":i";
			}
			String executable = dudeDir + File.separator + avrdude;
			String conf = "-C" + dudeDir + File.separator+"avrdude.conf";
			String baudRate = "-b57600";
			String programmer = "-carduino";
			//change for mega 2560
			if(device.equals("atmega2560")){
				programmer = "-cwiring";
			}
			if(SystemUtils.IS_OS_MAC_OSX){
				/*
				 I managed to program the atmega2560 on OSX using avrdude from the Arduino files with this string:
					/Applications/Arduino.app/Contents/Resources/Java/hardware/tools/avr/bin/avrdude -C/Applications/Arduino.app/Contents/Resources/Java/hardware/tools/avr/etc/avrdude.conf -v -v -v -v -patmega2560 -cwiring -P/dev/tty.usbmodem1d21 -b115200 -D -Uflash:w:/Users/freddie/Desktop/Arduino_Sensors_Stuff/Freeboard_System/freeboardPLC-master/Release2560/FreeBoardPLC.hex:i 
				 */
				//on osx /Applications/Arduino.app/Contents/Resources/Java/hardware/tools/avr/bin/avrdude
				logger.debug("We are on a Mac :-)");
				logger.debug("Ignoring selected arduino directory, using \"/Applications/Arduino.app/Contents/Resources/Java/hardware/tools/avr\" instead");
				executable="/Applications/Arduino.app/Contents/Resources/Java/hardware/tools/avr/bin/avrdude";
				conf="-C/Applications/Arduino.app/Contents/Resources/Java/hardware/tools/avr/etc/avrdude.conf";
				baudRate = "-b115200";
				programmer = "-cwiring";
			}
			if(SystemUtils.IS_OS_WINDOWS){
				//wrap in "" for the stupid windoze spaces in filenames
				//check they exist
				if(!new File(executable).exists()){
					logger.debug("Cant find avrdude executable at : "+executable+", trying in avr/bin");
					//lets try avr/bin
					executable = dudeDir + File.separator +"avr"+ File.separator+"bin"+ File.separator+ avrdude;
					if(!new File(executable).exists()){
						logger.debug("Cant find avrdude executable at :"+executable);
					}
				}
				if(!new File(conf).exists()){
					logger.debug("Cant find avrdude.conf at : "+conf+", trying in avr/etc");
					//lets try avr/bin
					conf = "-C" + dudeDir + File.separator +"avr"+ File.separator+"etc"+File.separator+"avrdude.conf";
					if(!new File(executable).exists()){
						logger.debug("Cant find avrdude.conf at :"+executable);
					}
				}
				executable="\""+executable+"\"";
				conf="\""+conf+"\"";
				//we need "-P\\.\COM6", or "-PCOM6"
				//commPort="\"-P\\\\.\\"+commPort+"\"";
				commPort="\"-P"+commPort+"\"";
			}else{
				commPort="-P"+commPort;
			}
		executeAvrdude(hexFile, Arrays.asList(executable, pDevice, programmer, commPort, baudRate , "-D", "-v", "-v", "-v",
				"-Uflash:w:"+hexFile.getName()+avrType, conf));

	}

	/**
	 * Executes avrdude with relevant params
	 * 
	 * @param config2
	 * @param hexFile
	 * @param argList
	 * @param chartName
	 * @param list
	 * @throws IOException
	 * @throws InterruptedException
	 */
	@SuppressWarnings("static-access")
	private void executeAvrdude(File hexFile, List<String> argList) throws IOException, InterruptedException {

		ProcessBuilder pb = new ProcessBuilder(argList);
		pb.directory(hexFile.getParentFile());
		//pb.inheritIO();
		if (manager) {
			ForkWorker fork = new ForkWorker(textArea, pb);
			fork.execute();
			//fork.doInBackground();
			while (!fork.isDone()) {
				Thread.currentThread().sleep(500);
				// System.out.print(".");
			}
			if(fork.getResult()==0){
				System.out.print("Avrdude completed normally, and the code has been uploaded\n");
			}else{
				System.out.print("ERROR: avrdude did not complete normally, and the device may not work correctly\n");
			}
		} else {
			Process p = pb.start();
			p.waitFor();
			if (p.exitValue() > 0) {
				if (manager) {
					System.out.print("ERROR: avrdude did not complete normally\n");
				}
				logger.error("avrdude did not complete normally");
				return;
			} else {
				System.out.print("Completed upload\n");
			}
		}

	}

}
