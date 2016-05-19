/* 
Chat Server
Server that allows Chat clients to connect to make a chat room.
Must have port forward port 9998(Text chat) and port 9997(File Transfer)
Allows for both text chat and file transfer.
Author: Dillon Johnson
Version: 1.2
*/
//package server;

import java.awt.Color;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.text.DefaultCaret;
import javax.swing.JPanel;
import javax.swing.JFrame;
import javax.swing.BorderFactory;
import javax.swing.border.Border;
import java.io.*;
import java.net.*;
import java.util.*;
import java.text.*;
import java.nio.file.*;


public class DChatServer extends JFrame{
	private static JPanel panel;
	private static JTextPane chatfield;
	private static JScrollPane chatfieldscroll;
	
	public DChatServer() throws IOException{ //Build Server window
		panel=new JPanel();
		chatfield=new JTextPane();
		chatfield.setEditable(false);
		chatfieldscroll = new JScrollPane(chatfield);
		this.setSize(485,355);
		this.setVisible(true);
		panel.setLayout(null);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		this.add(panel);
		chatfieldscroll.setBounds(10, 10, 450, 295);
		chatfield.setBounds(10, 10, 450, 295);
		Border border = BorderFactory.createLineBorder(Color.BLACK);
		chatfield.setBorder(BorderFactory.createCompoundBorder(border, BorderFactory.createEmptyBorder(2,2,2,2)));
		DefaultCaret caret = (DefaultCaret) chatfield.getCaret();
		caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
		panel.add(chatfieldscroll);
		this.setTitle("DChat Server");
	}

    private static final int textport = 9998;
	private static final int fileport = 9997;
    private static HashSet<String> usernames = new HashSet<String>();
    private static HashSet<PrintWriter> outputs = new HashSet<PrintWriter>();

    public static void main(String[] args) throws Exception {
		new DChatServer();
		chatfield.setText(chatfield.getText()+"DChat Server Online!"+'\n');
		
		//Get public IP Address
		URL whatismyip = new URL("http://checkip.amazonaws.com");
		BufferedReader in = new BufferedReader(new InputStreamReader(whatismyip.openStream()));
		String ip = in.readLine(); //you get the IP as a String
		chatfield.setText(chatfield.getText()+"Server IP Address: "+ip+'\n');
		chatfield.setText(chatfield.getText()+"Port: "+textport+" (Text Chat)"+'\n');
		chatfield.setText(chatfield.getText()+"Port: "+fileport+" (File Transfer)"+'\n');
		chatfield.setText(chatfield.getText()+"Remember to Port Foward!"+'\n');
		in.close();
        //System.out.println("Starting Chat Server");
        ServerSocket listener = new ServerSocket(textport);
        try {
            while (true) {
                new Handler(listener.accept()).start();
            }
        } finally {
            listener.close();
        }
    }

    private static class Handler extends Thread {
        private String username, filepath;
		private File f;
        private Socket socket, filesocket;
		private ServerSocket fileserversocket;
        private BufferedReader in;
        private PrintWriter out;
		private DataInputStream finput;
		private FileOutputStream fstream;
		private DataOutputStream foutput;
		private BufferedInputStream binput;
		private Date date;
		private String input;
		int filetransfer = 0;
		SimpleDateFormat dateformat = new SimpleDateFormat ("h:mm");

        public Handler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(
                    socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                while (true) { //SUBMITNAME Username certification for new User
                    out.println("SUBMITNAME");
                    username = in.readLine();
                    if (username == null) {
                        return;
                    }
                    synchronized (usernames) {
                        if (!usernames.contains(username)) {
                            usernames.add(username);
                            break;
                        }
                    }
                }
				newUserJoin(username);//Welcome message to new User
				
                while (true) { //loop to receive messages
                    input = in.readLine();
					date = new Date();
                    if (input == null) { //no message
                        return;
                    }
					
					//File Transfer
					if(input.equals("fsend")){// || filetransfer == 1){//indicator to receive a file
						String fname = in.readLine();//receive file name first
						//if (filetransfer == 0){//haven't receive file yet
							receiveFile(fname);
							filetransfer = 1;
						//}
						//else {
							sendFile(username,fname, out);
							filetransfer = 0;
						//}
					}
					else { //sending text message to users
						sendText(username, input);
					}
				}
				
            } 
			catch (IOException e) {
                //System.out.println(e);
            } 
			finally {// A Client is disconnecting
                userExit(username, out);
            }
        }
		
		public void newUserJoin(String username){
		//Initial Server Messages
            out.println("Welcome to the DChatServer!");
            outputs.add(out);
			date = new Date();
			chatfield.setText(chatfield.getText()+ "[" + dateformat.format(date) + "] " +
									username + " has joined the chat room. (IP: "+ 
									socket.getRemoteSocketAddress().toString() + ")"+'\n');
			chatfield.setText(chatfield.getText()+"[" + dateformat.format(date) + "] "+
									outputs.size() + " user(s) on the server."+'\n');
				
			if(outputs.size() == 1){ //empty chat room
				out.println("The chat room is currently empty.");
			}
			if(outputs.size() == 2){ // 1 user currently in chat
				out.println("There is 1 user in the chat room.");
			}
			if(outputs.size() > 2){ //2 of more users in chat
				out.println("There are " + (outputs.size()-1) + " users in the chat room.");
			}
			for(PrintWriter output : outputs) { //notify chat room users new user has entered
					if (output != out){
						output.println(username);
						output.println(" has joined the chat.");
					}
            }
		}
		
		public void sendText(String username, String text){//send text to all users in chat
			chatfield.setText(chatfield.getText()+"[" + dateformat.format(date) + "] " +
							username + ": " + text +'\n');
			for (PrintWriter output : outputs) {
				output.println(username + ": ");
				output.println(text);
			}
		}//end sendText()
		
		public void receiveFile(String fname){
		try{
			chatfield.setText(chatfield.getText()+"[" + dateformat.format(date) + "] " +
								username + " is sending the file: " + fname+'\n');
						
			fileserversocket = new ServerSocket(fileport);//setup file transfer socket
			filesocket = fileserversocket.accept();
		
			finput = new DataInputStream(filesocket.getInputStream());
			fstream = new FileOutputStream(fname);
			byte[] filebytes = new byte[8192];
			int count;
			while ((count = finput.read(filebytes))>0)
			{
				fstream.write(filebytes,0,count);
			}
			fstream.flush();
			chatfield.setText(chatfield.getText()+"[" + dateformat.format(date) + "] " +
								"File Received"+'\n');
			fstream.close();
			finput.close();
			filesocket.close();
			//fileserversocket.close();
		
		}
		catch (Exception eFile){
			chatfield.setText(chatfield.getText()+"[" + dateformat.format(date) + "] " +
							"Error Receiving File: " + eFile.toString() +'\n');
		}
		}//end receiveFile()
		
		public void sendFile(String username, String fname, PrintWriter out){//Ask and Send file to each other user
			for (PrintWriter output : outputs) {//Tell the user about file transfer
				if (output == out){//message to original file sender
					output.println("Server: ");
					output.println("File was received successfully.");
					try{
						Thread.sleep(500);
					}
					catch(Exception e) {}
					output.println("Server: ");
					output.println("Sending file to other users.");
				}
				else {//ask and send file to other users
					/* output.println(username);
					output.println(" wants to send you the file: " + fname);
					output.println("Do you want to receive the file ");
					output.println("(send Yes or No)?");
					try{
						input = in.readLine();
					}
					catch (Exception eFileAns){
						chatfield.setText(chatfield.getText()+"[" + dateformat.format(date) + "] " +
							"Error Sending Answer: " + eFileAns.toString() +'\n');
					}
					String [] useranswer = input.split(":");
					if (useranswer[1].trim().startsWith("y") || useranswer[1].trim().startsWith("Y")){//user wants the file
					//if (input.trim().startsWith("y") || input.trim().startsWith("Y")){//user wants the file
						//output.println("fsend");
						output.println(username + ": ");
						output.println("Sends File");
					}
					else {
						out.println(useranswer[0] + ": ");
						out.println("did not accept the file transfer.");
						//out.println("A user did not accept the file transfer.");
						//output.println("");
					} */
					
					filepath = System.getProperty("user.dir") + "\\" + fname;
					chatfield.setText(chatfield.getText()+"Sending file: "+ fname + '\n');	
					File f = new File(filepath);
					//chatfield.setText(chatfield.getText()+"File path: "+ filepath + '\n');
					if(!f.exists() || f.isDirectory())
					{//checking if file exists
						chatfield.setText(chatfield.getText()+ "File does not exist: "+ fname + '\n');
					}
					else{
						byte[] filebytes = new byte[8192];
						try{//sending the file bytes
								output.println(username);
								output.println(" is sending you the file: " + fname);
								Thread.sleep(500);
								output.println("fsend");//indicator that I am sending a file
								output.println(f.getName());//sending the file name first
				
								filesocket = fileserversocket.accept();
								
								binput = new BufferedInputStream(new FileInputStream(f));
								foutput = new DataOutputStream(filesocket.getOutputStream());
								int count = 0;
								while ((count = binput.read(filebytes))>0)
								{
									foutput.write(filebytes,0,count);
									foutput.flush();
								}
								chatfield.setText(chatfield.getText()+"File Send Complete"+ '\n');
								foutput.close();
								binput.close();
								filesocket.close();
								//fileserversocket.close();
						} 
						catch (Exception e)
						{
							chatfield.setText(chatfield.getText()+"File Send Failed"+ '\n');
							chatfield.setText(chatfield.getText()+e.toString()+ '\n');
						}
					}
				}
			}
			try {
				Path path = Paths.get(filepath);
				Files.delete(path);
				chatfield.setText(chatfield.getText()+"File deleted from Server Directory"+ '\n');
			}
			catch (Exception x) {
				chatfield.setText(chatfield.getText()+ "File did not delete: "+ filepath + '\n');
				chatfield.setText(chatfield.getText()+ "Error: " + x.toString() + '\n');
			}
		}
	
		public void userExit(String username, PrintWriter out){
			if (username != null) {
                    usernames.remove(username);
					chatfield.setText(chatfield.getText()+"[" + dateformat.format(date) + "] " +
										username + " has left the chat."+'\n');
					for (PrintWriter output : outputs) {
						if (output != out){
							output.println(username);
							output.println(" has left the chat.");
						}
                    }
                }
                if (out != null) {
                    outputs.remove(out);
                }
                try {
                    socket.close();
                } 
				catch (IOException e) {
					chatfield.setText(chatfield.getText()+"[" + dateformat.format(date) + "] " +
										" Error closing Socket: "+ e.toString() +'\n');
                }
		}
	}

}