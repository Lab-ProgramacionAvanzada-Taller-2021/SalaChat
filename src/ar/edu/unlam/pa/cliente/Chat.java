package ar.edu.unlam.pa.cliente;

import java.awt.EventQueue;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.logging.Logger;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.text.DefaultCaret;

import ar.edu.unlam.pa.cliente.entidades.Usuario;

public class Chat extends JFrame {

	private Conexion net;
	private DefaultListModel<String> mlu;

	private JTextArea areaMensajes;
	private JButton btEnviar;
	private JTextField fieldMsg;
	private JList jList1;
	private JScrollPane jScrollPane2;
	private JScrollPane jScrollPane3;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		try {
			for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
				if ("Nimbus".equals(info.getName())) {
					UIManager.setLookAndFeel(info.getClassName());
					break;
				}
			}
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException
				| javax.swing.UnsupportedLookAndFeelException ex) {
			Logger.getLogger(Chat.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
		}
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					Chat frame = new Chat();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the frame.
	 */
	public Chat() {

		net = Conexion.getInstance();
		// ingresa la ip
		net.setServer(leerIP(), 2021);
		net.setInterfaz(this);
		// ingresa el nick
		net.enviar("NICK " + leerNick());

		mlu = new DefaultListModel<>();

		componenteSalaChat();

		setComponentsExtras();

		new Thread(new Runnable() {
			@Override
			public void run() {
				net.escucharServidor();
			}
		}).start();
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				net.enviar("EXIT");
			}
		});

	}

	private void componenteSalaChat() {
		fieldMsg = new javax.swing.JTextField();
		btEnviar = new javax.swing.JButton();
		jScrollPane2 = new javax.swing.JScrollPane();
		areaMensajes = new javax.swing.JTextArea();
		jScrollPane3 = new javax.swing.JScrollPane();
		jList1 = new javax.swing.JList();

		setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

		fieldMsg.addKeyListener(new java.awt.event.KeyAdapter() {
			public void keyPressed(java.awt.event.KeyEvent evt) {
				fieldMsgKeyPressed(evt);
			}
		});

		btEnviar.setText("Enviar");
		btEnviar.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				btEnviarActionPerformed(evt);
			}
		});

		areaMensajes.setEditable(false);
		areaMensajes.setColumns(20);
		areaMensajes.setLineWrap(true);
		areaMensajes.setRows(5);
		areaMensajes.setToolTipText("");
		areaMensajes.setWrapStyleWord(true);
		jScrollPane2.setViewportView(areaMensajes);

		jList1.setModel(mlu);
		jList1.setFixedCellHeight(20);
		jScrollPane3.setViewportView(jList1);

		javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
		getContentPane().setLayout(layout);
		layout.setHorizontalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addGroup(layout.createSequentialGroup().addContainerGap()
						.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
								.addComponent(fieldMsg)
								.addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 320, Short.MAX_VALUE))
						.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
						.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
								.addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 135, Short.MAX_VALUE)
								.addComponent(btEnviar, javax.swing.GroupLayout.DEFAULT_SIZE,
										javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
						.addContainerGap()));
		layout.setVerticalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addGroup(javax.swing.GroupLayout.Alignment.TRAILING,
						layout.createSequentialGroup().addContainerGap()
								.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
										.addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 309,
												Short.MAX_VALUE)
										.addComponent(jScrollPane3))
								.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
								.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
										.addComponent(fieldMsg, javax.swing.GroupLayout.PREFERRED_SIZE,
												javax.swing.GroupLayout.DEFAULT_SIZE,
												javax.swing.GroupLayout.PREFERRED_SIZE)
										.addComponent(btEnviar))
								.addContainerGap()));

		pack();
	}

	private void btEnviarActionPerformed(java.awt.event.ActionEvent evt) {
		net.enviar(fieldMsg.getText());
		fieldMsg.setText("");
	}

	private void fieldMsgKeyPressed(java.awt.event.KeyEvent evt) {
		if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
			btEnviarActionPerformed(null);
		}
	}

	public void agregarUsuario(Usuario u) {
		mlu.addElement(u.getNick());
	}

	public void agregarMensaje(String s) {
		areaMensajes.append(s + "\n");
	}

	public void limpiarListado() {
		mlu.clear();
	}

	private void setComponentsExtras() {
		DefaultCaret caret = (DefaultCaret) areaMensajes.getCaret();
		caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
		jList1.setFixedCellHeight(20);
		setLocationRelativeTo(null);
		fieldMsg.requestFocus();
	}

	private String leerIP() {
		return JOptionPane.showInputDialog(null, "Ingrese la IP del servidor", "127.0.0.1");
	}

	private String leerNick() {
		return JOptionPane.showInputDialog(null, "Ingrese su nick", "Usuario");
	}
}
