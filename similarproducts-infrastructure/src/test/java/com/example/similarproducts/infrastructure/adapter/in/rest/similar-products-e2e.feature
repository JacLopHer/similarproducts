@e2e
Feature: Similar Products API - Test de Flujo Principal

  Background:
    * def baseUrl = 'http://localhost:5000'
    * url baseUrl

  Scenario: Obtener productos similares exitosamente (producto con similares)
    Given path '/v1/product/1/similar'
    When method GET
    Then status 200
    And match response == '#[]'
    And match response[0] == { id: '#(^^)', name: '#string', price: '#number', availability: '#boolean' }

  Scenario: Obtener productos similares cuando no hay (lista vacía)
    Given path '/v1/product/2/similar'
    When method GET
    Then status 200
    And match response == '[]'

  Scenario: Producto no encontrado
    Given path '/v1/product/999/similar'
    When method GET
    Then status 404
    And match response == { error: '#string', message: '#string', timestamp: '#string' }

  Scenario: ProductId inválido (vacío)
    Given path '/v1/product//similar'
    When method GET
    Then status 404

  Scenario: ProductId muy largo (validación)
    Given path '/v1/product/abc123XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX/similar'
    When method GET
    Then status 400
    And match response == { error: '#string', message: '#string', timestamp: '#string' }

