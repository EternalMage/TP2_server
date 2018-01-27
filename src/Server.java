import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.util.Scanner;
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
import java.util.List;
import java.util.Stack;

import javax.imageio.ImageIO;

//Impl�mentation possible du serveur � l'aide d'une stack pour l'exercice 2 de la 
//section 4.1 du TP1 - INF3405 H2018
//****************************************Attention****************************************
//L'impl�mentation du serveur n'est pas multi-threaded. Ainsi, la connection de 
//plusieurs clients en m�me temps au serveur ne fonctionnera pas! � vous de threader le serveur 
//pour qu'il puisse avoir la capacit� d'accepter plusieurs clients.
public class Server {
	public static void main(String[] args) throws IOException, ClassNotFoundException {
		while (true) {
			Scanner sc = new Scanner(System.in);
			ServerSocket serverSocket = null;
			Socket socket = null;
			ObjectInputStream in = null;
			ObjectOutputStream out = null;
			System.out.println("Loop");
			try {
				System.out.println("Entrez un port entre 5000 et 5050: ");
				int port = sc.nextInt();
				verifyPort(port);
				// Cr�ation du socket du serveur en utilisant le port 5000.
				serverSocket = new ServerSocket(port);
				// Ici, la fonction accept est bloquante! Ainsi, l'ex�cution du serveur s'arr�te
				// ici et attend la connection d'un client avant de poursuivre.
				socket = serverSocket.accept();
				// Cr�ation d'un input stream. Ce stream contiendra les donn�es envoy�es par le
				// client.
				if (getLoginInfo(socket, in, out)){
					convertImage(socket);
				};
				/*// La fonction readObject est bloquante! Ainsi, le serveur arr�te son ex�cution
				// et attend la r�ception de l'objet envoy� par le client!
				List<String> strings = (List<String>) in.readObject();
				Stack<String> stackOfLines = new Stack<String>();
				// Remplissage de la stack avec les lignes. La premi�re ligne entr�e sera la
				// derni�re � ressortir.
				for (int i = 0; i < strings.size(); i++) {
					stackOfLines.push(strings.get(i));
				}
				// Cr�ation du output stream. Ce stream contiendra les donn�es qui seront
				// envoy�es au client.
				out = new ObjectOutputStream(new BufferedOutputStream(socket.getOutputStream()));
				// �criture des donn�es dans la pile.
				out.writeObject(stackOfLines);
				// Envoi des donn�es vers le client. */
				//out.flush();
			} finally {
				System.out.println("Closing server socket");
				serverSocket.close();
				socket.close();
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

	public static boolean getLoginInfo(Socket socket, ObjectInputStream in, ObjectOutputStream out) throws IOException, ClassNotFoundException{
		in = new ObjectInputStream(new BufferedInputStream(socket.getInputStream()));
		out = new ObjectOutputStream(new BufferedOutputStream(socket.getOutputStream()));
		String user = (String) in.readObject();
		String pass = (String) in.readObject();
		System.out.println(user + pass);
		boolean loginIsSuccessful;

		// CHERCHER AVEC JSON if (pass and user in database)
		if (user.equals("monster") && pass.equals("123")){
			System.out.println("Logged in successfully");
			loginIsSuccessful = true;
		}
		else{
			System.out.println("Login error!");
			loginIsSuccessful = false;
		}
		out.writeObject(loginIsSuccessful);
		out.flush();
		return loginIsSuccessful;

	}
	
	public static void convertImage(Socket socket) throws IOException{
		InputStream inputStream = socket.getInputStream();
        byte[] sizeAr = new byte[4];
        inputStream.read(sizeAr);
        int sizeI = ByteBuffer.wrap(sizeAr).asIntBuffer().get();
        byte[] imageAr = new byte[sizeI];
        inputStream.read(imageAr);
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageAr));
        
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
}