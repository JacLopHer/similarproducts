function fn() {
    var config = {
        baseUrl: 'http://localhost:5000',
        mockServiceUrl: 'http://localhost:3001'
    };

    // Permitir configurar URLs mediante variables de entorno (útil en CI/CD)
    if (java.lang.System.getenv('APP_URL')) {
        config.baseUrl = java.lang.System.getenv('APP_URL');
    }
    if (java.lang.System.getenv('MOCK_SERVICE_URL')) {
        config.mockServiceUrl = java.lang.System.getenv('MOCK_SERVICE_URL');
    }

    karate.log('Base URL: ' + config.baseUrl);
    karate.log('Mock Service URL: ' + config.mockServiceUrl);

    return config;
}

