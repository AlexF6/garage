package io.github.alexf6.garage.config;

import io.github.alexf6.garage.GarageMod;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Operaciones de archivo comunes a las configs del mod: copia de seguridad y escritura atómica. */
final class ConfigFiles {

    /**
     * Renombra {@code file} a {@code file.bak} (sustituyendo una copia anterior).
     *
     * @return la copia, o {@code null} si no se pudo hacer; entonces no se debe sobrescribir el original
     */
    static Path backUp(Path file) {
        Path backup = file.resolveSibling(file.getFileName() + ".bak");
        try {
            Files.move(file, backup, StandardCopyOption.REPLACE_EXISTING);
            return backup;
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Archivo ilegible como JSON: se guarda como {@code .bak} y se avisa. El llamador regenera el archivo solo si
     * esto devuelve {@code true}; sin copia de seguridad no se sobrescribe el original.
     */
    static boolean backUpCorrupt(Path file, Exception cause) {
        Path backup = backUp(file);
        if (backup == null) {
            GarageMod.LOGGER.warn("{} no es válido ({}) y no se pudo guardar una copia; se usan los valores por defecto",
                    file, cause.getMessage());
            return false;
        }
        GarageMod.LOGGER.warn("{} no es válido ({}); se guardó como {} y se creó uno nuevo con los valores por defecto",
                file, cause.getMessage(), backup);
        return true;
    }

    /** Escribe de forma atómica y avisa si falla en vez de lanzar: la config nunca debe tumbar el juego. */
    static boolean write(Path file, String text) {
        try {
            writeAtomic(file, text);
            return true;
        } catch (IOException e) {
            GarageMod.LOGGER.warn("No se pudo escribir {}: {}", file, e.toString());
            return false;
        }
    }

    /** Escribe en un archivo temporal y lo mueve de golpe, para que un corte no deje la config a medias. */
    static void writeAtomic(Path file, String text) throws IOException {
        Path temp = file.resolveSibling(file.getFileName() + ".tmp");
        Files.writeString(temp, text);
        try {
            Files.move(temp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private ConfigFiles() {}
}
