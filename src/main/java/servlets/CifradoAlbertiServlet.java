package servlets;

import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import tecnicasdecifrado.CifradoAlberti;

@WebServlet(name = "CifradoAlbertiServlet", urlPatterns = {"/alberti"})
public class CifradoAlbertiServlet extends HttpServlet {

    private static final String[] ALFABETOS = {"Español (A-Z y Ñ)", "Americano (A-Z)"};
    private static final String[] ALFABETO_VALUES = {"es", "en"};
    private static final String[] DISCOS_EXTERIOR = {
        "ABCDEFGHIJKLMNÑOPQRSTUVWXYZ", // Español
        "ABCDEFGHIJKLMNOPQRSTUVWXYZ" // Americano
    };
    private static final String[] DISCOS_INTERIOR = {
        "BDFHJLNÑPRTVXZACEGIKMOQSUWY", // Español
        "BDFHJLNPRTVXZACEGIKMOQSUWY" // Americano
    };

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        response.setContentType("text/html;charset=UTF-8");

        // Obtener parámetros
        String textoOriginal = request.getParameter("texto");
        String alfabeto = request.getParameter("alfabeto");
        String claveLetras = request.getParameter("claveLetras");
        String grupo = request.getParameter("grupo");
        String rotacion = request.getParameter("rotacion");
        String direccion = request.getParameter("direccion");
        String modo = request.getParameter("modo");

        // Procesar dirección (aceptar "derecha" o "izquierda" además de "d"/"i")
        if (direccion != null) {
            if (direccion.toLowerCase().startsWith("d")) {
                direccion = "d";
            } else if (direccion.toLowerCase().startsWith("i")) {
                direccion = "i";
            }
        }

        // Determinar índice del alfabeto
        int indexAlfabeto = "en".equals(alfabeto) ? 1 : 0;
        String nombreAlfabeto = ALFABETOS[indexAlfabeto];
        String discoExt = DISCOS_EXTERIOR[indexAlfabeto];
        String discoInt = DISCOS_INTERIOR[indexAlfabeto];

        // Procesar texto (mantener mayúsculas/minúsculas originales)
        String textoLimpio = textoOriginal.replaceAll(indexAlfabeto == 0 ? "[^A-Za-zÑñ]" : "[^A-Za-z]", "");

        // Validaciones
        String error = validarEntrada(textoLimpio, claveLetras, grupo, rotacion, direccion, discoExt, discoInt);

        String resultado = "";
        String claveUsada = "";
        String alineacion = "";

        if (error == null) {
            try {
                // Construir clave en formato para CifradoAlberti
                claveUsada = String.format("%s,%s,%s%s", claveLetras, grupo, rotacion, direccion);

                char letraExterior = claveLetras.toUpperCase().charAt(0);
                char letraInterior = claveLetras.charAt(1); // No forzar minúscula
                alineacion = letraExterior + " (ext) → " + letraInterior + " (int)";

                // Usar los nuevos métodos de CifradoAlberti
                boolean esEspanol = "es".equals(alfabeto);
                if ("descifrar".equals(modo)) {
                    resultado = CifradoAlberti.descifrar(textoLimpio, claveUsada, esEspanol);
                } else {
                    resultado = CifradoAlberti.cifrar(textoLimpio, claveUsada, esEspanol);
                }
            } catch (Exception e) {
                error = "Error al procesar: " + e.getMessage();
            }
        }

        // Generar respuesta HTML
        generarRespuesta(response, request, textoOriginal, resultado, error,
                nombreAlfabeto, claveUsada, alineacion, discoExt, discoInt, modo);
    }

    private String validarEntrada(String textoLimpio, String claveLetras, String grupo,
            String rotacion, String direccion, String discoExt, String discoInt) {

        if (textoLimpio.isEmpty()) {
            return "Ingresa un texto válido (solo letras permitidas).";
        }

        // Validar letras en el texto
        for (char c : textoLimpio.toCharArray()) {
            char mayus = Character.toUpperCase(c);
            if (discoExt.indexOf(mayus) == -1) {
                return "El texto contiene letras no presentes en el alfabeto elegido.";
            }
        }

        if (claveLetras == null || claveLetras.length() != 2) {
            return "Las letras clave deben ser exactamente 2 caracteres (ej: Mb)";
        }

        char letraExt = claveLetras.toUpperCase().charAt(0);
        char letraInt = claveLetras.charAt(1); // No forzar minúscula

        if (discoExt.indexOf(letraExt) == -1) {
            return "Letra exterior no encontrada en el disco exterior";
        }

        // Aceptar mayúscula o minúscula en letra interior
        if (discoInt.indexOf(Character.toUpperCase(letraInt)) == -1 && 
            discoInt.indexOf(Character.toLowerCase(letraInt)) == -1) {
            return "Letra interior '" + letraInt + "' no encontrada en el disco interior";
        }

        try {
            int tamanoGrupo = Integer.parseInt(grupo);
            if (tamanoGrupo <= 0) {
                return "El tamaño de grupo debe ser mayor que cero";
            }
        } catch (NumberFormatException e) {
            return "Tamaño de grupo inválido";
        }

        try {
            int rot = Integer.parseInt(rotacion);
            if (rot <= 0) {
                return "La rotación debe ser mayor que cero";
            }
        } catch (NumberFormatException e) {
            return "Rotación inválida";
        }

        if (!"d".equals(direccion) && !"i".equals(direccion)) {
            return "Dirección de rotación inválida (use 'd' o 'i')";
        }

        return null;
    }

    private void generarRespuesta(HttpServletResponse response, HttpServletRequest request,
            String textoOriginal, String resultado, String error, String nombreAlfabeto,
            String claveUsada, String alineacion, String discoExt, String discoInt,
            String modo) throws IOException {

        try (PrintWriter out = response.getWriter()) {
            out.println("<!DOCTYPE html>");
            out.println("<html lang='es'>");
            out.println("<head>");
            out.println("<meta charset='UTF-8'>");
            out.println("<title>Cifrado de Alberti - Resultado</title>");
            out.println("<meta name='viewport' content='width=device-width, initial-scale=1'>");
            out.println("<link href='https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css' rel='stylesheet'>");
            out.println("<link rel='stylesheet' href='https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.1/font/bootstrap-icons.css'>");
            out.println("<style>");
            out.println("body { min-height: 100vh; display: flex; align-items: center; justify-content: center; background: #f5f7fa; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; }");
            out.println(".alberti-container { max-width: 600px; width: 100%; padding: 2rem; }");
            out.println(".alberti-card { padding: 2rem; border-radius: 1rem; box-shadow: 0 4px 12px rgba(0,0,0,0.1); background: #fff; }");
            out.println(".form-label { font-weight: 500; color: #495057; }");
            out.println(".visual-guide { background: #f8f9fa; border-radius: 0.5rem; padding: 1rem; margin-bottom: 1.5rem; border-left: 4px solid #0d6efd; }");
            out.println(".disco-visual { display: flex; justify-content: center; margin: 1rem 0; gap: 2rem; flex-wrap: wrap; }");
            out.println(".disco { text-align: center; font-weight: bold; }");
            out.println(".disco-exterior { color: #0d6efd; }");
            out.println(".disco-interior { color: #dc3545; }");
            out.println(".btn-action { min-width: 100px; }");
            out.println(".example { font-size: 0.9em; color: #6c757d; margin-top: 0.5rem; }");
            out.println(".result-disks { font-family: monospace; letter-spacing: 2px; }");
            out.println("</style>");
            out.println("</head>");
            out.println("<body>");
            out.println("<div class='alberti-container'>");
            out.println("<div class='alberti-card'>");

            // Título
            out.println("<h2 class='text-center mb-4'><i class='bi bi-lock'></i> Cifrado de Alberti</h2>");

            // Mostrar error si existe
            if (error != null) {
                out.println("<div class='alert alert-danger mb-4'><i class='bi bi-exclamation-triangle'></i> " + error + "</div>");
            }

            // Formulario
            out.println("<form method='post' action='alberti' id='albertiForm'>");

            // Alfabeto
            out.println("<div class='mb-3'>");
            out.println("<label for='alfabeto' class='form-label'>Alfabeto</label>");
            out.println("<select name='alfabeto' id='alfabeto' class='form-select' required>");
            for (int i = 0; i < ALFABETOS.length; i++) {
                out.printf("<option value='%s'%s>%s</option>",
                        ALFABETO_VALUES[i],
                        ALFABETO_VALUES[i].equals(error == null ? request.getParameter("alfabeto") : "es") ? " selected" : "",
                        ALFABETOS[i]);
            }
            out.println("</select>");
            out.println("</div>");

            // Texto
            out.println("<div class='mb-3'>");
            out.println("<label for='texto' class='form-label'>Texto Plano</label>");
            out.printf("<textarea class='form-control' id='texto' name='texto' rows='3' placeholder='Escribe el texto a cifrar o descifrar...' required>%s</textarea>",
                    textoOriginal != null ? textoOriginal : "");
            out.println("</div>");

            // Configuración de clave
            out.println("<div class='mb-3'>");
            out.println("<label class='form-label'>Configuración de Clave</label>");
            out.println("<div class='row g-2'>");

            // Letras clave
            out.println("<div class='col-md-4'>");
            out.printf("<input type='text' class='form-control' id='claveLetras' name='claveLetras' placeholder='Ej: Mb' maxlength='2' required value='%s'>",
                    request.getParameter("claveLetras") != null ? request.getParameter("claveLetras") : "");
            out.println("<div class='example'>Letra exterior + interior (Ej: Mb)</div>");
            out.println("</div>");

            // Tamaño grupo
            out.println("<div class='col-md-4'>");
            out.printf("<input type='number' class='form-control' id='grupo' name='grupo' placeholder='Tamaño grupo' min='1' required value='%s'>",
                    request.getParameter("grupo") != null ? request.getParameter("grupo") : "5");
            out.println("<div class='example'>Tamaño de grupo (Ej: 5)</div>");
            out.println("</div>");

            // Rotación
            out.println("<div class='col-md-4'>");
            out.println("<div class='input-group'>");
            out.printf("<input type='number' class='form-control' id='rotacion' name='rotacion' placeholder='Rotación' min='1' required value='%s'>",
                    request.getParameter("rotacion") != null ? request.getParameter("rotacion") : "2");
            out.println("<select class='form-select' id='direccion' name='direccion' required>");
            out.printf("<option value='d'%s>Derecha</option>",
                    "d".equalsIgnoreCase(request.getParameter("direccion")) ? " selected" : "");
            out.printf("<option value='i'%s>Izquierda</option>",
                    "i".equalsIgnoreCase(request.getParameter("direccion")) ? " selected" : "");
            out.println("</select>");
            out.println("</div>");
            out.println("<div class='example'>Rotación cada grupo (Ej: 2 posiciones)</div>");
            out.println("</div>");
            out.println("</div>"); // cierre row
            out.println("</div>"); // cierre mb-3

            // Botones
            out.println("<div class='d-flex justify-content-center gap-3 mt-4'>");
            out.println("<button type='submit' name='modo' value='cifrar' class='btn btn-primary btn-action'>");
            out.println("<i class='bi bi-lock'></i> Cifrar");
            out.println("</button>");
            out.println("<button type='submit' name='modo' value='descifrar' class='btn btn-success btn-action'>");
            out.println("<i class='bi bi-unlock'></i> Descifrar");
            out.println("</button>");
            out.println("<button type='button' class='btn btn-outline-secondary btn-action' onclick='resetForm()'>");
            out.println("<i class='bi bi-arrow-counterclockwise'></i> Reiniciar");
            out.println("</button>");
            out.println("</div>");

            out.println("</form>");

            // Mostrar resultados si no hay error
            if (error == null && !resultado.isEmpty()) {
                out.println("<div class='alert alert-success mt-4'>");
                out.println("<h5 class='alert-heading'><i class='bi bi-check-circle'></i> Resultado del " + ("descifrar".equals(modo) ? "descifrado" : "cifrado") + "</h5>");
                out.println("<hr>");

                out.println("<div class='row'>");
                out.println("<div class='col-md-6'>");
                out.println("<p><strong><i class='bi bi-translate'></i> Alfabeto:</strong> " + nombreAlfabeto + "</p>");
                out.println("<p><strong><i class='bi bi-text-left'></i> Texto Plano:</strong><br>" + textoOriginal + "</p>");
                out.println("<p><strong><i class='bi bi-key'></i> Clave usada:</strong> " + claveUsada + "</p>");
                out.println("<p><strong><i class='bi bi-arrow-left-right'></i> Alineación:</strong> " + alineacion + "</p>");
                out.println("</div>");

                out.println("<div class='col-md-6'>");
                out.println("<p class='mb-2'><strong>Discos:</strong></p>");
                out.println("<div class='result-disks disco-exterior mb-2'>Exterior: " + discoExt + "</div>");
                out.println("<div class='result-disks disco-interior'>Interior: " + discoInt + "</div>");
                out.println("</div>");
                out.println("</div>");

                out.println("<hr>");
                out.println("<p class='mb-0'><strong><i class='bi bi-" + ("descifrar".equals(modo) ? "unlock" : "lock") + "'></i> " + ("descifrar".equals(modo) ? "Texto descifrado" : "Criptograma") + ":</strong><br>" + resultado + "</p>");
                out.println("</div>");
            }

            // Script para resetear el formulario
            out.println("<script>");
            out.println("function resetForm() {");
            out.println("  document.getElementById('albertiForm').reset();");
            out.println("  document.getElementById('texto').value = '';");
            out.println("  document.getElementById('claveLetras').value = '';");
            out.println("  document.getElementById('grupo').value = '5';");
            out.println("  document.getElementById('rotacion').value = '2';");
            out.println("  document.getElementById('direccion').value = 'd';");
            out.println("}");
            out.println("</script>");

            out.println("</div>"); // cierre alberti-card
            out.println("</div>"); // cierre alberti-container
            out.println("</body>");
            out.println("</html>");
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.sendRedirect("alberti.html");
    }
}