package ar.edu.unlam.pa.servidor;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class Servidor {

	public static final int PUERTO = 2021;
	public static ArrayList<Sala> listadoSalas;
	public static final String ADMIN_PASSWORD = "admin";
	public static boolean saveLogs = true;

	public static void main(String[] args) throws IOException {
		if (saveLogs) {
			System.out.println("Guardado de logs habilitado");
		} else {
			System.out.println("Guardado de logs deshabilitado");
		}
		ServerSocket ss = new ServerSocket(PUERTO);
		Log.log("Servidor inicializado en el puerto " + PUERTO);
		listadoSalas = new ArrayList<>();
		Sala sala = new Sala("Principal");
		agregarSala(sala);
		while (true) {
			Socket socket = ss.accept();
			Log.log("Conexion establecida con " + socket.getInetAddress().getHostAddress());
			new Thread(new Usuario(socket, sala)).start();
		}
	}

	public static void agregarSala(Sala s) {
		if (obtenerSala(s.getNombre()) == null) {
			listadoSalas.add(s);
			Log.log("Se ha creado la sala con nombre " + s.getNombre());
		}
	}

	public static void eliminarSala(Sala s) {
		if (obtenerSala(s.getNombre()) != null && !s.getNombre().equalsIgnoreCase("Principal")) {
			s.moverASala(listadoSalas.get(0));
			listadoSalas.remove(s);
			Log.log("Se ha eliminado la sala con nombre " + s.getNombre());
		}
	}

	public static Sala obtenerSala(String nombre) {
		for (Sala s : listadoSalas) {
			if (s.getNombre().equalsIgnoreCase(nombre)) {
				return s;
			}
		}
		return null;
	}

	public static Sala[] obtenerSalas() {
		Sala[] s = new Sala[listadoSalas.size()];
		for (int i = 0; i < listadoSalas.size(); i++) {
			s[i] = listadoSalas.get(i);
		}
		return s;
	}

	public static boolean existeSala(Sala s) {
		for (int i = 0; i < listadoSalas.size(); i++) {
			if (listadoSalas.get(i).getNombre().equalsIgnoreCase(s.getNombre())) {
				return true;
			}
		}
		return false;
	}

}