package tecnicasdecifrado;

public class CifradoAlberti {

    // Alfabetos
    public static final String DISCO_EXTERIOR_ESP = "ABCDEFGHIJKLMNÑOPQRSTUVWXYZ";
    public static final String DISCO_INTERIOR_ESP = "BDFHJLNÑPRTVXZACEGIKMOQSUWY";
    public static final String DISCO_EXTERIOR_ENG = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    public static final String DISCO_INTERIOR_ENG = "BDFHJLNPRTVXZACEGIKMOQSUWY";

    // aquí encapsulo la configuracióin del cifrado
    public static class Configuracion {

        private final String discoExterior;
        private final String discoInterior;
        private int desplazamiento;
        private final int tamanoGrupo;
        private final int rotacion;
        private final char direccionRotacion;

        public Configuracion(String discoExterior, String discoInterior,
                int desplazamientoInicial, int tamanoGrupo,
                int rotacion, char direccionRotacion) {
            this.discoExterior = discoExterior;
            this.discoInterior = discoInterior;
            this.desplazamiento = desplazamientoInicial;
            this.tamanoGrupo = tamanoGrupo;
            this.rotacion = rotacion;
            this.direccionRotacion = Character.toLowerCase(direccionRotacion);
        }

        public String getDiscoExterior() {
            return discoExterior;
        }

        public String getDiscoInterior() {
            return discoInterior;
        }

        public int getDesplazamiento() {
            return desplazamiento;
        }

        public int getTamanoGrupo() {
            return tamanoGrupo;
        }

        public int getRotacion() {
            return rotacion;
        }

        public char getDireccionRotacion() {
            return direccionRotacion;
        }

        public void rotar() {
            if (direccionRotacion == 'd') {
                desplazamiento += rotacion;
            } else {
                desplazamiento -= rotacion;
            }
            desplazamiento = (desplazamiento + discoInterior.length()) % discoInterior.length();
        }
    }


    public static String cifrar(String texto, String clave, boolean esEspanol) {
        String discoExt = esEspanol ? DISCO_EXTERIOR_ESP : DISCO_EXTERIOR_ENG;
        String discoInt = esEspanol ? DISCO_INTERIOR_ESP : DISCO_INTERIOR_ENG;
        Configuracion config = parsearClave(clave, discoExt, discoInt);
        return procesarTexto(texto, config, true);
    }


    public static String descifrar(String texto, String clave, boolean esEspanol) {
        String discoExt = esEspanol ? DISCO_EXTERIOR_ESP : DISCO_EXTERIOR_ENG;
        String discoInt = esEspanol ? DISCO_INTERIOR_ESP : DISCO_INTERIOR_ENG;
        Configuracion config = parsearClave(clave, discoExt, discoInt);
        return procesarTexto(texto, config, false);
    }


    private static String procesarTexto(String texto, Configuracion config, boolean cifrar) {
        StringBuilder resultado = new StringBuilder();

        for (int i = 0; i < texto.length(); i++) {
            // Rotar disco al inicio de cada grupo (excepto el primero)
            if (i > 0 && i % config.getTamanoGrupo() == 0) {
                config.rotar();
            }

            char original = texto.charAt(i);
            resultado.append(procesarCaracter(original, config, cifrar));
        }

        return resultado.toString();
    }


    private static char procesarCaracter(char c, Configuracion config, boolean cifrar) {

        if (!Character.isLetter(c)) {
            return c;
        }

        boolean esMinuscula = Character.isLowerCase(c);
        char letra = Character.toUpperCase(c);
        char resultado;

        if (cifrar) {
            int pos = config.getDiscoExterior().indexOf(letra);
            if (pos == -1) {
                return c; 
            }
            int nuevaPos = (pos + config.getDesplazamiento()) % config.getDiscoInterior().length();
            resultado = config.getDiscoInterior().charAt(nuevaPos);
        } else {

            int pos = config.getDiscoInterior().indexOf(letra);
            if (pos == -1) {
                return c; 
            }
            int nuevaPos = (pos - config.getDesplazamiento() + config.getDiscoExterior().length())
                    % config.getDiscoExterior().length();
            resultado = config.getDiscoExterior().charAt(nuevaPos);
        }

        return esMinuscula ? Character.toLowerCase(resultado) : resultado;
    }

    public static Configuracion parsearClave(String clave, String discoExt, String discoInt) {
        if (clave == null || clave.trim().isEmpty()) {
            throw new IllegalArgumentException("La clave no puede estar vacía");
        }

        String[] partes = clave.trim().split("\\s*,\\s*");
        if (partes.length != 3) {
            throw new IllegalArgumentException("Formato de clave inválido. Debe ser: letras,tamañoGrupo,rotaciónDirección");
        }


        String letras = partes[0].trim();
        if (letras.length() != 2) {
            throw new IllegalArgumentException("Las letras clave deben ser exactamente 2 caracteres");
        }

        char letraExt = Character.toUpperCase(letras.charAt(0));
        char letraInt = letras.charAt(1);

        if (discoExt.indexOf(letraExt) == -1) {
            throw new IllegalArgumentException("Letra exterior '" + letraExt + "' no encontrada en el disco exterior");
        }

        if (discoInt.indexOf(Character.toUpperCase(letraInt)) == -1
                && discoInt.indexOf(Character.toLowerCase(letraInt)) == -1) {
            throw new IllegalArgumentException("Letra interior '" + letraInt + "' no encontrada en el disco interior");
        }

 
        int tamanoGrupo;
        try {
            tamanoGrupo = Integer.parseInt(partes[1].trim());
            if (tamanoGrupo <= 0) {
                throw new IllegalArgumentException("El tamaño de grupo debe ser mayor que cero");
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Tamaño de grupo inválido: " + partes[1]);
        }


        String rotacionDir = partes[2].trim().toLowerCase();
        if (rotacionDir.length() < 2) {
            throw new IllegalArgumentException("Formato de rotación inválido. Debe ser: número + dirección (d/i)");
        }

        int rotacion;
        try {
            rotacion = Integer.parseInt(rotacionDir.substring(0, rotacionDir.length() - 1));
            if (rotacion <= 0) {
                throw new IllegalArgumentException("La rotación debe ser mayor que cero");
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Rotación inválida: " + rotacionDir.substring(0, rotacionDir.length() - 1));
        }

        char direccion = rotacionDir.charAt(rotacionDir.length() - 1);
        if (direccion != 'd' && direccion != 'i') {
            throw new IllegalArgumentException("Dirección de rotación inválida. Use 'd' (derecha) o 'i' (izquierda)");
        }

        int posExt = discoExt.indexOf(letraExt);
        int posInt = discoInt.indexOf(letraInt);
        int desplazamientoInicial = (posInt - posExt + discoExt.length()) % discoExt.length();

        return new Configuracion(discoExt, discoInt, desplazamientoInicial, tamanoGrupo, rotacion, direccion);
    }


    public static String getDiscoExterior(boolean esEspanol) {
        return esEspanol ? DISCO_EXTERIOR_ESP : DISCO_EXTERIOR_ENG;
    }


    public static String getDiscoInterior(boolean esEspanol) {
        return esEspanol ? DISCO_INTERIOR_ESP : DISCO_INTERIOR_ENG;
    }

    public static int getModuloAlfabeto(boolean esEspanol) {
        return esEspanol ? DISCO_EXTERIOR_ESP.length() : DISCO_EXTERIOR_ENG.length();
    }
}
