package ar.edu.unlam.pa.servidor;

import static ar.edu.unlam.pa.servidor.Servidor.ADMIN_PASSWORD;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Usuario implements Runnable {

	private static final String PRIVILEGIOS_INSUFICIENTES = "500 Privilegios insuficientes";
	private static final String LA_SALA_PRINCIPAL_NO_PUEDE_SER_ELIMINADA = "500 La sala principal no puede ser eliminada!";
	private static final String NO_EXISTE_SALA = "500 No existe ninguna sala llamada ";
	private static final String PASS_ADMIN_INCORRECTA = "500 La contraseña de privilegios es incorrecta";
	private static final String EXISTE_USUARIO_SALA = "500 Ya hay un usuario llamado ";
	private static final String PAQUETE_INVALIDO = "Comando invalido: ";
	private static final String SINTAXIS_INCORRECTA = "500 Sintaxis incorrecta";
	private static final String DESCONECTADO_DEL_CHAT = "400 Has sido desconectado del chat";
	private static final String USUARIO_LARGO_NICK_INTENTO_ENTRAR = "Un usuario ha tratado de entrar con un nick demasiado largo. Nick: ";
	private static final String NICK_ELEGIDO_DEMASIADO_LARGO = "400 El nick elegido es demasiado largo, introduce un nick de como máximo 12 carácteres";
	private static final String PAQUETE_DE_LOGIN_INVALIDO = "Paquete de login inválido: ";
	private static final String PAQUETE_INVÁLIDO_RECIBIDO = "400 Paquete inválido recibido";
	private String nick;
	private BufferedReader entrada;
	private BufferedWriter salida;
	private long loginTime;
	private boolean conectado, superUser, heartBeatOn;
	private String IP;
	private long ping;
	private Sala sala;
	private long lastBeat;

	public Usuario(String nick) {
		this.nick = nick;
	}

	public Usuario(Socket s, Sala sala) throws IOException {
		this.sala = sala;
		this.loginTime = System.currentTimeMillis();
		this.IP = s.getInetAddress().getHostAddress();
		this.ping = 0;
		this.superUser = false;
		this.heartBeatOn = true;
		entrada = new BufferedReader(new InputStreamReader(s.getInputStream()));
		salida = new BufferedWriter(new PrintWriter(s.getOutputStream()));
	}

	@Override
	public void run() {
		String login = recibir();
		if (!login.startsWith("NICK")) {
			enviar(PAQUETE_INVÁLIDO_RECIBIDO);
			Log.log(PAQUETE_DE_LOGIN_INVALIDO + login);
			conectado = false;
		} else {
			if (login.split("[ ]")[1].length() >= 12) {
				enviar(NICK_ELEGIDO_DEMASIADO_LARGO);
				Log.log(USUARIO_LARGO_NICK_INTENTO_ENTRAR + login.split("[ ]")[1]);
			} else {
				conectado = !sala.existeUsuario(this);
			}
		}
		if (conectado) {
			nick = login.split("[ ]")[1];
			enviar(sala.entrar(this));
			enviarListaUsuarios();
			if (heartBeatOn) {
				lastBeat = System.currentTimeMillis();
				asyncBeatCheck();
			}
			enviar("SALA " + sala.getNombre());
			do {
				String packet = recibir();
				if (packet != null && !packet.isEmpty()) {
					analizarPacket(packet);
				}
			} while (conectado);
			enviar(DESCONECTADO_DEL_CHAT);
			sala.salir(this);
		}
	}

	public void analizarPacket(String s) {

		if (s.startsWith("EXIT")) {
			conectado = false;
		} else if (s.startsWith("/NICK ")) {
			String[] p;
			p = s.split("[ ]");
			if (p.length < 2) {
				enviar(SINTAXIS_INCORRECTA);
				Log.log(PAQUETE_INVALIDO + s);
			} else {
				String oldname = nick;
				if (sala.existeUsuario(new Usuario(p[1]))) {
					enviar(EXISTE_USUARIO_SALA + nick + " en la sala");
					nick = oldname;
				} else {
					if (!(p[1].length() > 12)) {
						nick = p[1];
						sala.actualizarListadoUsuarios();
						Log.log(oldname + " a cambiado de nombre a " + nick);
						sala.difundir(oldname + " a cambiado de nombre a " + nick);
						enviar("200 OK");
					} else {
						enviar(NICK_ELEGIDO_DEMASIADO_LARGO);
					}
				}
			}
		} else if (s.startsWith("/INFO")) {
			String[] p;
			p = s.split("[ ]");
			if (p.length < 2) {
				enviar(SINTAXIS_INCORRECTA);
				Log.log(PAQUETE_INVALIDO + s);
			} else {
				if (!sala.existeUsuario(new Usuario(p[1]))) {
					enviar("500 No hay ningun usuario con el nombre " + p[1]);
				} else {
					Usuario tmp = sala.obtenerUsuario(p[1]);
					enviar("======================\nNombre: " + tmp.getNick() + "\nIP: " + tmp.getIP() + "\nPing: "
							+ tmp.getPing() + "ms\nEntrada: " + new Date(tmp.getLoginTime()).toGMTString()
							+ "======================");
					Log.log("Infomacion de " + nick + " sobre " + tmp.getNick());
				}
			}
		} else if (s.startsWith("/HELP")) {
			String[] p;
			p = s.split("[ ]");
			enviar("======================\nComandos\n======================\n- /INFO <usr>: Muestra informacion del usuario");
			enviar("- /P <usr> <msg>: Envia un mensaje privado a un usuario de la sala\n- /NICK <nuevo>: Cambia tu nombre de usuario");
			enviar("- /C <nombre> : Crea una sala nueva y te mete en ella.\n- /J <nombre> : Cambia a la sala especificada");
			enviar("- /LIST: Lista las salas disponibles en el servidor\n- EXIT: Sale del chat\n======================");
		} else if (s.startsWith("/P")) {
			String[] p;
			p = s.split("[ ]");
			if (!sala.existeUsuario(new Usuario(p[1]))) {
				enviar("500 No hay ningun usuario con el nombre " + p[1]);
			} else {
				Usuario tmp = sala.obtenerUsuario(p[1]);
				sala.enviarMensajePrivado(this, tmp, s.substring(3 + tmp.getNick().length() + 1));
				Log.log("Mensaje privado de " + this.getNick() + " y " + tmp.getNick() + ": "
						+ s.substring(3 + tmp.getNick().length()));
			}
		} else if (s.startsWith("/ADMIN ")) {
			String[] p;
			p = s.split("[ ]");
			if (p.length > 2) {
				if (!p[1].equals(ADMIN_PASSWORD)) {
					enviar(PASS_ADMIN_INCORRECTA);
				} else {
					superUser = true;
					enviar("Has obtenido privilegios ADMIN");
					Log.log("Privilegios ADMIN otorgados a " + nick);
				}
			}
		} else if (s.startsWith("/C ")) {
			String[] p;
			p = s.split("[ ]");
			if (p.length > 2) {
				enviar(SINTAXIS_INCORRECTA);
				Log.log(PAQUETE_INVALIDO + s);
			} else {
				if (!Servidor.existeSala(new Sala(p[1]))) {
					Sala sl = null;
					if (p.length == 2) {
						sl = new Sala(p[1]);
					}
					if (sl != null) {
						Servidor.agregarSala(sl);
						sala.salir(this);
						sl.entrar(this);
						sala = sl;
						enviar("SALA " + sala.getNombre());
						sala.actualizarListadoUsuarios();
					}
				} else {
					enviar("500 Ya existe una sala con ese nombre");
				}
			}
		} else if (s.startsWith("/J ")) {
			String[] p;
			p = s.split("[ ]");
			if (p.length > 3) {
				enviar(SINTAXIS_INCORRECTA);
				Log.log(PAQUETE_INVALIDO + s);
			} else {
				if (Servidor.existeSala(new Sala(p[1]))) {

					if (p.length == 2) {
						Sala sl = Servidor.obtenerSala(p[1]);

						sala.salir(this);
						sl.entrar(this);
						sala = sl;
						enviar("SALA " + sala.getNombre());
						sala.actualizarListadoUsuarios();
					}

				} else {
					enviar(NO_EXISTE_SALA + p[1]);
				}
			}
		} else if (s.startsWith("/D ")) {
			String[] p;
			p = s.split("[ ]");
			if (p.length > 2) {
				enviar(SINTAXIS_INCORRECTA);
				Log.log(PAQUETE_INVALIDO + s);
			} else {
				if (p.length == 2 && superUser) {
					if (p[1].equalsIgnoreCase("Principal")) {
						enviar(LA_SALA_PRINCIPAL_NO_PUEDE_SER_ELIMINADA);
					} else {
						if (Servidor.existeSala(new Sala(p[1]))) {
							Sala sl = Servidor.obtenerSala(p[1]);
							sl.difundir(nick + " ha eliminado la sala");
							Servidor.eliminarSala(sl);
						} else {
							enviar(NO_EXISTE_SALA + p[1]);
						}
					}
				} else {
					enviar(PRIVILEGIOS_INSUFICIENTES);
				}
			}
		} else if (s.startsWith("/LIST")) {
			Sala[] sl = Servidor.obtenerSalas();
			enviar("===========================");
			enviar("Salas disponibles: " + sl.length);
			enviar("===========================");
			for (Sala sl1 : sl) {
				enviar(sl1.getNombre() + " - Usuarios: " + sl1.getCountUsuarios());
			}
			enviar("===========================");
		} else {
			if (s.length() < 140) {
				sala.difundir(nick + ": " + s);
				Log.log("Recibido mensaje de " + nick + " en la sala " + sala.getNombre() + ". Contenido: " + s);
			} else {
				Log.log("Recibido mensaje demasiado largo de " + nick);
			}
		}
	}

	public void enviarListaUsuarios() {
		StringBuilder strb = new StringBuilder();
		strb.append("LIST ");
		for (Usuario usr : sala.getUsuarios()) {
			strb.append(usr.getNick());
			strb.append(" ");
		}
		enviar(strb.toString());
	}

	public void enviar(String s) {
		try {
			salida.write(s + "\n");
			salida.flush();
		} catch (IOException ex) {
			Logger.getLogger(Usuario.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	public String recibir() {
		String s = "";
		try {
			s = entrada.readLine();
		} catch (IOException ex) {

		}
		return s;
	}

	public String getNick() {
		return nick;
	}

	public void setNick(String nick) {
		this.nick = nick;
	}

	public long getLoginTime() {
		return loginTime;
	}

	public void setLoginTime(long loginTime) {
		this.loginTime = loginTime;
	}

	public String getIP() {
		return IP;
	}

	public void setIP(String IP) {
		this.IP = IP;
	}

	public long getPing() {
		return ping;
	}

	public void setPing(long ping) {
		this.ping = ping;
	}

	public boolean isSuperUser() {
		return superUser;
	}

	public void setSuperUser(boolean superUser) {
		this.superUser = superUser;
	}

	public boolean isConectado() {
		return conectado;
	}

	public void setConectado(boolean conectado) {
		this.conectado = conectado;
	}

	private void asyncBeatCheck() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				while (conectado) {
					try {
						Thread.sleep(1000);
					} catch (InterruptedException ex) {

					}
				}
			}
		}).start();
	}

	public Sala getSala() {
		return sala;
	}

	public void setSala(Sala sala) {
		this.sala = sala;
	}

}
