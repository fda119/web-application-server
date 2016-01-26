package webserver;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.file.Files;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import db.DataBase;
import model.User;
import util.IOUtils;

public class RequestHandler extends Thread {
	
	//Field Section
	private static final Logger log = LoggerFactory.getLogger(RequestHandler.class);
	private Socket connection;
	private String url;
	private String header = null;
	private String form = null;
	private String style = "";
	private int contentLength = 0;
	private BufferedReader br;
	
	//Method Section
	public RequestHandler(Socket connectionSocket) {
		this.connection = connectionSocket;
	}

	public void run() {
		log.debug("New Client Connect! Connected IP : {}, Port : {}", connection.getInetAddress(), connection.getPort());
		
		try (InputStream in = connection.getInputStream(); OutputStream out = connection.getOutputStream()) {
			// TODO 사용자 요청에 대한 처리는 이 곳에 구현하면 된다.
			DataOutputStream dos = new DataOutputStream(out);
			InputStreamReader isr = new InputStreamReader(in);
			br = new BufferedReader(isr);
			
			url = br.readLine();
			header = url;
			
			while(!header.equals("")){
				System.out.println(header);
				header = br.readLine();
				if(header.contains("Content-Length:"))
					contentLength = Integer.parseInt(header.substring("Content-Length: ".length()));
				else if(header.contains("stylesheets"))
					style = header;
			}
			
			if(url.contains("/create")||url.contains("/logincheck"))
					form = IOUtils.readData(br, contentLength);
			
			String html = "";
			
			//'GET '과 ' HTTP/1.1'을 잘라냄
			url = url.substring("GET ".length(), url.length()-" HTTP/1.1".length());
			
			//reference : http://misoboy.tistory.com/7
			BufferedReader reader = null;
			try{
				reader = new BufferedReader(new FileReader("./webapp" +url));
				String html_line;
				
				while((html_line = reader.readLine()) != null){
					html = html.concat(html_line);
					//System.out.println(html_line);
				}
				reader.close();
				
			} catch(IOException e){
				
			}
			
			//아래와 같이 작성해도 무방
			//byte[] body = Files.readAllBytes(new File("./webapp" + url).toPath());
			
			byte[] body = html.getBytes();
			
			applyForm(dos, body);
		} catch (IOException e) {
			log.error(e.getMessage());
		}
		
		
	}

	private void applyForm(DataOutputStream dos, byte[] body) {
		if(url.contains("create")){
			registerForm(form);
			try {
				body = Files.readAllBytes(new File("./webapp/index.html").toPath());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			response302Header(dos, body.length);
			responseBody(dos, body);
		}
		else if(url.contains("logincheck")){
			String loginStatus = confirmUser(form);
			try {
				body = Files.readAllBytes(new File("./webapp/index.html").toPath());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			responseLoginHeader(dos, body.length, loginStatus);
			responseBody(dos, body);
		}
		else if(url.contains("stylesheets")){
			responseCssHeader(dos, body.length);
			responseBody(dos, body);
		}
		else{
			response200Header(dos, body.length);
			responseBody(dos, body);
		}
	}

	private void response200Header(DataOutputStream dos, int lengthOfBodyContent) {
		try {
			dos.writeBytes("HTTP/1.1 200 OK \r\n");
			dos.writeBytes("Content-Type: text/html;charset=utf-8\r\n");
			dos.writeBytes("Content-Length: " + lengthOfBodyContent + "\r\n");
			dos.writeBytes("\r\n");
		} catch (IOException e) {
			log.error(e.getMessage());
		}
	}
	
	private void response302Header(DataOutputStream dos, int lengthOfBodyContent) {
		try {
			dos.writeBytes("HTTP/1.1 302 Found Location: /index.html \r\n");
			dos.writeBytes("Content-Type: text/html;charset=utf-8\r\n");
			dos.writeBytes("Content-Length: " + lengthOfBodyContent + "\r\n");
			dos.writeBytes("\r\n");
		} catch (IOException e) {
			log.error(e.getMessage());
		}
	}
	
	private void responseLoginHeader(DataOutputStream dos, int lengthOfBodyContent, String loginStatus) {
		try {
		
			dos.writeBytes("HTTP/1.1 302 Found Location: /index.html \r\n");
			dos.writeBytes("Content-Type: text/html;charset=utf-8\r\n");
			dos.writeBytes("Content-Length: " + lengthOfBodyContent + "\r\n");
			dos.writeBytes("Set-Cookie: logined=" + loginStatus + "\r\n");
			dos.writeBytes("\r\n");
		} catch (IOException e) {
			log.error(e.getMessage());
		}
	}
	
	private void responseCssHeader(DataOutputStream dos, int lengthOfBodyContent) {
		try {
			
			dos.writeBytes("HTTP/1.1 302 Found Location: /index.html \r\n");
			dos.writeBytes("Content-Type: text/css;charset=utf-8\r\n");
			dos.writeBytes("Content-Length: " + lengthOfBodyContent + "\r\n");
			dos.writeBytes("\r\n");
		} catch (IOException e) {
			log.error(e.getMessage());
		}
	}
	
	private void responseBody(DataOutputStream dos, byte[] body) {
		try {
			dos.write(body, 0, body.length);
			dos.writeBytes("\r\n");
			dos.flush();
		} catch (IOException e) {
			log.error(e.getMessage());
		}
	}
	
	
	private void registerForm(String Form){
		
		String userId = Form.substring((Form.indexOf("userId=")+"userId=".length()),Form.indexOf("&password"));
		String password = Form.substring((Form.indexOf("password=")+"password=".length()),Form.indexOf("&name"));
		String name = Form.substring((Form.indexOf("name=")+"name=".length()),Form.indexOf("&email"));
		String email = Form.substring((Form.indexOf("email=")+"email=".length()));
		
		User user = new User(userId, password, name, email);
		
		DataBase.addUser(user);
		if(DataBase.findUserById(userId) != null){
			//System.out.println("회원가입 성공");
			
		}
		
	}
	
	private String confirmUser(String Form){
		
		String userId = Form.substring((Form.indexOf("userId=")+"userId=".length()),Form.indexOf("&password"));
		String password = Form.substring((Form.indexOf("password=")+"password=".length()));
		
		System.out.println(userId + "     " + password);
		User user = DataBase.findUserById(userId);
		
		if(user == null){
			return "false";
		}
		
		else if(!user.getPassword().equals(password)){
			return "false";
		}
			
		return "true";
	}
}
