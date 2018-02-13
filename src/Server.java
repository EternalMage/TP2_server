import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.util.*;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;

import javax.imageio.ImageIO;

//Implémentation possible du serveur à l'aide d'une stack pour l'exercice 2 de la 
//section 4.1 du TP1 - INF3405 H2018
//****************************************Attention****************************************
//L'implémentation du serveur n'est pas multi-threaded. Ainsi, la connection de 
//plusieurs clients en même temps au serveur ne fonctionnera pas! À vous de threader le serveur 
//pour qu'il puisse avoir la capacité d'accepter plusieurs clients.
public class Server {
	@SuppressWarnings("resource")
	private static String IPAddress = "127.0.0.1";
	private static int port;
	
	public static void main(String[] args) throws IOException, ClassNotFoundException {
		
		Scanner sc = new Scanner(System.in);
		ServerSocket serverSocket = null;
		Socket socket = null;
		ObjectInputStream in = null;
		ObjectOutputStream out = null;
		ArrayList<String> userArray = new ArrayList<String>();
		ArrayList<String> passArray = new ArrayList<String>();
		String dbName = "text.txt";
		System.out.println("Entrez un port entre 5000 et 5050: ");
		port = sc.nextInt();
		
		if (verifyPort(port)){ // si port est valide, on peut run le server
			serverSocket = new ServerSocket(port);
			while (true) {
					System.out.println("Loop");
					readDB(dbName, userArray, passArray); // update db
					socket = serverSocket.accept();
					int temp = userArray.size();
					if (getLoginInfo(socket, in, out, userArray, passArray)){
						if (temp < userArray.size()) {
							try {
								FileWriter fileWriter = new FileWriter(dbName, true);
								BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
								bufferedWriter.newLine();
								bufferedWriter.write(userArray.get(temp));
								bufferedWriter.newLine();
								bufferedWriter.write(passArray.get(temp));
								bufferedWriter.close();
							}
							catch(IOException ex) {
								System.out.println(
										"Error writing to file '" + dbName + "'");
							}
						}
						Thread thread = new socketHandler(socket);
					    thread.start();
					};
			}
		}
	}

	public static boolean verifyPort(int port) {
		// check if port is positive integer and in 5000 and 5050
		if (port >= 5000 && port <= 5050)
			return true;
		else{
			System.out.println("Port not valid");
			return false;
		}
	}

	public static boolean getLoginInfo(Socket socket, ObjectInputStream in, ObjectOutputStream out, ArrayList<String> userL, ArrayList<String> passL) throws IOException, ClassNotFoundException{
		in = new ObjectInputStream(new BufferedInputStream(socket.getInputStream()));
		out = new ObjectOutputStream(new BufferedOutputStream(socket.getOutputStream()));
		String user = (String) in.readObject();
		String pass = (String) in.readObject();
		boolean loginIsSuccessful = false;

		for (int i = 0; i < userL.size(); i++) {
			if (user.equals(userL.get(i))) {
				if (pass.equals(passL.get(i))){
					System.out.println(user + " logged in successfully!");
					loginIsSuccessful = true;
					i = userL.size();
				} else {
					System.out.println("Login error!");
					i = userL.size();
				}
			} else {
				if (i == userL.size() - 1){
					userL.add(user);
					passL.add(pass);
					System.out.println("Created this new user.");
					loginIsSuccessful = true;
				}
			}
		}

		out.writeObject(loginIsSuccessful);
		out.flush();
		return loginIsSuccessful;

	}

	public static void convertImage(Socket socket) throws IOException, ClassNotFoundException{
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		Date date = new Date();
		
		ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(socket.getInputStream()));
		String username = (String) in.readObject();
		String imageFileName = (String) in.readObject();
		InputStream inputStream = socket.getInputStream();
		byte[] sizeAr = new byte[4];
		inputStream.read(sizeAr);
		int sizeI = ByteBuffer.wrap(sizeAr).asIntBuffer().get();
		byte[] imageAr = new byte[sizeI];
		inputStream.read(imageAr);
		BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageAr));
		
		System.out.println(username + " - " + IPAddress + ":" + port + " - " + dateFormat.format(date) + ": Image " + imageFileName + " reçue pour traitement.");
		
		Sobel sobel = new Sobel();
		BufferedImage newImage = sobel.process(image);

		OutputStream outputStream = socket.getOutputStream();
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		ImageIO.write(newImage, "jpg", byteArrayOutputStream);
		byte[] sizeO = ByteBuffer.allocate(4).putInt(byteArrayOutputStream.size()).array();
		outputStream.write(sizeO);
		outputStream.write(byteArrayOutputStream.toByteArray());
		outputStream.flush();
	}

	public static void readDB(String dbName, ArrayList<String> userArray, ArrayList<String> passArray){
		String line = null;
		int i = 0;
		try {
			FileReader fileReader = new FileReader(dbName);
			BufferedReader bufferedReader = new BufferedReader(fileReader);
			while((line = bufferedReader.readLine()) != null) {
				i++;
				if (i % 2 == 0)
					userArray.add(line);
				else
					passArray.add(line);

			}
			bufferedReader.close();
		}
		catch(FileNotFoundException ex) {
			System.out.println("Unable to open file '" + dbName + "'");
		}
		catch(IOException ex) {
			System.out.println("Error reading file '" + dbName + "'");
		}
	}

	//http://www.java2s.com/Tutorial/Java/0320__Network/ThreadbasedServerSocket.htm
	private static class socketHandler extends Thread {

		private Socket socket;

		public socketHandler(Socket socket) {
			this.socket = socket;
		}

		public void run() {
			try {
				convertImage(socket);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}