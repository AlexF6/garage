package io.github.alexf6.garage.config;

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
