package com.example.similarproducts.infrastructure.adapter.in.rest;

import com.intuit.karate.junit5.Karate;

/**
 * Runner para tests E2E con Karate.
 * Ejecuta todos los archivos .feature en el mismo directorio y subdirectorios.
 */
class SimilarProductsE2ERunner {

    @Karate.Test
    Karate testAll() {
        return Karate.run("similar-products-e2e").relativeTo(getClass());
    }

}

