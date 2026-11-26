package com.vortise.gestion.infrastructure.web;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vortise.gestion.controller.authentication.InvitacionController;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Default-deny for new endpoints: every write endpoint declares who may call it, unless it is one of the
 * deliberate exceptions below. Reads carry no role guard. Context-free: scans the classpath only.
 */
class WriteEndpointsGuardTest {
    // Authenticated by something other than a role: credentials, a signed link or a signature.
    private static final Set<String> PUBLIC_OR_SELF = Set.of(
        "AuthController", "PasswordResetController", "InvitacionPublicaController", "StripeWebhookController");

    private static boolean isWrite(Method method) {
        return AnnotatedElementUtils.hasAnnotation(method, PostMapping.class)
            || AnnotatedElementUtils.hasAnnotation(method, PutMapping.class)
            || AnnotatedElementUtils.hasAnnotation(method, PatchMapping.class)
            || AnnotatedElementUtils.hasAnnotation(method, DeleteMapping.class);
    }

    @Test
    void everyWriteEndpointDeclaresItsRoles() throws Exception {
        var scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));
        List<String> unguarded = new ArrayList<>();
        int controllers = 0;
        for (BeanDefinition definition : scanner.findCandidateComponents("com.vortise.gestion.controller")) {
            Class<?> controller = Class.forName(definition.getBeanClassName());
            controllers++;
            if (PUBLIC_OR_SELF.contains(controller.getSimpleName())) continue;
            boolean classGuard = AnnotatedElementUtils.hasAnnotation(controller, PreAuthorize.class);
            for (Method method : controller.getDeclaredMethods()) {
                // The existing public invitation endpoint validates its invitation code before accepting it.
                if (controller == InvitacionController.class
                        && method.getName().equals("aceptarInvitacion")) continue;
                if (isWrite(method) && !classGuard && !AnnotatedElementUtils.hasAnnotation(method, PreAuthorize.class)) {
                    unguarded.add(controller.getSimpleName() + "." + method.getName());
                }
            }
        }
        assertTrue(controllers > 10, "The scan should find the controllers");
        assertTrue(unguarded.isEmpty(), "Write endpoints without @PreAuthorize: " + unguarded);
    }
}
