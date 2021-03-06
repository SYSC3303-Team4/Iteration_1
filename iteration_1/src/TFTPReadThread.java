/**
 *Class:             TFTPReadThread.java
 *Project:           TFTP Project - Group 4
 *Author:            Nathaniel Charlebois                                            
 *Date of Update:    29/09/2016                                              
 *Version:           1.1.0                                                      
 *                                                                                    
 *Purpose:           Handles the RRQs by:
 *						-Sending DATA
 *						-Receiving ACK
 *
 *To do:
 *	-Clean up code 
 *	-Test functionality with other components
 *	-Use the UI
 *
 *					
 * 
 * 
 *Update Log:    	v1.1.1
 *                       - null
 *                       
 *                   v1.1.2
 *                   	- Cleaned up code
 *                   	- Fixed Block numbers
 */
import java.io.ByteArrayOutputStream;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.*;

import javax.swing.JTextArea;

class TFTPReadThread  extends ServerThread implements Runnable
{
	/**
	 * The text area where this thread's output will be displayed.
	 */
	private JTextArea transcript;
	private DatagramPacket receivePacket;
	private DatagramPacket sendPacket;
	private DatagramSocket sendReceiveSocket;
	private boolean verbose;
	private int blockNumber = 1;
	private String threadNumber;
	public static final byte[] response = {0, 3, 0, 0};


	public TFTPReadThread(JTextArea transcript, DatagramPacket receivePacketInfo, String thread, Boolean verboseMode) {
		this.transcript = transcript;
		receivePacket = receivePacketInfo;
		threadNumber  = thread;
		verbose = verboseMode;
		try {
			sendReceiveSocket = new DatagramSocket();
		} catch (SocketException e) {
			// TODO Auto-generated catch block    
			e.printStackTrace();
		}
	}

	public void run() {

		System.out.println("Server: Received packet:");
		if(verbose){
		System.out.println("From host: " + receivePacket.getAddress());
		System.out.println("From host port: " + receivePacket.getPort());
		System.out.println("Length: " + receivePacket.getLength());
		System.out.println("Containing: ");
		System.out.println(new String(receivePacket.getData(),0,receivePacket.getLength()));
		}
		/* Exit Gracefully if the stop is requested. */
		if(stopRequested){exitGraceFully();}
		//Parsing Data for filename
		ByteArrayOutputStream filename = new ByteArrayOutputStream();
		ByteArrayOutputStream mode = new ByteArrayOutputStream();
		boolean change = false; 
		for(int i = 2; i<receivePacket.getData().length;i++){
			if(stopRequested){exitGraceFully();}
			if(receivePacket.getData()[i]>=32){
				if(change == false){
					filename.write(receivePacket.getData()[i]);
				}
				else{
					mode.write(receivePacket.getData()[i]);
				}
			}
			if(receivePacket.getData()[i]!=0){
				if(receivePacket.getData()[i+1] == 0){
					change = true;
					i++;
				}
			}
		}
		

		/* Exit Gracefully if the stop is requested. */
		if(stopRequested){exitGraceFully();}
		if(verbose){
		System.out.println("Request parsed for:");
		System.out.println("	Filename: " + new String(filename.toByteArray(),0,filename.toByteArray().length));
		System.out.println("	Mode: " + new String(mode.toByteArray(),0,mode.toByteArray().length) + "\n");
		}

		TFTPReader reader = new TFTPReader();
		try {
			reader.readAndSplit(filename.toString());

		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		while(true){
			int len;




			//Encode the block number into the response block 
			response[3]=(byte)(blockNumber & 0xFF);
			response[2]=(byte)((blockNumber >> 8)& 0xFF);
			blockNumber++;

			//Building datagram		


			byte[] data = reader.pop();
			/* If there's no more data to be read exit. */
			if(data == null){exitGraceFully();}



			//Builds the datagram in format
			/*
			2 bytes    2 bytes       n bytes 
			---------------------------------
		 DATA  | 03    |   Block #  |    Data    |
			---------------------------------
			 */

			byte dataPrime[] = Arrays.copyOf(response, response.length + data.length); 
			System.arraycopy(data, 0, dataPrime, response.length, data.length);

			/* Exit Gracefully if the stop is requested. */
			if(stopRequested){exitGraceFully();}
			sendPacket = new DatagramPacket(dataPrime, dataPrime.length,
					receivePacket.getAddress(), receivePacket.getPort());
			len = sendPacket.getLength();
			System.out.println("Server: Sending packet:");
			if(verbose){
			System.out.println("To host: " + sendPacket.getAddress());
			System.out.println("Destination host port: " + sendPacket.getPort());

			System.out.println("Length: " + len);
			System.out.println("Containing: ");
			System.out.println(Arrays.toString(sendPacket.getData()));
			}

			// Send the datagram packet to the client via a new socket.


			try {
				sendReceiveSocket.send(sendPacket);
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1); 
			}

			/* Exit Gracefully if the stop is requested. */
			if(stopRequested){exitGraceFully();}
			if(verbose){
			System.out.println("Server: packet sent using port " + sendReceiveSocket.getLocalPort());
			System.out.println();
			}

			//Waiting to receive ACK
			try {
				sendReceiveSocket.receive(receivePacket);
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			} 
			System.out.println("Server: Received packet:");
			if(verbose){
			System.out.println("From host: " + receivePacket.getAddress());
			System.out.println("From host port: " + receivePacket.getPort());
			System.out.println("Length: " + receivePacket.getLength());
			System.out.println("Containing: ");
			System.out.println(new String(receivePacket.getData(),0,receivePacket.getLength()));
			/* Exit Gracefully if the stop is requested. */
			if(stopRequested){exitGraceFully();}
			System.out.println("Request parsed for:");
			System.out.println("	Filename: " + new String(filename.toByteArray(),
					0,filename.toByteArray().length));
			System.out.println("	Mode: " + new String(mode.toByteArray(),
					0,mode.toByteArray().length) + "\n");
			}
			//Check for ACK in format  
			/*
			2 bytes    2 bytes
			-------------------
		 ACK   | 04    |   Block #  |
			--------------------
			 */
			if(receivePacket.getData()[0] == 0 && receivePacket.getData()[1] == 4){
				blockNumber++;
			}
		}


		// We're finished with this socket, so close it.
		//sendSocket.close();

		//transcript.append(Thread.currentThread() + " finished\n");
	}

}