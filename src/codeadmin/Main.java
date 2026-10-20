package codeadmin;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

public class Main extends JFrame {

	private static final long serialVersionUID = 1L;

	// Archivo con los hashes SHA-256 (una línea "password: ..." y otra "codigo:
	// ...")
	private static final String ARCHIVO_HASH = "archivo_hash.txt";

	// GIFs que se muestran al comprobar el código final (deben estar junto a
	// archivo_hash.txt)
	private static final String GIF_EXITO = "exito.gif";
	private static final String GIF_ERROR = "error.gif";

	// ====== CAMBIA ESTE TEXTO por el que quieras que tenga que descifrar ======
	private static final String TEXTO_A_DESCIFRAR = "Surcando entre los restos, habéis tenido este acierto\r\n"
			+ "Ahora un ultimo desafío os acecha entre todo este chandrío\r\n"
			+ "La pista es sencilla, ya que es vuestra mascotilla\r\n"
			+ "Dadle a la cabecilla para sacar esa contraseñilla";
	// ==========================================================================

	private static final String PANTALLA_PASSWORD = "password";
	private static final String PANTALLA_CODIGO = "codigo";
	private static final String PANTALLA_EXITO = "exito";
	private static final String PANTALLA_ERROR = "error";

	private static final Color FONDO = new Color(30, 33, 48);
	private static final Color PANEL = new Color(42, 46, 66);
	private static final Color ACENTO = new Color(98, 114, 255);
	private static final Color TEXTO = new Color(235, 237, 245);
	private static final Color ERROR = new Color(255, 100, 110);

	private final CardLayout cartas = new CardLayout();
	private final JPanel contenedor = new JPanel(cartas);

	private final String hashPassword;
	private final String hashCodigo;

	private JPasswordField campoPassword;
	private JLabel mensajePassword;
	private JTextField campoCodigo;
	private PanelGif gifExito;
	private PanelGif gifError;
	private JButton botonVolver;

	public Main(Map<String, String> hashes) {
		this.hashPassword = hashes.get("password");
		this.hashCodigo = hashes.get("codigo");

		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setSize(520, 480);
		setLocationRelativeTo(null);
		setResizable(false);
		
		ImageIcon icono = new ImageIcon("icono.png");
		if (icono.getImage() != null) {
			setIconImage(icono.getImage());
		}

		contenedor.setBackground(FONDO);
		contenedor.add(crearPantallaPassword(), PANTALLA_PASSWORD);
		contenedor.add(crearPantallaCodigo(), PANTALLA_CODIGO);
		contenedor.add(crearPantallaExito(), PANTALLA_EXITO);
		contenedor.add(crearPantallaError(), PANTALLA_ERROR);
		setContentPane(contenedor);

		cartas.show(contenedor, PANTALLA_PASSWORD);
	}

	// ---------- Pantalla 1: contraseña ----------
	private JPanel crearPantallaPassword() {
		JPanel p = new JPanel();
		p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
		p.setBackground(FONDO);
		p.setBorder(new EmptyBorder(40, 60, 40, 60));

		JLabel titulo = etiqueta("🔒 Acceso restringido", 26, true);
		JLabel sub = etiqueta("Introducid la contraseña para continuar", 14, false);

		campoPassword = new JPasswordField();
		estilizarCampo(campoPassword);

		JToggleButton ojo = new JToggleButton("👁");
		ojo.setFont(new Font("SansSerif", Font.PLAIN, 16));
		ojo.setBackground(PANEL);
		ojo.setForeground(TEXTO);
		ojo.setOpaque(true);
		ojo.setFocusPainted(false);
		ojo.setBorder(BorderFactory.createLineBorder(ACENTO, 1));
		ojo.setPreferredSize(new Dimension(48, 42));
		ojo.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

		char ocultar = campoPassword.getEchoChar();
		ojo.addActionListener(e -> campoPassword.setEchoChar(ojo.isSelected() ? (char) 0 : ocultar));

		JPanel filaPassword = new JPanel(new BorderLayout(6, 0));
		filaPassword.setOpaque(false);
		filaPassword.add(campoPassword, BorderLayout.CENTER);
		filaPassword.add(ojo, BorderLayout.EAST);
		filaPassword.setAlignmentX(Component.CENTER_ALIGNMENT);
		filaPassword.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));

		JButton boton = crearBoton("Entrar");
		mensajePassword = etiqueta(" ", 13, false);

		boton.addActionListener(e -> comprobarPassword());
		campoPassword.addActionListener(e -> comprobarPassword());

		p.add(Box.createVerticalGlue());
		p.add(titulo);
		p.add(Box.createVerticalStrut(8));
		p.add(sub);
		p.add(Box.createVerticalStrut(35));
		p.add(filaPassword);
		p.add(Box.createVerticalStrut(20));
		p.add(boton);
		p.add(Box.createVerticalStrut(18));
		p.add(mensajePassword);
		p.add(Box.createVerticalGlue());
		return p;
	}

	private void comprobarPassword() {
		String intento = new String(campoPassword.getPassword());
		if (coincide(intento, hashPassword)) {
			campoPassword.setText("");
			mensajePassword.setText(" ");
			cartas.show(contenedor, PANTALLA_CODIGO);
			campoCodigo.requestFocusInWindow();
		} else {
			mensajePassword.setForeground(ERROR);
			mensajePassword.setText("❌ Parece que no habéis ordenado bien las piezas...");
			campoPassword.setText("");
			campoPassword.requestFocusInWindow();
		}
	}

	// ---------- Pantalla 2: texto y código final ----------
	private JPanel crearPantallaCodigo() {
		JPanel p = new JPanel();
		p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
		p.setBackground(FONDO);
		p.setBorder(new EmptyBorder(30, 40, 30, 40));

		JLabel titulo = etiqueta("🔓 Os toca pensar", 22, true);

		JTextArea areaTexto = new JTextArea(TEXTO_A_DESCIFRAR);
		areaTexto.setEditable(false);
		areaTexto.setLineWrap(true);
		areaTexto.setWrapStyleWord(true);
		areaTexto.setFont(new Font("Monospaced", Font.PLAIN, 15));
		areaTexto.setBackground(PANEL);
		areaTexto.setForeground(TEXTO);
		areaTexto.setBorder(new EmptyBorder(15, 15, 15, 15));

		JLabel pide = etiqueta("¿Lo habréis descifrado correctamente?:", 14, false);

		campoCodigo = new JTextField();
		estilizarCampo(campoCodigo);
		campoCodigo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));

		JButton botonCodigo = crearBoton("Comprobar");

		botonCodigo.addActionListener(e -> comprobarCodigo());
		campoCodigo.addActionListener(e -> comprobarCodigo());

		p.add(titulo);
		p.add(Box.createVerticalStrut(15));
		p.add(areaTexto);
		p.add(Box.createVerticalStrut(20));
		p.add(pide);
		p.add(Box.createVerticalStrut(8));
		p.add(campoCodigo);
		p.add(Box.createVerticalStrut(15));
		p.add(botonCodigo);
		return p;
	}

	private void comprobarCodigo() {
		String intento = campoCodigo.getText().trim();
		if (coincide(intento, hashCodigo)) {
			gifExito.cargar(GIF_EXITO);
			cartas.show(contenedor, PANTALLA_EXITO);
		} else {
			campoCodigo.setText("");
			gifError.cargar(GIF_ERROR);
			cartas.show(contenedor, PANTALLA_ERROR);
			getRootPane().setDefaultButton(botonVolver);
			botonVolver.requestFocusInWindow();
		}
	}

	// ---------- Pantalla 3: éxito (solo el GIF) ----------
	private JPanel crearPantallaExito() {
		JPanel p = new JPanel(new BorderLayout());
		p.setBackground(FONDO);

		gifExito = new PanelGif("🎉 ¡Enhorabuena!");

		p.add(crearTitulo("¡Ole! El dinerito es vuestro", TEXTO), BorderLayout.NORTH);
		p.add(gifExito, BorderLayout.CENTER);
		return p;
	}

	// ---------- Pantalla 4: error (GIF + botón para volver) ----------
	private JPanel crearPantallaError() {
		JPanel p = new JPanel(new BorderLayout());
		p.setBackground(FONDO);

		gifError = new PanelGif("❌ Código incorrecto");

		botonVolver = crearBoton("Volver a intentarlo");
		botonVolver.setPreferredSize(new Dimension(220, 42));
		botonVolver.addActionListener(e -> volverAIntentar());

		JPanel abajo = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 15));
		abajo.setBackground(FONDO);
		abajo.add(botonVolver);

		p.add(crearTitulo("Hay que darle más al coco", ERROR), BorderLayout.NORTH);
		p.add(gifError, BorderLayout.CENTER);
		p.add(abajo, BorderLayout.SOUTH);
		return p;
	}

	private void volverAIntentar() {
		getRootPane().setDefaultButton(null);
		cartas.show(contenedor, PANTALLA_CODIGO);
		campoCodigo.requestFocusInWindow();
	}

	// ---------- Panel que muestra un GIF animado ocupando todo el espacio
	// ----------
	private static class PanelGif extends JPanel {
		private static final long serialVersionUID = 1L;

		private final String textoRespaldo;
		private String nombre = "";
		private Image imagen;
		private int ancho, alto;

		PanelGif(String textoRespaldo) {
			this.textoRespaldo = textoRespaldo;
			setBackground(FONDO);
		}

		/**
		 * Carga el GIF desde cero (así la animación empieza de nuevo cada vez que se
		 * muestra).
		 */
		void cargar(String nombreArchivo) {
			nombre = nombreArchivo;
			imagen = null;
			try {
				Path ruta = localizarArchivo(nombreArchivo);
				// createImage (no getImage) para que no se reutilice una copia en caché
				ImageIcon icono = new ImageIcon(Toolkit.getDefaultToolkit().createImage(ruta.toString()));
				if (icono.getIconWidth() > 0 && icono.getIconHeight() > 0) {
					imagen = icono.getImage();
					ancho = icono.getIconWidth();
					alto = icono.getIconHeight();
				}
			} catch (IOException e) {
				// se mostrará el texto de respaldo
			}
			repaint();
		}

		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			Graphics2D g2 = (Graphics2D) g.create();
			try {
				if (imagen == null) {
					pintarRespaldo(g2);
					return;
				}
				double escala = Math.min((double) getWidth() / ancho, (double) getHeight() / alto);
				int w = (int) Math.round(ancho * escala);
				int h = (int) Math.round(alto * escala);
				int x = (getWidth() - w) / 2;
				int y = (getHeight() - h) / 2;
				g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
				g2.drawImage(imagen, x, y, w, h, this); // 'this' repinta en cada fotograma
			} finally {
				g2.dispose();
			}
		}

		private void pintarRespaldo(Graphics2D g2) {
			g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			g2.setColor(TEXTO);

			g2.setFont(new Font("SansSerif", Font.BOLD, 26));
			FontMetrics fm = g2.getFontMetrics();
			g2.drawString(textoRespaldo, (getWidth() - fm.stringWidth(textoRespaldo)) / 2, getHeight() / 2);

			if (!nombre.isEmpty()) {
				String aviso = "(no encuentro " + nombre + ")";
				g2.setFont(new Font("SansSerif", Font.PLAIN, 13));
				fm = g2.getFontMetrics();
				g2.drawString(aviso, (getWidth() - fm.stringWidth(aviso)) / 2, getHeight() / 2 + 30);
			}
		}
	}

	// ---------- Hashes ----------
	private static boolean coincide(String introducido, String hashGuardado) {
		if (hashGuardado == null)
			return false;
		return MessageDigest.isEqual(sha256Hex(introducido).getBytes(StandardCharsets.UTF_8),
				hashGuardado.getBytes(StandardCharsets.UTF_8));
	}

	private static String sha256Hex(String texto) {
		try {
			byte[] h = MessageDigest.getInstance("SHA-256").digest(texto.getBytes(StandardCharsets.UTF_8));
			StringBuilder sb = new StringBuilder();
			for (byte b : h)
				sb.append(String.format("%02x", b & 0xFF));
			return sb.toString();
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}

	/**
	 * Lee archivo_hash.txt (líneas "clave: hash"). Lo busca en la carpeta actual y
	 * junto al programa.
	 */
	private static Map<String, String> cargarHashes() throws IOException {
		Path archivo = localizarArchivo(ARCHIVO_HASH);
		Map<String, String> hashes = new HashMap<>();
		for (String linea : Files.readAllLines(archivo, StandardCharsets.UTF_8)) {
			int i = linea.indexOf(':');
			if (i < 0)
				continue;
			hashes.put(linea.substring(0, i).trim(), linea.substring(i + 1).trim().toLowerCase());
		}
		if (!hashes.containsKey("password") || !hashes.containsKey("codigo")) {
			throw new IOException("Faltan las líneas 'password' y/o 'codigo' en " + archivo);
		}
		return hashes;
	}

	/**
	 * Busca un archivo en la carpeta actual y, si no está, junto a las clases
	 * compiladas (y hasta 2 carpetas por encima).
	 */
	private static Path localizarArchivo(String nombre) throws IOException {
		Path enCarpetaActual = Paths.get(nombre);
		if (Files.exists(enCarpetaActual))
			return enCarpetaActual;
		try {
			Path programa = Paths.get(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI())
					.toAbsolutePath();
			Path dir = Files.isDirectory(programa) ? programa : programa.getParent();
			Path d = dir;
			for (int nivel = 0; nivel < 3 && d != null; nivel++, d = d.getParent()) {
				Path candidato = d.resolve(nombre);
				if (Files.exists(candidato))
					return candidato;
			}
		} catch (Exception ignorada) {
			// se informa abajo
		}
		throw new IOException(
				"No encuentro el archivo " + nombre + ". Debe estar en la carpeta desde la que ejecutas el programa.");
	}

	// ---------- Utilidades de estilo ----------
	private JLabel etiqueta(String texto, int tam, boolean negrita) {
		JLabel l = new JLabel(texto);
		l.setFont(new Font("SansSerif", negrita ? Font.BOLD : Font.PLAIN, tam));
		l.setForeground(TEXTO);
		l.setAlignmentX(Component.CENTER_ALIGNMENT);
		return l;
	}

	private void estilizarCampo(JTextField c) {
		c.setFont(new Font("SansSerif", Font.PLAIN, 16));
		c.setBackground(PANEL);
		c.setForeground(TEXTO);
		c.setCaretColor(TEXTO);
		c.setHorizontalAlignment(JTextField.CENTER);
		c.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(ACENTO, 1),
				new EmptyBorder(6, 10, 6, 10)));
	}

	private JLabel crearTitulo(String texto, Color color) {
		JLabel l = etiqueta(texto, 28, true);
		l.setForeground(color);
		l.setHorizontalAlignment(SwingConstants.CENTER);
		l.setBorder(new EmptyBorder(20, 0, 10, 0));
		return l;
	}

	private JButton crearBoton(String texto) {
		JButton b = new JButton(texto);
		b.setFont(new Font("SansSerif", Font.BOLD, 15));
		b.setBackground(ACENTO);
		b.setForeground(Color.WHITE);
		b.setFocusPainted(false);
		b.setOpaque(true);
		b.setBorderPainted(false);
		b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		b.setAlignmentX(Component.CENTER_ALIGNMENT);
		b.setMaximumSize(new Dimension(200, 42));
		return b;
	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(() -> {
			try {
				new Main(cargarHashes()).setVisible(true);
			} catch (IOException e) {
				JOptionPane.showMessageDialog(null, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
				System.exit(1);
			}
		});
	}
}
