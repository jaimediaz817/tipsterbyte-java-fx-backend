// ─────────────────────────────────────────────
// [QUÉ]: Servicio de dominio que parsea las fechas de Wplay ("15 Ago 2026", "14:30")
//        y las convierte a UTC, infiriendo el año cuando no viene explícito.
// [POR QUÉ]: Wplay usa formato localizado (mes abreviado en español) y la zona
//            horaria del scraper es America/Bogota (-05:00, sin DST). El dominio
//            necesita Instant UTC para comparar y persistir. La inferencia de año
//            maneja el rollover dic→ene (si el mes es dic y estamos en ene, usa
//            el año siguiente).
// [ALTERNATIVAS]: Lógica inline en el adapter; se descarta porque dificulta el
//                 test unitario y mezcla parsing con HTTP.
// [RELACIONES]: HU-14 AC4.1 → WplayCuotasAdapter + SincronizarCuotasUseCase.
// ─────────────────────────────────────────────
package com.tipsterbyte.tipsterbytefxv2.domain.service;

import com.tipsterbyte.tipsterbytefxv2.domain.DomainException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Map;

public final class ParserFechaWplay {

    // [QUÉ]: Zona horaria del scraper Python (configurada en el contenedor).
    public static final ZoneId ZONA_BOGOTA = ZoneId.of("America/Bogota");

    private static final Map<String, Integer> MESES_ES = Map.ofEntries(
            Map.entry("ene", 1), Map.entry("feb", 2), Map.entry("mar", 3),
            Map.entry("abr", 4), Map.entry("may", 5), Map.entry("jun", 6),
            Map.entry("jul", 7), Map.entry("ago", 8), Map.entry("sep", 9),
            Map.entry("oct", 10), Map.entry("nov", 11), Map.entry("dic", 12));

    // [QUÉ]: Parsea date_match ("15 Ago 2026" o "15 Ago") + time_match ("14:30" o null)
    //        y devuelve un Instant en UTC.
    // [POR QUÉ]: El año puede venir explícito o no; si no viene, se infiere del año
    //            actual con rollover dic→ene. La hora viene en zona Bogota.
    public static Instant parsear(String dateMatch, String timeMatch) {
        if (dateMatch == null || dateMatch.isBlank()) {
            throw new DomainException("date_match no puede ser nulo o vacío");
        }
        String[] partes = dateMatch.trim().split("\\s+");
        if (partes.length < 2 || partes.length > 3) {
            throw new DomainException("Formato de date_match inválido: " + dateMatch);
        }

        int dia = Integer.parseInt(partes[0]);
        int mes = MESES_ES.getOrDefault(partes[1].toLowerCase(Locale.ROOT), -1);
        if (mes == -1) {
            throw new DomainException("Mes inválido en date_match: " + partes[1]);
        }

        int anio;
        if (partes.length == 3) {
            anio = Integer.parseInt(partes[2]);
        } else {
            // Inferir año: mes actual. Rollover dic→ene.
            int mesActual = LocalDate.now(ZONA_BOGOTA).getMonthValue();
            anio = LocalDate.now(ZONA_BOGOTA).getYear();
            if (mes == 12 && mesActual == 1) {
                anio += 1;
            } else if (mes == 1 && mesActual == 12) {
                anio -= 1;
            }
        }

        LocalDate fecha = LocalDate.of(anio, mes, dia);
        LocalTime hora;
        if (timeMatch != null && !timeMatch.isBlank()) {
            try {
                hora = LocalTime.parse(timeMatch.trim());
            } catch (DateTimeParseException e) {
                throw new DomainException("Hora inválida en time_match: " + timeMatch);
            }
        } else {
            hora = LocalTime.MIDNIGHT;
        }

        LocalDateTime fechaHoraLocal = LocalDateTime.of(fecha, hora);
        ZonedDateTime zdt = fechaHoraLocal.atZone(ZONA_BOGOTA);
        return zdt.toInstant();
    }
}
