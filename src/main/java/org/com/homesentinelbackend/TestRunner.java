package org.com.homesentinelbackend;

import org.com.homesentinelbackend.models.LoginSession;
import org.com.homesentinelbackend.models.auth.VerisurePhone;
import org.com.homesentinelbackend.services.VerisureService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.util.List;
import java.util.Scanner;

import static java.lang.System.*;

@Component
public class TestRunner implements CommandLineRunner {

    private final VerisureService verisureService;

    @Value("${installation.numinst}")
    private String numinst;

    public TestRunner(VerisureService verisureService) {
        this.verisureService = verisureService;
    }

    @Override
    public void run(String... args) {
        out.println("\n================================================================================");
        out.println(">>> INICIANDO FLUJO INTERACTIVO VERISURE GRAPHQL (JAVA 21)");
        out.println("================================================================================\n");

        try (Scanner scanner = new Scanner(in)) {

            LoginSession session = verisureService.createSession();
            out.println("[INFO] Sesión inicializada con ID: " + session.getId());

            out.println("\n--------------------------------------------------------------------------------");
            out.println("--> PASO 1: Ejecutando mkLoginToken (Login inicial)...");
            out.println("--------------------------------------------------------------------------------");
            JsonNode resStep1 = verisureService.step1Login(session);
            out.println(resStep1.toPrettyString());

            boolean mfaRequired = verisureService.isMfaRequired(resStep1);

            if (mfaRequired) {
                out.println("\n[!] Verisure requiere autorización de dispositivo / MFA.");

                out.println("\n--------------------------------------------------------------------------------");
                out.println("--> PASO 2: Solicitando desafío OTP y lista de teléfonos...");
                out.println("--------------------------------------------------------------------------------");
                JsonNode resStep2 = verisureService.step2RequestDeviceValidation(session);

                List<VerisurePhone> phones = session.getAvailablePhones();
                if (phones.isEmpty()) {
                    phones = verisureService.extractAvailablePhones(resStep2);
                }

                if (phones.isEmpty()) {
                    err.println("[ERROR] No se detectaron teléfonos disponibles en la respuesta de Verisure.");
                    return;
                }

                out.println("\n================================================================================");
                out.println(">>> SELECCIÓN DE DISPOSITIVO PARA RECIBIR SMS OTP");
                out.println("================================================================================");
                for (VerisurePhone phone : phones) {
                    out.printf(" [%d] Teléfono: %s (ID: %d)%n", phone.id(), phone.phone(), phone.id());
                }
                out.println("--------------------------------------------------------------------------------");
                out.print("Introduce el ID del dispositivo que prefieras: ");

                int selectedId = -1;
                while (selectedId < 0) {
                    String line = scanner.nextLine().trim();
                    try {
                        int parsed = Integer.parseInt(line);
                        boolean exists = phones.stream().anyMatch(p -> p.id() == parsed);
                        if (exists) {
                            selectedId = parsed;
                        } else {
                            out.print("ID no válido. Selecciona uno de los mostrados arriba: ");
                        }
                    } catch (NumberFormatException e) {
                        out.print("Por favor, introduce un número válido: ");
                    }
                }

                out.println("\n--------------------------------------------------------------------------------");
                out.printf("--> PASO 3: Enviando SMS al dispositivo ID %d...%n", selectedId);
                out.println("--------------------------------------------------------------------------------");
                JsonNode resStep3 = verisureService.step3SendOtpSms(session, selectedId);
                out.println(resStep3.toPrettyString());

                out.println("\n--------------------------------------------------------------------------------");
                out.println("--> PASO 4: Validación de código SMS OTP");
                out.println("--------------------------------------------------------------------------------");
                out.print("Introduce el código de verificación recibido por SMS en tu móvil: ");
                String otpCode = scanner.nextLine().trim();

                while (otpCode.isEmpty()) {
                    out.print("El código no puede estar vacío. Introdúcelo de nuevo: ");
                    otpCode = scanner.nextLine().trim();
                }

                out.println("Validando código contra Verisure...");
                JsonNode resStep4 = verisureService.step4ValidateOtp(session, otpCode);
                out.println(resStep4.toPrettyString());
            }

            if (session.isAuthenticated()) {
                out.println("\n--------------------------------------------------------------------------------");
                out.printf("--> PASO 5: Sesión autenticada. Consultando estado de la alarma (numinst: %s)...%n", numinst);
                out.println("--------------------------------------------------------------------------------");
                JsonNode resStep5 = verisureService.step5GetInstallationStatus(session, numinst);
                out.println(resStep5.toPrettyString());

                out.println("\n================================================================================");
                out.println(">>> FLUJO COMPLETO DE LOGIN Y CONSULTA DE ALARMA FINALIZADO CON ÉXITO");
                out.println("================================================================================\n");
            } else {
                err.println("\n[AVISO] La sesión no quedó autenticada tras el proceso.");
            }

        } catch (Exception e) {
            err.println("\n[ERROR] Ocurrió un fallo durante la ejecución: " + e.getMessage());
            e.printStackTrace();
        } finally {
            exit(0);
        }
    }
}
