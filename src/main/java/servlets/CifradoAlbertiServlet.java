package servlets;

import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import tecnicasdecifrado.CifradoAlberti;

@WebServlet(name = "CifradoAlbertiServlet", urlPatterns = {"/alberti"})
public class CifradoAlbertiServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");

        String textoOriginal = request.getParameter("texto");
        String clave = request.getParameter("clave");
        String alfabetoParam = request.getParameter("alfabeto");
        String modo = request.getParameter("modo");
        boolean esEspanol = "es".equals(alfabetoParam);

        String discoExterior = CifradoAlberti.getDiscoExterior(esEspanol);
        String discoInterior = CifradoAlberti.getDiscoInterior(esEspanol);
        int modulo = CifradoAlberti.getModuloAlfabeto(esEspanol);

        String error = null;
        String resultado = "";
        String alineacion = "";
        String claveUsada = "";
        String nombreAlfabeto = esEspanol ? "Español (A-Z y Ñ)" : "Americano (A-Z)";
        String textoProcesado = textoOriginal != null ? textoOriginal : "";

        // Procesar texto: quitar caracteres fuera del alfabeto, pero mantener mayús/minús para mostrar
        String textoLimpio = textoProcesado.replaceAll(esEspanol ? "[^A-Za-zÑñ]" : "[^A-Za-z]", "");

        for (char c : textoLimpio.toCharArray()) {
            char mayus = Character.toUpperCase(c);
            if (discoExterior.indexOf(mayus) == -1) {
                error = "El texto contiene letras no presentes en el alfabeto elegido.";
                break;
            }
        }
        if (error == null && textoLimpio.isEmpty()) {
            error = "Ingresa un texto válido (solo letras permitidas).";
        } else if (error == null && (clave == null || clave.trim().isEmpty())) {
            error = "Por favor ingresa una clave (ejemplo: Mb,5,2d)";
        } else if (error == null) {
            String[] partesClave = CifradoAlberti.procesarClave(clave.trim(), esEspanol);
            if (partesClave == null) {
                error = "Clave inválida. Usa el formato: Mb,5,2d";
            } else {
                char letraExterior = partesClave[0].charAt(0);
                char letraInterior = partesClave[0].charAt(1);
                int tamanoGrupo = Integer.parseInt(partesClave[1]);
                int rotacion = Integer.parseInt(partesClave[2].substring(0, partesClave[2].length()-1));
                char direccionRotacion = partesClave[2].charAt(partesClave[2].length()-1);

                if (!CifradoAlberti.validarParametros(letraExterior, letraInterior, tamanoGrupo, rotacion, direccionRotacion, discoExterior, discoInterior)) {
                    error = "Parámetros inválidos. Revisa la clave y sus valores.";
                } else {
                    if ("descifrar".equals(modo)) {
                        resultado = CifradoAlberti.descifrarAlberti(
                            textoLimpio, letraExterior, letraInterior, tamanoGrupo, rotacion,
                            direccionRotacion, discoExterior, discoInterior, modulo, textoProcesado
                        );
                    } else {
                        resultado = CifradoAlberti.cifrarAlberti(
                            textoLimpio, letraExterior, letraInterior, tamanoGrupo, rotacion,
                            direccionRotacion, discoExterior, discoInterior, modulo, textoProcesado
                        );
                    }
                    claveUsada = "" + letraExterior + letraInterior + "," + tamanoGrupo + "," + rotacion + direccionRotacion;
                    alineacion = letraExterior + " (ext) → " + letraInterior + " (int)";
                }
            }
        }

        // Si se obtuvo un resultado exitoso, bloquea el textarea
        boolean bloquearTexto = (error == null && !resultado.isEmpty());

        response.setContentType("text/html;charset=UTF-8");
        try (PrintWriter out = response.getWriter()) {
            out.println("<!DOCTYPE html>");
            out.println("<html lang='es'><head>");
            out.println("<meta charset='UTF-8'><title>Círculo de Alberti</title>");
            out.println("<meta name='viewport' content='width=device-width, initial-scale=1'>");
            out.println("<link href='https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css' rel='stylesheet'>");
            out.println("<style>body{min-height:100vh;display:flex;align-items:center;justify-content:center;background:#f8fafc;} .alberti-card{max-width:400px;margin:auto;padding:2rem 2.2rem;border-radius:1.25rem;box-shadow:0 0 16px #0001;background:#fff;} .form-select, .form-control { margin-bottom: 1rem; }</style>");
            out.println("</head><body>");
            out.println("<div class='alberti-card'>");
            out.println("<h3 class='text-center mb-3'>Círculo de Alberti</h3>");
            out.println("<div class='instructions'><b>¿Qué hace?</b> Puedes cifrar y descifrar textos usando el cifrado de Alberti.<br><b>¿Cómo usar?</b><br>1. Elige el alfabeto.<br>2. Escribe tu texto (mayúsculas y minúsculas permitidas).<br>3. Ingresa la clave (<code>Mb,5,2d</code>).<br>4. Elige <b>cifrar</b> o <b>descifrar</b>.</div>");
            out.println("<form method='post' action='alberti' id='albertiForm'>");
            out.printf("<select name='alfabeto' id='alfabeto' class='form-select' required%s><option value='es'%s>Español (A-Z y Ñ)</option><option value='en'%s>Americano (A-Z)</option></select>",
                bloquearTexto ? " disabled" : "",
                esEspanol ? " selected" : "",
                !esEspanol ? " selected" : ""
            );
            out.printf(
                "<input type='text' class='form-control' id='texto' name='texto' placeholder='Texto aquí...' required value='%s'%s>",
                textoOriginal != null ? textoOriginal : "",
                bloquearTexto ? " disabled" : ""
            );
            out.printf(
                "<input type='text' class='form-control' id='clave' name='clave' placeholder='Clave (ej: Mb,5,2d)' required value='%s'%s>",
                clave != null ? clave : "",
                bloquearTexto ? " disabled" : ""
            );
            out.println("<input type='hidden' name='modo' id='modo' value='cifrar'>");
            out.println("<div class='form-text mb-3'><b>Clave:</b> <code>Mb,5,2d</code> = M (ext) con b (int), grupos de 5, rota 2 der.<br><b>Ejemplo:</b> <code>Ab,4,1i</code> = A con b, grupos de 4, rota 1 izq.</div>");
            out.println("<div class='d-flex justify-content-center gap-2 mb-2'>");
            out.println("<button type='submit' class='btn btn-primary' onclick=\"document.getElementById('modo').value='cifrar'\""
                + (bloquearTexto ? " disabled" : "") + ">Cifrar</button>");
            out.println("<button type='submit' class='btn btn-secondary' onclick=\"document.getElementById('modo').value='descifrar'\""
                + (bloquearTexto ? " disabled" : "") + ">Descifrar</button>");
            out.println("<button type='button' class='btn btn-outline-success' onclick='nuevoAlberti()'>Nuevo</button>");
            out.println("</div>");
            out.println("</form>");

            // Mensajes de error o resultado
            if (error != null) {
                out.printf("<div class='alert alert-danger mt-2'>%s</div>", error);
            } else if (!resultado.isEmpty()) {
                out.println("<div class='alert alert-success mt-2'>");
                out.printf("<strong>Alfabeto:</strong> %s <br>", nombreAlfabeto);
                out.printf("<strong>Texto original:</strong> %s <br>", textoProcesado);
                out.printf("<strong>Clave usada:</strong> %s <br>", claveUsada);
                out.printf("<strong>Disco exterior:</strong> %s <br>", discoExterior);
                out.printf("<strong>Disco interior:</strong> %s <br>", discoInterior);
                out.printf("<strong>Alineación:</strong> %s <br>", alineacion);
                out.printf("<strong>Texto %s:</strong> %s", "descifrado".equals(modo) ? "descifrado" : "cifrado", resultado);
                out.println("</div>");
            }

            // Script para botón Nuevo
            out.println("<script>");
            out.println("function nuevoAlberti() {");
            out.println("  document.getElementById('texto').disabled = false;");
            out.println("  document.getElementById('clave').disabled = false;");
            out.println("  document.getElementById('alfabeto').disabled = false;");
            out.println("  document.getElementById('texto').value = '';");
            out.println("  document.getElementById('clave').value = '';");
            out.println("  document.getElementById('modo').value = 'cifrar';");
            out.println("  // Opcional: Limpia mensajes de resultado, recarga la página o limpia alerts");
            out.println("  document.getElementById('albertiForm').reset();");
            out.println("  // También podrías hacer location.href = 'alberti' para recargar limpio");
            out.println("}");
            out.println("</script>");

            out.println("</div></body></html>");
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.sendRedirect("alberti.html");
    }
}