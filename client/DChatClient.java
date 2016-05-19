/* 
Chat client
Connects to a ChatServer using the server's IP address.
Allows for both text chat and file transfer.
Author: Dillon Johnson
Version: 1.3
*/
package client;

import java.net.Socket;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.text.DefaultCaret;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JFileChooser;
import javax.swing.JScrollPane;
import javax.swing.JRootPane;
import javax.swing.JTextPane;
import javax.swing.JOptionPane;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import javax.swing.ImageIcon;
import java.awt.event.ActionEvent;
import java.io.*;
import java.util.*;
import java.text.*;

public class DChatClient extends JFrame {
	JTextPane chatfield;
    JButton connectbtn;
    JButton fsendbtn;
    JScrollPane chatfieldscroll;
    JPanel panel;
    JButton sendbtn;
    JTextField serverip;
    JLabel serveriplabel;
    JTextField txtmsg, fsend;
    JTextField username;
    JLabel usernamelabel;
    StyledDocument doc;
	SimpleAttributeSet bold, normal;
	SimpleDateFormat dateformat;
	DataInputStream dinput;
	DataOutputStream foutput;
	BufferedInputStream finput;
	BufferedReader in;
	PrintWriter out;
	String message;
	static Socket connection,fileconnection;
	static volatile int setupdone;
	Date date;
	int textport = 9998;
	int fileport = 9997;
	
public DChatClient() throws IOException{
	//Building Chat window
    initComponents();
	doc = (StyledDocument) chatfield.getDocument();
	normal = new SimpleAttributeSet();
        StyleConstants.setFontFamily(normal, "Arial");
        StyleConstants.setFontSize(normal, 12);
	bold = new SimpleAttributeSet(normal);
        StyleConstants.setBold(bold, true);
		StyleConstants.setFontFamily(bold, "Arial");
        StyleConstants.setFontSize(bold, 12);
	dateformat = new SimpleDateFormat ("h:mm");
	ImageIcon img = new ImageIcon(System.getProperty("user.dir") + "\\client\\DChat_pic.png");
	this.setIconImage(img.getImage());

	DefaultCaret caret = (DefaultCaret) chatfield.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
    JRootPane rootPane = panel.getRootPane();
    rootPane.setDefaultButton(sendbtn);
    setupdone = 0;
	
}

public void receiveText(String message, BufferedReader in) {
	try {
		date = new Date();
		doc.insertString(doc.getLength(),"[" + dateformat.format(date) + "] " + message, bold);//username of sender
		message = in.readLine();
		doc.insertString(doc.getLength(), message + '\n', normal);//message from sender
		chatfieldscroll.validate();
	}
	catch (Exception eChat) {
		chatfield.setText(chatfield.getText()+ "Error Posting Message to Chat");
	}
}

public void receiveFile(String fname) {
	try{
		date = new Date();
		doc.insertString(doc.getLength(),"[" + dateformat.format(date) + "] " + "Receiving File..." + '\n', bold);
	}
	catch (Exception eChat) {
		chatfield.setText(chatfield.getText()+ "Error Posting Message to Chat");
	}
	try {
			Thread.sleep(1000);//wait for server to set up file transfer port
			fileconnection = new Socket(serverip.getText(), fileport);//connect to file transfer port
			dinput = new DataInputStream(fileconnection.getInputStream());
			FileOutputStream fstream = new FileOutputStream(fname);
			byte[] filebytes = new byte[8192];
			int count;
			while ((count = dinput.read(filebytes))>0)
			{
				fstream.write(filebytes,0,count);
			}
			fstream.flush();
			date = new Date();
			try{
				doc.insertString(doc.getLength(),"[" + dateformat.format(date) + "] " + "File Received" + '\n', bold);
				doc.insertString(doc.getLength(),"[" + dateformat.format(date) + "] " + "File Location: ", bold);
				doc.insertString(doc.getLength(), System.getProperty("user.dir") + '\n', normal);
			}
			catch (Exception eChat) {
				chatfield.setText(chatfield.getText()+ "Error Posting Message to Chat");
			}
			fstream.close();
			dinput.close();
			fileconnection.close();
	}
	catch (Exception e) {//Getting Disconnected with Server
		chatfield.setText(chatfield.getText()+"Disconnected with Server. Try to reconnect."+ '\n');
		chatfield.setText(chatfield.getText()+ e.toString());
		setupdone = 0;
		chatfieldscroll.validate();
	}
}

public void connectbtnActionPerformed(ActionEvent evt) {  
    if(evt.getSource()==connectbtn)
    {
        connectSetup();
    }
}
 
public void sendbtnActionPerformed(ActionEvent evt) {                                           
    if((evt.getSource()==sendbtn)&&(!txtmsg.getText().equals("")))
    {
	sendText();
    }
}

public void fsendbtnActionPerformed(ActionEvent evt) { 
    if(evt.getSource()==fsendbtn){
        sendFile();
    }
}

public void sendText() {
		if (!username.getText().equals(""))
		{
			try 
			{
				out = new PrintWriter(connection.getOutputStream(), true);
				out.println(txtmsg.getText());
				//out.close();
			}
			catch (Exception e)
			{
				chatfield.setText(chatfield.getText()+"Message Send Failed"+ '\n');
			}
			txtmsg.setText("");//reset txtmsg field
		}
		else
		{
			JOptionPane.showMessageDialog(null, "Input a Username", "Error", JOptionPane.INFORMATION_MESSAGE);
		}
	}

public void sendFile() {
		JFileChooser filefind = new JFileChooser();//File Browser to select file  
        try{
			filefind.showOpenDialog(this);//choosing file
		}
		catch(Exception e){}  
        String filename = filefind.getSelectedFile().getAbsolutePath();
		try{
			date = new Date();
			doc.insertString(doc.getLength(),"[" + dateformat.format(date) + "] " + "Sending File..." + '\n', bold);
		}
		catch (Exception eChat) {
			chatfield.setText(chatfield.getText()+ "Error Posting Message to Chat");
		}	
		File f = new File(filename);
		if(!f.exists() || f.isDirectory())
		{//checking if file exists
			chatfield.setText(chatfield.getText()+"File does not exist: "+ filename+ '\n');
		}
		else{
			long filesize = f.length();
            if(filesize < 52428800){//50MB max file send size
			byte[] filebytes = new byte[8192];
			try
			{//sending the file bytes
				out.println("fsend");//indicator that I am sending a file
				out.println(f.getName());//sending the file name first
				Thread.sleep(1000);//wait for server to set up file transfer port
				
				fileconnection = new Socket(serverip.getText(), fileport);//connect to file transfer port
				finput = new BufferedInputStream(new FileInputStream(f));
				foutput = new DataOutputStream(fileconnection.getOutputStream());
				int count = 0;
				while ((count = finput.read(filebytes))>0)
				{
					foutput.write(filebytes,0,count);
					foutput.flush();
				}
				//chatfield.setText(chatfield.getText()+"File Send Complete"+ '\n');
				foutput.close();
				finput.close();
				fileconnection.close();
			} 
			catch (Exception e)
			{
				chatfield.setText(chatfield.getText()+"File Send Failed"+ '\n');
			}
			}
			else {
                try{
					date = new Date();
					doc.insertString(doc.getLength(),"[" + dateformat.format(date) + "] " + "File Too Large (50MB Max)" + '\n', bold);
                 }
                catch (Exception eChat) {
					chatfield.setText(chatfield.getText()+ "Error Posting Message to Chat");
                }
            }
		}
	}

public void connectSetup() {
		if (!username.getText().equals(""))
		{
			chatfield.setText(chatfield.getText()+"Waiting for connection..."+'\n');
			try
			{
				connection = new Socket(serverip.getText(), textport);
				chatfield.setText(chatfield.getText()+"Connected to "+ serverip.getText()+'\n');
				in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
				String message = in.readLine();
				try 
				{
					out = new PrintWriter(connection.getOutputStream(), true);
					out.println(username.getText());//sending username to server
					message = in.readLine();//welcome to server message
					chatfield.setText(chatfield.getText()+message+'\n');
					message = in.readLine();//# of users in chat message
					chatfield.setText(chatfield.getText()+message+'\n');
					
					setupdone = 1; //starts the receive message loop
				}
				catch (Exception e)
				{
					chatfield.setText(chatfield.getText()+"Server Authorization Failed"+ '\n');
				}
			}
			catch(Exception e) 
			{
				chatfield.setText(chatfield.getText()+"Cannot connect to "+serverip.getText()+'\n');
			}
		}
		else 
		{
			JOptionPane.showMessageDialog(null, "Input a Username", "Error", JOptionPane.INFORMATION_MESSAGE);
		}
	}

public void initComponents() {

        panel = new javax.swing.JPanel();
        usernamelabel = new javax.swing.JLabel();
        username = new javax.swing.JTextField();
        serveriplabel = new javax.swing.JLabel();
        serverip = new javax.swing.JTextField();
        chatfieldscroll = new javax.swing.JScrollPane();
        chatfield = new javax.swing.JTextPane();
        txtmsg = new javax.swing.JTextField();
        sendbtn = new javax.swing.JButton();
        fsendbtn = new javax.swing.JButton();
        connectbtn = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        usernamelabel.setFont(new java.awt.Font("Arial Unicode MS", 0, 12)); // NOI18N
        usernamelabel.setText("Username:");

        username.setFont(new java.awt.Font("Arial Unicode MS", 0, 12)); // NOI18N

        serveriplabel.setFont(new java.awt.Font("Arial Unicode MS", 0, 12)); // NOI18N
        serveriplabel.setText("Server IP:");

        serverip.setFont(new java.awt.Font("Arial Unicode MS", 0, 12)); // NOI18N

        chatfieldscroll.setFont(new java.awt.Font("Arial Unicode MS", 0, 12)); // NOI18N
        chatfieldscroll.setViewportView(chatfield);

        txtmsg.setFont(new java.awt.Font("Arial Unicode MS", 0, 12)); // NOI18N

        sendbtn.setFont(new java.awt.Font("Arial Unicode MS", 1, 12)); // NOI18N
        sendbtn.setText("Send");
        sendbtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sendbtnActionPerformed(evt);
            }
        });

        fsendbtn.setFont(new java.awt.Font("Arial Unicode MS", 1, 12)); // NOI18N
        fsendbtn.setText("Send File");
	fsendbtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                fsendbtnActionPerformed(evt);
            }
        });

        connectbtn.setFont(new java.awt.Font("Arial Unicode MS", 1, 12)); // NOI18N
        connectbtn.setText("Connect");
        connectbtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                connectbtnActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout panelLayout = new javax.swing.GroupLayout(panel);
        panel.setLayout(panelLayout);
        panelLayout.setHorizontalGroup(
            panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(panelLayout.createSequentialGroup()
                        .addComponent(txtmsg, javax.swing.GroupLayout.PREFERRED_SIZE, 225, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(sendbtn, javax.swing.GroupLayout.PREFERRED_SIZE, 65, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(fsendbtn)
                        .addGap(0, 10, Short.MAX_VALUE))
                    .addGroup(panelLayout.createSequentialGroup()
                        .addGroup(panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(chatfieldscroll)
                            .addGroup(panelLayout.createSequentialGroup()
                                .addGroup(panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(usernamelabel)
                                    .addComponent(serveriplabel))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(panelLayout.createSequentialGroup()
                                        .addComponent(serverip, javax.swing.GroupLayout.PREFERRED_SIZE, 107, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGap(4, 4, 4)
                                        .addComponent(connectbtn))
                                    .addComponent(username, javax.swing.GroupLayout.PREFERRED_SIZE, 107, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(0, 0, Short.MAX_VALUE)))
                        .addContainerGap())))
        );
        panelLayout.setVerticalGroup(
            panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(username)
                    .addComponent(usernamelabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(serveriplabel)
                        .addComponent(connectbtn))
                    .addComponent(serverip, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(chatfieldscroll, javax.swing.GroupLayout.DEFAULT_SIZE, 180, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addGroup(panelLayout.createSequentialGroup()
                        .addGap(2, 2, 2)
                        .addComponent(txtmsg, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(sendbtn)
                    .addComponent(fsendbtn))
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(panel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(panel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        pack();
    }

public void disconnectMessage(){
    chatfield.setText(chatfield.getText()+"Disconnected with Server. Try to reconnect."+ '\n');
}

public static void main(String args[]) throws IOException{
		//Nimbus look to chat window
        // If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(DChatClient.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(DChatClient.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(DChatClient.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(DChatClient.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }

    DChatClient dchat = new DChatClient();
	dchat.setTitle("DChat");
    dchat.setVisible(true);
    BufferedReader instream;
    String message;
    		while(true)
		{//receive messages/files
			if (setupdone == 1) {
                try
				{
					//Receiving a Message
					instream = new BufferedReader(new InputStreamReader(connection.getInputStream()));
					message = instream.readLine();
					if(message.equals("fsend")){//indicator to receive a file
						String fname = instream.readLine();//receive file name first
						dchat.receiveFile(fname);
					}
					else
					{//receive a text message
						dchat.receiveText(message, instream);
					}
                }
                catch (Exception e)//Getting Disconnected with Server
                {
                    dchat.disconnectMessage();
					setupdone = 0;
                }
			}
		}
    
    }
}