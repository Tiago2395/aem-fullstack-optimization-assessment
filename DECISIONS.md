1. Configuration Strategy: Tenant-Aware CAConfig
Instead of using hardcoded values or standard JCR properties, I implemented Context-Aware Configurations (CAConfig).

Decision: Use WeatherTenantConfig to allow different settings (API endpoints/keys) per site branch (e.g., /content/assessment/us vs /content/assessment/es).

Reasoning: Enterprise AEM environments require multi-tenant support. CAConfig allows the component to behave differently depending on where it is placed in the content hierarchy without changing the code.

Fallback: The service implements a "Global-to-Local" hierarchy, falling back to OSGi global configurations if no tenant-specific config is found.

2. Performance: Backend Caching Strategy
To satisfy performance requirements and minimize external API calls, a caching layer was introduced directly into the OSGi service.

Decision: Implementation of Guava Cache with a "Self-healing" approach.

Configuration: * TTL (Time To Live): 15 minutes by default, but fully configurable via OSGi.

Eviction Policy: Based on size (100 entries) and time to prevent memory exhaustion.

Reasoning: Weather data is relatively static. Caching at the service level reduces page load time and protects the system from external API rate limiting.

3. Frontend: Logic Decoupling (HTL + Sling Models)
Following AEM's "Separation of Concerns" principle, the frontend was completely refactored.

Decision: Removal of all inline JavaScript and business logic from the HTL files.

Implementation: A backend Sling Model (WeatherModel) acts as the Data Provider. It manages the interaction with the WeatherService and performs data sanitization.

HTL: The template was reduced to a purely presentational layer, consuming clean POJOs from the Sling Model. This improves maintainability and security (XSS protection).

4. Backend Resilience & Clean Code
The WeatherServiceImpl was refactored to improve maintainability and error handling.

Resilience: Added connection and read timeouts to prevent the AEM thread pool from being exhausted by slow external responses.

Clean Code: Encapsulated HTTP execution into dedicated private methods and used Gson for robust JSON to model mapping.

5. Template Architecture & Authoring
Decision: Refactored the Editable Template structure to ensure a standard enterprise authoring experience.

Fix: Implemented correct policy mapping within the /conf hierarchy. This ensures the component is correctly registered in the Content Tree and allows authors to use the drag-and-drop functionality through a properly configured responsive grid.

6. Assumptions
External API: The solution assumes the goweather.xyz API (or a compatible substitute) is reachable.

Dependencies: The solution relies on Google Guava and Gson, which are standard in AEM environments.