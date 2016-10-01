/**
*Class:             TFTPClient.java
*Project:           TFTP Project - Group 4
*Author:            Jason Van Kerkhoven                                             
*Date of Update:    29/09/2016                                              
*Version:           1.1.5                                                      
*                                                                                   
*Purpose:           Generates a datagram following the format of [0,R/W,STR1,0,STR2,0],
					in which R/W signifies read (1) or write (2), STR1 is a filename,
					and STR2 is the mode. Sends this datagram to the IntermediateHost
					and waits for response from intermediateHost. Repeats this
					process ten times, then sends a datagram packet that DOES NOT
					follow the expected format stated above. Waits for response from
					IntermediateHost. We DO NOT expect a response to the badly formated
					packet. Datagram can be 512B max
* 
* 
*Update Log:		v1.1.5
*						- recieve method implimented
*						- generateDATA() method patched to send OPcode
*						- client now sends ACKs
*						- port error patched
*						- block numbers working now (shoutout to Nate for the code to do that)
*						- ACKs fixed FOR REAL THIS TIME
*					v1.1.4 
*						- numerous dangerous accessors/mutators removed
*						  (they were [and should] never called)
*						- TFTPWriter class implemented
*						- ACKs now received (their validity not checked)
*						- method printDatagram(..) added for ease of printing
*						- method readAndEcho() repurposed to readPacket()
*					v1.1.3
*						- verbose formating altered
*						- RRQ/WRQ method now uses class constants as opposed to magic numbers
*						  (code smell removed)
*						- both generateRWRW(..) and generateDATA changed from public --> private
*						- sendAndEcho() renamed sendPacket() for naming accuracy, also now private
*						- IN_PORT_ERRORSIM renamed to IN_PORT_HOST to keep up proper naming conventions
*					v1.1.2
*						- now implements TFTPReader class
*						- can read data from file
*						- DOES NOT SEND ACKS
*						- DOES NOT HAVE BLOCK NUMS
*						- bug with java.nullpointer exception in send() method fixed
*						- some methods renamed
*						- generateDatagram(..) replaced with 2 new methods:
*								generateRWRQ(...) | for RRQ or WRQ
*								generateDATA()	  | for DATA
*						- sendAndEcho() is now private, only called from master send() method
*						- a ton of constants added (see class constants section)
*						- a bunch of old test code commented out - will remove in future when sure it
*						  will not be necessary
*					v1.1.0
*						- verbose mode added (method added)
*						- client can now send datagrams to errorSim OR
*						  directly to server (ie method testMode)
*						- renamed constant PORT --> IN_PORT_ERRORSIM
*						- added cnst IN_PORT_SERVER
*						- added var outPort
*						- added method testMode(...)
*						- added var verbose
*						- close method added
						- name changed from 'Client' to 'TFTPClient'
						 (are you happy now Sarah??!?!?!?!?!)
*					v1.0.0
*                       - null
*/

/*
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ TO DO ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~(as of v1.1.3)
 *		[x] !!!!!!!!FIX CLIENT RECEIVE ERROR!!!!!!!!
 *		[x] Add receiving and checking of ACKs
 *		[ ] Further check reader to make sure it is sending the correct packets
 *		[ ] Patch receive method with a master method that can handle multiple packets
 *		[x] Figure out what to do with all incoming packets
 *		[ ] Add packet numbering (is it a 16bit int or a 0byte followed by a 8bit number????)
 *		[ ] Test functionality w/ modified server
 *		[ ] Integrate with UI
 *		[ ] Write a test class if time permits (????)
 *		[ ] Update ReadMe.txt
 *		[x] TFTPWriter class
 */


//import external libraries
import java.io.*;
import java.net.*;
import java.util.Scanner;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JTextArea;


public class TFTPClient extends JFrame
{
	//declaring local instance variables
	private DatagramPacket sentPacket;
	private DatagramPacket recievedPacket;
	private DatagramSocket generalSocket;
	private boolean verbose;
	private int outPort;
	private TFTPReader reader;
	private TFTPWriter writer;
	private static Scanner scan= new Scanner(System.in);
	private static JTextArea fileChooserFrame;
	private static File file;
	private static JFileChooser fileChooser;
	
	//declaring local class constants
	private static final int IN_PORT_HOST = 23;
	private static final int IN_PORT_SERVER = 69;
	private static final int MAX_SIZE = 512;
	private static final byte[] OPCODE_RRQ =  {0,1}; 
	private static final byte[] OPCODE_WRQ =  {0,2};
	private static final byte[] OPCODE_DATA = {0,3};
	private static final byte[] OPCODE_ACK = {0,4};
	
	
	//generic constructor
	public TFTPClient()
	{
		//construct a socket, bind to any local port
		try
		{
			generalSocket = new DatagramSocket();
		}
		//enter if socket creation results in failure
		catch (SocketException se)
		{
			se.printStackTrace();
			System.exit(1);
		}
		//initialize echo --> off
		verbose = false;
		//initialize test mode --> off
		outPort = IN_PORT_SERVER ;
		//make an empty reader
		reader = new TFTPReader();
		//make an empty writer
		writer = new TFTPWriter();
	}
	
	
	//generic accessors and mutators
	public DatagramPacket getSentPacket()
	{
		return sentPacket;
	}
	public DatagramPacket getRecievedPacket()
	{
		return recievedPacket;
	}
	
	
	//enable/disable verbose mode
	public void verboseMode(boolean v)
	{
		verbose = v;
	}
	
	
	//enable/disable test mode
	public void testMode(boolean t)
	{
		//test mode ON
		if (t)
		{
			outPort = IN_PORT_HOST;
		}
		//test mode OFF
		else
		{
			outPort = IN_PORT_SERVER;
		}
	}
	
	
	//close client properly
	//***FUNCTIONALITY OF CLIENT WILL CEASE ONCE CALLED***
	public void close()
	{
		//close sockets
		generalSocket.close();
	}
	
	
	//generate DatagramPacket, save as sentPacket
	//type: DATA
	private void generateDATA(int blockNum)
	{
		//prep for block num
		byte[] blockNumArr = new byte[2];
		blockNumArr[0]=(byte)(blockNum & 0xFF);
		blockNumArr[1]=(byte)((blockNum >> 8)& 0xFF);
	    
		if(verbose)
		{
			System.out.println("Client: Prepping DATA packet #" + "NULL");
		}
		
		//construct array to hold data
		byte[] data = reader.pop();
		byte[] toSend = new byte[data.length + 4];
		
		//constuct array
		for(int i=0; i<2; i++)
		{
			toSend[i] = OPCODE_DATA[i];
		}
		for(int i = 2; i < 4; i++)
		{
			toSend[i] = blockNumArr[i-2] ;
		}
		for(int i = 0; i < data.length; i++)
		{
			toSend[i+4] = data[i] ;
		}
		
		//generate and save datagram packet
		try
		{

			sentPacket = new DatagramPacket(data, data.length, InetAddress.getLocalHost(), outPort);

			sentPacket = new DatagramPacket(toSend, toSend.length, InetAddress.getLocalHost(), outPort);
			if(verbose)
			{
				System.out.println("Client: Packet successfully created");
			}
		}
		catch(UnknownHostException e)
		{
			e.printStackTrace();
			System.exit(1);
		}
		
	}
	
	
	//generate ACK
	private void generateACK(byte[] ACKNum)
	{
		byte[] ack = new byte[4];
		ack[0] = OPCODE_ACK[0];
		ack[1] = OPCODE_ACK[1];
		//add block num
		ack[2] = ACKNum[0];
		ack[3] = ACKNum[1];
		
		//generate and save datagram packet
		try
		{
			sentPacket = new DatagramPacket(ack, ack.length, InetAddress.getLocalHost(), outPort);
			if(verbose)
			{
				System.out.println("Client: Packet successfully created");
			}
		}
		catch(UnknownHostException e)
		{
			e.printStackTrace();
			System.exit(1);
		}
		
	}
	
	
	//generate DatagramPacket, save as sentPacket
	//type: RRW or WRQ
	private void generateRWRQ(String fileName, String mode, byte[] RWval)
	{
		//generate the data to be sent in datagram packet
		if(verbose)
		{
			System.out.println("Client: Prepping packet containing '" + fileName + "'...");
		}	
		//convert various strings to Byte arrays
		byte[] fileNameBA = fileName.getBytes();
		byte[] modeBA = mode.getBytes();
			
		//compute length of data being sent (metadata include) and create byte array
		byte[] data = new byte[fileNameBA.length + modeBA.length + 4];
		int i = 2;
			
		//add first 2 bytes of opcode
		for(int c=0; c<2; c++)
		{
			data[c] = RWval[c] ;
		}
		//add text
		for(int c=0; c<fileNameBA.length; c++, i++)
		{
			data[i] = fileNameBA[c];
		}
		//add pesky 0x00
		data[i] = 0x00;
		i++;
		//add mode
		for(int c=0; c<modeBA.length; c++, i++)
		{
			data[i] = modeBA[c];
		}
		//add end metadata
		data[i] = 0x00;
			
		
		//generate and save datagram packet
		try
		{
			sentPacket = new DatagramPacket(data, data.length, InetAddress.getLocalHost(), outPort);
			if(verbose)
			{
				System.out.println("Client: Packet successfully created");
			}
		}
		catch(UnknownHostException e)
		{
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	
	//send datagram, recieve ACKs
	public void sendWRQ(String file, String mode)
	{
		//initial
		int blockNum = 1;
		int oldPort = outPort;
		
		//read and split file
		try
		{
			reader.readAndSplit(file);
			
		}
		catch (IOException e)
		{
			e.printStackTrace();
			System.exit(1);
		}
		
		//prep RRQ/RRW to send
		generateRWRQ(file, mode, OPCODE_WRQ);
		//send RRQ/RRW
		sendPacket();
		//wait for ACK
		receivePacket("ACK");
		//change port to wherever ACK came from
		outPort = recievedPacket.getPort();
		
		//send DATA
		while ( !(reader.isEmpty()) )
		{
			//send DATA
			generateDATA(blockNum);
			sendPacket();
			
			//wait for ACK
			receiveACK();
			
		}
		
		//reset port
		outPort = oldPort;
	}
	
	
	//send a single packet
	private void sendPacket()
	{
		//print packet info IF in verbose
		if(verbose)
		{
			byte[] data = sentPacket.getData();
			int packetSize = sentPacket.getLength();
			System.out.println("Client: Sending packet...");
			System.out.println("        Host:  " + sentPacket.getAddress());
			System.out.println("        Port:  " + sentPacket.getPort());
			System.out.println("        Bytes: " + sentPacket.getLength());
			System.out.printf("%s", "        Cntn:  ");
			for(int i = 0; i < packetSize; i++)
			{
				System.out.printf("0x%02X", data[i]);
				System.out.printf("%-2c", ' ');
			}
			System.out.println("");
			System.out.println("        Cntn:  " + (new String(data,0,packetSize)));
			
		}
		//send packet
		try
		{
			generalSocket.send(sentPacket);
		}
		catch (IOException e)
		{
			e.printStackTrace();
			System.exit(1);
		}
		System.out.println("Client: Packet Sent");
	}
	
	
	//receive ACK
	public void receiveACK()
	{		
		//receive ACK
		receivePacket("ACK");
		
		//analyze ACK for format
		if (verbose)
		{
			System.out.println("Client: Checking ACK...");
		}
		byte[] data = recievedPacket.getData();
		
		//print data if verbose
		if (verbose)
		{
			printDatagram(recievedPacket);
		}
		
		//check ACK for validity
		// _________________PUT CODE HERE_____________
	}
	
	
	//receive data and save
	public void sendRRQ(String file, String mode)
	{
		int oldPort = outPort;
		
		//send read request
		generateRWRQ(file, mode, OPCODE_RRQ);
		sendPacket();
		
		//receive loop for data
		byte[] data;
		boolean loop = true;
		while(loop)
		{
			receivePacket("DATA");
			outPort = recievedPacket.getPort();
			data = recievedPacket.getData();
			byte[] blockNum = new byte[2];
			
			//check to see if this is final packet
			if (data.length < MAX_SIZE+4)
			{
				loop = false;
			}
			
			//get block num
			blockNum[0] = data[2];
			blockNum[1] = data[3];
			
			//send out ACK and prep for more data
			generateACK(blockNum);
			sendPacket();
		}
		outPort = oldPort;
	}
	
	
	//receive and echo received packet
	public void receivePacket(String type)
	{	
		//prep for response
		byte[] response = new byte[MAX_SIZE+4];
		recievedPacket = new DatagramPacket(response, response.length);
		
		//wait for response
		if (verbose)
		{
			System.out.println("Client: Waiting for " + type + " packet...");
		}
		try
		{
			generalSocket.receive(recievedPacket);
		}
		catch(IOException e)
		{
			e.printStackTrace();
			System.exit(1);
		}
		if (verbose)
		{
		System.out.println("Client: " + type + " packet received");
		}
		
		//Process and print the response
		if(verbose)
		{
			printDatagram(recievedPacket);
		}
	}
	

	//print datagram contents
	private void printDatagram(DatagramPacket datagram)
	{
		byte[] data = datagram.getData();
		int packetSize = datagram.getLength();
		System.out.println("        Source: " + recievedPacket.getAddress());
		System.out.println("        Port:   " + recievedPacket.getPort());
		System.out.println("        Bytes:  " + packetSize);
		System.out.printf("%s", "        Cntn:  ");
		for(int i = 0; i < packetSize; i++)
		{
			System.out.printf("0x%02X", data[i]);
			System.out.printf("%-2c", ' ');
		}
		System.out.println("\n        Cntn:  " + (new String(data,0,packetSize)));
	}
	
	
	public static void main (String[] args) 
	{
		//declaring local variables
		TFTPClient client = new TFTPClient();
		byte flipFlop = 0x01;
		fileChooserFrame = new JTextArea(5,40);
		fileChooser = new JFileChooser();
		
		//Find whether you want to run in test mode or not
		System.out.println("Test mode: (T)rue or (F)alse?");
		String testBool = scan.nextLine();
		if (testBool.equals("T")) client.testMode(true);
		else {client.testMode(false);}
		
		//Find whether you want to run in verbose mode or not
		System.out.println("Verbose mode: (T)rue or (F)alse?");
		String verboseBool = scan.nextLine();
		if (verboseBool.equals("T")) client.verboseMode(true);
		else {client.verboseMode(false);}
		
		fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));
		int result = fileChooser.showOpenDialog(fileChooser);
		if (result == JFileChooser.APPROVE_OPTION) {//file is found
		    file = fileChooser.getSelectedFile();//get file name
		}
		
		//send full fille (includes wait for ACK)
		client.sendWRQ("1ByteDataTest.txt", "octet");

		
		
		
		
		
		/*
		while (reader.peek() != null)
		{
			//generate datagram RRW
			client.generateDatagram("DatagramsOutForHarambe.txt","octet", flipFlop);
		
			//send and echo outgoing datagram
			client.sendAndEcho();
		
			//idle until packet is received, echo and and save
			client.receiveAndEcho();
			
			//flip R/W byte
			if (flipFlop == 0x01)
			{
				flipFlop = 0x02;
			}
			else
			{
				flipFlop = 0x01;
			}
			
			System.out.println("----------------------------------------\n");
		}
		*/
		
		/*
		//generate and send bad datagram
		client.generateDatagram("gArBaGe.trash","trascii", (byte)0x05);
		client.sendAndEcho();
		client.receiveAndEcho();
		*/
	}
}