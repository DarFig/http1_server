package http1_server;




public class Servidor {
	static int puerto;
	/**
	 * @author Dariel
	 * @param args[0] -t|
	 * @param args[1] # puerto para lanzar el servidor
	 */
	public static void main(String[] args){
		
		if (args.length != 2) {
			System.err.println("Error numero de argumentos.");
		} else {
			if (args[0].equalsIgnoreCase("-t")) {
				// Lanzar servidor multi-threads
				ServerMultihilos server = new ServerMultihilos(Integer.parseInt(args[1]));
				server.correr();
			} else {
				System.err.println("Error, argumento incorrecto");
			}
		}
	}

	
}
