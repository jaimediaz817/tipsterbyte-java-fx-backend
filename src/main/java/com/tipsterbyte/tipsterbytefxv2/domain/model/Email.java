// ─────────────────────────────────────────────
// [QUÉ]: Value object que representa un correo electrónico de usuario.
// [POR QUÉ]: Un email es un concepto con formato validable y repetible (tipsters
//            y clientes lo usan). Como VO, se valida una sola vez en el constructor.
// [ALTERNATIVAS]: String suelto; se descarta porque permitiría emails inválidos
//                 esparciendo la validación por todo el sistema.
// [RELACIONES]: Usado por entities Tipster y Cliente.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.domain.model;

import com.tipsterbyte.tipsterbytefxv2.domain.DomainException;

import java.util.regex.Pattern;

public record Email(String direccion) {

    private static final Pattern PATRON_EMAIL = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    // [QUÉ]: Compact constructor que valida el formato básico de un email.
    // [POR QUÉ]: Evita correos malformados desde el origen (validación defensiva).
    public Email {
        if (direccion == null || !PATRON_EMAIL.matcher(direccion).matches()) {
            throw new DomainException("Email con formato inválido");
        }
    }
}