package com.tpi.paseadores.shared.config;

import java.io.IOException;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

// Solo aplica al deploy de un solo jar (ver Dockerfile), donde el backend también
// sirve el bundle del front. React Router rutea del lado del cliente; el servidor
// solo necesita devolver index.html para una recarga o un link directo a una ruta
// de la SPA (ej. /walks/{id}) en vez de 404. /api/** nunca llega hasta acá: lo
// resuelven antes los @RestController, que tienen prioridad sobre el resource handler.
@Configuration
public class SpaForwardingConfig implements WebMvcConfigurer {
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        Resource requested = location.createRelative(resourcePath);
                        return requested.exists() && requested.isReadable()
                                ? requested
                                : new ClassPathResource("/static/index.html");
                    }
                });
    }
}
