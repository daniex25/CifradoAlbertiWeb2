package tecnicasdecifrado;

public class CifradoAlberti {
    // Alfabetos exterior/interior para ambos idiomas
    private static final String DISCO_EXTERIOR_ESP = "ABCDEFGHIJKLMNÑOPQRSTUVWXYZ";
    private static final String DISCO_INTERIOR_ESP = "BDFHJLNÑPRTVXZACEGIKMOQSUWY";
    private static final String DISCO_EXTERIOR_AME = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String DISCO_INTERIOR_AME = "BDFHJLNPRTVXZACEGIKMOQSUWY";

    public static String[] procesarClave(String claveCompleta, boolean esEspanol) {
        try {
            claveCompleta = claveCompleta.replaceAll("[()]", "").trim();
            String[] partes = claveCompleta.split("\\s*,\\s*");
            if (partes.length != 3) return null;
            String regexLetras = esEspanol ? "[A-Za-zÑñ]{2}" : "[A-Za-z]{2}";
            if (!partes[0].matches(regexLetras) || 
                !partes[1].matches("\\d+") || 
                !partes[2].matches("\\d+[diDI]")) {
                return null;
            }
            return partes;
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean validarParametros(
        char letraExterior, 
        char letraInterior, 
        int tamanoGrupo, 
        int rotacion, 
        char direccion,
        String discoExterior,
        String discoInterior
    ) {
        if (discoExterior.indexOf(Character.toUpperCase(letraExterior)) == -1) return false;
        if (discoInterior.indexOf(Character.toLowerCase(letraInterior)) == -1 &&
            discoInterior.indexOf(Character.toUpperCase(letraInterior)) == -1) return false;
        if (tamanoGrupo <= 0) return false;
        if (rotacion <= 0) return false;
        if (direccion != 'd' && direccion != 'D' && direccion != 'i' && direccion != 'I') return false;
        return true;
    }

    // Soporta mayúsculas y minúsculas en la salida según el texto original
    public static String cifrarAlberti(
        String texto, 
        char letraExterior, 
        char letraInterior, 
        int tamanoGrupo, 
        int rotacion, 
        char direccionRotacion,
        String discoExterior,
        String discoInterior,
        int moduloAlfabeto,
        String textoOriginal // Nuevo: para mantener las mayúsculas y minúsculas originales
    ) {
        StringBuilder resultado = new StringBuilder();
        int posExterior = discoExterior.indexOf(Character.toUpperCase(letraExterior));
        int posInterior = discoInterior.indexOf(Character.toLowerCase(letraInterior));
        if (posInterior == -1) posInterior = discoInterior.indexOf(Character.toUpperCase(letraInterior));
        int desplazamientoInicial = (posInterior - posExterior) % moduloAlfabeto;
        if (desplazamientoInicial < 0) desplazamientoInicial += moduloAlfabeto;
        int rotacionAcumulada = 0;
        int idxTexto = 0;
        for (int i = 0; i < texto.length(); i++) {
            if (i > 0 && i % tamanoGrupo == 0) {
                if (direccionRotacion == 'd' || direccionRotacion == 'D') {
                    rotacionAcumulada += rotacion;
                } else {
                    rotacionAcumulada -= rotacion;
                }
            }
            char c = texto.charAt(i);
            char cOrig = textoOriginal.charAt(idxTexto++);
            int posExteriorActual = discoExterior.indexOf(Character.toUpperCase(c));
            if (posExteriorActual != -1) {
                int posInteriorActual = (posExteriorActual + desplazamientoInicial + rotacionAcumulada) % moduloAlfabeto;
                if (posInteriorActual < 0) posInteriorActual += moduloAlfabeto;
                char cifrado = discoInterior.charAt(posInteriorActual);
                // Mantener mayúsculas/minúsculas
                resultado.append(Character.isLowerCase(cOrig) ? Character.toLowerCase(cifrado) : cifrado);
            }
        }
        return resultado.toString();
    }

    // Descifrado: busca en el disco interior y mapea al exterior, respetando mayus/minus de entrada
    public static String descifrarAlberti(
        String texto, 
        char letraExterior, 
        char letraInterior, 
        int tamanoGrupo, 
        int rotacion, 
        char direccionRotacion,
        String discoExterior,
        String discoInterior,
        int moduloAlfabeto,
        String textoOriginal
    ) {
        StringBuilder resultado = new StringBuilder();
        int posExterior = discoExterior.indexOf(Character.toUpperCase(letraExterior));
        int posInterior = discoInterior.indexOf(Character.toLowerCase(letraInterior));
        if (posInterior == -1) posInterior = discoInterior.indexOf(Character.toUpperCase(letraInterior));
        int desplazamientoInicial = (posInterior - posExterior) % moduloAlfabeto;
        if (desplazamientoInicial < 0) desplazamientoInicial += moduloAlfabeto;
        int rotacionAcumulada = 0;
        int idxTexto = 0;
        for (int i = 0; i < texto.length(); i++) {
            if (i > 0 && i % tamanoGrupo == 0) {
                if (direccionRotacion == 'd' || direccionRotacion == 'D') {
                    rotacionAcumulada += rotacion;
                } else {
                    rotacionAcumulada -= rotacion;
                }
            }
            char c = texto.charAt(i);
            char cOrig = textoOriginal.charAt(idxTexto++);
            int posInteriorActual = discoInterior.indexOf(Character.toUpperCase(c));
            if (posInteriorActual == -1) posInteriorActual = discoInterior.indexOf(Character.toLowerCase(c));
            if (posInteriorActual != -1) {
                int posExteriorActual = (posInteriorActual - desplazamientoInicial - rotacionAcumulada) % moduloAlfabeto;
                if (posExteriorActual < 0) posExteriorActual += moduloAlfabeto;
                char descifrado = discoExterior.charAt(posExteriorActual);
                // Mantener mayúsculas/minúsculas
                resultado.append(Character.isLowerCase(cOrig) ? Character.toLowerCase(descifrado) : descifrado);
            }
        }
        return resultado.toString();
    }

    public static String getDiscoExterior(boolean esEspanol) {
        return esEspanol ? DISCO_EXTERIOR_ESP : DISCO_EXTERIOR_AME;
    }
    public static String getDiscoInterior(boolean esEspanol) {
        return esEspanol ? DISCO_INTERIOR_ESP : DISCO_INTERIOR_AME;
    }
    public static int getModuloAlfabeto(boolean esEspanol) {
        return esEspanol ? 27 : 26;
    }
}