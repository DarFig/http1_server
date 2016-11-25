package http1_server;

import java.io.*;
import java.net.*;


/**
 * 
 * @author Dariel
 * @param puerto: puerto para lanzar el servidor
 */
public class ServerMultihilos extends Thread {
	private final static int BUFFERSIZE = 1024;
	
	public int puerto;
	public ServerSocket servidor;
	
	ServerMultihilos(int puerto) {
		this.puerto = puerto;
		try {
			this.servidor = new ServerSocket(this.puerto);
		} catch (IOException e) {
			
			e.printStackTrace();
		}
	}
	
	public void correr() {
		while(true) {
			Socket cliente;
			try {
				cliente = servidor.accept();
				new hilo(cliente).start();
			} catch (IOException e) {
				
				e.printStackTrace();
			}
		}	
	}

	
	/*Permite gestionar las peticiones http de un cliente*/
	static class hilo extends Thread {
		Socket cliente;
		
		hilo(Socket client) {
			this.cliente = client;
		}

		public void run() {

			try {
				
				InputStream in = cliente.getInputStream();
				DataOutputStream out = new DataOutputStream(cliente.getOutputStream());
				
				//Creamos un parser HTTP para tratar los mensajes del cliente
				BlockingHTTPParser parser = new BlockingHTTPParser();
				
				//le pasamos la peticion del cliente
				parser.parseRequest(in);
				
				
				
				//Variables necesarias para construir la respuesta
				String statusLine = null;
				String contentTypeLine = null;
				String entityBody = null;
				String contentLength = null;
				
				//Se asume que el fichero no existe hasta que se verifique
				boolean existeFichero = false;
				FileInputStream fich = null;
				
				
				if (parser.isComplete() ) {
					//la petición esta bien formada
					
					if (parser.getMethod().equals("GET") ) {
						//es una peticion GET
						
						if (parser.getPath().replaceFirst("/", "").contains("/") ) {
							//intenta acceder a un fichero no permitido
							
							statusLine = "HTTP/1.1 403 Forbidden" + " \r\n";
							contentTypeLine = "Content-Type: " + "text/html" + " \r\n";
							contentLength =  "Content-Length" + "90"+ " \r\n";
							
							entityBody = "<html><head>"+
										"<title>403 Forbidden</title>"+
										"</head><body>"+
										"<h1>Forbidden</h1>"+
										"</body></html>";
						
						} else {
							//intenta acceder a un fichero permitido
							
							//intentar abrir el fichero
							existeFichero = true;
							try {
								fich = new FileInputStream("."+parser.getPath());
							} catch(FileNotFoundException e) {
								existeFichero = false;
							}
							
							if(existeFichero) {
								statusLine = "HTTP/1.1 200 OK" + " \r\n";
								contentTypeLine = "Content-Type: " + tipoContenido(("."+parser.getPath()))+" \r\n";
								contentLength = "Content-Length" + fich.available() + " \r\n";
							} else {
								statusLine = "HTTP/1.1 404 Not Found" + " \r\n";
								contentTypeLine = "Content-Type: " + "text/html" + " \r\n";
								contentLength =  "Content-Length" + "90"+ " \r\n";
								
								entityBody = "<html><head>"+
											"<title>404 Not Found</title>"+
											"</head><body>"+
											"<h1>Not Found</h1>"+
											"</body></html>";
							}
						}
						
					} else if(parser.getMethod().equals("POST")	) {
						//es una peticion POST
						
						//pasamos a un string el cuerpo de la peticion POST
						String cuerpo = new String(parser.getBody().array());
						
						String nombreFich = cuerpo.substring(6, cuerpo.indexOf("&"));
						cuerpo = cuerpo.substring(cuerpo.indexOf("&") + 9, cuerpo.length());
						
						
						FileOutputStream fichDst = new FileOutputStream("./" + nombreFich);
						
						fichDst.write(cuerpo.getBytes());
						statusLine = "HTTP/1.1 200 Ok" + " \r\n";
						contentTypeLine = "Content-Type: " + "text/html" + " \r\n";
						
						entityBody = "<html><head>" +
									"<title>¡Exito!</title>" +
									"</head><body>" +
									"<h1>¡Exito!</h1>" +
									"<p>Se ha escrito lo siguiente en el fichero " + nombreFich + "</p>" +
									"<pre>" +
									cuerpo +
									"</pre>" +
									"</body></html>";
						contentLength =  "Content-Length" + entityBody.length() + " \r\n";
						fichDst.close();
					} else {
						//es una peticion no implementada
						statusLine = "HTTP/1.1 501 Not Implemented" + " \r\n";
						contentTypeLine = "Content-Type: " + "text/html" + " \r\n";
						contentLength = "Content-Length" + "102"+ " \r\n";
						
						entityBody = "<html><head>"+
									"<title>501 Not Implemented</title>"+
									"</head><body>"+
									"<h1>Not Implemented</h1>"+
									"</body></html>";
					}
				}
				if(parser.failed() ) {
					//Bad Request
					statusLine = "HTTP/1.1 400 Bad Request" + " \r\n";
					contentTypeLine = "Content-Type: " + "text/html" + " \r\n";
					contentLength =  "Content-Length" + "94"+ " \r\n";
					
					entityBody = "<html><head>"+
								"<title>400 Bad Request</title>"+
								"</head><body>"+
								"<h1>Bad Request</h1>"+
								"</body></html>";
				}
				 
				out.writeBytes(statusLine);
				out.writeBytes(contentTypeLine);
				out.writeBytes(contentLength);
				/*paso del body, dos casos
				 * 1-enviamos un fichero
				 * 2-enviamos un html
				 */
				if(existeFichero) {
					out.writeBytes("\r\n");
					
					int size;
					byte[] buff = new byte[BUFFERSIZE];
					while((size = fich.read(buff)) != -1) {
						out.write(buff, 0, size);
					}
					
					fich.close();
					
				} else {
					
					out.writeBytes("\r\n");
					out.writeBytes(entityBody);
					
				}
				
				cliente.close();//cerrar conexion
				
			} catch (IOException e) {
				
				e.printStackTrace();
			}

		}

		private String tipoContenido(String fichero) {
			if(fichero.endsWith(".html")) return "text/html";
			if(fichero.endsWith(".css")) return "text/css";
			if(fichero.endsWith(".txt")) return "text/plain";
			if(fichero.endsWith(".jpg") || fichero.startsWith("jpeg")) return "image/jpeg";
			if(fichero.endsWith(".gif")) return "image/gif";
			return "otro/otro";
		}
	}

}
