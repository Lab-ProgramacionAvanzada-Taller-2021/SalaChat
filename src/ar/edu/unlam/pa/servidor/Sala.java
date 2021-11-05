package ar.edu.unlam.pa.servidor;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;

public class Sala {

	private ArrayList<Usuario> usuarios;
	private String nombre;

	public Sala(String nombre) {
		this.nombre = nombre;
		this.usuarios = new ArrayList<>();
	}

	public String entrar(Usuario u) {
		if (!existeUsuario(u)) {

			u.setConectado(true);
			usuarios.add(u);
			difundir(u.getNick() + " a entrado a la sala " + this.nombre);
			actualizarListadoUsuarios();
			Log.log(u.getNick() + " a entrado a la sala " + this.nombre);
			u.enviar("SALA " + this.nombre);
			return "200 OK";

		} else {
			u.setConectado(false);
			return "400 El usuario ya esta en la sala";
		}
	}

	public void salir(Usuario u) {
		if (existeUsuario(u)) {
			usuarios.remove(u);
			difundir(u.getNick() + " a salido de la sala " + this.nombre);
			actualizarListadoUsuarios();
			Log.log(u.getNick() + " a salido de la sala " + this.nombre);
		}
	}

	public boolean existeUsuario(Usuario u) {
		for (Usuario usr : usuarios) {
			if (usr.getNick().equalsIgnoreCase(u.getNick())) {
				return true;
			}
		}
		return false;
	}

	public void difundir(String mensaje) {
		for (Usuario usr : usuarios) {
			usr.enviar(mensaje);
		}
	}

	public Usuario obtenerUsuario(String nick) {
		for (Usuario usr : usuarios) {
			if (usr.getNick().equalsIgnoreCase(nick)) {
				return usr;
			}
		}
		return null;
	}

	public void enviarMensajePrivado(Usuario de, Usuario a, String mensaje) {
		de.enviar("(Privado)" + de.getNick() + ": " + mensaje);
		a.enviar("(Privado)" + de.getNick() + ": " + mensaje);
	}

	public void moverASala(Sala destino) {
		try {
			for (int i = usuarios.size() - 1; i >= 0; i--) {
				usuarios.get(0).setSala(destino);
				destino.entrar(usuarios.get(0));
				salir(usuarios.get(0));
			}
			destino.actualizarListadoUsuarios();
		} catch (ConcurrentModificationException ex) {
			// Empty
		} finally {
			actualizarListadoUsuarios();
		}
	}

	public String getNombre() {
		return nombre;
	}

	public void setNombre(String nombre) {
		this.nombre = nombre;
	}

	public int getCountUsuarios() {
		return usuarios.size();
	}

	public ArrayList<Usuario> getUsuarios() {
		return usuarios;
	}

	public void actualizarListadoUsuarios() {
		for (Usuario usr : usuarios) {
			usr.enviarListaUsuarios();
		}
	}
}
