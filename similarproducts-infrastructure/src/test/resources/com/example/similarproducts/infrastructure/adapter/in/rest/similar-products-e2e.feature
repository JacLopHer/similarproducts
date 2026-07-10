@e2e
Feature: Similar Products API - Test de Flujo Principal

  Background:
    * def baseUrl = 'http://localhost:5000'
    * url baseUrl

  Scenario: Obtener productos similares exitosamente
    Given path '/v1/product/1/similar'
    When method GET
    Then status 200
    # Validar que es un array de exactamente 3 productos
    And match response == '#[3]'
    # Validar estructura de cada elemento
    And match response[0] == { id: '#string', name: '#string', price: '#number', availability: '#boolean' }
    And match response[1] == { id: '#string', name: '#string', price: '#number', availability: '#boolean' }
    And match response[2] == { id: '#string', name: '#string', price: '#number', availability: '#boolean' }
    # Validar primer producto (similares: [2,3,4])
    And match response[0].id == '2'
    And match response[0].name == 'Dress'
    And response[0].price > 0
    # Validar segundo producto
    And match response[1].id == '3'
    And match response[1].name == 'Blazer'
    And response[1].price > 0
    # Validar tercero producto
    And match response[2].id == '4'
    And match response[2].name == 'Boots'
    And response[2].price > 0

  Scenario: Obtener productos similares - lista con varios items
    Given path '/v1/product/2/similar'
    When method GET
    Then status 200
    # Validar que es un array de exactamente 3 productos
    And match response == '#[3]'
    # Validar primer producto
    And match response[0].id == '3'
    And match response[0].name == 'Blazer'
    # Validar segundo producto
    And match response[1].id == '100'
    And match response[1].name == 'Trousers'
    # Validar tercero producto
    And match response[2].id == '1000'
    And match response[2].name == 'Coat'

  Scenario: Producto no encontrado retorna 404
    Given path '/v1/product/999/similar'
    When method GET
    Then status 404
    And match response == { error: '#string', message: '#string', timestamp: '#string', path: '#string', status: '#number' }
    And match response.error == 'PRODUCT_NOT_FOUND'
    And match response.status == 404

  Scenario: ProductId muy largo - validación de tamaño
    Given path '/v1/product/abc123XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX/similar'
    When method GET
    Then status 400
    And match response == { error: '#string', message: '#string', timestamp: '#string', path: '#string', status: '#number' }
    And match response.error == 'VALIDATION_ERROR'
    And match response.status == 400



