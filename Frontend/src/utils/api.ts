const API_BASE_URL = 'http://localhost:3000';

// Request cache
const requestCache = new Map<string, { data: any; timestamp: number }>();
const CACHE_TTL = 30000; // 30 seconds

// Abort controllers for request cancellation
const abortControllers = new Map<string, AbortController>();

interface RequestOptions extends RequestInit {
  skipCache?: boolean;
  cacheKey?: string;
}

/**
 * Centralized API client with caching and request cancellation
 */
export async function apiRequest<T>(
  endpoint: string,
  options: RequestOptions = {}
): Promise<T> {
  const { skipCache = false, cacheKey, ...fetchOptions } = options;
  const url = `${API_BASE_URL}${endpoint}`;
  
  // Create a more unique key that includes auth token hash to avoid conflicts
  // For GET requests, use cache key if provided, otherwise create one
  // For POST/PUT/DELETE, always use a unique key to avoid cancellation
  const authHeader = fetchOptions.headers?.['Authorization'] as string || '';
  const authHash = authHeader ? authHeader.substring(0, 20) : 'no-auth';
  const method = fetchOptions.method || 'GET';
  
  // For GET requests, allow sharing cache key
  // For other methods, make key unique to prevent cancellation
  // If cacheKey is provided, use it directly (caller knows what they're doing)
  const key = cacheKey || (method === 'GET' 
    ? `${method}:${endpoint}:${authHash}` 
    : `${method}:${endpoint}:${authHash}:${Date.now()}-${Math.random()}`);

  // Check cache first (only for GET requests) - do this before creating controller
  if (!skipCache && method === 'GET') {
    const cached = requestCache.get(key);
    if (cached && Date.now() - cached.timestamp < CACHE_TTL) {
      return cached.data as T;
    }
  }

  // For GET requests, allow concurrent requests - they're idempotent
  // Only cancel if it's a POST/PUT/DELETE with the exact same key (shouldn't happen)
  // Don't store abort controllers for GET requests to avoid conflicts
  const controller = new AbortController();
  
  // Only track abort controllers for non-GET requests or when using explicit cache key
  // This prevents GET requests from cancelling each other
  if (method !== 'GET' || cacheKey) {
    const existingController = abortControllers.get(key);
    if (existingController) {
      // Only abort if it's a non-GET request or explicit cache key (intentional duplicate)
      if (method !== 'GET' || cacheKey) {
        existingController.abort();
      }
    }
    abortControllers.set(key, controller);
  }

  try {
    const response = await fetch(url, {
      ...fetchOptions,
      signal: controller.signal,
      headers: {
        'Content-Type': 'application/json',
        ...fetchOptions.headers,
      },
    });

    // Remove abort controller on completion (if it was stored)
    if (method !== 'GET' || cacheKey) {
      abortControllers.delete(key);
    }

    if (!response.ok) {
      const errorData = await response.json().catch(() => ({}));
      throw new Error(errorData.detail || errorData.message || `HTTP ${response.status}`);
    }

    const data = await response.json();

    // Cache GET requests
    if (!skipCache && method === 'GET') {
      requestCache.set(key, { data, timestamp: Date.now() });
    }

    return data as T;
  } catch (error: any) {
    // Remove abort controller on error (if it was stored)
    if (method !== 'GET' || cacheKey) {
      abortControllers.delete(key);
    }
    
    // Handle abort error more gracefully
    if (error.name === 'AbortError' || error.message === 'The user aborted a request.') {
      // For aborted GET requests, check if we can return cached data
      if (method === 'GET' && !skipCache) {
        // Try to get cached data without the timestamp/random part
        const baseKey = key.split(':').slice(0, -1).join(':'); // Remove last part
        const cached = requestCache.get(baseKey);
        if (cached) {
          return cached.data as T;
        }
        // Try the original key
        const originalCached = requestCache.get(key);
        if (originalCached) {
          return originalCached.data as T;
        }
      }
      // Only throw error if we can't use cache
      throw new Error('Request cancelled');
    }
    
    throw error;
  }
}

/**
 * Get request with authentication
 */
export async function apiGet<T>(
  endpoint: string,
  token: string | null,
  options: RequestOptions = {}
): Promise<T> {
  return apiRequest<T>(endpoint, {
    ...options,
    method: 'GET',
    headers: {
      ...options.headers,
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
  });
}

/**
 * POST request with authentication
 */
export async function apiPost<T>(
  endpoint: string,
  token: string | null,
  data?: any,
  options: RequestOptions = {}
): Promise<T> {
  return apiRequest<T>(endpoint, {
    ...options,
    method: 'POST',
    headers: {
      ...options.headers,
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: data ? JSON.stringify(data) : undefined,
  });
}

/**
 * PUT request with authentication
 */
export async function apiPut<T>(
  endpoint: string,
  token: string | null,
  data?: any,
  options: RequestOptions = {}
): Promise<T> {
  return apiRequest<T>(endpoint, {
    ...options,
    method: 'PUT',
    headers: {
      ...options.headers,
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: data ? JSON.stringify(data) : undefined,
  });
}

/**
 * PATCH request with authentication
 */
export async function apiPatch<T>(
  endpoint: string,
  token: string | null,
  data?: any,
  options: RequestOptions = {}
): Promise<T> {
  return apiRequest<T>(endpoint, {
    ...options,
    method: 'PATCH',
    headers: {
      ...options.headers,
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: data ? JSON.stringify(data) : undefined,
  });
}

/**
 * DELETE request with authentication
 */
export async function apiDelete<T>(
  endpoint: string,
  token: string | null,
  options: RequestOptions = {}
): Promise<T> {
  return apiRequest<T>(endpoint, {
    ...options,
    method: 'DELETE',
    headers: {
      ...options.headers,
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
  });
}

/**
 * Clear request cache
 */
export function clearCache(pattern?: string): void {
  if (pattern) {
    for (const key of requestCache.keys()) {
      if (key.includes(pattern)) {
        requestCache.delete(key);
      }
    }
  } else {
    requestCache.clear();
  }
}

/**
 * Cancel all pending requests
 */
export function cancelAllRequests(): void {
  for (const controller of abortControllers.values()) {
    controller.abort();
  }
  abortControllers.clear();
}

