# FitMaxxer Caching Guide: Backend & Frontend Integration

This document outlines the implementation of caching for the FitMaxxer application using both **Backend Caching** (Spring Cache with Caffeine) for external API calls and **Frontend/Client-side Caching** for user search history and favorites.

---

## 1. Architectural Overview & Why We Cache

When building an application that integrates with external third-party APIs (such as Open Food Facts) and handles frequent user interactions, making a fresh HTTP request or repetitive manual lookups introduces several bottlenecks:
- **Rate Limits:** External providers often throttle or block applications that exceed request limits.
- **Network Latency:** Waiting for an external HTTP round-trip on every user query makes the interface feel sluggish.
- **Redundant Processing:** Users frequently search for common items (e.g., "Oatmeal", "Banana", "Chicken Breast").

To solve this, we introduce a dual caching strategy:
1. **Backend Cache (Caffeine):** Manages server-side memory for external API results, protecting rate limits and speeding up responses across all users.
2. **Frontend/Client Cache (CLI Memory / Local State):** Manages local user history and favorite items to provide instant recall without hitting the backend server.

---

## 2. What Was Added (Implementation Steps)

To enable robust caching across the stack, the following components were introduced:

1. **Backend Dependencies (`build.gradle`):** Added `spring-boot-starter-cache` and the high-performance `caffeine` caching library.
2. **Backend Configuration (`@EnableCaching`):** Enabled Spring's caching infrastructure on the main application class.
3. **Application Properties (`application.properties`):** Configured cache bounds, names, and expiration policies using standard Java properties format.
4. **Service Annotation (`@Cacheable`):** Annotated the catalog search service method to automatically store and retrieve query results from server memory.
5. **Client-Side History & Favorites:** Implemented local state structures in the CLI/client layer to keep track of recent user searches.

---

## 3. Technical Implementation & Code Examples

### Step 3.1: Add Dependencies
In the `build.gradle` file:
```groovy
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-cache'
    implementation 'com.github.ben-manes.caffeine:caffeine'
}

```

### Step 3.2: Enable Caching in the Application:

```java
package com.pedropacheco.nutrition.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class BackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(BackendApplication.class, args);
    }
}
```

### Step 3.3: Configure Cache Policies (`application.properties`)
Instead of YAML, configure cache limits and time-to-live settings directly in your `src/main/resources/application.properties` file to keep memory usage optimal:

```properties
# Spring Cache Configuration
spring.cache.cache-names=foodSearchCache
spring.cache.caffeine.spec=maximumSize=500,expireAfterWrite=24h
```

### Step 3.4: Apply `@Cacheable` to the relevant Services
Annotate your catalog service method so Spring intercepts calls and checks memory before hitting the external API:

```java
package com.pedropacheco.nutrition.backend.userNutritionalProfile.service;

import com.pedropacheco.nutrition.backend.userNutritionalProfile.domain.FoodItem;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class OpenFoodFactsService {

    private final OpenFoodFactsClient openFoodFactsClient;

    public OpenFoodFactsService(OpenFoodFactsClient openFoodFactsClient) {
        this.openFoodFactsClient = openFoodFactsClient;
    }

    @Cacheable(value = "foodSearchCache", key = "#query.toLowerCase().trim()")
    public List<FoodItem> searchFood(String query) {
        System.out.println("[CACHE MISS] Fetching data from Open Food Facts API for query: " + query);
        return openFoodFactsClient.fetchFoodsFromApi(query);
    }
}
```


## 4. Frontend & Client-Side Caching

While backend caching saves server resources and external API calls, **client-side caching** focuses on user experience. In a terminal CLI or frontend application, maintaining a local session cache or recent search history avoids redundant inputs.

### Conceptual Implementation in the CLI Client:
```java
import java.util.ArrayList;
import java.util.List;

public class ClientSearchCache {
    private final List<String> recentSearches = new ArrayList<>();
    private static final int MAX_HISTORY = 5;

    public void addSearch(String query) {
        recentSearches.remove(query); // Remove if exists to push to top
        recentSearches.add(0, query);
        if (recentSearches.size() > MAX_HISTORY) {
            recentSearches.remove(recentSearches.size() - 1);
        }
    }

    public List<String> getRecentSearches() {
        return recentSearches;
    }
}
```
This ensures that the user can instantly view or re-select their last few queries without re-typing them, operating entirely locally on the client interface.

## 5. Situational Example: The "Banana" Scenario

To understand how this operates in practice, consider the following real-world scenario involving backend and frontend caching:

1. **User A searches for "Banana":**
    - **Client Action:** Checks local recent history (empty). Sends request to backend.
    - **Backend Action:** Spring checks the `foodSearchCache` for the key `"banana"`. Because no entry exists yet, a **Cache Miss** occurs.
    - **Execution:** The application invokes `OpenFoodFactsService.searchFood("Banana")`, performs an HTTP request to the Open Food Facts API, maps the JSON response into `FoodItem` domain entities, and returns the list to User A.
    - **Caching:** Automatically, Spring stores the returned list in server memory under the key `"banana"`, and the client adds `"Banana"` to its local recent searches history.

2. **User B (or User A again) searches for "Banana" (or `"BANANA "`, which normalizes via `#query.toLowerCase().trim()`) 10 minutes later:**
    - **Backend Action:** Spring checks the `foodSearchCache` for the key `"banana"`. Since it exists and hasn't expired, a **Cache Hit** occurs.
    - **Execution:** The external HTTP call is **bypassed entirely**. The cached list of `FoodItem` objects is returned instantly from server memory.
    - **Result:** Zero network latency, zero consumption of Open Food Facts rate limits, and an instantaneous response for the user.