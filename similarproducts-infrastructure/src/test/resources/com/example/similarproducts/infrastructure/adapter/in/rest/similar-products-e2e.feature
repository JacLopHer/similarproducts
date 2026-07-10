@e2e
Feature: Similar Products API - Test de Flujo Principal

  Background:
    * def baseUrl = 'http://localhost:5000'
    * url baseUrl

  Scenario: Obtener productos similares exitosamente (producto con similares)
    Given path '/v1/product/1/similar'
    When method GET
    Then status 200
    # Validar array no vacío con exactamente 2 productos
    And match response == '#[]'
    And match response.length == 2
    # Validar estructura de cada elemento
    And match response[0] == { id: '#string', name: '#string', price: '#number', availability: '#boolean' }
    And match response[1] == { id: '#string', name: '#string', price: '#number', availability: '#boolean' }
    # Validar orden coincide con similarIds retornado (2, 3)
    And match response[0].id == '2'
    And match response[0].name == 'Dress'
    And match response[0].price == 19.99
    And match response[0].availability == true
    # Segundo producto
    And match response[1].id == '3'
    And match response[1].name == 'Blazer'
    And match response[1].price == 29.99
    And match response[1].availability == false
    # Validar que los valores son válidos (price > 0)
    And response[0].price > 0
    And response[1].price > 0
    # Validar sin duplicados
    And response[0].id != response[1].id

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


